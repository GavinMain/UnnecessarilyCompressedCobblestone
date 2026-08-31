package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPrimedTntEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowTier;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedTntEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A volley of lightning, laid down one bolt per tick.
 * <p>
 * Multibolt turns a single cast into several strikes, and dropping them all on the tick the cast
 * ends would read as one bolt rather than as several - they would share a frame, a sound and a
 * flash. A bolt a tick makes the volley audible and gives whatever is being struck a moment between
 * hits. Volleys live only as long as the server runs and are dropped on shutdown, the same way
 * {@link DeferredFill}'s jobs are.
 */
public final class DeferredStrikes {
    private static final List<Job> VOLLEYS = new ArrayList<>();

    private DeferredStrikes() {
    }

    /**
     * Queues {@code bolts} strikes on {@code pos}, the first of them on the next tick.
     *
     * @param interval ticks between bolts, which is how Multicast turns into a rate
     * @param cause    the caster, so a kill is credited to them
     */
    public static void queue(ServerLevel level, BlockPos pos, float damage, int bolts, int interval, Player cause) {
        if (bolts > 0) {
            VOLLEYS.add(new Volley(level.dimension(), pos, damage, bolts, Math.max(1, interval), cause));
        }
    }

    /**
     * Rains arrows onto {@code target} wherever it goes. They are dropped from overhead rather than
     * fired at it, so they land on whatever it is standing on if it steps aside in time.
     *
     * @param interval ticks between arrows
     * @param cause    whoever marked the target, so a kill is credited to them
     */
    public static void queueArrowRain(ServerLevel level, LivingEntity target, int arrows, int interval,
                                      float velocity, @Nullable LivingEntity cause) {
        if (arrows > 0) {
            VOLLEYS.add(new ArrowRain(level.dimension(), target, arrows, Math.max(1, interval), velocity, cause));
        }
    }

    /**
     * Drops lit Arrow TNT onto {@code target} wherever it goes, within a few blocks of it and always
     * from above, each going off where it lands. Nothing it drops breaks ground, so the whole of
     * every blast lands on whoever was marked.
     *
     * @param blastScale what each explosion is worth
     * @param cause      whoever marked the target, so a kill is credited to them
     */
    public static void queueTntRain(ServerLevel level, LivingEntity target, int count, int interval,
                                    float blastScale, @Nullable LivingEntity cause, boolean spiral) {
        if (count > 0) {
            VOLLEYS.add(new TntRain(level.dimension(), target, count, Math.max(1, interval), blastScale, cause, spiral));
        }
    }

    /**
     * Plays a song out of the sky at {@code origin}: one bolt per note, at the note's own volume.
     * <p>
     * Nothing about it is tied to whoever set it off. It is anchored to the ground it started on and
     * keeps playing whether or not that player is still alive, and stops only when the song ends or
     * the chunk it is standing on unloads.
     */
    public static void queueSong(ServerLevel level, BlockPos origin, LightningSong song, double radius) {
        if (!song.notes().isEmpty()) {
            VOLLEYS.add(new Song(level.dimension(), origin, song, radius));
        }
    }

    /** Advances every job belonging to this level by one tick. */
    public static void tick(ServerLevel level) {
        if (VOLLEYS.isEmpty()) {
            return;
        }

        Iterator<Job> jobs = VOLLEYS.iterator();
        while (jobs.hasNext()) {
            Job job = jobs.next();
            if (job.dimension.equals(level.dimension()) && job.advance(level)) {
                jobs.remove();
            }
        }
    }

    /** Dropped on shutdown so a volley cannot outlive the level it was striking. */
    public static void clear() {
        VOLLEYS.clear();
    }

    /** One piece of scheduled work: a bolt or an arrow at a time, until it is spent. */
    private abstract static class Job {
        private final ResourceKey<Level> dimension;
        private final int interval;
        private int cooldown;
        int remaining;

        private Job(ResourceKey<Level> dimension, int count, int interval) {
            this.dimension = dimension;
            this.remaining = count;
            this.interval = interval;
        }

        private boolean advance(ServerLevel level) {
            if (this.cooldown-- > 0) {
                return false;
            }

            this.cooldown = this.interval - 1;
            return step(level);
        }

