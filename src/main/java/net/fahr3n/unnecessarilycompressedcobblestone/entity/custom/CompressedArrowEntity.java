package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A compressed arrow in flight. Base damage belongs to the projectile rather than to the bow that
 * fired it, so it is set here and every shot carries it, whatever it was fired from.
 * <p>
 * Which strength of arrow this is comes off its own entity type through
 * {@link CompressedArrowTier#of}, so all of them share this class and none of them need it saved.
 * Everything else is stock {@link AbstractArrow}: it arcs, sticks in blocks, can be picked back up,
 * and takes the firing weapon's enchantments into account on impact.
 */
public class CompressedArrowEntity extends AbstractArrow {
    /** What vanilla gives every arrow: a minute on the ground and then it is gone. */
    public static final int DEFAULT_DESPAWN_DELAY = 1200;

    private static final String TAG_DESPAWN_DELAY = "despawn_delay";
    private static final String TAG_LIFE = "compressed_life";
    private static final String TAG_RAIN_ARROWS = "rain_arrows";
    private static final String TAG_RAIN_INTERVAL = "rain_interval";
    private static final String TAG_RAIN_VELOCITY = "rain_velocity";
    private static final String TAG_RAIN_TNT = "rain_tnt";
    private static final String TAG_RAIN_SPIRAL = "rain_spiral";

    private int despawnDelay = DEFAULT_DESPAWN_DELAY;
    private int life;

    private int rainArrows;
    private int rainInterval = 1;
    private float rainVelocity;
    private boolean rainTnt;
    private boolean rainSpiral;

    /** Used when the client and the save file recreate the entity; the saved damage overwrites this. */
    public CompressedArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
        setBaseDamage(tier().baseDamage());
    }

    /** Fired from a bow. */
    public CompressedArrowEntity(CompressedArrowTier tier, Level level, LivingEntity shooter, ItemStack pickupItem,
                                 @Nullable ItemStack weapon) {
        super(tier.entityType().get(), shooter, level, pickupItem, weapon);
        setBaseDamage(tier.baseDamage() + CompressionEnergy.bonus(pickupItem));
    }

    /** Fired from a dispenser. */
    public CompressedArrowEntity(CompressedArrowTier tier, Level level, double x, double y, double z,
                                 ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(tier.entityType().get(), x, y, z, level, pickupItem, weapon);
        setBaseDamage(tier.baseDamage() + CompressionEnergy.bonus(pickupItem));
    }

    /**
     * Read off the entity type rather than kept in a field: {@link AbstractArrow}'s constructor calls
     * {@link #getDefaultPickupItem()} before any subclass field has been assigned, so a field here is
     * still null the first time it is needed.
     */
    public CompressedArrowTier tier() {
        return CompressedArrowTier.of(getType());
    }

    /**
     * Turns this arrow into the Arrow Staff's marker: whatever it hits has arrows fall on it. The
     * numbers travel on the arrow because the rain is decided by what it finds, and until it lands
     * nothing else knows what that will be.
     *
     * @param arrows   how many fall in total
     * @param interval ticks between them
     * @param velocity how fast each one comes down
     */
    public void markRain(int arrows, int interval, float velocity) {
        this.rainArrows = arrows;
        this.rainInterval = interval;
        this.rainVelocity = velocity;
        this.rainTnt = false;
    }

    /**
     * The same mark, for the Arrow TNT Staff: what falls is lit Arrow TNT rather than arrows, and
     * {@code blastScale} is what each one's explosion is worth.
     */
    public void markTntRain(int count, int interval, float blastScale, boolean spiral) {
        this.rainArrows = count;
        this.rainInterval = interval;
        this.rainVelocity = blastScale;
        this.rainTnt = true;
        this.rainSpiral = spiral;
    }

    /** Anything the marker hits is rained on, which is the whole of what the Arrow Staff does. */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (this.rainArrows > 0 && level() instanceof ServerLevel serverLevel
                && result.getEntity() instanceof LivingEntity struck) {
            Entity shooter = getOwner();
            LivingEntity cause = shooter instanceof LivingEntity living ? living : null;

            if (this.rainTnt) {
                DeferredStrikes.queueTntRain(serverLevel, struck, this.rainArrows, this.rainInterval,
                        this.rainVelocity, cause, this.rainSpiral);
            } else {
                DeferredStrikes.queueArrowRain(serverLevel, struck, this.rainArrows, this.rainInterval,
                        this.rainVelocity, cause);
            }

            this.rainArrows = 0;
        }
    }

    /**
     * How long this arrow lies where it landed before it vanishes. Vanilla's minute is the default;
     * the Compressed Skeleton sets its stream arrows to five, because its last attack is picking
     * them all back up again and a minute is not long enough to build a floor of them.
     */
    public void setDespawnDelay(int ticks) {
        this.despawnDelay = ticks;
    }

    /**
     * Vanilla's own despawn, with the minute made adjustable. Its {@code life} counter is private, so
     * this counts its own rather than reading that one.
     */
    @Override
    protected void tickDespawn() {
        if (++this.life >= this.despawnDelay) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_DESPAWN_DELAY, this.despawnDelay);
        compound.putInt(TAG_LIFE, this.life);
        compound.putInt(TAG_RAIN_ARROWS, this.rainArrows);
        compound.putInt(TAG_RAIN_INTERVAL, this.rainInterval);
        compound.putFloat(TAG_RAIN_VELOCITY, this.rainVelocity);
        compound.putBoolean(TAG_RAIN_TNT, this.rainTnt);
        compound.putBoolean(TAG_RAIN_SPIRAL, this.rainSpiral);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(TAG_DESPAWN_DELAY)) {
            this.despawnDelay = compound.getInt(TAG_DESPAWN_DELAY);
        }

        this.life = compound.getInt(TAG_LIFE);
        this.rainArrows = compound.getInt(TAG_RAIN_ARROWS);
        this.rainInterval = Math.max(1, compound.getInt(TAG_RAIN_INTERVAL));
        this.rainVelocity = compound.getFloat(TAG_RAIN_VELOCITY);
        this.rainTnt = compound.getBoolean(TAG_RAIN_TNT);
        this.rainSpiral = compound.getBoolean(TAG_RAIN_SPIRAL);
    }

    /** What the arrow turns back into when it is picked up. */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(tier().item().get());
    }
}
