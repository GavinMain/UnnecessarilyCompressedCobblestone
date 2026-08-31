package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.worldgen.ModConfiguredFeatures;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowTier;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneChickenEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPrimedTntEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
}
