package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The deck a Ghast Mount is built on: a small grid of blocks that belongs to the entity rather than
 * to the world, which is the whole of what makes it a platform saddle.
 * <p>
 * Nothing here is a real block. A block placed on the deck is a {@link Block} in this map at a cell
 * in the mount's own coordinates - x and z out to {@link #RADIUS} from the mount's centre, y up from
 * its back - and everything a player can do with it is done from that map: the renderer draws each
 * cell, {@code PlatformColliderEntity} is what a foot lands on, and breaking one hands back an item.
 * The world underneath is never touched, so the deck flies with the mount rather than being rebuilt
 * beneath it, and a mount parked over a village damages nothing.
 * <p>
 * Only the <em>block</em> is kept, never a {@code BlockState}. A cell is always the block's default
 * state, which is lossless for what may be built with - see {@link #buildable} - and means a deck
 * survives a save, a resync and a version change as a list of block names and nothing else.
 * <p>
 * The grid is deliberately small. {@link #MAX_BLOCKS} is what stops a mount being a flying fortress
 * of a thousand cells, each of which is drawn every frame and swept for every step a player takes,
 * and it is a count rather than a shape so a tower and a floor cost the same.
 */
public final class MountPlatform {
    /** How far out from the mount's centre the deck reaches. 4 is a nine-by-nine floor. */
    public static final int RADIUS = 4;

    /** How many layers may be built. The floor is 0, so a wall may be three blocks high. */
    public static final int HEIGHT = 4;

    /** How many cells there may be in total, whatever shape they are put in. */
    public static final int MAX_BLOCKS = 64;

    /**
     * How far above the mount's feet the floor layer sits. A ghast is four blocks tall, so this is
     * its back: the deck is laid on top of the creature and the rider stands on it.
     */
    public static final double FLOOR_OFFSET = 4.0;

    private static final int SPAN = RADIUS * 2 + 1;

    /** Cell key to block. Insertion-ordered so a resync and a save list the deck the same way. */
    private final Map<Integer, Block> cells = new LinkedHashMap<>();

    /** What {@link #forEach} hands out: a cell and what is in it. */
    public interface CellConsumer {
        void accept(int x, int y, int z, Block block);
    }

    /** A cell and the face of it that was looked at. */
    public record Hit(int x, int y, int z, Direction face) {}

    /* THE GRID */

    public static int pack(int x, int y, int z) {
        return ((x + RADIUS) * SPAN + (z + RADIUS)) * HEIGHT + y;
    }

    public static int unpackX(int key) {
        return key / HEIGHT / SPAN - RADIUS;
    }

    public static int unpackY(int key) {
        return key % HEIGHT;
    }

    public static int unpackZ(int key) {
        return key / HEIGHT % SPAN - RADIUS;
    }

    public static boolean inBounds(int x, int y, int z) {
        return x >= -RADIUS && x <= RADIUS && z >= -RADIUS && z <= RADIUS && y >= 0 && y < HEIGHT;
    }

    public int size() {
        return this.cells.size();
    }

    public boolean isEmpty() {
        return this.cells.isEmpty();
    }

    public boolean isFull() {
        return this.cells.size() >= MAX_BLOCKS;
    }

    @Nullable
    public Block get(int x, int y, int z) {
        return inBounds(x, y, z) ? this.cells.get(pack(x, y, z)) : null;
    }

    public boolean set(int x, int y, int z, Block block) {
        if (!inBounds(x, y, z) || isFull() || this.cells.containsKey(pack(x, y, z))) {
            return false;
        }

        this.cells.put(pack(x, y, z), block);
        return true;
    }

    @Nullable
    public Block remove(int x, int y, int z) {
        return inBounds(x, y, z) ? this.cells.remove(pack(x, y, z)) : null;
    }

    public void clear() {
        this.cells.clear();
    }

    public void forEach(CellConsumer consumer) {
        for (Map.Entry<Integer, Block> cell : this.cells.entrySet()) {
            int key = cell.getKey();
            consumer.accept(unpackX(key), unpackY(key), unpackZ(key), cell.getValue());
        }
    }

    /* WHAT MAY BE BUILT WITH */

    /**
     * The block a stack may be laid on the deck as, or null if it may not be.
     * <p>
     * Two rules, and both are about not lying to the player. A cell's collision is one cube - see
     * {@code PlatformColliderEntity} - so only blocks whose own collision is that cube are allowed,
     * which is a check on the shape rather than a list and so covers every other mod's stone for
     * free. And a cell holds a block, never a block entity: a chest on the deck would have nowhere
     * to keep its contents, so anything with one is refused rather than quietly emptied.
     */
    @Nullable
    public static Block buildable(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return null;
        }

        Block block = item.getBlock();
        if (block instanceof EntityBlock || block == Blocks.AIR) {
            return null;
        }

        BlockState state = block.defaultBlockState();
        if (state.hasBlockEntity() || !Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))) {
            return null;
        }

        return block;
    }

    /* WHERE THE CELLS ARE */

    /**
     * The corner the deck is measured from: the mount's own x and z, less the half block that
     * centres cell zero on it, at the height of the floor layer. It is not block-aligned and is not
     * meant to be - the deck belongs to the mount, and moves off it by fractions of a block.
     */
    public static Vec3 origin(Vec3 mountPosition) {
        return new Vec3(mountPosition.x - 0.5, mountPosition.y + FLOOR_OFFSET, mountPosition.z - 0.5);
    }

    public static AABB cellBox(Vec3 origin, int x, int y, int z) {
        return new AABB(origin.x + x, origin.y + y, origin.z + z,
                origin.x + x + 1, origin.y + y + 1, origin.z + z + 1);
    }

    /** The whole deck's box in world space, or null when there is no deck. Used to find riders. */
    @Nullable
    public AABB bounds(Vec3 origin) {
        AABB box = null;
        for (int key : this.cells.keySet()) {
            AABB cell = cellBox(origin, unpackX(key), unpackY(key), unpackZ(key));
            box = box == null ? cell : box.minmax(cell);
        }

        return box;
    }

    /**
     * The deck, cut into as few boxes as it takes to cover it, one of which becomes one collider
     * entity. A flat floor comes out as a single box however wide it is, which is what makes the
     * common case cost one entity rather than eighty-one.
     * <p>
     * Greedy, per layer: take the first cell not yet covered, run it as far as it goes in z, then
     * widen in x for as long as every cell of the new column is filled and uncovered. Which block is
     * in a cell is not looked at - this is collision, and every cell collides as the same cube.
     */
    public List<int[]> boxes() {
        List<int[]> boxes = new ArrayList<>();
        boolean[] covered = new boolean[SPAN * SPAN * HEIGHT];

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (covered[pack(x, y, z)] || !filled(x, y, z)) {
                        continue;
                    }

                    int depth = 1;
                    while (z + depth <= RADIUS && filled(x, y, z + depth) && !covered[pack(x, y, z + depth)]) {
                        depth++;
                    }

                    int width = 1;
                    while (x + width <= RADIUS && column(covered, x + width, y, z, depth)) {
                        width++;
                    }

                    for (int dx = 0; dx < width; dx++) {
                        for (int dz = 0; dz < depth; dz++) {
                            covered[pack(x + dx, y, z + dz)] = true;
                        }
                    }

                    boxes.add(new int[] {x, y, z, width, 1, depth});
                }
            }
        }

        return boxes;
    }

    /**
     * Whether a box is standing on the deck, which is what decides who the deck carries with it. It
     * is a test on the feet rather than on the whole box, so something flying past a moving platform
     * flies past it rather than being swept along.
     * <p>
     * The band runs from the bottom of a cell to a block above its top, and both ends are needed. A
     * deck climbing at eight tenths of a block a tick moves <em>into</em> whoever is on it, leaving
     * their feet below the surface they were standing on - counting that as standing is what carries
     * them back up out of it, and counting it as not standing would leave them inside the floor
     * with nowhere to walk. The top end is the height of a jump, so a jump on deck lands on deck.
     */
    public boolean supports(Vec3 origin, AABB box) {
        for (int key : this.cells.keySet()) {
            double top = origin.y + unpackY(key) + 1.0;
            if (box.minY < top - 1.0 || box.minY > top + 1.0) {
                continue;
            }

            double x = origin.x + unpackX(key);
            double z = origin.z + unpackZ(key);
            if (box.maxX > x && box.minX < x + 1.0 && box.maxZ > z && box.minZ < z + 1.0) {
                return true;
            }
        }

        return false;
    }

    private boolean filled(int x, int y, int z) {
        return this.cells.containsKey(pack(x, y, z));
    }

    private boolean column(boolean[] covered, int x, int y, int z, int depth) {
        for (int dz = 0; dz < depth; dz++) {
            if (!filled(x, y, z + dz) || covered[pack(x, y, z + dz)]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Which cell a look ray meets first, and which face of it. This is the whole of aiming at the
     * deck: the same answer serves placing (the neighbour through the face) and breaking (the cell
     * itself), so neither has to work out where a player was pointing on its own.
     * <p>
     * Every cell is tested rather than the merged boxes, because a merged box cannot say which of
     * the cells inside it was hit, and there are at most {@link #MAX_BLOCKS} of them.
     */
    @Nullable
    public Hit clip(Vec3 origin, Vec3 from, Vec3 to) {
        Hit best = null;
        double nearest = Double.MAX_VALUE;

        for (int key : this.cells.keySet()) {
            int x = unpackX(key);
            int y = unpackY(key);
            int z = unpackZ(key);
            BlockHitResult hit = AABB.clip(List.of(cellBox(origin, x, y, z)), from, to, BlockPos.ZERO);
            if (hit == null) {
                continue;
            }

            double distance = from.distanceToSqr(hit.getLocation());
            if (distance < nearest) {
                nearest = distance;
                best = new Hit(x, y, z, hit.getDirection());
            }
        }

        return best;
    }

    /**
     * Where a look ray crosses the floor layer, for the first block of all: with an empty deck there
     * is nothing to aim at, so the plane the floor will sit in is aimed at instead.
     */
    @Nullable
    public static Hit clipFloor(Vec3 origin, Vec3 from, Vec3 to) {
        double top = origin.y + 1.0;
        if ((from.y - top) * (to.y - top) > 0.0) {
            return null;
        }

        double travel = (top - from.y) / (to.y - from.y);
        Vec3 at = from.add(to.subtract(from).scale(travel));
        int x = (int) Math.floor(at.x - origin.x);
        int z = (int) Math.floor(at.z - origin.z);

        return inBounds(x, 0, z) ? new Hit(x, 0, z, Direction.UP) : null;
    }

    /* SAVING AND SYNCING */

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        forEach((x, y, z, block) -> {
            CompoundTag cell = new CompoundTag();
            cell.putInt("Cell", pack(x, y, z));
            cell.putString("Block", BuiltInRegistries.BLOCK.getKey(block).toString());
            list.add(cell);
        });

        tag.put("Platform", list);
    }

    public void load(CompoundTag tag) {
        this.cells.clear();

        for (Tag entry : tag.getList("Platform", Tag.TAG_COMPOUND)) {
            CompoundTag cell = (CompoundTag) entry;
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(cell.getString("Block")));
            int key = cell.getInt("Cell");
            if (block != Blocks.AIR && key >= 0 && key < SPAN * SPAN * HEIGHT && !isFull()) {
                this.cells.put(key, block);
            }
        }
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.cells.size());
        for (Map.Entry<Integer, Block> cell : this.cells.entrySet()) {
            buffer.writeVarInt(cell.getKey());
            buffer.writeVarInt(BuiltInRegistries.BLOCK.getId(cell.getValue()));
        }
    }

    /**
     * The deck off the wire. Everything in it is bounds-checked and the count is capped, because a
     * payload is not to be trusted even when it is the server's own - a client that is fed a
     * thousand cells would draw a thousand cells.
     */
    public void read(RegistryFriendlyByteBuf buffer) {
        this.cells.clear();

        int count = Math.min(buffer.readVarInt(), MAX_BLOCKS);
        for (int i = 0; i < count; i++) {
            int key = buffer.readVarInt();
            Block block = BuiltInRegistries.BLOCK.byId(buffer.readVarInt());
            if (key >= 0 && key < SPAN * SPAN * HEIGHT && block != Blocks.AIR) {
                this.cells.put(key, block);
            }
        }
    }
}
