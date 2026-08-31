package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * What every conjurer in this mod has in common: it picks something in reach, spends a few seconds
 * calling the sky down on it, strikes, and picks again. The strike wants no line of sight at all -
 * the bolt is put on the target itself, so walls, water and a roof of stone are no defence - and the
 * choice of target runs players first, then anything that fights at range, then whatever is nearest.
 * <p>
 * It extends {@link Drowned} purely for the model, the renderer and the trident. None of the vanilla
 * drowned's behaviour is used: {@link #registerGoals} deliberately does not call {@code super}, and
 * a subclass that wants to move has to add its own goals back.
 * <p>
 * What a subclass decides is how long the channel is, how far it reaches, and what the strike
 * actually does - which is the whole difference between the boss, whose bolt is real and lands for
 * thousands, and the wandering one, whose bolt is a light show with five damage behind it.
 */
public abstract class AbstractConjurerEntity extends Drowned {
    private static final EntityDataAccessor<Integer> DATA_CHANNEL =
            SynchedEntityData.defineId(AbstractConjurerEntity.class, EntityDataSerializers.INT);

    /** How often it looks for something to point at while it has nothing. */
    private static final int SEARCH_INTERVAL = 20;

    @Nullable
    private LivingEntity channelTarget;

    private int searchCooldown;

    protected AbstractConjurerEntity(EntityType<? extends Drowned> entityType, Level level) {
        super(entityType, level);
        arm();
    }

    /** How long the wind-up lasts, in ticks. */
    public abstract int channelTicks();

    /** How far it will reach for a target, in blocks. */
    public abstract double attackRadius();

    /** What actually happens to {@code target} when the channel ends. */
    protected abstract void strike(ServerLevel level, LivingEntity target);

    /** Whether it stands still even when it has nothing to cast at. */
    protected boolean isRooted() {
        return false;
    }

    /**
     * Deliberately does not call {@code super}: every goal a drowned has either walks it somewhere or
     * swims it somewhere, and the target here is chosen below rather than by a goal, because the
     * priority order - players, then ranged attackers, then nearest - is not something
     * {@code NearestAttackableTargetGoal} can express.
     */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHANNEL, 0);
    }

    /**
     * Vanilla's drowned equipment roll can hand it a fishing rod or nothing at all, and can make it a
     * baby. Neither suits a conjurer, so the roll is allowed to happen and then overruled.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        setBaby(false);
        arm();
        return data;
    }

    /** The trident it is drawn holding. It never throws it and never drops it. */
    protected void arm() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    /** Zombies burn at dawn. Something that fights with lightning is not waited out until sunrise. */
    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    /**
     * Lightning cannot touch it, which matters because its own strike can: the target may be standing
     * next to it, and a bolt reaches three blocks. Without this a melee opponent would only have to
     * stand still to make the conjurer kill itself.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_LIGHTNING) || super.isInvulnerableTo(source);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (isRooted()) {
            hold();
        }

        if (this.channelTarget != null && !isValidTarget(this.channelTarget)) {
            this.channelTarget = null;
        }

        if (this.channelTarget == null) {
            setChannel(0);
            if (this.searchCooldown-- > 0) {
                return;
            }

            this.searchCooldown = SEARCH_INTERVAL;
            this.channelTarget = chooseTarget(serverLevel);
            if (this.channelTarget == null) {
                return;
            }

            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 4.0F, 0.6F);
        }

        // Committed for the whole wind-up: whatever a subclass's goals were doing, the cast is not
        // walked out of.
        hold();
        setTarget(this.channelTarget);
        getLookControl().setLookAt(this.channelTarget, 30.0F, 30.0F);

        int channel = getChannel() + 1;
        setChannel(channel);
        markTarget(serverLevel, this.channelTarget, channel);

        if (channel >= channelTicks()) {
            strike(serverLevel, this.channelTarget);
            setChannel(0);
            // A fresh choice every cycle, so it follows whoever is the biggest threat now rather than
            // staying locked on the first thing it ever saw.
            this.channelTarget = null;
            this.searchCooldown = 0;
        }
    }

    /** Stops it dead where it stands. Buoyancy and currents move a mob that is not held. */
    private void hold() {
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        getNavigation().stop();
    }

    /**
     * Sparks over the target for the whole channel, thickening as it closes. This is the only warning
     * there is, and since the strike ignores walls it has to be visible through them - which it is,
     * because particles are drawn without an occlusion test.
     */
    private void markTarget(ServerLevel level, LivingEntity target, int channel) {
        int count = 1 + channel * 4 / channelTicks();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() + 0.5,
                target.getZ(), count, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getEyeY() + 0.5, getZ(),
                count, 0.3, 0.3, 0.3, 0.05);
    }

    /** The rule every boss here shares, with a conjurer's own kind as the exception it makes. */
    @Nullable
    private LivingEntity chooseTarget(ServerLevel level) {
        return BossTargeting.choose(level, this, attackRadius(), AbstractConjurerEntity::isConjurer);
    }

    protected boolean isValidTarget(LivingEntity entity) {
        return BossTargeting.isValid(this, entity, attackRadius(), AbstractConjurerEntity::isConjurer);
    }

    private static boolean isConjurer(LivingEntity entity) {
        return entity instanceof AbstractConjurerEntity;
    }

    public int getChannel() {
        return this.entityData.get(DATA_CHANNEL);
    }

    private void setChannel(int channel) {
        this.entityData.set(DATA_CHANNEL, channel);
    }
}
