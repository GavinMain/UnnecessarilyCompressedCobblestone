package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.worldgen.ModConfiguredFeatures;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowTier;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneChickenEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPrimedTntEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Work too large to do in one tick, spread over the following ones a slice at a time. Most of it is
 * block writes; the anvil rain and the flock are entity spawns, which cost less each but are just as
 * capable of stalling a tick in their thousands.
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
     * The wide, thin version of the glass field: a two hundred block disc that is only ever a few
     * percent glass, laid down one ring at a time so no single tick walks more than a thin band of
     * it. A whole disc that size is a hundred and twenty thousand columns, which is why it is walked
     * as rings rather than as layers.
     */
    public static void queueWideGlass(ServerLevel level, BlockPos origin) {
        JOBS.add(new WideGlassField(level.dimension(), origin));
    }

    /**
     * Lays a disc of soil that rolls {@code relief} blocks either side of {@code origin} and grows
     * {@code trees} compressed cobblestone trees on it.
     */
    public static void queueGrove(ServerLevel level, BlockPos origin, int radius, int relief, int trees) {
        JOBS.add(new Grove(level.dimension(), origin, radius, relief, trees));
    }

    /**
     * Throws arrows out in a widening spiral for {@code ticks} ticks. The pattern is the whole point
     * of it, so it cannot be done on one tick even if the cost allowed: it is a shape drawn over
     * time.
     */
    public static void queueArrowSpiral(ServerLevel level, Vec3 origin, int ticks, int arrowsPerTick,
                                        float startVelocity, float endVelocity, @Nullable LivingEntity owner) {
        JOBS.add(new ArrowSpiral(level.dimension(), origin, ticks, arrowsPerTick, startVelocity, endVelocity, owner));
    }

    /**
     * Drops primed TNT out of the sky over a disc, spread across {@code ticks} rather than all at
     * once - which is what makes it a rain rather than a single salvo. Each one goes off where it
     * lands rather than on the way down, so whatever it throws is thrown across the ground.
     * <p>
     * The rain is exactly {@code drops}: one TNT per entry, in a random order, so a caller that
     * wants two of one kind and one of another asks for precisely that rather than for a count and
     * a bag to draw from. Where each lands is still random; only how many of each is not.
     *
     * @param drops       one TNT block per drop, shuffled before the first of them falls
     * @param damageScale what each one's blast is worth
     */
    public static void queueTntRain(ServerLevel level, BlockPos origin, int radius, int ticks,
                                    List<Block> drops, @Nullable LivingEntity owner, float damageScale) {
        if (!drops.isEmpty()) {
            JOBS.add(new TntRain(level.dimension(), origin, radius, ticks, drops, owner, damageScale));
        }
    }

    /** Drops {@code count} anvils out of the sky over a disc of {@code radius}, a few per tick. */
    public static void queueAnvilRain(ServerLevel level, BlockPos origin, int radius, int count) {
        JOBS.add(new AnvilRain(level.dimension(), origin, radius, count));
    }

    /**
     * Drops {@code count} chickens in over several ticks and then bursts them apart. Spawning that
     * many entities on one tick is the same kind of stall a big block write is, and the burst has to
     * wait for the last of them anyway.
     */
    public static void queueFlock(ServerLevel level, Vec3 origin, int count, double cluster, double dropHeight,
                                  ExplosionDamageCalculator burst, float burstRadius) {
        JOBS.add(new Flock(level.dimension(), origin, count, cluster, dropHeight, burst, burstRadius));
    }

    /**
     * Blows a crater worth {@code power} open as a swarm of smaller blasts spread through it, a
     * couple a tick, rather than as one blast or as widening blasts from one point - see
     * {@link ClusterBlast} for why neither of those leaves a crater anyone can see. {@code source}
     * is only carried so a kill is still credited to whoever lit it.
     */
    public static void queueClusterBlast(ServerLevel level, Entity source, Vec3 center, float power) {
        JOBS.add(new ClusterBlast(level.dimension(), source, center, power));
    }

    /**
     * Scatters cobwebs through a cylinder reaching to the edge of what a player can see. See
     * {@link WebField} for why this is thrown at rather than walked.
     */
    public static void queueWebs(ServerLevel level, BlockPos origin) {
        JOBS.add(new WebField(level.dimension(), origin));
    }

    /** Lays snow over the ground around {@code origin}, deep in the middle and thinning to the rim. */
    public static void queueSnowfield(ServerLevel level, BlockPos origin) {
        JOBS.add(new Snowfield(level.dimension(), origin));
    }

    /**
     * Stands a Menger sponge of ancient debris over {@code origin} and turns the ground under it to
     * Slippery Ice. See {@link Fractal} for why it is the width it is.
     */
    public static void queueFractal(ServerLevel level, BlockPos origin) {
        JOBS.add(new Fractal(level.dimension(), origin));
    }

    /**
     * Raises a one block wide column of {@code fluid} out of {@code origin}, a block a tick. See
     * {@link Geyser} for why the eruption is the job rather than a thing the job produces.
     */
    public static void queueGeyser(ServerLevel level, BlockPos origin, BlockState fluid, int height) {
        JOBS.add(new Geyser(level.dimension(), origin, fluid, height));
    }

    /**
     * Digs a basin around {@code origin}, fills it with water and grows a coral reef in it. See
     * {@link Reef} for why the shape is a bowl rather than a slab.
     */
    public static void queueReef(ServerLevel level, BlockPos origin) {
        JOBS.add(new Reef(level.dimension(), origin));
    }

    /**
     * Ten seconds of a weak black hole at {@code centre}: everything loose nearby is pulled towards
     * it, and the ground around it is torn up a few blocks at a time and thrown in after them.
     */
    public static void queueSingularity(ServerLevel level, Vec3 centre) {
        JOBS.add(new Singularity(level.dimension(), centre));
    }

    /**
     * Builds a hollow hemisphere of {@code fill}, one block thick, standing on the layer
     * {@code origin} is on: a dome, one horizontal ring of it per tick. See {@link Dome}.
     */
    public static void queueDome(ServerLevel level, BlockPos origin, int radius, BlockState fill) {
        JOBS.add(new Dome(level.dimension(), origin, radius, fill));
    }

    /**
     * Takes every block within {@code radius} of {@code centre} that can be taken at all, one
     * horizontal layer a tick. See {@link Crater}.
     */
    public static void queueCrater(ServerLevel level, Vec3 centre, int radius) {
        JOBS.add(new Crater(level.dimension(), BlockPos.containing(centre), radius));
    }

    /**
     * Flattens a disc of {@code radius} around {@code origin} into somewhere a boss fight can
     * actually happen: every hole in the floor filled and everything standing on it taken away. See
     * {@link Arena} for the shape of it.
     */
    public static void queueArena(ServerLevel level, BlockPos origin, int radius) {
        JOBS.add(new Arena(level.dimension(), origin, radius));
    }

    /**
     * Works out where and how a vanilla structure would be built at {@code origin}, exactly as
     * {@code /place structure} does: {@code Structure#generate} with a biome predicate that accepts
     * everything.
     * <p>
     * That predicate is <em>not</em> a way past every check. It only replaces the
     * "is this structure allowed in this biome" test; a structure may still refuse on rules written
     * inside its own {@code findGenerationPoint}, and two of the three used here do - an ocean
     * monument insists every biome within twenty-nine blocks is ocean or river, and a woodland
     * mansion refuses below y 60. A refusal comes back as {@link StructureStart#INVALID_START}, so
     * the caller must check {@link StructureStart#isValid()} and decide what to do about it.
     *
     * @return the start to build, or {@link StructureStart#INVALID_START} if it declined
     */
    public static StructureStart generateStructure(ServerLevel level, BlockPos origin, ResourceKey<Structure> key) {
        Optional<Holder.Reference<Structure>> holder = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE).get(key);
        if (holder.isEmpty()) {
            return StructureStart.INVALID_START;
        }

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return holder.get().value().generate(
                level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                new ChunkPos(origin), 0, level, biome -> true);
    }

    /**
     * Lays a generated structure into the world, one chunk of it per tick.
     * <p>
     * Only the placing is deferred, and that is the half worth deferring: a mansion is some five
     * chunks square and several thousand blocks in each of them, which is a chunk fill's worth of
     * writes on one tick if it is all done at once. See {@code StructureBuild} for why the slice is
     * a chunk rather than a layer.
     */
    public static void queueStructure(ServerLevel level, StructureStart start) {
        if (start.isValid()) {
            JOBS.add(new StructureBuild(level.dimension(), start));
        }
    }

    /**
     * Flattens a disc of {@code radius} around {@code origin} outright: a solid floor of
     * {@code fill} laid across the whole disc, and every block above it up to the world's ceiling
     * taken away. See {@link Flatten} for why it is laid floor-first and cleared top-down.
     */
    public static void queueFlatten(ServerLevel level, BlockPos origin, int radius, BlockState fill) {
        JOBS.add(new Flatten(level.dimension(), origin, radius, fill, level.getMaxBuildHeight() - 1));
    }

    /**
     * Raises one-block-wide columns of {@code fill} from the bottom of the world to the top, at
     * random columns of a disc of {@code radius} around {@code origin} - one column in
     * {@code chance} of them.
     */
    public static void queuePillars(ServerLevel level, BlockPos origin, int radius, int chance, BlockState fill) {
        JOBS.add(new Pillars(level.dimension(), origin, radius, chance, fill, level.random));
    }

    /** Advances every job belonging to this level by one step. */
    public static void tick(ServerLevel level) {
        if (JOBS.isEmpty()) {
            return;
        }

        // Over a snapshot rather than over the list, because a job may queue another one as it
        // finishes - the Singularity's collapse queues the hole it leaves - and an iterator over
        // the live list would throw the moment it did. A job added during this loop simply waits
        // for the next tick, which is what a job queued mid-tick does anyway.
        for (Job job : List.copyOf(JOBS)) {
            if (job.dimension().equals(level.dimension()) && job.advance(level)) {
                JOBS.remove(job);
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
                        pos.set(x, this.y, z);

                        // Bedrock, the world's ceiling and the hardened compression levels all read
                        // as unbreakable, and a fill that wrote over them would be a way around
                        // them. The glass field has always skipped these; this now does too.
                        if (level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F) {
                            continue;
                        }

                        level.setBlock(pos, this.fill, FLAGS);
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
     * One very large explosion, done as a swarm of ordinary ones spread through the volume it is
     * meant to clear, a couple a tick, working outwards from the middle.
     * <p>
     * It cannot be one blast, and it cannot be widening blasts from one point either, which is what
     * this used to be. A vanilla explosion casts a fixed 1352 rays whatever its radius, and each ray
     * only removes the blocks it personally passes through. At a radius of a few blocks those rays
     * overlap and the crater is solid; at a radius of forty they are twenty blocks apart by the time
     * they get there, so all a huge blast does past its own core is drill a handful of spokes. That
     * is why a twenty times blast read as weaker than a five times one: the first stage dug the only
     * crater anyone could see and the rest went straight through the holes it left.
     * <p>
     * Small blasts at many points keep every ray dense. Each shot is {@link #SHOT_POWER_SCALE} of the
     * nominal power - big enough to open a proper hole of its own - and they are placed at random
     * directions from the middle at a distance that grows with the cube root of the shot number, so
     * equal numbers of shots land in equal volumes and the crater opens outwards instead of all over
     * at once.
     */
    private static final class ClusterBlast implements Job {
        /** How many blasts the crater is made of. */
        private static final int SHOTS = 96;

        /** How many go off each tick. Ninety-six of them is a little under two and a half seconds. */
        private static final int SHOTS_PER_TICK = 2;

        /** How far out the shots are spread, as a fraction of the nominal power. */
        private static final double SPREAD_SCALE = 0.4;

        /** What each individual shot is worth, as a fraction of the nominal power. */
        private static final float SHOT_POWER_SCALE = 0.6F;

        private final ResourceKey<Level> dimension;
        @Nullable
        private final Entity source;
        private final Vec3 center;
        private final float power;
        private int shot;

        private ClusterBlast(ResourceKey<Level> dimension, @Nullable Entity source, Vec3 center, float power) {
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
            RandomSource random = level.random;
            double spread = this.power * SPREAD_SCALE;

            for (int i = 0; i < SHOTS_PER_TICK && this.shot < SHOTS; i++) {
                this.shot++;

                // The first shot is the middle itself; the rest walk outwards, by volume rather than
                // by distance, in a direction drawn off the sphere rather than the box.
                double distance = this.shot == 1 ? 0.0 : spread * Math.cbrt((double) this.shot / SHOTS);
                Vec3 offset = this.shot == 1
                        ? Vec3.ZERO
                        : new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
                                .normalize().scale(distance);
                Vec3 at = this.center.add(offset);

                level.explode(this.source, null, null, at.x, at.y, at.z,
                        this.power * SHOT_POWER_SCALE, false, Level.ExplosionInteraction.TNT);
            }

            return this.shot >= SHOTS;
        }
    }

    /**
     * A disc two hundred blocks across and forty-one blocks tall, in every colour of glass there is:
     * densest at the middle and thinning out to almost nothing at the rim.
     * <p>
     * It is walked as rings rather than as layers because of its size: a layer of it is a hundred and
     * twenty thousand columns, and even at two percent that is thousands of writes in one tick. A
     * ring only ever costs its own area, so every tick does about the same amount of work however far
     * out it has got.
     */
    private static final class WideGlassField implements Job {
        private static final int RADIUS = 200;

        /** How far above and below the blast it reaches: a slab forty-one blocks tall, not a sheet. */
        private static final int HEIGHT = 20;

        /**
         * The chance of glass at the middle and at the rim; everything between is a straight line.
         * <p>
         * Both are the per block chance, so they have to fall as the field gets taller or the same
         * five percent over forty-one layers would be more than a dozen times as much glass as it
         * was over three. {@link #LAYER_SCALE} is what keeps the total the size it was: a haze of
         * glass hanging in the air rather than a solid block of it.
         */
        private static final double LAYER_SCALE = 3.0 / (2 * HEIGHT + 1);
        private static final double CENTRE_CHANCE = 0.05 * LAYER_SCALE;
        private static final double RIM_CHANCE = 0.005 * LAYER_SCALE;

        /**
         * How much further out each tick reaches. Narrower than the shallow field's because every
         * ring is now forty-one layers deep, and the work a tick does is the ring's area times that.
         */
        private static final int RING_WIDTH = 3;

        /** A full block most of the time, a pane the rest, for a bit of texture. */
        private static final double PANE_CHANCE = 0.3;

        /**
         * Every colour of glass and its matching pane, plus plain glass at the front. Paired by
         * index so a block and a pane in the same spot are always the same colour.
         */
        private static final Block[] GLASS = {
                Blocks.GLASS, Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS,
                Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS,
                Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS,
                Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS,
                Blocks.BLUE_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS,
                Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS,
        };

        private static final Block[] PANES = {
                Blocks.GLASS_PANE, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE,
                Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
                Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS_PANE,
                Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS_PANE,
                Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS_PANE,
                Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE,
        };

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private int inner;

        private WideGlassField(ResourceKey<Level> dimension, BlockPos origin) {
            this.dimension = dimension;
            this.origin = origin;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            RandomSource random = level.random;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int outer = Math.min(RADIUS, this.inner + RING_WIDTH);

            // Only the ring's own bounding chords are walked, not the whole disc out to it: for each
            // column the inner circle carves a hole out of the middle of the run.
            for (int dx = -outer; dx <= outer; dx++) {
                int outerZ = (int) Math.sqrt((double) outer * outer - (double) dx * dx);
                int innerZ = Math.abs(dx) < this.inner
                        ? (int) Math.sqrt((double) this.inner * this.inner - (double) dx * dx)
                        : -1;

                for (int dz = -outerZ; dz <= outerZ; dz++) {
                    if (Math.abs(dz) <= innerZ) {
                        continue;
                    }

                    double radius = Math.sqrt((double) dx * dx + (double) dz * dz);
                    if (radius > RADIUS) {
                        continue;
                    }

                    double chance = Mth.lerp(radius / RADIUS, CENTRE_CHANCE, RIM_CHANCE);
                    for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                        if (random.nextDouble() >= chance) {
                            continue;
                        }

                        pos.set(this.origin.getX() + dx, this.origin.getY() + dy, this.origin.getZ() + dz);
                        if (!level.isLoaded(pos) || level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F) {
                            continue;
                        }

                        int colour = random.nextInt(GLASS.length);
                        BlockState glass = (random.nextDouble() < PANE_CHANCE ? PANES : GLASS)[colour]
                                .defaultBlockState();
                        level.setBlock(pos, glass, FLAGS);
                    }
                }
            }

            this.inner = outer;
            return this.inner >= RADIUS;
        }
    }

    /**
     * Anvils, out of the sky, over and over. Each one is a real falling block, so it lands, hurts
     * whatever is under it and stays there as an anvil - which is what makes this expensive enough to
     * want spreading out, since a few hundred falling entities on one tick is a visible stall.
     */
    private static final class AnvilRain implements Job {
        /** How many go up each tick. */
        private static final int PER_TICK = 10;

        /** How far above the blast they appear. */
        private static final int DROP_HEIGHT = 30;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final int radius;
        private int remaining;

        private AnvilRain(ResourceKey<Level> dimension, BlockPos origin, int radius, int count) {
            this.dimension = dimension;
            this.origin = origin;
            this.radius = radius;
            this.remaining = count;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            RandomSource random = level.random;

            for (int i = 0; i < PER_TICK && this.remaining > 0; i++) {
                this.remaining--;

                int dx = random.nextInt(this.radius * 2 + 1) - this.radius;
                int dz = random.nextInt(this.radius * 2 + 1) - this.radius;
                BlockPos pos = new BlockPos(this.origin.getX() + dx, this.origin.getY() + DROP_HEIGHT,
                        this.origin.getZ() + dz);

                // An anvil is only dropped into open sky: FallingBlockEntity.fall writes the block it
                // is falling from, so starting it inside something would eat that something.
                if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()) {
                    continue;
                }

                // Chipped and damaged ones as well, so a landed blanket is not all one texture.
                Block anvil = switch (random.nextInt(3)) {
                    case 0 -> Blocks.CHIPPED_ANVIL;
                    case 1 -> Blocks.DAMAGED_ANVIL;
                    default -> Blocks.ANVIL;
                };

                FallingBlockEntity.fall(level, pos, anvil.defaultBlockState());
            }

            return this.remaining <= 0;
        }
    }

    /**
     * A flock dropped in overhead a few at a time, and then blown apart once the last of them is in
     * the air. The spread is what keeps them alive on the way down: the {@code maxEntityCramming}
     * rule suffocates anything more than twenty-four deep in a pile.
     */
    private static final class Flock implements Job {
        /** How many appear each tick. */
        private static final int PER_TICK = 20;

        private final ResourceKey<Level> dimension;
        private final Vec3 origin;
        private final double cluster;
        private final double dropHeight;
        private final ExplosionDamageCalculator burst;
        private final float burstRadius;
        private int remaining;

        private Flock(ResourceKey<Level> dimension, Vec3 origin, int count, double cluster, double dropHeight,
                      ExplosionDamageCalculator burst, float burstRadius) {
            this.dimension = dimension;
            this.origin = origin;
            this.remaining = count;
            this.cluster = cluster;
            this.dropHeight = dropHeight;
            this.burst = burst;
            this.burstRadius = burstRadius;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            RandomSource random = level.random;
            double y = this.origin.y + this.dropHeight;

            for (int i = 0; i < PER_TICK && this.remaining > 0; i++) {
                this.remaining--;

                CompressedCobblestoneChickenEntity chicken = ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get().create(level);
                if (chicken == null) {
                    continue;
                }

                chicken.moveTo(this.origin.x + (random.nextDouble() - 0.5) * this.cluster,
                        y + random.nextDouble() * this.cluster,
                        this.origin.z + (random.nextDouble() - 0.5) * this.cluster,
                        random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(chicken);
            }

            if (this.remaining > 0) {
                return false;
            }

            // The burst is let off in the middle of the flock directly rather than by throwing a wind
            // charge: a thrown one only goes off when it hits something, and it sails through a crowd
            // of entities without touching any of them.
            level.explode(null, null, this.burst, this.origin.x, y + this.cluster / 2.0, this.origin.z,
                    this.burstRadius, false, Level.ExplosionInteraction.NONE,
                    ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                    SoundEvents.WIND_CHARGE_BURST);
            return true;
        }
    }

    /**
     * Soil, and then a wood of stone on top of it.
     * <p>
     * The ground comes first and is laid a slice of columns at a time: a disc of sixteen is some
     * eight hundred columns, and each of them is a dozen writes once the air above it is cleared, so
     * doing it in one tick is ten thousand block changes. Only when the last column is down does it
     * start planting, because a tree checks the ground it is given and would refuse half a hillside
     * that had not been levelled yet.
     * <p>
     * The height of each column is a pair of sine waves rather than noise: it costs nothing, it is
     * smooth in every direction, and it gives the grove the rolling look that a random height per
     * column never does.
     */
    private static final class Grove implements Job {
        /** How many columns of soil are laid each tick. */
        private static final int COLUMNS_PER_TICK = 64;

        /** How many trees are attempted each tick once the soil is down. */
        private static final int TREES_PER_TICK = 2;

        /** How far above each column the air is cleared, so there is room for what grows there. */
        private static final int CLEARANCE = 12;

        /** How deep the soil goes under its surface. */
        private static final int SOIL_DEPTH = 3;

        /** How tight the rolling is; a full wave every twenty-odd blocks. */
        private static final double RELIEF_SCALE = 0.3;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final int radius;
        private final int relief;
        private final List<BlockPos> columns;
        private int laid;
        private int trees;

        private Grove(ResourceKey<Level> dimension, BlockPos origin, int radius, int relief, int trees) {
            this.dimension = dimension;
            this.origin = origin;
            this.radius = radius;
            this.relief = relief;
            this.trees = trees;
            this.columns = new ArrayList<>();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        this.columns.add(origin.offset(dx, 0, dz));
                    }
                }
            }
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            if (this.laid < this.columns.size()) {
                layGround(level);
                return false;
            }

            return plant(level);
        }

        /** The next slice of columns: soil up to the rolling surface, air above it. */
        private void layGround(ServerLevel level) {
            int end = Math.min(this.laid + COLUMNS_PER_TICK, this.columns.size());
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (; this.laid < end; this.laid++) {
                BlockPos column = this.columns.get(this.laid);
                if (!level.isLoaded(column)) {
                    continue;
                }

                int surface = surfaceAt(column);
                for (int y = surface - SOIL_DEPTH; y <= surface + CLEARANCE; y++) {
                    pos.set(column.getX(), y, column.getZ());
                    if (level.isOutsideBuildHeight(pos)
                            || level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }

                    BlockState laying = y < surface ? Blocks.DIRT.defaultBlockState()
                            : y == surface ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                    level.setBlock(pos, laying, FLAGS);
                }
            }
        }

        /**
         * Plants the next few trees. Each is the same random draw between shapes that worldgen makes,
         * so a grove comes out mixed; a spot the shape will not fit is simply skipped, which is what
         * vanilla does when a sapling has no room.
         *
         * @return true when the last tree has been attempted
         */
        private boolean plant(ServerLevel level) {
            Optional<Holder.Reference<ConfiguredFeature<?, ?>>> tree =
                    level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                            .getHolder(ModConfiguredFeatures.COMPRESSED_COBBLESTONE_TREE);
            if (tree.isEmpty()) {
                return true;
            }

            RandomSource random = level.random;
            for (int i = 0; i < TREES_PER_TICK && this.trees > 0; i++) {
                this.trees--;

                BlockPos column = this.columns.get(random.nextInt(this.columns.size()));
                BlockPos ground = new BlockPos(column.getX(), surfaceAt(column) + 1, column.getZ());
                if (level.isLoaded(ground)) {
                    tree.get().value().place(level, level.getChunkSource().getGenerator(), random, ground);
                }
            }

            return this.trees <= 0;
        }

        /** Where the soil surfaces in this column: the blast's own height, plus the rolling. */
        private int surfaceAt(BlockPos column) {
            double roll = Math.sin(column.getX() * RELIEF_SCALE) + Math.cos(column.getZ() * RELIEF_SCALE);
            return this.origin.getY() + (int) Math.round(roll / 2.0 * this.relief);
        }
    }

    /**
     * A fountain of arrows that climbs as it turns: every tick a few more go out, a little further
     * round the circle, a little faster and a little higher than the last. Five seconds of it leaves
     * a cone of arrows standing in the ground around wherever it went off.
     */
    private static final class ArrowSpiral implements Job {
        /** How far round the circle each tick moves. A shade under a right angle, so it never repeats. */
        private static final double TURN_PER_TICK = Math.toRadians(83.0);

        /** How far apart the arrows of one tick are spread around the circle. */
        private static final double SPOKE_SPREAD = Math.toRadians(360.0);

        private final ResourceKey<Level> dimension;
        private final Vec3 origin;
        private final int ticks;
        private final int arrowsPerTick;
        private final float startVelocity;
        private final float endVelocity;
        @Nullable
        private final LivingEntity owner;
        private int tick;

        private ArrowSpiral(ResourceKey<Level> dimension, Vec3 origin, int ticks, int arrowsPerTick,
                            float startVelocity, float endVelocity, @Nullable LivingEntity owner) {
            this.dimension = dimension;
            this.origin = origin;
            this.ticks = ticks;
            this.arrowsPerTick = arrowsPerTick;
            this.startVelocity = startVelocity;
            this.endVelocity = endVelocity;
            this.owner = owner;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            BlockPos pos = BlockPos.containing(this.origin);
            if (!level.isLoaded(pos)) {
                return true;
            }

            float progress = (float) this.tick / this.ticks;
            float velocity = Mth.lerp(progress, this.startVelocity, this.endVelocity);

            // The climb is what turns a ring into a spiral: flat at the start and steep by the end.
            double climb = progress;

            for (int spoke = 0; spoke < this.arrowsPerTick; spoke++) {
                double angle = this.tick * TURN_PER_TICK + spoke * SPOKE_SPREAD / this.arrowsPerTick;
                CompressedArrowEntity arrow = arrow(level, this.owner, this.origin);
                arrow.shoot(Math.cos(angle), climb, Math.sin(angle), velocity, 0.0F);
                level.addFreshEntity(arrow);
            }

            return ++this.tick >= this.ticks;
        }
    }

    /** One Super Compressed Arrow at {@code origin}, credited to {@code owner} where there is one. */
    private static CompressedArrowEntity arrow(ServerLevel level, @Nullable LivingEntity owner, Vec3 origin) {
        ItemStack ammo = new ItemStack(CompressedArrowTier.SUPER.item().get());
        CompressedArrowEntity arrow = owner != null
                ? new CompressedArrowEntity(CompressedArrowTier.SUPER, level, owner, ammo, null)
                : new CompressedArrowEntity(CompressedArrowTier.SUPER, level, origin.x, origin.y, origin.z, ammo, null);

        arrow.setPos(origin.x, origin.y, origin.z);
        return arrow;
    }

    /**
     * TNT falling out of the sky over an area for a few seconds. Each one is spawned already lit,
     * high enough to be seen coming and with a fuse short enough that it goes off around the time it
     * lands - a rain that arrived silently and sat there would be a minefield rather than a barrage.
     */
    private static final class TntRain implements Job {
        /** How far overhead each one appears. */
        private static final int DROP_HEIGHT = 24;

        /**
         * The fuse each is given, which is only a backstop: they are set to go off on landing, and
         * this is what happens to one that never finds ground - over a cliff, or in water. It has to
         * be longer than the fall or it would be the thing that decides when they go off: TNT takes
         * 44 ticks to fall {@link #DROP_HEIGHT} blocks, and a fuse under that is why this rain used
         * to burst in mid-air and throw its arrows across the sky instead of over the ground.
         */
        private static final int MIN_FUSE = 80;
        private static final int MAX_FUSE = 100;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final int radius;
        private final int ticks;

        /** The drops in the order they will fall: the caller's list, shuffled once, one TNT each. */
        private final List<Block> drops;
        @Nullable
        private final LivingEntity owner;
        private final float damageScale;
        private int tick;
        private int dropped;

        private TntRain(ResourceKey<Level> dimension, BlockPos origin, int radius, int ticks,
                        List<Block> drops, @Nullable LivingEntity owner, float damageScale) {
            this.dimension = dimension;
            this.origin = origin;
            this.radius = radius;
            this.ticks = Math.max(1, ticks);

            List<Block> shuffled = new ArrayList<>(drops);
            Collections.shuffle(shuffled);
            this.drops = List.copyOf(shuffled);

            this.owner = owner;
            this.damageScale = damageScale;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            this.tick++;

            // However many are due by now, less however many have already gone: the drops come out
            // evenly across the whole rain however long it was asked for.
            int due = this.drops.size() * this.tick / this.ticks;
            RandomSource random = level.random;

            while (this.dropped < due) {
                Block kind = this.drops.get(this.dropped);
                this.dropped++;

                double x = this.origin.getX() + 0.5 + (random.nextDouble() - 0.5) * this.radius * 2.0;
                double z = this.origin.getZ() + 0.5 + (random.nextDouble() - 0.5) * this.radius * 2.0;
                double y = this.origin.getY() + DROP_HEIGHT;
                if (!level.isLoaded(BlockPos.containing(x, y, z))) {
                    continue;
                }

                if (!(kind instanceof CompressedTntBlock tntBlock)) {
                    continue;
                }

                CompressedPrimedTntEntity tnt = new CompressedPrimedTntEntity(level, x, y, z, this.owner,
                        kind.defaultBlockState(), tntBlock.getEffect());
                tnt.setFuse(MIN_FUSE + random.nextInt(MAX_FUSE - MIN_FUSE));
                tnt.setDamageScale(this.damageScale);
                tnt.setDetonateOnLanding(true);
                level.addFreshEntity(tnt);
            }

            return this.tick >= this.ticks;
        }
    }

    /**
     * Neighbour updates are left off deliberately: these fills rewrite whole volumes at once, so
     * there is nothing for a neighbour to usefully react to, and asking for them would multiply the
     * cost of the very thing this class exists to spread out.
     */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /**
     * Cobwebs scattered through a cylinder six hundred and forty blocks in radius and twenty-one
     * deep - about twenty-seven million blocks, which is five thousand chunk columns' worth.
     * <p>
     * Every other job here walks its volume. This one cannot: at {@code ChunkFill}'s thousand blocks
     * a tick, reading twenty-seven million of them would take twenty-two minutes, and reading them
     * any faster is the stall the whole class exists to avoid. So it throws darts instead - a fixed
     * number of uniformly drawn positions per tick - which costs one block read per web attempted
     * rather than one per block in range. The price of sampling is that the same position can come
     * up twice; at this density that is a rounding error, and the second dart simply finds a web
     * already there.
     * <p>
     * Only air becomes a web. Underground the darts land in stone and miss, above the surface they
     * land in sky and hit, so what a player actually walks through is a thin haze of webs in the
     * open air - one in a few thousand blocks - and nothing is ever sealed in.
     */
    private static final class WebField implements Job {
        /** How far out it reaches: forty chunks, which is past any render distance in the settings. */
        private static final int RADIUS = 640;

        /** How deep the band is, centred on the blast: ten either way. */
        private static final int HALF_HEIGHT = 10;

        /**
         * How long the scattering goes on for, and how many darts are thrown on each of those
         * ticks. The two multiply out to twenty thousand darts into twenty-seven million blocks,
         * which is the density the whole thing is priced at: seven hundredths of a percent, or one
         * web per thirteen hundred blocks of open air. Sparse is the point - a web every few paces
         * is a wall, and this is meant to be something a player walks into once in a while.
         */
        private static final int TICKS = 500;
        private static final int DARTS_PER_TICK = 40;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private int tick;

        private WebField(ResourceKey<Level> dimension, BlockPos origin) {
            this.dimension = dimension;
            this.origin = origin;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            RandomSource random = level.random;
            BlockState web = Blocks.COBWEB.defaultBlockState();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int i = 0; i < DARTS_PER_TICK; i++) {
                // Uniform over the disc rather than over the square that contains it: the radius is
                // the square root of a flat draw, or every web crowds into the middle.
                double angle = random.nextDouble() * Math.PI * 2.0;
                double radius = Math.sqrt(random.nextDouble()) * RADIUS;

                pos.set(this.origin.getX() + (int) Math.round(Math.cos(angle) * radius),
                        this.origin.getY() + random.nextInt(HALF_HEIGHT * 2 + 1) - HALF_HEIGHT,
                        this.origin.getZ() + (int) Math.round(Math.sin(angle) * radius));

                // isLoaded rather than a chunk load: this reaches far past the chunks anyone is
                // looking at, and dragging them in to hang a web in each would be the whole world.
                if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
                        || !level.isLoaded(pos) || !level.getBlockState(pos).isAir()) {
                    continue;
                }

                level.setBlock(pos, web, FLAGS);
            }

            return ++this.tick >= TICKS;
        }
    }
    /**
     * Snow laid over whatever the ground turns out to be, a row of columns per tick.
     * <p>
     * It follows the surface rather than filling a slab, so it drapes over hills instead of burying
     * them: each column is resolved through the motion-blocking heightmap and the snow goes on top
     * of whatever that finds. Depth slides from a full block at the middle to a single dusting at
     * the rim, so the field has an edge rather than a wall.
     */
    private static final class Snowfield implements Job {
        /** How far out it reaches. */
        private static final int RADIUS = 24;

        /** How many rows of columns are laid each tick; the radius over this is how long it takes. */
        private static final int ROWS_PER_TICK = 2;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private int dx = -RADIUS;

        private Snowfield(ResourceKey<Level> dimension, BlockPos origin) {
            this.dimension = dimension;
            this.origin = origin;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            int last = Math.min(this.dx + ROWS_PER_TICK, RADIUS + 1);

            for (; this.dx < last; this.dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    double radius = Math.sqrt((double) this.dx * this.dx + (double) dz * dz);
                    if (radius > RADIUS) {
                        continue;
                    }

                    lay(level, this.origin.offset(this.dx, 0, dz), radius);
                }
            }

            return this.dx > RADIUS;
        }

        /** One column: the first free space above whatever is solid there, if snow will stand on it. */
        private void lay(ServerLevel level, BlockPos column, double radius) {
            if (!level.isLoaded(column)) {
                return;
            }

            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            BlockState existing = level.getBlockState(surface);
            if (!existing.isAir() && !existing.canBeReplaced()) {
                return;
            }

            // Vanilla's own test for whether snow will lie here, so it never hangs off a fence post
            // or floats over a hole in the ground.
            BlockState snow = Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, layersAt(radius));
            if (snow.canSurvive(level, surface)) {
                level.setBlock(surface, snow, FLAGS);
            }
        }

        /** Eight layers - a full block - in the middle, sliding down to one at the rim. */
        private static int layersAt(double radius) {
            return Mth.clamp(Math.round(Mth.lerp((float) (radius / RADIUS), 8.0F, 1.0F)), 1, 8);
        }
    }

    /**
     * A Menger sponge of ancient debris standing over the blast, with the ground under it turned to
     * Slippery Ice. One horizontal slice of the sponge per tick.
     * <p>
     * The sponge is the carpet's own rule in three dimensions, and it is worth the extra axis: a
     * carpet is a pattern you have to be above to see at all, while a sponge is a solid you can walk
     * around, look through in all three directions and climb into. Every level cuts the cube into
     * twenty-seven and throws away the middle of the cube and the middle of each of its six faces,
     * leaving twenty of the twenty-seven - so a sponge {@link #DEPTH} levels deep is {@code 20^DEPTH}
     * blocks and every one of its tunnels goes all the way through.
     * <p>
     * The width is not a free choice, for the same reason the carpet's was not: the smallest hole is
     * the width over three to the depth, and a hole under one block wide is not a hole. {@link #WIDTH}
     * is exactly {@code 3^DEPTH}, which lands the last level of detail on single blocks. Three levels
     * at 27 blocks is what a block grid can hold and what a fight can stand next to; a fourth would
     * mean 81 blocks on a side and 160,000 blocks of ancient debris, which is a mountain rather than
     * a sculpture.
     * <p>
     * Membership is read straight off the base-3 digits rather than by recursing: a cell is dropped
     * as soon as two of its three digits at some level are 1, which is the same statement as "it is
     * in the middle of the cube or the middle of a face at that level".
     */
    private static final class Fractal implements Job {
        /** How many times the cube is cut into twenty-seven. */
        private static final int DEPTH = 3;

        /** Three to the power of the depth: 27 blocks on every side. */
        private static final int WIDTH = 27;

        /** How high over the blast the sponge's underside sits. */
        private static final int HEIGHT = 4;

        /**
         * How many horizontal slices are laid each tick. One slice is {@code WIDTH * WIDTH} columns,
         * so a slice a tick is 729 block reads - well under what reads as a stall - and the whole
         * sponge stands in {@link #WIDTH} ticks.
         */
        private static final int LAYERS_PER_TICK = 1;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private int y;

        private Fractal(ResourceKey<Level> dimension, BlockPos origin) {
            this.dimension = dimension;
            this.origin = origin;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            BlockState debris = Blocks.ANCIENT_DEBRIS.defaultBlockState();
            BlockState ice = ModBlocks.SLIPPERY_ICE.get().defaultBlockState();
            int last = Math.min(this.y + LAYERS_PER_TICK, WIDTH);

            for (; this.y < last; this.y++) {
                for (int x = 0; x < WIDTH; x++) {
                    for (int z = 0; z < WIDTH; z++) {
                        BlockPos column = this.origin.offset(x - WIDTH / 2, 0, z - WIDTH / 2);
                        if (!level.isLoaded(column)) {
                            continue;
                        }

                        // The floor is laid on the first slice only: every column gets ice, sponge
                        // or no sponge, so the field underneath is a solid sheet and the pattern is
                        // entirely overhead. Doing it once rather than on all 27 slices is what
                        // keeps a slice cheap.
                        if (this.y == 0) {
                            BlockPos surface = level
                                    .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
                            if (level.getBlockState(surface).getDestroySpeed(level, surface) >= 0.0F) {
                                level.setBlock(surface, ice, FLAGS);
                            }
                        }

                        if (!inSponge(x, this.y, z)) {
                            continue;
                        }

                        BlockPos at = column.atY(this.origin.getY() + HEIGHT + this.y);
                        if (level.getBlockState(at).getDestroySpeed(level, at) >= 0.0F) {
                            level.setBlock(at, debris, FLAGS);
                        }
                    }
                }
            }

            return this.y >= WIDTH;
        }

        /** Whether {@code (x, y, z)} survives every level of the cut. */
        private static boolean inSponge(int x, int y, int z) {
            for (int i = 0; i < DEPTH; i++) {
                int middles = (x % 3 == 1 ? 1 : 0) + (y % 3 == 1 ? 1 : 0) + (z % 3 == 1 ? 1 : 0);
                if (middles >= 2) {
                    return false;
                }

                x /= 3;
                y /= 3;
                z /= 3;
            }

            return true;
        }
    }

    /**
     * A column of fluid one block wide, laid a block a tick from the ground upwards.
     * <p>
     * This is the clearest case in the class of spreading work being the <em>effect</em> rather than
     * a budget for it, the same way the Pinball TNT's bursts are. Forty blocks is nothing to write in
     * one tick - the chunk filler does twenty-five times that - but written all at once a geyser is
     * a column that simply exists, and written a block a tick it is something climbing out of the
     * ground. Two seconds is the whole of the difference.
     * <p>
     * The column is source blocks and nothing holds them in, which is deliberate: vanilla's own fluid
     * ticking takes over the moment each one is placed, so what a player actually sees is a spout
     * that climbs and a sheet that falls back off it. Nothing here has to model that.
     */
    private static final class Geyser implements Job {
        /** How many blocks of column go up each tick. One, because the climb is the point. */
        private static final int PER_TICK = 1;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final BlockState fluid;
        private final int height;
        private int laid;

        private Geyser(ResourceKey<Level> dimension, BlockPos origin, BlockState fluid, int height) {
            this.dimension = dimension;
            this.origin = origin;
            this.fluid = fluid;
            this.height = height;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            for (int i = 0; i < PER_TICK && this.laid < this.height; i++) {
                BlockPos pos = this.origin.above(this.laid++);
                if (pos.getY() >= level.getMaxBuildHeight() || !level.isLoaded(pos)) {
                    return true;
                }

                // Only where there is room. A geyser that ate its way through an overhang would be
                // a drill, and the hardened compression levels are unbreakable to everything - so
                // anything solid is passed over and the column carries on above it.
                BlockState existing = level.getBlockState(pos);
                if (existing.isAir() || existing.canBeReplaced()) {
                    // UPDATE_ALL rather than the class's usual quiet flags: this one wants its
                    // neighbour updates, because the flow off the column is the whole spectacle.
                    level.setBlock(pos, this.fluid, Block.UPDATE_ALL);
                }
            }

            return this.laid >= this.height;
        }
    }

    /**
     * A coral reef: a bowl of water dug out of whatever the blast landed on, with coral growing in
     * it. One row of columns per tick, the same shape of walk the snowfield does.
     * <p>
     * The bowl is the whole reason this is not simply the Pool TNT with decoration. A flat disc of
     * water one block deep has nowhere for coral to be - a reef needs depth, and depth on land means
     * digging rather than pouring. The floor is a paraboloid, deepest in the middle and reaching the
     * original ground level at the rim, so the water has a shore instead of a wall and the tallest
     * outcrops have room to stand without breaking the surface.
     * <p>
     * Coral is the one decoration in this class with a survival rule of its own: a coral plant or
     * fan out of water turns to its dead variant on the next random tick, and vanilla decides that
     * by reading the block's own {@code WATERLOGGED} property rather than by looking around it.
     * Every plant placed here therefore sets that property true - which is also what keeps the reef
     * alive if a player later drains it, since the plant is carrying its own water.
     */
    private static final class Reef implements Job {
        /** How far out the basin reaches. */
        private static final int RADIUS = 20;

        /** And how deep it is at the middle. The rim is at the height the TNT went off at. */
        private static final int DEPTH = 7;

        /** How many rows of columns are dug each tick; the width over this is how long it takes. */
        private static final int ROWS_PER_TICK = 2;

        /** How high the water stands over the rim, so the basin reads as full rather than as a hole. */
        private static final int SURFACE = 1;

        /** How much headroom over the water is cleared, so the surface is open sky and not a ceiling. */
        private static final int CLEARANCE = 2;

        /** The chance a column grows an outcrop, and how tall one may be. */
        private static final float OUTCROP_CHANCE = 0.14F;
        private static final int OUTCROP_MIN = 1;
        private static final int OUTCROP_MAX = 4;

        /** The chance a column with no outcrop still puts something on the floor. */
        private static final float FAN_CHANCE = 0.18F;

        /** And the chance the bed under the water is coral rock rather than whatever was there. */
        private static final float BED_CHANCE = 0.35F;

        /** The chance a cap or a floor decoration is the upright plant rather than the fan. */
        private static final float PLANT_CHANCE = 0.4F;

        /** The five colours, each as the three blocks vanilla gives it. */
        private static final List<Coral> CORALS = List.of(
                new Coral(Blocks.TUBE_CORAL_BLOCK, Blocks.TUBE_CORAL, Blocks.TUBE_CORAL_FAN),
                new Coral(Blocks.BRAIN_CORAL_BLOCK, Blocks.BRAIN_CORAL, Blocks.BRAIN_CORAL_FAN),
                new Coral(Blocks.BUBBLE_CORAL_BLOCK, Blocks.BUBBLE_CORAL, Blocks.BUBBLE_CORAL_FAN),
                new Coral(Blocks.FIRE_CORAL_BLOCK, Blocks.FIRE_CORAL, Blocks.FIRE_CORAL_FAN),
                new Coral(Blocks.HORN_CORAL_BLOCK, Blocks.HORN_CORAL, Blocks.HORN_CORAL_FAN));

        /** One colour of coral: the rock, the upright plant and the fan. */
        private record Coral(Block block, Block plant, Block fan) {
        }

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private int dx = -RADIUS;

        private Reef(ResourceKey<Level> dimension, BlockPos origin) {
            this.dimension = dimension;
            this.origin = origin;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            int last = Math.min(this.dx + ROWS_PER_TICK, RADIUS + 1);

            for (; this.dx < last; this.dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    double radius = Math.sqrt((double) this.dx * this.dx + (double) dz * dz);
                    if (radius > RADIUS) {
                        continue;
                    }

                    dig(level, this.origin.offset(this.dx, 0, dz), radius);
                }
            }

            return this.dx > RADIUS;
        }

        /** One column of the basin: the hole, the water in it, and whatever grows on the floor. */
        private void dig(ServerLevel level, BlockPos column, double radius) {
            if (!level.isLoaded(column)) {
                return;
            }

            // A paraboloid rather than a cone: the middle is a broad flat lagoon and the sides come
            // up quickly, which is the shape a reef ring actually has.
            double share = radius / RADIUS;
            int depth = (int) Math.round(DEPTH * (1.0 - share * share));
            BlockPos floor = column.below(depth);
            RandomSource random = level.random;

            // The bed the reef stands on, coloured in patches so the floor is not one flat grey.
            BlockPos bed = floor.below();
            if (breakable(level, bed) && !level.getBlockState(bed).isAir()
                    && random.nextFloat() < BED_CHANCE) {
                place(level, bed, coral(random).block().defaultBlockState());
            }

            for (int y = floor.getY(); y <= column.getY() + SURFACE; y++) {
                place(level, new BlockPos(column.getX(), y, column.getZ()),
                        Blocks.WATER.defaultBlockState());
            }

            // Open sky over the water. Without this a reef blasted under an overhang is a flooded
            // cave, which is a fine thing to be but is not what the fuse promised.
            for (int y = column.getY() + SURFACE + 1; y <= column.getY() + SURFACE + CLEARANCE; y++) {
                place(level, new BlockPos(column.getX(), y, column.getZ()),
                        Blocks.AIR.defaultBlockState());
            }

            grow(level, floor, column.getY() + SURFACE, random);
        }

        /**
         * What stands on this column's floor. An outcrop is a stack of coral rock with a plant on
         * top of it; anything else is at most a single fan on the floor itself. The stack is capped
         * below the surface so nothing pokes out of the water and dries.
         */
        private void grow(ServerLevel level, BlockPos floor, int surface, RandomSource random) {
            Coral coral = coral(random);

            if (random.nextFloat() < OUTCROP_CHANCE) {
                int height = Math.min(Mth.nextInt(random, OUTCROP_MIN, OUTCROP_MAX),
                        surface - floor.getY());

                for (int i = 0; i < height; i++) {
                    place(level, floor.above(i), coral.block().defaultBlockState());
                }

                if (height > 0) {
                    decorate(level, floor.above(height), coral, random);
                }

                return;
            }

            if (random.nextFloat() < FAN_CHANCE) {
                decorate(level, floor, coral, random);
            }
        }

        /** A plant or a fan, waterlogged either way so it does not die the moment it is placed. */
        private void decorate(ServerLevel level, BlockPos pos, Coral coral, RandomSource random) {
            Block chosen = random.nextFloat() < PLANT_CHANCE ? coral.plant() : coral.fan();
            BlockState state = chosen.defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, true);

            // Vanilla's own test, so nothing is left hanging where it could not have grown.
            if (breakable(level, pos) && state.canSurvive(level, pos)) {
                level.setBlock(pos, state, FLAGS);
            }
        }

        private static Coral coral(RandomSource random) {
            return CORALS.get(random.nextInt(CORALS.size()));
        }

        /** One block, if it is loaded and is not one of the levels nothing can move. */
        private static void place(ServerLevel level, BlockPos pos, BlockState state) {
            if (breakable(level, pos)) {
                level.setBlock(pos, state, FLAGS);
            }
        }

        private static boolean breakable(ServerLevel level, BlockPos pos) {
            return level.isLoaded(pos) && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0.0F;
        }
    }

    /**
     * A weak singularity: ten seconds of everything near a point being pulled towards it.
     * <p>
     * Two halves, and the second is what makes it read as a black hole rather than as a wind. The
     * pull is applied to every entity in range each tick, falling off with distance so the edge is
     * a drift and the middle is a fall - a player who commits to running gets out, which is the
     * whole of the counterplay. The other half tears the ground up: a handful of blocks a tick,
     * drawn as darts rather than walked (the sphere is some fourteen thousand blocks, and only a
     * few hundred of them are ever taken), each turned into a real {@link FallingBlockEntity} so
     * the pull then applies to it like anything else and it lands as a block again afterwards.
     * <p>
     * Nothing unbreakable is taken - the hardened compression levels and bedrock are read off the
     * block's own destroy speed rather than named - and nothing holding an inventory is, so a chest
     * near the blast keeps what is in it.
     */
    /**
     * The floor a boss brings with it: a disc of {@code RADIUS} blocks around where it landed, made
     * flat by two writes per column - the block under the fight's ground level filled in if there
     * is nothing there, and the {@link #CLEAR_HEIGHT} blocks above it taken away.
     * <p>
     * That is a hole filled and a hill removed in the same pass, which is what "flat" has to mean:
     * a platform laid over a ravine is a bridge, and one laid through a hillside is a tunnel, and
     * neither is a place to fight. Only the one layer of floor is filled - a hole ten blocks deep
     * still reads as a hole from underneath - because what the fight needs is a surface to stand on
     * and not a solid plug down to bedrock.
     * <p>
     * Nothing here is thrown away for free: the ground level is {@code origin.getY() - 1}, which is
     * the block the boss is standing on, so a player fighting on their own floor keeps it.
     * Unbreakable blocks are skipped the way every other job in this class skips them, so bedrock,
     * the world's ceiling and the mod's own hardened levels are not a way through the floor.
     * <p>
     * A disc of fifty is just under eight thousand columns and six writes apiece - close to fifty
     * thousand blocks, which is half a chunk fill and far past what a tick will take. It is walked
     * {@link #ROWS_PER_TICK} rows at a time, which puts it at about three thousand writes a tick and
     * a second for the whole platform: long enough to watch it happen, which is the point.
     */
    /**
     * The Flat TNT's disc: a floor laid across it and everything above that floor removed, all the
     * way to the world's ceiling.
     * <p>
     * The volume is what shapes the job. A disc of radius thirty is about two thousand eight hundred
     * columns, and a column runs the whole build height - some eight hundred thousand positions,
     * which is eight full chunk fills and two orders past what may be written on one tick. So the
     * work is cut into <em>layers</em>, {@value #LAYERS_PER_TICK} of them a tick, and the disc is
     * walked once per layer rather than the columns being walked once each.
     * <p>
     * Two orderings matter and neither is arbitrary. The floor is laid on the very first tick, in
     * one pass, so that nothing is ever standing over a hole waiting for the clear to reach it - the
     * ground arrives before the sky goes. The clear then runs <em>downwards</em> from the ceiling,
     * so what a player watching sees is the landscape being shaved off from the top; running upwards
     * would take the ground out from under a hill and leave it hanging until the job caught up.
     * <p>
     * Nothing is refused except what cannot be broken at all - bedrock, the world's ceiling and any
     * other mod's unbreakable block, all answered by a negative destroy speed, which is this mod's
     * test for that everywhere. Containers are <em>not</em> spared, unlike the Vapor Slash's carve:
     * this is a landscaping charge with a thirty block radius that a player places deliberately, not
     * an ability fired every second at head height.
     */
    private static final class Flatten implements Job {
        /**
         * Layers cleared per tick. The disc is about 2,800 columns, so this is that many block reads
         * a tick - a third of what reads as a stall, and the build height over it is how long the
         * whole thing takes: about two minutes at the world's full 384, though the great majority of
         * that is empty sky and passes in seconds of wall clock.
         */
        private static final int LAYERS_PER_TICK = 3;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final int radius;
        private final BlockState fill;

        /** The layer the clear is working on, counting down; the floor is laid before it starts. */
        private int y;

        private boolean floorLaid;

        private Flatten(ResourceKey<Level> dimension, BlockPos origin, int radius, BlockState fill, int top) {
            this.dimension = dimension;
            this.origin = origin;
            this.radius = radius;
            this.fill = fill;
            this.y = top;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            if (!this.floorLaid) {
                this.floorLaid = true;
                layer(level, this.origin.getY() - 1, this.fill);
                return false;
            }

            BlockState air = Blocks.AIR.defaultBlockState();
            int last = Math.max(this.y - LAYERS_PER_TICK, this.origin.getY() - 1);

            for (; this.y > last; this.y--) {
                layer(level, this.y, air);
            }

            return this.y <= this.origin.getY() - 1;
        }

        /** One horizontal slice of the disc, set to {@code state} wherever it may be written. */
        private void layer(ServerLevel level, int y, BlockState state) {
            int radiusSqr = this.radius * this.radius;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int x = -this.radius; x <= this.radius; x++) {
                for (int z = -this.radius; z <= this.radius; z++) {
                    if (x * x + z * z > radiusSqr) {
                        continue;
                    }

                    pos.set(this.origin.getX() + x, y, this.origin.getZ() + z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState here = level.getBlockState(pos);
                    if (here == state || (here.isAir() && state.isAir())) {
                        continue;
                    }

                    // A negative destroy speed is bedrock, the world's ceiling and this mod's own
                    // hardened levels - the one thing a flattening charge leaves standing.
                    if (here.getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }

                    level.setBlock(pos, state, FLAGS);
                }
            }
        }
    }

    private static final class Arena implements Job {
        /** How high above the floor is cleared. Five is two players deep and one jump over that. */
        private static final int CLEAR_HEIGHT = 5;

        /** Rows of the disc laid per tick; twice the radius over this is how long it takes. */
        private static final int ROWS_PER_TICK = 5;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final int radius;
        private int x;

        private Arena(ResourceKey<Level> dimension, BlockPos origin, int radius) {
            this.dimension = dimension;
            this.origin = origin;
            this.radius = radius;
            this.x = -radius;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            BlockState floor = Blocks.COBBLESTONE.defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            int last = Math.min(this.x + ROWS_PER_TICK, this.radius + 1);
            int radiusSqr = this.radius * this.radius;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (; this.x < last; this.x++) {
                for (int z = -this.radius; z <= this.radius; z++) {
                    if (this.x * this.x + z * z > radiusSqr) {
                        continue;
                    }

                    pos.set(this.origin.getX() + this.x, this.origin.getY() - 1, this.origin.getZ() + z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    // The floor. Anything that is not a full solid block - air, a hole, water, grass -
                    // becomes one; anything already solid is left exactly as the player left it.
                    BlockState under = level.getBlockState(pos);
                    if (!under.isFaceSturdy(level, pos, Direction.UP)
                            && under.getDestroySpeed(level, pos) >= 0.0F) {
                        level.setBlock(pos, floor, FLAGS);
                    }

                    // And everything standing on it.
                    for (int y = 0; y < CLEAR_HEIGHT; y++) {
                        pos.setY(this.origin.getY() + y);

                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F) {
                            level.setBlock(pos, air, FLAGS);
                        }
                    }
                }
            }

            return this.x > this.radius;
        }
    }

    private static final class Singularity implements Job {
        /** Ten seconds of it. */
        private static final int TICKS = 200;

        /** How far the pull reaches. */
        private static final double RADIUS = 12.0;

        /**
         * How hard it pulls at the very middle, in blocks per tick added to the delta each tick.
         * Weak by design: it beats a walk and loses to a sprint with a running start.
         */
        private static final double PULL = 0.12;

        /** How many blocks are torn up each tick. Two hundred over the whole ten seconds. */
        private static final int BLOCKS_PER_TICK = 1;

        /** How far out the ground is taken from - the pull's reach is wider than the digging. */
        private static final double DIG_RADIUS = 8.0;

        /** Nothing is dragged out of the last half block, or a mob at the middle jitters. */
        private static final double DEAD_ZONE = 0.5;

        private final ResourceKey<Level> dimension;
        private final Vec3 centre;
        private int ticks;

        private Singularity(ResourceKey<Level> dimension, Vec3 centre) {
            this.dimension = dimension;
            this.centre = centre;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            BlockPos origin = BlockPos.containing(this.centre);
            if (!level.isLoaded(origin)) {
                return true;
            }

            pull(level);
            dig(level);

            level.sendParticles(ParticleTypes.PORTAL, this.centre.x, this.centre.y, this.centre.z,
                    30, 1.5, 1.5, 1.5, 0.6);

            return ++this.ticks >= TICKS;
        }

        /** Everything in reach, moved a little further in. */
        private void pull(ServerLevel level) {
            AABB reach = new AABB(this.centre, this.centre).inflate(RADIUS);

            for (Entity entity : level.getEntities((Entity) null, reach, entity -> !entity.isSpectator())) {
                Vec3 toCentre = this.centre.subtract(entity.position());
                double distance = toCentre.length();
                if (distance < DEAD_ZONE || distance > RADIUS) {
                    continue;
                }

                double strength = PULL * (1.0 - distance / RADIUS);
                entity.setDeltaMovement(entity.getDeltaMovement().add(toCentre.scale(strength / distance)));

                // A player's own client is what actually moves them, so the server has to say so.
                // hurtMarked is what makes the tracker send the velocity it just set.
                entity.hurtMarked = true;
            }
        }

        /** A few blocks of the ground, thrown in after them. */
        private void dig(ServerLevel level) {
            RandomSource random = level.random;

            for (int i = 0; i < BLOCKS_PER_TICK; i++) {
                // A dart rather than a walk: the sphere is far too large to iterate, and what is
                // wanted from it is sparse. The cube root is what keeps the draw even through the
                // volume rather than crowding it into the middle.
                double radius = Math.cbrt(random.nextDouble()) * DIG_RADIUS;
                double theta = random.nextDouble() * Math.PI * 2.0;
                double y = random.nextDouble() * 2.0 - 1.0;
                double ring = Math.sqrt(1.0 - y * y);

                BlockPos pos = BlockPos.containing(
                        this.centre.x + radius * ring * Math.cos(theta),
                        this.centre.y + radius * y,
                        this.centre.z + radius * ring * Math.sin(theta));

                if (!level.isLoaded(pos)) {
                    continue;
                }

                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                        || state.getDestroySpeed(level, pos) < 0.0F) {
                    continue;
                }

                // fall() writes the air behind it, so the block is never in two places at once. It
                // is left as a plain falling block: it hurts nobody on the way down and lays itself
                // back as a block where it lands, which is what keeps this a shove rather than a
                // demolition.
                FallingBlockEntity.fall(level, pos, state);
            }
        }
    }

    /**
     * Lays a generated structure into the world a chunk at a time.
     * <p>
     * The {@link StructureStart} was worked out before this job existed, so the only thing left is
     * the writing - and vanilla already cuts that into chunks, one call per chunk of the bounding
     * box, which is why this walks chunks where every other job here walks layers or rows. There is
     * nothing finer available: a piece decides for itself what it puts in the chunk it is handed.
     * <p>
     * A chunk that is not loaded is skipped rather than waited for or dragged in, which is the same
     * rule the chunk fill follows - a structure whose far corner is past the edge of what anybody is
     * looking at comes out with that corner missing, and that is a better failure than paying to
     * load chunks nobody asked for. The job walks on regardless, because the far chunks of a
     * structure's bounding box are often empty of it anyway.
     */
    private static final class StructureBuild implements Job {
        /**
         * Chunks placed per tick. One, because a single chunk of a mansion or a monument is already
         * a few thousand block writes - the same budget the chunk fill runs at - and because a
         * building that assembles itself over a second or two reads as the effect rather than as a
         * stutter.
         */
        private static final int CHUNKS_PER_TICK = 1;

        private final ResourceKey<Level> dimension;
        private final StructureStart start;
        private final List<ChunkPos> chunks;
        private int next;

        private StructureBuild(ResourceKey<Level> dimension, StructureStart start) {
            this.dimension = dimension;
            this.start = start;

            BoundingBox box = start.getBoundingBox();
            ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()),
                    SectionPos.blockToSectionCoord(box.minZ()));
            ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()),
                    SectionPos.blockToSectionCoord(box.maxZ()));

            this.chunks = ChunkPos.rangeClosed(min, max).toList();
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            int last = Math.min(this.next + CHUNKS_PER_TICK, this.chunks.size());

            for (; this.next < last; this.next++) {
                ChunkPos chunk = this.chunks.get(this.next);
                if (!level.hasChunk(chunk.x, chunk.z)) {
                    continue;
                }

                // The whole column of the chunk, which is what /place hands it: the piece clips
                // itself to its own bounding box inside that.
                this.start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                        new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                                chunk.getMaxBlockX(), level.getMaxBuildHeight(), chunk.getMaxBlockZ()),
                        chunk);
            }

            return this.next >= this.chunks.size();
        }
    }

    /**
     * Full-height columns at scattered points of a disc.
     * <p>
     * Which columns is decided once, in the constructor, and it is decided by rolling every column
     * in the disc rather than by throwing darts - the two are different things and only the roll
     * gives an honest one-in-{@code chance}. It is affordable here where the dart rule would
     * normally apply because a roll is only a random number: no block is read to decide, so a disc
     * of fifty is eight thousand calls to the RNG and no world access at all. The darts in the web
     * field exist to avoid *reads*, and there are none to avoid here.
     * <p>
     * Placing is the expensive half and is what the ticks are for. A pillar is the whole height of
     * the world - three hundred and eighty-four blocks in the overworld - so at
     * {@link #COLUMNS_PER_TICK} the job runs at about the chunk fill's thousand writes a tick, and
     * the pillars visibly grow in one after another rather than appearing together.
     */
    private static final class Pillars implements Job {
        /** Pillars raised per tick. Each is a full world column, so this is the whole budget. */
        private static final int COLUMNS_PER_TICK = 3;

        private final ResourceKey<Level> dimension;
        private final BlockState fill;
        private final List<BlockPos> columns;
        private int next;

        private Pillars(ResourceKey<Level> dimension, BlockPos origin, int radius, int chance,
                        BlockState fill, RandomSource random) {
            this.dimension = dimension;
            this.fill = fill;

            List<BlockPos> columns = new ArrayList<>();
            int radiusSqr = radius * radius;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radiusSqr && random.nextInt(chance) == 0) {
                        columns.add(new BlockPos(origin.getX() + x, 0, origin.getZ() + z));
                    }
                }
            }

            this.columns = columns;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            int last = Math.min(this.next + COLUMNS_PER_TICK, this.columns.size());
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (; this.next < last; this.next++) {
                BlockPos column = this.columns.get(this.next);

                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    pos.set(column.getX(), y, column.getZ());
                    if (!level.isLoaded(pos)) {
                        break;
                    }

                    // Bedrock, the world's ceiling and the hardened levels are skipped the way every
                    // other job here skips them: a pillar is not a way through them.
                    if (level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }

                    level.setBlock(pos, this.fill, FLAGS);
                }
            }

            return this.next >= this.columns.size();
        }
    }

    /**
     * A dome: the shell of a hemisphere, laid one horizontal ring at a time from the ground up.
     * <p>
     * Only the shell is written, which is what makes this affordable at all - a solid hemisphere of
     * radius R is two thirds of pi R cubed, some sixty thousand blocks at radius 30, where its shell
     * is nearer six thousand. The ring for a given height is every column whose distance from the
     * middle falls inside a band {@value #THICKNESS} blocks wide, and the band is measured on the
     * sphere's radius rather than on the horizontal one, so the shell keeps its thickness all the way
     * over the top instead of thinning to nothing at the crown and flaring out at the base.
     * <p>
     * It writes over whatever is there, air included, and skips only what the world refuses to let
     * go of - bedrock, the world's ceiling and this mod's own hardened tiers, all of which read as a
     * destroy speed below zero. A dome built into a hillside is therefore a dome with a hill inside
     * it, which is the honest result: this lays a shell, it does not clear a room.
     */
    private static final class Dome implements Job {
        /** How thick the shell is. One block; a dome is a roof, not a bunker. */
        private static final int THICKNESS = 1;

        /** Rings per tick. One, because a ring at radius 30 is already a few hundred columns. */
        private static final int RINGS_PER_TICK = 1;

        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final int radius;
        private final BlockState fill;
        private int dy;

        private Dome(ResourceKey<Level> dimension, BlockPos origin, int radius, BlockState fill) {
            this.dimension = dimension;
            this.origin = origin;
            this.radius = radius;
            this.fill = fill;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int ring = 0; ring < RINGS_PER_TICK && this.dy <= this.radius; ring++, this.dy++) {
                layer(level, pos);
            }

            return this.dy > this.radius;
        }

        /** One horizontal ring: every column of the shell at this height. */
        private void layer(ServerLevel level, BlockPos.MutableBlockPos pos) {
            // How far out the shell sits at this height, and how far out its inner face does. Both
            // come off the sphere, so the band between them is THICKNESS thick along the surface.
            double outer = this.radius;
            double inner = this.radius - THICKNESS;
            double outerRingSq = outer * outer - (double) this.dy * this.dy;
            double innerRingSq = inner * inner - (double) this.dy * this.dy;
            if (outerRingSq < 0.0) {
                return;
            }

            int reach = Mth.ceil(Math.sqrt(outerRingSq));

            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double flatSq = (double) dx * dx + (double) dz * dz;
                    if (flatSq > outerRingSq || flatSq < innerRingSq) {
                        continue;
                    }

                    pos.set(this.origin.getX() + dx, this.origin.getY() + this.dy,
                            this.origin.getZ() + dz);

                    if (!level.isLoaded(pos)
                            || level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }

                    level.setBlock(pos, this.fill, FLAGS);
                }
            }
        }
    }

    /**
     * How wide a hole a Singularity's blast leaves, in blocks. Not the blast's own power.
     * <p>
     * The power and the hole cannot be the same number. A blast of the Singularity's power would be a
     * hole tens of thousands of blocks across; there is no writing that, deferred or otherwise. So the
     * blast's <em>damage</em> is the full figure - vanilla's falloff is linear in the radius and
     * reaches twice it, which is already the whole dimension - and the hole is a taste decision: a
     * quarter of a kilometre across, nine million positions, and a good minute of {@link Crater}
     * walking it layer by layer.
     */
    private static final int SINGULARITY_CRATER_RADIUS = 128;

    /**
     * The one explosion a Singularity ends on, and the hole it leaves.
     * <p>
     * Fired as a real explosion, so the damage, the shove, the particles and the noise are all
     * vanilla's own and everything with an opinion about an explosion has it - armour, Protection,
     * Blast Protection, {@code EXPLOSION_KNOCKBACK_RESISTANCE} and any other mod's reduction.
     * <p>
     * The blocks cannot be done by explosion, for two separate reasons. Every compression level from
     * {@code ModBlocks.HARDENED_LEVEL} up carries an explosion resistance nothing gets through, and
     * vanilla's block phase at this radius is hundreds of millions of block reads. So the calculator
     * refuses the block phase outright - a resistance nothing survives kills every ray on its first
     * step - and the hole is taken afterwards by {@link Crater}.
     */
    public static void detonateSingularity(ServerLevel level, Vec3 centre, float power) {
        ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter reader,
                                                               BlockPos pos, BlockState state,
                                                               FluidState fluid) {
                return Optional.of(Float.MAX_VALUE);
            }

            @Override
            public boolean shouldBlockExplode(Explosion explosion, BlockGetter reader, BlockPos pos,
                                              BlockState state, float power) {
                return false;
            }
        };

        // The mod's own type rather than minecraft:explosion: it is worth several hundred thousand
        // points, so something has to be able to say "not that one" about it without saying it
        // about every creeper.
        level.explode(null, level.damageSources().source(ModDamageTypes.SINGULARITY), calculator,
                centre.x, centre.y, centre.z, power, false, Level.ExplosionInteraction.NONE);

        queueCrater(level, centre, SINGULARITY_CRATER_RADIUS);
    }

    /**
     * The hole a Singularity leaves: everything inside a sphere that can be taken at all, taken.
     * <p>
     * The layers are walked from the bottom up so the hole opens rather than caves, but a layer is
     * no longer a tick's work: the sphere is 256 blocks across and some nine million positions, and
     * its widest layer alone is fifty thousand - five times what reads as a freeze. So the job
     * carries a block budget instead and remembers which row of which layer it stopped on, which
     * makes the crater's size a free choice rather than something the tick budget decides. It takes
     * around a minute of real time to open, and that is the right shape for it: a hole that appears
     * instantly is a command, and a hole that spreads is a collapse.
     * <p>
     * Nothing drops. This is the one job in the file with no ceiling on what it takes:
     * {@code getDestroySpeed} below zero is bedrock, the world's roof and every other mod's
     * unbreakable block, and everything else goes - the hardened compression levels included, which
     * nothing else in the game moves. That is what tier 254 is: the Exploding Sword engraving cuts
     * off at a level and this deliberately does not.
     * <p>
     * Written with {@link #FLAGS} the way the Flatten job writes its air, rather than through
     * {@code destroyBlock}: at nine million positions the difference between a plain write
     * and a full break - neighbour updates, particles, a loot roll to suppress - is the difference
     * between a job and a stall. A container inside the sphere therefore goes with it rather than
     * spilling, which is what flattening already does and is the honest answer here anyway: what
     * this leaves is a hole, not a demolition site.
     */
    private static final class Crater implements Job {
        /**
         * How many positions are walked each tick. Well under the ten thousand that reads as a
         * freeze, because unlike most jobs here nearly every position walked is also a write.
         */
        private static final int BLOCKS_PER_TICK = 8000;

        private final ResourceKey<Level> dimension;
        private final BlockPos centre;
        private final int radius;

        /** Where the last tick stopped: the layer, and the row within it. */
        private int layer;
        private int row;

        private Crater(ResourceKey<Level> dimension, BlockPos centre, int radius) {
            this.dimension = dimension;
            this.centre = centre;
            this.radius = radius;
            this.layer = -radius;
            this.row = Integer.MIN_VALUE;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public boolean advance(ServerLevel level) {
            if (!level.isLoaded(this.centre)) {
                return true;
            }

            BlockState air = Blocks.AIR.defaultBlockState();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int budget = BLOCKS_PER_TICK;

            while (budget > 0) {
                if (this.layer > this.radius) {
                    return true;
                }

                // The half-width of this layer's disc, off the sphere's own radius: a layer near
                // the top or the bottom is a small circle, and only the middle one is full width.
                double reach = Math.sqrt((double) this.radius * this.radius - (double) this.layer * this.layer);
                int span = Mth.floor(reach);
                double spanSqr = reach * reach;

                // A layer only just begun starts at its own left edge; one resumed carries on from
                // wherever the budget ran out last tick.
                if (this.row == Integer.MIN_VALUE) {
                    this.row = -span;
                }

                for (; this.row <= span && budget > 0; this.row++) {
                    int dx = this.row;
                    for (int dz = -span; dz <= span; dz++) {
                        if (dx * dx + dz * dz > spanSqr) {
                            continue;
                        }

                        budget--;
                        pos.set(this.centre.getX() + dx, this.centre.getY() + this.layer,
                                this.centre.getZ() + dz);
                        if (!level.isLoaded(pos)) {
                            continue;
                        }

                        BlockState state = level.getBlockState(pos);
                        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
                            continue;
                        }

                        level.setBlock(pos, air, FLAGS);
                    }
                }

                if (this.row > span) {
                    this.layer++;
                    this.row = Integer.MIN_VALUE;
                }
            }

            return this.layer > this.radius;
        }
    }
}
