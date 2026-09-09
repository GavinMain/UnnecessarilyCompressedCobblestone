package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Sweep;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The crescent an Omni Slash engraving looses: a swing thrown at the horizon, which nothing in its
 * way stops.
 * <p>
 * It is an {@link AbstractArrow} for the movement and the tracking and for nothing else. Every hit
 * test that class offers is switched off - {@code setNoPhysics(true)} is what does it, and reading
 * {@code AbstractArrow#tick} is the quickest way to see how much it turns off at once: with the flag
 * set, the block clip, the entity search, the impact event, gravity and the sticking are all skipped
 * and what is left is a position advanced by a delta each tick. That is exactly the half wanted
 * here, because a slash that passes through blocks and through creatures alike cannot be described
 * by a projectile that stops at the first of either.
 * <p>
 * The two things it does instead are done against the line actually travelled rather than against
 * the box it ended the tick in, because at {@link #SPEED} blocks a tick a test at either end would
 * leave most of the path untouched:
 * <ul>
 * <li>the <em>carve</em>, which takes every block within {@link #RADIUS} of that line, and</li>
 * <li>the <em>cut</em>, which hurts every living thing within the same distance of it, once.</li>
 * </ul>
 * "Once" is the whole of {@link #struck}: a slash that ran along a creature's length would otherwise
 * land a hit for every tick that creature stayed in the capsule, and what this fires is one swing.
 * <p>
 * Its range is the render distance in the honest sense - nothing here counts blocks. The crescent
 * keeps flying and stops being ticked when it leaves the chunks the server keeps loaded around its
 * players, which is the server view distance. {@link #MAX_LIFETIME} is a backstop for the one case
 * that does not reach: a slash loosed inside a chunk somebody is standing in and aimed at the sky.
 * <p>
 * Nothing it breaks drops. This is a way of opening ground rather than of collecting it, and a
 * crescent five blocks across running to the view distance is a few thousand blocks - the same call
 * the Exploding Sword makes for the same reason, one order of magnitude further out.
 */
public class OmniSlashEntity extends AbstractArrow {
    /**
     * Blocks a tick. It is held rather than allowed to decay: vanilla takes a hundredth off a
     * projectile's speed every tick, which over a flight this long would land the far end of the cut
     * at two thirds of the speed the near end left at, and a cut does not slow down.
     */
    public static final float SPEED = 2.5F;

    /**
     * How tall the crescent is, in blocks - the figure the whole ability is described by. The cut
     * and the carve are a capsule of half this radius about the line travelled, so it is the width
     * as well: a slash opens a five block tunnel whichever way it is aimed, which is what makes the
     * one number honest for a blade that can be pointed at the floor.
     */
    public static final float HEIGHT = 5.0F;

    /** Half of {@link #HEIGHT}, which is what both the carve and the cut are measured with. */
    public static final double RADIUS = HEIGHT / 2.0;

    /**
     * How long a slash lives at most, in ticks. A backstop rather than the range - see the class
     * note - and generous: at {@link #SPEED} it is five hundred blocks, well past any view distance
     * a server is likely to be set to.
     */
    public static final int MAX_LIFETIME = 200;

    private static final String TAG_DAMAGE = "omni_slash_damage";

    /**
     * The whole of "one swing, not one a tick". Kept in memory rather than saved: a slash lives a
     * few seconds and is thrown away the moment its chunks stop being ticked, so there is no reload
     * for the list to survive.
     */
    private final Set<UUID> struck = new HashSet<>();

    /** One standard attack from the weapon this was loosed off, worked out at the wielder's end. */
    private float damage;

    public OmniSlashEntity(EntityType<? extends OmniSlashEntity> entityType, Level level) {
        super(entityType, level);
        setNoPhysics(true);
    }

    public OmniSlashEntity(Level level, LivingEntity wielder, ItemStack weapon) {
        super(ModEntities.OMNI_SLASH.get(), wielder, level, defaultPickup(), weapon);
        setNoPhysics(true);
    }

    /**
     * Never picked up and never dropped, but it cannot be empty: {@code AbstractArrow} writes this
     * stack straight into its save data, and an empty {@code ItemStack} refuses to be written.
     * Called from that class's own constructor, before any field here is assigned, so it can only
     * ever answer with a constant - see {@code CompressedArrowEntity} for the trap in full.
     */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return defaultPickup();
    }

    private static ItemStack defaultPickup() {
        return new ItemStack(Engraving.OMNI_SLASH.item());
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void tick() {
        // Held at a constant speed, and taken off the delta rather than from a field of its own, so
        // the client works the same figure out from the velocity it was sent at spawn and no heading
        // has to be synced or saved.
        Vec3 heading = getDeltaMovement();
        if (heading.lengthSqr() > 1.0E-6) {
            setDeltaMovement(heading.normalize().scale(SPEED));
        }

        Vec3 from = position();
        super.tick();
        Vec3 to = position();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        carve(serverLevel, from, to);
        cut(serverLevel, from, to);

        if (this.tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    /**
     * The cut: one standard attack to everything within {@link #RADIUS} of the stretch just
     * travelled, and never twice to the same creature.
     * <p>
     * The damage type is the whole of "cannot be blocked" - see {@code ModDamageTypes.OMNI_SLASH},
     * which is in every {@code bypasses_*} tag that means a form of protection does not apply. The
     * blow is credited to the wielder rather than to the crescent, so a kill counts as theirs and
     * anything reading {@code getKillCredit} pays out to the right player.
     */
    private void cut(ServerLevel level, Vec3 from, Vec3 to) {
        Entity owner = getOwner();

        for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                Sweep.bounds(from, to, RADIUS),
                entity -> entity != owner && entity.isAlive() && !(entity instanceof ArmorStand)
                        && !this.struck.contains(entity.getUUID())
                        && (owner == null || !owner.isAlliedTo(entity))
                        && Sweep.caught(entity, from, to, RADIUS))) {
            this.struck.add(caught.getUUID());
            caught.hurt(level.damageSources().source(ModDamageTypes.OMNI_SLASH, this, owner),
                    this.damage);
        }
    }

    /**
     * The carve: every block within {@link #RADIUS} of the stretch just travelled, removed.
     * <p>
     * What may be taken is read off the block rather than listed. Compressed cobblestone is answered
     * by having a compression level at all, which is what lets this go through the hardened tiers -
     * they carry a destroy speed of -1 and an explosion resistance of three and a half million, so
     * neither the mod's usual unbreakable test nor any explosion in the game would touch one.
     * Everything else falls back on that destroy speed, which is how bedrock, the world's ceiling
     * and every other mod's unbreakable block are refused here without being named.
     * <p>
     * The candidate box is walked and each position tested against the line, rather than the line
     * being stepped along, so every block is looked at exactly once however the slash is aimed. It
     * is a few dozen blocks a tick - two orders short of anything {@code DeferredFill} exists for -
     * because the work is already spread over the flight by the flight itself.
     */
    private void carve(ServerLevel level, Vec3 from, Vec3 to) {
        AABB box = Sweep.bounds(from, to, RADIUS);
        Entity owner = getOwner();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
            for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                    if (Sweep.distanceToSegment(new Vec3(x + 0.5, y + 0.5, z + 0.5), from, to)
                            > RADIUS) {
                        continue;
                    }

                    pos.set(x, y, z);

                    // Guarded, because a slash outruns the chunks the player it was fired by keeps
                    // loaded, and a write past them would drag chunks into memory.
                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !breaks(level, pos, state)) {
                        continue;
                    }

                    level.destroyBlock(pos.immutable(), false, owner);
                }
            }
        }
    }

    /** Whether the slash takes the block at {@code pos}: everything short of bedrock. */
    private static boolean breaks(ServerLevel level, BlockPos pos, BlockState state) {
        return ModBlocks.levelOf(state.getBlock()) != null
                || state.getDestroySpeed(level, pos) >= 0.0F;
    }

    /**
     * Both hit hooks are dead code while {@code noPhysics} is set - {@code AbstractArrow#tick} never
     * reaches the impact branch - and are overridden to nothing anyway, so that a future change to
     * that flag cannot quietly turn the slash back into an arrow that stops at the first wall.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat(TAG_DAMAGE, this.damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.damage = compound.getFloat(TAG_DAMAGE);
    }
}
