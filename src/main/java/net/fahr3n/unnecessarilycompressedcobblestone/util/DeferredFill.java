package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Block writes too large to do in one tick, spread over the following ones a layer at a time.
 * <p>
 * A TNT that rewrites tens of thousands of blocks stalls the server hard enough to look like a
 * crash if it does the lot at once. Handing the work here instead turns that into a second or two
 * of the effect visibly sweeping through the world, which reads as intentional.
 * <p>
 * Jobs live only as long as the server runs, and stop if their ground unloads. An interrupted fill
 * leaves a partial result, which is untidy but never broken.
 */
public final class DeferredFill {
    private static final List<Job> JOBS = new ArrayList<>();

    private DeferredFill() {
    }

    /** Fills {@code chunk} solid with {@code fill}, bottom of the world upwards. */
    public static void queueChunk(ServerLevel level, ChunkPos chunk, BlockState fill) {
        JOBS.add(new ChunkFill(level.dimension(), chunk, fill, level.getMinBuildHeight()));
    }

    /** Turns a wide, shallow disc around {@code origin} to glass, densest at the middle. */
    public static void queueGlass(ServerLevel level, BlockPos origin) {
        JOBS.add(new GlassField(level.dimension(), origin));
    }

    /**
     * Blows a crater of {@code power} open as a series of widening blasts from one point instead of
     * all at once. {@code source} is only carried so a kill is still credited to whoever lit it.
     */
    public static void queueStagedBlast(ServerLevel level, Entity source, Vec3 center, float power) {
        JOBS.add(new StagedBlast(level.dimension(), source, center, power));
    }

    /** Advances every job belonging to this level by one step. */
    public static void tick(ServerLevel level) {
        if (JOBS.isEmpty()) {
            return;
        }

        Iterator<Job> jobs = JOBS.iterator();
        while (jobs.hasNext()) {
            Job job = jobs.next();
            if (job.dimension().equals(level.dimension()) && job.advance(level)) {
                jobs.remove();
            }
        }
    }

    /** Dropped on shutdown so a job cannot outlive the level it was working on. */
    public static void clear() {
        JOBS.clear();
    }

    private interface Job {
        ResourceKey<Level> dimension();

        /** @return true when the job is finished and should be dropped */
        boolean advance(ServerLevel level);
    }

    /**
     * Packs a chunk solid. A chunk is sixteen by sixteen by the whole height of the world, around
     * ninety-eight thousand blocks, so it climbs a few layers a tick instead.
     */
    private static final class ChunkFill implements Job {
        /**
         * Layers per tick. Sixteen by sixteen by this is how many blocks are written each tick, and
         * the world height divided by this is how long the whole chunk takes: at four, that is about
         * a thousand blocks a tick and just under five seconds for a full height chunk.
         */
        private static final int LAYERS_PER_TICK = 4;

        private final ResourceKey<Level> dimension;
        private final ChunkPos chunk;
        private final BlockState fill;
        private int y;

        private ChunkFill(ResourceKey<Level> dimension, ChunkPos chunk, BlockState fill, int y) {
            this.dimension = dimension;
            this.chunk = chunk;
            this.fill = fill;
            this.y = y;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            // Never drags the chunk back into memory to keep filling it; walking away stops the job
            // rather than paying to load a chunk nobody is looking at.
            if (!level.hasChunk(this.chunk.x, this.chunk.z)) {
                return true;
            }

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int top = Math.min(this.y + LAYERS_PER_TICK, level.getMaxBuildHeight());

            for (; this.y < top; this.y++) {
                for (int x = this.chunk.getMinBlockX(); x <= this.chunk.getMaxBlockX(); x++) {
                    for (int z = this.chunk.getMinBlockZ(); z <= this.chunk.getMaxBlockZ(); z++) {
                        level.setBlock(pos.set(x, this.y, z), this.fill, FLAGS);
                    }
                }
            }

            return this.y >= level.getMaxBuildHeight();
        }
    }

    /**
     * Turns a disc of the world to glass, one horizontal layer per tick. Air is turned to glass
     * along with everything else, so this hollows nothing out - it converts.
     * <p>
     * The chance is set by three figures - {@link #CORE_CHANCE} within {@link #CORE_RADIUS},
     * {@link #MID_CHANCE} at {@link #MID_RADIUS} and {@link #OUTER_CHANCE} at the rim - and slides
     * between them rather than stepping, so there are no rings where one band gives way to the next.
     * Every position in range is rolled, so the total is whatever the falloff produces and differs
     * each time.
     */
    private static final class GlassField implements Job {
        /** How far out it reaches. */
        private static final int RADIUS = 50;

