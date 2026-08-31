package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * Ordinary lightning that can be told how loud to be.
 * <p>
 * Everything a vanilla bolt does it still does - the fire, the lightning rods, the copper, the
 * damage, {@link #setVisualOnly(boolean)}, {@link #setCause}, the advancement triggers - because all
 * of that is inherited untouched. The one thing it changes is the pair of sounds vanilla plays.
 * <p>
 * A bolt makes exactly two sounds, both client side, both on the single tick {@code life} reads 2,
 * and both at volumes written into {@code LightningBolt#tick} with no way in from outside:
 * {@code entity.lightning_bolt.thunder} at {@value #VANILLA_THUNDER} and
 * {@code entity.lightning_bolt.impact} at {@value #VANILLA_IMPACT}. Neither is silenced by
 * {@code setVisualOnly}, which only gates the damage.
 * <p>
 * Volume means two different things either side of 1, and both are useful here. The sound engine
 * clamps the <em>gain</em> to 1 and multiplies the <em>attenuation distance</em> by whatever is left
 * over, so above 1 a volume is range and not loudness - which is why vanilla's two numbers are so
 * far apart, and why a storm bolt is heard by every player in the dimension while its crack carries
 * only 32 blocks. Both still play at full gain, so the pair sounds the same standing next to it.
 * Below 1 the opposite: the range is a flat 16 blocks and the number is loudness, which is the half
 * of the scale a bolt playing a note needs.
 */
public class VanillaLightningBoltEntity extends LightningBolt {
    /** What vanilla plays its thunder at: far enough to be heard everywhere in the dimension. */
    public static final float VANILLA_THUNDER = 10000.0F;

    /** What vanilla plays its impact at. It is the TNT explosion sample, and it carries 32 blocks. */
    public static final float VANILLA_IMPACT = 2.0F;

    /**
     * The two volumes, packed as one so a bolt is one data entry rather than two. Synced because the
     * sounds are played by the client, off its own copy of the entity.
     */
    private static final EntityDataAccessor<Float> DATA_THUNDER_VOLUME =
            SynchedEntityData.defineId(VanillaLightningBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_IMPACT_VOLUME =
            SynchedEntityData.defineId(VanillaLightningBoltEntity.class, EntityDataSerializers.FLOAT);

    /** The tick vanilla's own sounds are keyed off, and the one this bolt steps past. */
    private static final int SOUND_LIFE = 2;

    public VanillaLightningBoltEntity(EntityType<? extends VanillaLightningBoltEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** Both sounds at whatever volume is asked for. 0 is a bolt that is seen and not heard. */
    public void setVolume(float volume) {
        setVolumes(volume, volume);
    }

    public void setVolumes(float thunder, float impact) {
        this.entityData.set(DATA_THUNDER_VOLUME, Math.max(0.0F, thunder));
        this.entityData.set(DATA_IMPACT_VOLUME, Math.max(0.0F, impact));
    }

    /**
     * How far this bolt carries, as a fraction of how far a vanilla one does: 1 is a storm bolt, 0 is
     * one heard from the next room and no further.
     * <p>
     * Each sound is raised to the power of the scale against its <em>own</em> vanilla volume, rather
     * than both being cut by one shared factor, because the two are four orders of magnitude apart:
     * anything that took thunder from 10000 down to something local would have taken the crack below
     * hearing long before. This way both reach vanilla at 1, both bottom out at 1 - the shortest
     * range the engine carries a sound at full gain - and every scale between is a geometric step of
     * the distance the bolt is heard over, with the gain never touched.
     */
    public void setVolumeScale(float scale) {
        setVolumes(volumeAt(VANILLA_THUNDER, scale), volumeAt(VANILLA_IMPACT, scale));
    }

    public float getThunderVolume() {
        return this.entityData.get(DATA_THUNDER_VOLUME);
    }

    public float getImpactVolume() {
        return this.entityData.get(DATA_IMPACT_VOLUME);
    }

    /** What {@code vanillaVolume} becomes at {@code scale}. See {@link #setVolumeScale}. */
    public static float volumeAt(float vanillaVolume, float scale) {
        return (float) Math.pow(vanillaVolume, Mth.clamp(scale, 0.0F, 1.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_THUNDER_VOLUME, VANILLA_THUNDER);
        builder.define(DATA_IMPACT_VOLUME, VANILLA_IMPACT);
    }

    @Override
    public void tick() {
        // Vanilla's pair is played on the one tick life reads 2. Playing ours and stepping the
        // counter past that tick is the whole of the change: super does everything else, and on the
        // server that same branch is the fire and the copper, which is why this is client only.
        // It costs the first flash one of its three ticks on the client - the renderer reads nothing
        // but the seed, so the flash lasts as long as the entity does - which is 50ms of a bolt that
        // then re-flashes up to twice more.
        if (this.level().isClientSide() && this.life == SOUND_LIFE) {
            this.life = SOUND_LIFE - 1;

            // A volume of 0 is skipped rather than handed over: the engine logs a line for every
            // silent sound it is asked to play, and a song is a thousand of them.
            float thunder = getThunderVolume();
            if (thunder > 0.0F) {
                this.level().playLocalSound(getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, thunder, 0.8F + this.random.nextFloat() * 0.2F, false);
            }

            float impact = getImpactVolume();
            if (impact > 0.0F) {
                this.level().playLocalSound(getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundSource.WEATHER, impact, 0.5F + this.random.nextFloat() * 0.2F, false);
            }
        }

        super.tick();
    }
}
