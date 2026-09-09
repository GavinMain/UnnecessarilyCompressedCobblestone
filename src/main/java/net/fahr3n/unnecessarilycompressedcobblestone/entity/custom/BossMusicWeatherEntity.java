package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.LightningSong;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The storm the Compressed Dragon's second phase fights inside, and the only thing in this mod whose
 * whole job is a piece of music.
 * <p>
 * It plays a song out of the sky, one real lightning bolt per note, at random places across the
 * ground within render distance of the fight - and when the song ends it starts again. The song is a
 * datapack file under {@code data/<namespace>/songs/}, written out of a MIDI by
 * {@code tools/mid_to_song.py}, so what the dragon fights to can be replaced by a pack without
 * touching this class.
 * <p>
 * Two things about it are worth stating plainly, because both look wrong until they are read
 * together with how sound works in this game.
 * <ul>
 * <li><b>The volume is above one on purpose.</b> Past one, the sound engine clamps the gain and
 *     multiplies the <em>attenuation distance</em> with the rest, so a volume of
 *     {@value #RANGE_SCALE} is not a louder note but a note heard {@value #RANGE_SCALE} times
 *     further. That is the only way a tune scattered across a hundred blocks is a tune at all
 *     rather than a hundred private ones, and the written dynamics still mean what they say because
 *     every note is scaled by the same factor.</li>
 * <li><b>The bolts are real.</b> They damage, they set fire and they are counted by the dragon,
 *     whose own storm heals it - the mark on them is what {@code CompressedDragonTier2Entity} reads.
 *     Across a disc this wide the odds of any one note landing on a player are small, and the
 *     thunderstorm it sets is what keeps the fires it starts from taking the arena.</li>
 * </ul>
 * It follows the fight rather than the ground it was summoned on, and it ends when the fight does:
 * with no dragon left within {@link #REACH}, it removes itself. That is also what makes it safe for
 * the dragon to re-summon one whenever it changes style - a lost storm comes back, and a storm that
 * is still playing is left alone rather than doubled.
 */
public class BossMusicWeatherEntity extends Entity {
    /** The song it plays, which is a datapack file and not compiled in. */
    public static final ResourceLocation SONG = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "dragon_boss");

    /**
     * What every note's written volume is multiplied by, which buys range rather than loudness. See
     * the class note.
     */
    private static final float RANGE_SCALE = 8.0F;

    /** How far the bolts may fall from the fight, and the ceiling on what render distance can ask. */
    private static final int MAX_RADIUS = 128;

    /** How far a dragon may be before this storm decides the fight is over. */
    private static final double REACH = 160.0;

    /** How often it looks for the fight, in ticks. Once a second is far more often than it needs. */
    private static final int LOOK_INTERVAL = 20;

    /** How long a stretch of thunder it asks for, and how often it asks again. */
    private static final int WEATHER_TICKS = 12000;
    private static final int WEATHER_INTERVAL = 200;

    private static final String TAG_TICK = "song_tick";
    private static final String TAG_NEXT = "song_next";

    /** How far into the performance it is, and which note is due next. */
    private int songTick;
    private int next;

    public BossMusicWeatherEntity(EntityType<? extends BossMusicWeatherEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Nothing can hit it, stand on it or push it: it is weather. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount % LOOK_INTERVAL == 0 && !followFight(serverLevel)) {
            discard();
            return;
        }

        // Asked for again rather than set once: a stretch of thunder runs out, and something else
        // in the world is entitled to change the weather while this is playing.
        if (this.tickCount % WEATHER_INTERVAL == 0) {
            serverLevel.setWeatherParameters(0, WEATHER_TICKS, true, true);
        }

        play(serverLevel);
    }

    /**
     * Moves the storm onto the fight, and answers whether there is still a fight to move onto.
     * <p>
     * The bolts fall around this entity, so following the dragon is what keeps the music where the
     * fight is - a storm anchored to the ground it was summoned on would be inaudible the moment
     * the dragon crossed a hundred blocks, which it does regularly.
     */
    private boolean followFight(ServerLevel level) {
        List<CompressedDragonTier2Entity> dragons = level.getEntitiesOfClass(
                CompressedDragonTier2Entity.class, getBoundingBox().inflate(REACH), Entity::isAlive);
        if (dragons.isEmpty()) {
            return false;
        }

        CompressedDragonTier2Entity dragon = dragons.get(0);
        setPos(dragon.getX(), dragon.getY(), dragon.getZ());
        return true;
    }

    /** Everything the song has due on this tick, and the loop back to the beginning. */
    private void play(ServerLevel level) {
        MinecraftServer server = level.getServer();
        LightningSong song = LightningSong.get(server, SONG);
        List<LightningSong.Note> notes = song.notes();
        if (notes.isEmpty()) {
            return;
        }

        // Everything due on this tick, since two notes can fall on the same one - a chord is
        // several bolts at once and nothing else.
        while (this.next < notes.size() && notes.get(this.next).tick() <= this.songTick) {
            strike(level, notes.get(this.next++));
        }

        this.songTick++;

        if (this.next >= notes.size() && this.songTick > song.lengthTicks()) {
            this.songTick = 0;
            this.next = 0;
        }
    }

    /** One note: one bolt, somewhere on the ground within render distance of the fight. */
    private void strike(ServerLevel level, LightningSong.Note note) {
        int radius = Math.min(level.getServer().getPlayerList().getViewDistance() * 16, MAX_RADIUS);

        double angle = this.random.nextDouble() * Math.PI * 2.0;
        // The square root is what spreads the notes evenly over the disc; without it they crowd
        // into the middle, which is where the fight already is.
        double distance = Math.sqrt(this.random.nextDouble()) * radius;
        double x = getX() + Math.cos(angle) * distance;
        double z = getZ() + Math.sin(angle) * distance;
        BlockPos pos = BlockPos.containing(x, getY(), z);
        if (!level.isLoaded(pos)) {
            return;
        }

        VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
        if (bolt == null) {
            return;
        }

        bolt.moveTo(x, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY(), z);

        if (note.key() == VanillaLightningBoltEntity.NO_NOTE) {
            bolt.setVolume(note.volume() * RANGE_SCALE);
        } else {
            bolt.setNote(note.key(), note.volume() * RANGE_SCALE);
        }

        // The mark, which is what makes the storm the dragon's rather than a hazard to it.
        CompressedDragonTier2Entity.markBolt(bolt);
        level.addFreshEntity(bolt);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt(TAG_TICK, this.songTick);
        compound.putInt(TAG_NEXT, this.next);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.songTick = compound.getInt(TAG_TICK);
        this.next = compound.getInt(TAG_NEXT);
    }

}