        /** The dense middle. The chance is flat across it and only starts falling past the edge. */
        private static final double CORE_RADIUS = 10.0;
        private static final double CORE_CHANCE = 0.4;

        /** The halfway mark the falloff passes through. */
        private static final double MID_RADIUS = 30.0;
        private static final double MID_CHANCE = 0.05;

        /** Where it ends up at the rim. */
        private static final double OUTER_CHANCE = 0.02;

        /** How far above and below the blast it reaches; twice this plus one is how many ticks it takes. */
        private static final int HEIGHT = 10;

        /** Clear glass only - a full block most of the time, a pane the rest, for a bit of texture. */
        private static final double PANE_CHANCE = 0.3;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private int dy = -HEIGHT;

        private GlassField(ResourceKey<Level> dimension, BlockPos origin) {
            this.dimension = dimension;
            this.origin = origin;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        /**
         * Flat across the core, then a straight line down to the middle figure and another to the
         * rim. The two slopes differ, so the field still thins fastest just outside the core, but
         * the chance itself never jumps and no ring shows up where the slope changes.
         */
        private static double chanceAt(double radius) {
            if (radius <= CORE_RADIUS) {
                return CORE_CHANCE;
            }

            if (radius <= MID_RADIUS) {
                return Mth.lerp((radius - CORE_RADIUS) / (MID_RADIUS - CORE_RADIUS), CORE_CHANCE, MID_CHANCE);
            }

            return Mth.lerp((radius - MID_RADIUS) / (RADIUS - MID_RADIUS), MID_CHANCE, OUTER_CHANCE);
        }

        @Override
        public boolean advance(ServerLevel level) {
            RandomSource random = level.random;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    double radius = Math.sqrt((double) dx * dx + (double) dz * dz);
                    if (radius > RADIUS) {
                        continue;
                    }

                    if (random.nextDouble() >= chanceAt(radius)) {
                        continue;
                    }

                    pos.set(this.origin.getX() + dx, this.origin.getY() + this.dy, this.origin.getZ() + dz);

                    // Never drags an unloaded chunk into memory, and never eats bedrock or anything
                    // else the world refuses to let go of.
                    if (!level.isLoaded(pos) || level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }

                    BlockState glass = (random.nextDouble() < PANE_CHANCE ? Blocks.GLASS_PANE : Blocks.GLASS)
                            .defaultBlockState();
                    level.setBlock(pos, glass, Block.UPDATE_CLIENTS);
                }
            }

            return ++this.dy > HEIGHT;
        }
    }

    /**
     * One very large explosion, done as several widening ones from the same point.
     * <p>
     * The ray casting a blast does is fixed at 4096 rays whatever its size, so it costs about the
     * same however this is sliced; what actually stalls the server is the other half, where every
     * block in the crater is removed and rolled for drops in a single tick. Firing from the middle
     * outwards spreads that half over {@link #STAGES} ticks' worth of work, because each blast only
     * has the shell beyond the last one left to clear - everything inside is already air.
     * <p>
     * The radii are spaced by the cube root of the stage so each one opens an equal volume rather
     * than an equal distance; spacing them evenly would leave the last stage doing half the job.
     * The final stage is the full power, so the crater ends up the size it always was.
     * <p>
     * One thing does change: anything standing near the middle is caught by every stage rather than
     * once, so the centre of the blast is far more lethal than it used to be.
     */
    private static final class StagedBlast implements Job {
        /** How many widening blasts the crater is opened with. */
        private static final int STAGES = 6;

        /** Ticks between them, so the whole thing rolls outward over about a second and a half. */
        private static final int TICKS_PER_STAGE = 5;

        private final ResourceKey<Level> dimension;
        @Nullable
        private final Entity source;
        private final Vec3 center;
        private final float power;
        private int stage;
        private int cooldown;

        private StagedBlast(ResourceKey<Level> dimension, @Nullable Entity source, Vec3 center, float power) {
            this.dimension = dimension;
            this.source = source;
            this.center = center;
            this.power = power;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            if (this.cooldown-- > 0) {
                return false;
            }

            this.cooldown = TICKS_PER_STAGE;
            this.stage++;

            float radius = this.power * (float) Math.cbrt((double) this.stage / STAGES);
            level.explode(this.source, null, null, this.center.x, this.center.y, this.center.z,
                    radius, false, Level.ExplosionInteraction.TNT);

            return this.stage >= STAGES;
        }
    }

    /**
     * Neighbour updates are left off deliberately: these fills rewrite whole volumes at once, so
     * there is nothing for a neighbour to usefully react to, and asking for them would multiply the
     * cost of the very thing this class exists to spread out.
     */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
}