        /** @return true when the job is spent and should be dropped */
        abstract boolean step(ServerLevel level);
    }

    private static final class Volley extends Job {
        private final BlockPos pos;
        private final float damage;
        private final Player cause;

        private Volley(ResourceKey<Level> dimension, BlockPos pos, float damage, int bolts, int interval, Player cause) {
            super(dimension, bolts, interval);
            this.pos = pos;
            this.damage = damage;
            this.cause = cause;
        }

        @Override
        boolean step(ServerLevel level) {
            // Ground that has unloaded since the cast is ground the rest of the volley skips, rather
            // than being dragged back into memory a bolt at a time.
            if (!level.isLoaded(this.pos)) {
                return true;
            }

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                return true;
            }

            bolt.moveTo(this.pos.getX() + 0.5, this.pos.getY(), this.pos.getZ() + 0.5);
            bolt.setDamage(this.damage);
            if (this.cause instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                bolt.setCause(serverPlayer);
            }

            level.addFreshEntity(bolt);
            return --this.remaining <= 0;
        }
    }

    /**
     * A song played in lightning, and in nothing else.
     * <p>
     * One bolt per note, and the note is the bolt: lightning has no pitch to give - vanilla rolls a
     * fresh random one per bolt and nothing can set it - so a tune written for it is a rhythm and a
     * dynamic line played on one note, and how loud the bolt is is the whole of the performance.
     * That is why the score behind {@link LightningSong} is a one-note reduction, and why nothing but
     * lightning sounds here.
     * <p>
     * Every bolt is a real one - it burns what it lands on and kills what is standing there. A song
     * is over a thousand strikes long, so the ring is not a place to listen from so much as a place
     * to have been.
     * <p>
     * The ring the bolts fall in is deliberately tight. Below a volume of 1 the sound engine carries
     * a sound a flat sixteen blocks and fades it linearly over that distance, so the further apart
     * the bolts land the more of the song's dynamics is lost to where the listener happens to be
     * standing. At {@link CompressedTntEffect#SONG_RADIUS} the worst a note can lose is a quarter of
     * its volume, which is less than the step between two dynamic marks.
     */
    private static final class Song extends Job {
        private final BlockPos origin;
        private final LightningSong song;
        private final double radius;
        private int tick;
        private int next;

        private Song(ResourceKey<Level> dimension, BlockPos origin, LightningSong song, double radius) {
            // Every tick, because the song decides for itself which of them a note falls on.
            super(dimension, song.notes().size(), 1);
            this.origin = origin;
            this.song = song;
            this.radius = radius;
        }

        @Override
        boolean step(ServerLevel level) {
            if (!level.isLoaded(this.origin)) {
                return true;
            }

            RandomSource random = level.random;
            List<LightningSong.Note> notes = this.song.notes();

            // Everything due on this tick, since two notes can fall on the same one.
            while (this.next < notes.size() && notes.get(this.next).tick() <= this.tick) {
                LightningSong.Note note = notes.get(this.next++);

                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = Math.sqrt(random.nextDouble()) * this.radius;
                double x = this.origin.getX() + 0.5 + Math.cos(angle) * distance;
                double z = this.origin.getZ() + 0.5 + Math.sin(angle) * distance;
                BlockPos pos = BlockPos.containing(x, this.origin.getY(), z);
                if (!level.isLoaded(pos)) {
                    continue;
                }

                VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
                if (bolt == null) {
                    continue;
                }

                bolt.moveTo(x, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY(), z);
                bolt.setVolume(note.volume());
                level.addFreshEntity(bolt);
            }

            // Counted outside the return: behind a short-circuiting && it would only ever advance
            // once the song had run out of notes, which is to say never.
            this.tick++;
            return this.next >= notes.size() && this.tick > this.song.lengthTicks();
        }
    }

    /**
     * Arrow TNT dropped onto whatever the Arrow TNT Staff marked, the Spiral engraving included -
     * that only picks which of the two TNTs falls. Each one goes off the moment it lands: an Arrow
     * TNT throws its arrows outwards from wherever it burst, so one that goes off on the way down
     * throws them out of the sky, across an area far too wide to be an attack.
     */
    private static final class TntRain extends Job {
        /** How far above the target each one appears. */
        private static final double HEIGHT = 10.0;

        /** How far from the target it may land. Always above, never further out than this. */
        private static final double SPREAD = 4.0;

        /**
         * Only a backstop, for TNT that finds no ground - over a cliff, or in water. It has to be
         * longer than the fall or it would be the thing deciding when the blast happens: TNT takes
         * 29 ticks to fall the {@link #HEIGHT} blocks this drops it from.
         */
        private static final int FUSE = 80;

        private final LivingEntity target;
        private final float blastScale;
        @Nullable
        private final LivingEntity cause;

        /** Whether the Spiral engraving is on the staff, which is all that picks the TNT below. */
        private final boolean spiral;

        private TntRain(ResourceKey<Level> dimension, LivingEntity target, int count, int interval,
                        float blastScale, @Nullable LivingEntity cause, boolean spiral) {
            super(dimension, count, interval);
            this.target = target;
            this.blastScale = blastScale;
            this.cause = cause;
            this.spiral = spiral;
        }

        @Override
        boolean step(ServerLevel level) {
            if (!this.target.isAlive() || this.target.isRemoved()) {
                return true;
            }

            RandomSource random = level.random;
            double angle2 = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * SPREAD;
            double x = this.target.getX() + Math.cos(angle2) * distance;
            double z = this.target.getZ() + Math.sin(angle2) * distance;
            double y = this.target.getY() + HEIGHT;
            if (!level.isLoaded(BlockPos.containing(x, y, z))) {
                return true;
            }

            CompressedTntBlock block = (CompressedTntBlock)
                    (this.spiral ? ModBlocks.ARROW_SPIRAL_TNT.get() : ModBlocks.ARROW_TNT.get());
            CompressedPrimedTntEntity tnt = new CompressedPrimedTntEntity(level, x, y, z, this.cause,
                    block.defaultBlockState(), block.getEffect());
            tnt.setFuse(FUSE);
            tnt.setDamageScale(this.blastScale);
            tnt.setDetonateOnLanding(true);
            level.addFreshEntity(tnt);

            return --this.remaining <= 0;
        }
    }

    /**
     * Arrows dropped onto whatever the Arrow Staff marked, for as long as the mark lasts. They are
     * spawned well overhead and thrown straight down: the target can step out from under them, which
     * is the only answer there is to being marked.
     */
    private static final class ArrowRain extends Job {
        /** How far above the target each arrow appears. */
        private static final double HEIGHT = 12.0;

        /** How wide they scatter, so a rain reads as a rain rather than as one arrow repeated. */
        private static final double SPREAD = 1.5;

        private final LivingEntity target;
        private final float velocity;
        @Nullable
        private final LivingEntity cause;

        private ArrowRain(ResourceKey<Level> dimension, LivingEntity target, int arrows, int interval,
                          float velocity, @Nullable LivingEntity cause) {
            super(dimension, arrows, interval);
            this.target = target;
            this.velocity = velocity;
            this.cause = cause;
        }

        @Override
        boolean step(ServerLevel level) {
            if (!this.target.isAlive() || this.target.isRemoved()) {
                return true;
            }

            RandomSource random = level.random;
            double x = this.target.getX() + (random.nextDouble() - 0.5) * SPREAD;
            double z = this.target.getZ() + (random.nextDouble() - 0.5) * SPREAD;
            BlockPos above = BlockPos.containing(x, this.target.getY() + HEIGHT, z);
            if (!level.isLoaded(above)) {
                return true;
            }

            CompressedArrowEntity arrow = this.cause != null
                    ? new CompressedArrowEntity(CompressedArrowTier.SUPER, level, this.cause,
                            new ItemStack(CompressedArrowTier.SUPER.item().get()), null)
                    : new CompressedArrowEntity(CompressedArrowTier.SUPER, level, x, above.getY(), z,
                            new ItemStack(CompressedArrowTier.SUPER.item().get()), null);

            arrow.setPos(x, this.target.getY() + HEIGHT, z);
            arrow.shoot(0.0, -1.0, 0.0, this.velocity, 0.0F);
            level.addFreshEntity(arrow);

            return --this.remaining <= 0;
        }
    }
}
