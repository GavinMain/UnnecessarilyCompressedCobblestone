package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What every dragon in this mod is, underneath: a wingbeat, sixty-four ticks of memory, and a flag
 * saying whether it is in the air.
 * <p>
 * It exists because the drawing needs those three things and nothing else. Vanilla's dragon model is
 * posed entirely out of where the beast was pointing over the last sixty-four ticks - the neck is
 * one part drawn five times, bent by the difference between consecutive ticks of heading, and the
 * tail is twelve more of the same - so anything that wants to be drawn as a dragon has to keep that
 * history, and there are three such things now: both phases of the boss and the pet that comes out
 * of it. Written once here rather than three times.
 * <p>
 * It could not be done by subclassing {@code EnderDragon}. That class's constructor pins itself to
 * {@code EntityType.ENDER_DRAGON} whatever type it is handed, so a subclass would be an ender dragon
 * to the client, to the save file and to the loot tables alike.
 * <p>
 * Nothing about fighting is here. What a dragon <em>does</em> belongs to
 * {@link CompressedDragonEntity} and its second phase; what it is <em>for</em>, in the pet's case,
 * belongs to {@link CompressedDragonPetEntity}.
 */
public abstract class AbstractCompressedDragonEntity extends PathfinderMob {
    /** Whether it is in the air. Synced, because the model poses differently in each. */
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(AbstractCompressedDragonEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Whether it is currently ignoring walls, which is synced for one reason: {@code travel} runs on
     * both sides, so a client copy that disagreed about collision would be shoved out of the
     * hillside its server copy is flying through and jitter until the next position packet caught
     * it. It is a second flag rather than {@link #DATA_FLYING} because the two differ in exactly the
     * cases that matter - a dive has to be able to land, and a rush is on the ground and phasing.
     */
    private static final EntityDataAccessor<Boolean> DATA_PHASING =
            SynchedEntityData.defineId(AbstractCompressedDragonEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_FLYING = "flying";
    private static final String TAG_PHASING = "phasing";

    /**
     * The flight history the model bends the neck and the tail out of: sixty-four ticks of where it
     * was pointing and how high it was. Kept on both sides - the drawing is client side and the
     * hitbox is not - and rebuilt from scratch on load, since it is a second of animation rather
     * than anything about the fight.
     */
    public final double[][] positions = new double[64][3];
    public int posPointer = -1;

    /** Where the wings are in their beat, and where they were last tick. Both read by the model. */
    public float oFlapTime;
    public float flapTime;

    protected AbstractCompressedDragonEntity(EntityType<? extends AbstractCompressedDragonEntity> entityType,
                                             Level level) {
        super(entityType, level);
        // The model is thirty blocks of neck and tail drawn well outside the hitbox, so it must not
        // be culled by it.
        this.noCulling = true;
    }

    /**
     * {@code Monster.createMonsterAttributes()} written out, because this is deliberately not a
     * {@code Monster}: that class implements {@code Enemy}, and a pet every iron golem in the game
     * charges at is not a pet. The two bosses say they are enemies themselves.
     */
    protected static AttributeSupplier.Builder createDragonAttributes() {
        return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLYING, true);
        builder.define(DATA_PHASING, true);
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    /**
     * Moving between the air and the ground. Gravity is the flag's other half: it holds itself up
     * in the air and falls on the ground.
     */
    protected void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
        setNoGravity(flying);
    }

    /** Whether walls are currently being ignored. See {@link #DATA_PHASING}. */
    public boolean isPhasing() {
        return this.entityData.get(DATA_PHASING);
    }

    protected void setPhasing(boolean phasing) {
        this.entityData.set(DATA_PHASING, phasing);
        // Both halves of it, now rather than next tick: the flag is read a tick later than it is
        // written, since customServerAiStep runs before travel and the field below is applied after
        // it, and one tick of a three block a tick dive with walls off starts under the floor.
        this.noPhysics = phasing;
    }

    /**
     * The wingbeat and the flight history, which both sides keep. It runs while the dragon is dying
     * as well - a boss whose death is two seconds of text should still be moving through it.
     */
    @Override
    public void aiStep() {
        super.aiStep();

        this.oFlapTime = this.flapTime;
        Vec3 movement = getDeltaMovement();
        if (isFlying()) {
            float beat = 0.2F / ((float) movement.horizontalDistance() * 10.0F + 1.0F);
            this.flapTime += beat * (float) Math.pow(2.0, movement.y);
        } else {
            // Folded and shifting rather than beating, which is vanilla's sitting cadence.
            this.flapTime += 0.1F;
        }

        // Both sides, off the synced flag, for the reason DATA_PHASING gives.
        this.noPhysics = isPhasing();

        setYRot(Mth.wrapDegrees(getYRot()));

        if (this.posPointer < 0) {
            for (int i = 0; i < this.positions.length; i++) {
                this.positions[i][0] = getYRot();
                this.positions[i][1] = getY();
            }
        }

        if (++this.posPointer == this.positions.length) {
            this.posPointer = 0;
        }

        this.positions[this.posPointer][0] = getYRot();
        this.positions[this.posPointer][1] = getY();
    }

    /** Turns towards a heading at {@code rate} degrees a tick, head and body with it. */
    protected void face(double dx, double dz, float rate) {
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }

        float want = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        setYRot(Mth.approachDegrees(getYRot(), want, rate));
        this.yBodyRot = getYRot();
        this.yHeadRot = getYRot();
    }

    /* WHAT THE MODEL READS */

    /**
     * Where it was pointing and how high it was, {@code bufferIndexOffset} ticks ago, interpolated.
     * Copied from {@code EnderDragon}, which is the one thing about that class worth having.
     */
    public double[] getLatencyPos(int bufferIndexOffset, float partialTicks) {
        if (isDeadOrDying()) {
            partialTicks = 0.0F;
        }

        partialTicks = 1.0F - partialTicks;
        int i = this.posPointer - bufferIndexOffset & 63;
        int j = this.posPointer - bufferIndexOffset - 1 & 63;
        double[] result = new double[3];
        double heading = this.positions[i][0];
        double turn = Mth.wrapDegrees(this.positions[j][0] - heading);
        result[0] = heading + turn * partialTicks;
        heading = this.positions[i][1];
        turn = this.positions[j][1] - heading;
        result[1] = heading + turn * partialTicks;
        result[2] = Mth.lerp(partialTicks, this.positions[i][2], this.positions[j][2]);
        return result;
    }

    /**
     * How far the {@code partIndex}th piece of the neck rides above the one behind it.
     * <p>
     * On the ground it is the index itself, which is vanilla's sitting answer and rears the head up
     * off the body; in the air it is the difference in height between two ticks of flight, which is
     * what makes the neck lag behind a climb. The sixth piece is the head and answers zero, exactly
     * as vanilla's does.
     */
    public float getHeadPartYOffset(int partIndex, double[] spineEndOffsets, double[] headPartOffsets) {
        if (!isFlying()) {
            return partIndex;
        }

        return partIndex == 6 ? 0.0F : (float) (headPartOffsets[1] - spineEndOffsets[1]);
    }

    /* WHAT A DRAGON IS, WHATEVER IT IS FOR */

    /** A dragon is not pushed about, and no landing of its own hurts it. */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /**
     * Nothing here spawns naturally, so nothing here is scenery to be cleaned up. Peaceful is a
     * separate branch of {@code Mob.checkDespawn} that runs before the persistence flag is read, so
     * it needs its own answer.
     */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(TAG_FLYING, isFlying());
        compound.putBoolean(TAG_PHASING, isPhasing());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // Anything saved before these tags existed - or summoned by a command - starts in the air,
        // which is where every fight here begins.
        setFlying(!compound.contains(TAG_FLYING) || compound.getBoolean(TAG_FLYING));
        setPhasing(!compound.contains(TAG_PHASING) || compound.getBoolean(TAG_PHASING));
    }
}
