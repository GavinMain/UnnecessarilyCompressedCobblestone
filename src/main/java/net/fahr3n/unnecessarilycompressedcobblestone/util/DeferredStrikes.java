package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;

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
     * @param effects  whether each arrow carries a random harmful effect, which is the Arrow 2
     *                 engraving and the one thing that changes about this rain
     */
    public static void queueArrowRain(ServerLevel level, LivingEntity target, int arrows, int interval,
                                      float velocity, @Nullable LivingEntity cause, boolean effects) {
        if (arrows > 0) {
            VOLLEYS.add(new ArrowRain(level.dimension(), target, arrows, Math.max(1, interval), velocity,
                    cause, effects));
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
        queueSong(level, origin, song, radius, 1.0F);
    }

    /**
     * The same, played at {@code volumeScale} of what is written - which is how the Lightning Core
     * turns its redstone signal into a dynamic mark over a whole performance.
     * <p>
     * A song already playing at this spot is left alone rather than joined. The core strikes once a
     * second for as long as it is powered, and a song is minutes long: without this, holding a lever
     * down would start a fresh performance a second until they were piled up beyond hearing.
     */
    public static void queueSong(ServerLevel level, BlockPos origin, LightningSong song, double radius,
                                 float volumeScale) {
        if (!isPlayingAt(level, origin)) {
            playSong(level, origin, song, radius, volumeScale, DEFAULT_DAMAGE);
        }
    }

    /** What a song's bolts hit for when nothing names a figure: vanilla lightning's own. */
    public static final float DEFAULT_DAMAGE = -1.0F;

    /**
     * A song with no guard against one already playing, for a bolt's own strike. A shot bolt is one
     * strike per shot, so two shots at the same target are two performances; the repeating trigger
     * that needs the guard - the Lightning Core - checks {@link #isPlayingAt} itself.
     *
     * @param radius how wide the notes scatter. At 0 every note lands exactly on {@code origin},
     *               at its own height rather than on the ground under it, which is what a bolt that
     *               hit something in the air needs
     * @param damage what each note's bolt hits for, or {@link #DEFAULT_DAMAGE} for vanilla's
     */
    public static void playSong(ServerLevel level, BlockPos origin, LightningSong song, double radius,
                                float volumeScale, float damage) {
        if (!song.notes().isEmpty()) {
            VOLLEYS.add(new Song(level.dimension(), origin, song, radius, volumeScale, damage));
        }
    }

    /** Whether a song is already being played from {@code origin}. */
    public static boolean isPlayingAt(ServerLevel level, BlockPos origin) {
        return VOLLEYS.stream().anyMatch(job -> job instanceof Song song
                && song.dimension.equals(level.dimension()) && song.origin.equals(origin));
    }

    /**
     * Half a minute of wind bursts around {@code origin}: rings of them, a few ticks apart, widening
     * outwards in waves and climbing from the ground as they go, so whatever is caught is herded
     * back to the middle and lifted.
     * <p>
     * It is laid down over time for the effect rather than for the tick budget: a hundred bursts at
     * once is one enormous shove and then stillness, while the same hundred spread over thirty
     * seconds is a ball in play - every landing is somebody else's serve. Nothing here breaks a
     * block or deals a point of damage; what it costs a player is entirely where they end up.
     *
     * @param duration how long it goes on for, in ticks
     * @param radius   how wide a ring grows before the next wave starts, and what the climb is scaled by
     */
    public static void queuePinball(ServerLevel level, BlockPos origin, int duration, double radius) {
        if (duration > 0) {
            VOLLEYS.add(new Pinball(level.dimension(), origin, duration, radius));
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
        final ResourceKey<Level> dimension;
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
        final BlockPos origin;
        private final LightningSong song;
        private final double radius;
        private final float volumeScale;
        private final float damage;
        private int tick;
        private int next;

        private Song(ResourceKey<Level> dimension, BlockPos origin, LightningSong song, double radius,
                     float volumeScale, float damage) {
            // Every tick, because the song decides for itself which of them a note falls on.
            super(dimension, song.notes().size(), 1);
            this.origin = origin;
            this.song = song;
            this.radius = radius;
            this.volumeScale = volumeScale;
            this.damage = damage;
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

                // A scattered song finds the ground under each note; one aimed at a single point
                // lands on that point, so a bolt that hit something in the air still hits it.
                double y = this.radius > 0.0
                        ? level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY()
                        : this.origin.getY();
                bolt.moveTo(x, y, z);
                if (this.damage >= 0.0F) {
                    bolt.setDamage(this.damage);
                }

                float volume = note.volume() * this.volumeScale;
                if (note.key() == VanillaLightningBoltEntity.NO_NOTE) {
                    bolt.setVolume(volume);
                } else {
                    bolt.setNote(note.key(), volume);
                }

                level.addFreshEntity(bolt);
            }

            // Counted outside the return: behind a short-circuiting && it would only ever advance
            // once the song had run out of notes, which is to say never.
            this.tick++;
            return this.next >= notes.size() && this.tick > this.song.lengthTicks();
        }
    }

    /**
     * The Pinball TNT's half-minute of bursts. See {@link #queuePinball}.
     * <p>
     * Bursts are let off in <em>rings</em> around the origin, several evenly spaced at once, and the
     * ring widens from {@link #MIN_RING} out to the job's radius over {@link #WAVE_TICKS} before
     * starting small again. An explosion shoves directly away from itself, so a ring shoves
     * everything inside it towards its middle - the pushes from opposite sides cancel sideways and
     * the nearer side wins - and everything outside it further out. Scattering single bursts at
     * random, as this used to, threw a player out of the field on the first hit and never reached
     * them again. A widening ring catches whoever was thrown out and, once it is past them, throws
     * them back in; repeating the wave means that happens again and again for the whole fuse.
     * <p>
     * The rings start on the ground and climb over the whole duration, {@link #RISE} blocks in all. A burst below a player throws them upwards, so a field whose bursts climb keeps
     * lifting whoever it has caught rather than pinning them to the floor.
     * <p>
     * The stagger is still drawn rather than fixed - a gap of {@link #MIN_GAP} to {@link #MAX_GAP}
     * ticks between rings, a fresh strength and a fresh rotation for each - because a fixed rate
     * reads as a machine and a rate that is never quite the same reads as a ball still bouncing.
     * <p>
     * Every burst is a {@link SimpleExplosionDamageCalculator} that breaks nothing and hurts
     * nothing. Knockback is applied outside the damage check in {@code Explosion.explode}, so an
     * explosion that damages nobody still throws everybody - which is the only thing this wants.
     */
    private static final class Pinball extends Job {
        /** Ticks between rings, drawn fresh each time. */
        private static final int MIN_GAP = 3;
        private static final int MAX_GAP = 8;

        /** How many bursts make up one ring, spaced evenly around it. */
        private static final int BURSTS_PER_RING = 6;

        /**
         * How hard each burst is, as a multiple of a wind charge's own 1.22 knockback. Lower than a
         * lone burst would need, because a player in the middle takes the vertical half of every
         * burst in the ring at once.
         */
        private static final float MIN_POWER = 0.35F;
        private static final float MAX_POWER = 0.9F;

        /**
         * How wide each burst reaches - an explosion throws things out to twice this - which is
         * what lets a ring at its widest still reach the player in its middle.
         */
        private static final float BURST_RADIUS = 7.0F;

        /** The ring's radius as a wave begins: close enough in that its middle is a point. */
        private static final double MIN_RING = 1.5;

        /** How long one wave takes to widen from {@link #MIN_RING} to the full radius. */
        private static final int WAVE_TICKS = 60;

        /**
         * How many blocks the rings have climbed by the end. A fixed height rather than a multiple
         * of the radius, since the Chicken TNT runs this over a field four times as wide.
         */
        private static final double RISE = 15.0;

        private final BlockPos origin;
        private final double radius;
        private final int duration;
        private int cooldown;

        private Pinball(ResourceKey<Level> dimension, BlockPos origin, int duration, double radius) {
            // Every tick, and the countdown between rings is kept here rather than in the job's own
            // interval: the interval is fixed for the life of a job and this one changes each ring.
            super(dimension, duration, 1);
            this.origin = origin;
            this.radius = radius;
            this.duration = duration;
        }

        @Override
        boolean step(ServerLevel level) {
            if (!level.isLoaded(this.origin)) {
                return true;
            }

            if (this.cooldown-- <= 0) {
                RandomSource random = level.random;
                this.cooldown = MIN_GAP + random.nextInt(MAX_GAP - MIN_GAP + 1);

                int elapsed = this.duration - this.remaining;
                double wave = (elapsed % WAVE_TICKS) / (double) WAVE_TICKS;
                double ring = MIN_RING + (this.radius - MIN_RING) * wave;

                // From the ground the TNT sat on, climbing steadily for the whole fuse.
                double y = this.origin.getY() + 0.1 + RISE * elapsed / (double) this.duration;

                double phase = random.nextDouble() * Math.PI * 2.0;
                float power = MIN_POWER + random.nextFloat() * (MAX_POWER - MIN_POWER);
                SimpleExplosionDamageCalculator shove = new SimpleExplosionDamageCalculator(false, false,
                        Optional.of(WIND_CHARGE_KNOCKBACK * power), Optional.empty());

                for (int i = 0; i < BURSTS_PER_RING; i++) {
                    double angle = phase + i * Math.PI * 2.0 / BURSTS_PER_RING;
                    double x = this.origin.getX() + 0.5 + Math.cos(angle) * ring;
                    double z = this.origin.getZ() + 0.5 + Math.sin(angle) * ring;

                    if (level.isLoaded(BlockPos.containing(x, y, z))) {
                        level.explode(null, null, shove, x, y, z, BURST_RADIUS, false,
                                Level.ExplosionInteraction.NONE,
                                ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                                SoundEvents.WIND_CHARGE_BURST);
                    }
                }
            }

            return --this.remaining <= 0;
        }
    }

    /** What one wind charge throws at, which every burst here is priced in. */
    private static final float WIND_CHARGE_KNOCKBACK = 1.22F;

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
        private final boolean effects;

        private ArrowRain(ResourceKey<Level> dimension, LivingEntity target, int arrows, int interval,
                          float velocity, @Nullable LivingEntity cause, boolean effects) {
            super(dimension, arrows, interval);
            this.target = target;
            this.velocity = velocity;
            this.cause = cause;
            this.effects = effects;
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

            if (this.effects) {
                arrow.addEffect(ArrowEffects.random(random, true));
            }

            arrow.setPos(x, this.target.getY() + HEIGHT, z);
            arrow.shoot(0.0, -1.0, 0.0, this.velocity, 0.0F);
            level.addFreshEntity(arrow);

            return --this.remaining <= 0;
        }
    }
}
