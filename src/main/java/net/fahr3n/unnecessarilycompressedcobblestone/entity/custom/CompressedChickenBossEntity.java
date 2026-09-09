package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Sweep;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Chicken Boss. A chicken eight times the size of one, with two attacks and a second
 * life.
 * <p>
 * The fight is in two halves and the seam between them is the whole design. Nothing can kill it
 * while it is still in the first: a hit that would take it under {@link #PHASE_THRESHOLD} of its
 * health is clipped so that it lands on exactly {@link #PHASE_LOW} instead, and then it heals all
 * the way back and starts again. So the first half is not a damage race that can be won early - the
 * two thousand health have to be taken off twice - and the half that follows is the one worth
 * preparing for, because it keeps both of its attacks and gains a third.
 * <p>
 * That third one is the reason the second half differs in kind rather than in number. It calls ten
 * {@link CompressedCobblestoneChickenEntity} into the air over the fight, each carrying
 * {@link ModMobEffects#COMPRESSED_INFESTATION} on top of something that hurts it once a second and
 * Regeneration to survive being hurt - which turns each of them into a hatchery, since the
 * infestation is an effect that answers being hit rather than one that ticks. What comes out is this
 * mod's silverfish, whose bite ignores armour entirely, so the second half of the fight is fought
 * against the swarm as much as against the chicken.
 * <p>
 * The brood it makes will not turn on it or on each other - see
 * {@link CompressedSilverfishEntity#isAlliedTo} - so the swarm is the boss's, not a third party's.
 */
public class CompressedChickenBossEntity extends Chicken {
    public static final float MAX_HEALTH = 2000.0F;

    /**
     * How many times a chicken it is, and the only place that is said.
     * <p>
     * {@code Attributes.SCALE} is the whole of it: {@code LivingEntity#getDimensions} multiplies the
     * registered size by it, {@code LivingEntityRenderer#render} multiplies the model by it, and
     * {@code getShadowRadius} multiplies the shadow by it - so the hitbox, the drawing and the
     * shadow cannot drift apart, and the renderer needs no scaling of its own. That is why the
     * entity type is registered at a plain chicken's 0.4 by 0.7 rather than at the boss's size:
     * stating it in both places would multiply the two together.
     * <p>
     * The attribute is clamped by vanilla to at most 16, and this is well inside that. At eight it
     * stands about five and a half blocks tall and three and a bit wide - taller than a ravager and
     * wider than an iron golem - so it can never be mistaken for one of the ordinary compressed
     * chickens it calls up, which is the point: they share a model and a texture, and size is the
     * only thing telling them apart.
     */
    public static final double BOSS_SCALE = 8.0;

    /** A full set of netherite: twenty armour and twelve toughness. Well clear of the armour cap. */
    public static final double ARMOR = 20.0;
    public static final double ARMOR_TOUGHNESS = 12.0;

    /** What a ram is worth, which is also its melee: the two are the same attack. */
    public static final float RAM_DAMAGE = 200.0F;

    /** What one egg is worth where it lands. */
    public static final float EGG_DAMAGE = 100.0F;

    /** The share of its health at which the first half of the fight ends. */
    private static final float PHASE_THRESHOLD = 0.05F;

    /** And where the clipped hit leaves it, for the moment before it heals. */
    private static final float PHASE_LOW = 0.01F;

    /** How long it pours particles after the change, so the second half is announced, not guessed. */
    private static final int FLOURISH_TICKS = 60;

    /** Ticks between eggs, before and after the change. */
    private static final int EGG_INTERVAL = 40;
    private static final int EGG_INTERVAL_ENRAGED = 20;

    /** How far it will throw one, and how fast it leaves. */
    private static final double EGG_RANGE = 32.0;
    private static final float EGG_VELOCITY = 1.8F;

    /** Ticks between rams, how far one is started from, and how long it may last. */
    private static final int RAM_INTERVAL = 120;
    private static final int RAM_INTERVAL_ENRAGED = 80;
    private static final double RAM_RANGE = 24.0;
    private static final int RAM_TICKS = 30;

    /**
     * How fast it crosses ground during one, how high it rides, and how wide the swept hit is. The
     * width is measured from the body's centre line, so it has to clear the boss's own half-width -
     * at {@link #BOSS_SCALE} that is a little over a block and a half - or a ram would sweep past
     * things standing against its side.
     */
    private static final double RAM_SPEED = 1.35;
    private static final double RAM_LIFT = 0.42;
    private static final double RAM_WIDTH = 3.2;

    /** The second half only: ticks between broods, and how many chickens one is. */
    private static final int SUMMON_INTERVAL = 600;
    private static final int BROOD_SIZE = 10;

    /** How far out and how high over the boss they are put, and how long their effects run. */
    private static final double BROOD_SPREAD = 6.0;
    private static final double BROOD_HEIGHT = 6.0;
    private static final int BROOD_EFFECT_TICKS = 20 * 60;

    /**
     * What a summoned chicken is given to live on. The figure is the whole of the timing: at sixty
     * health it survives the freezing for something under a quarter of a minute, and every one of
     * those hits is a roll against the infestation.
     */
    private static final float BROOD_HEALTH = 60.0F;

    /** Freezing II: a hit every second, which is what makes the infestation hatch at all. */
    private static final int BROOD_DAMAGE_AMPLIFIER = 1;

    /** Regeneration II under it, so a brood member is being killed slowly rather than at once. */
    private static final int BROOD_REGENERATION_AMPLIFIER = 1;

    private static final String TAG_ENRAGED = "enraged";
    private static final String TAG_EGG_COOLDOWN = "egg_cooldown";
    private static final String TAG_RAM_COOLDOWN = "ram_cooldown";
    private static final String TAG_SUMMON_COOLDOWN = "summon_cooldown";

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_chicken_boss"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    /** Whether the fight is in its second half. */
    private boolean enraged;

    private int eggCooldown = EGG_INTERVAL;
    private int ramCooldown = RAM_INTERVAL;
    private int summonCooldown = SUMMON_INTERVAL;

    /** Ticks left of a ram that is under way, or 0 when it is not ramming. */
    private int ramming;

    /** Ticks left of the particles that mark the change. Not saved: it is a second of scenery. */
    private int flourish;

    public CompressedChickenBossEntity(EntityType<? extends Chicken> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 500;
        setPersistenceRequired();
    }

    /**
     * A chicken's numbers with everything about it replaced. {@code ATTACK_DAMAGE} has to be stated
     * rather than inherited: a chicken has none at all, and the melee goal below reads it.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, RAM_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.SCALE, BOSS_SCALE)
                // Tall enough that anything less would leave it stuck on the terrain it is chasing
                // somebody across; a step of two is still under half its own height.
                .add(Attributes.STEP_HEIGHT, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * Its own goals rather than a chicken's. Panicking, breeding, being tempted by seeds and
     * following a parent are all about being livestock, so {@code super.registerGoals()} is not
     * called and what is left is: swim, chase, wander and look.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isEnemyOf));
    }

    /**
     * Hostile to everything alive except its own brood and the things that only stand there, which
     * is the rule every boss in this mod follows.
     */
    private boolean isEnemyOf(LivingEntity entity) {
        return !isBrood(entity) && !(entity instanceof ArmorStand) && entity.canBeSeenAsEnemy();
    }

    /**
     * Its own side: itself, the chickens it calls up, and the silverfish those hatch. The silverfish
     * arrive out of an effect rather than out of the boss, so nothing marks them as its - being of
     * that kind is enough, and it is the same statement the silverfish makes back about chickens.
     */
    public static boolean isBrood(Entity entity) {
        return entity instanceof Chicken || entity instanceof CompressedSilverfishEntity;
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_CHICKEN_BOSS.get() && type != EntityType.ARMOR_STAND
                && super.canAttackType(type);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return isBrood(entity) || super.isAlliedTo(entity);
    }

    /** Nothing about this one is livestock: it does not breed and there is no chick. */
    @Nullable
    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    /* THE SEAM BETWEEN THE TWO HALVES */

    /**
     * The clip. Two separate things happen here and the order matters.
     * <p>
     * First, while the fight is still in its first half, no hit is allowed to be the one that kills
     * it: the incoming amount is cut so that a point of health survives whatever armour does or does
     * not take off. That is not the clip itself - it is what makes the clip reachable, since a boss
     * vanilla has already killed cannot decide where its health lands.
     * <p>
     * Then, if the hit took it under the threshold, its health is put on exactly {@link #PHASE_LOW}
     * of its maximum and it is healed from there. Damage that bypasses invulnerability -
     * {@code /kill}, the void - is deliberately left alone, or the boss would be a thing no command
     * could remove.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || this.enraged || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }

        boolean hurt = super.hurt(source, Math.min(amount, Math.max(getHealth() - 1.0F, 0.0F)));

        if (isAlive() && getHealth() < getMaxHealth() * PHASE_THRESHOLD) {
            enrage();
        }

        return hurt;
    }

    /** The change: down to a hundredth, then all the way back, and a second of noise about it. */
    private void enrage() {
        this.enraged = true;
        setHealth(getMaxHealth() * PHASE_LOW);
        setHealth(getMaxHealth());

        this.flourish = FLOURISH_TICKS;
        this.eggCooldown = EGG_INTERVAL_ENRAGED;
        this.ramCooldown = RAM_INTERVAL_ENRAGED;
        this.summonCooldown = 0;

        this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        level().playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 1.6F);
    }

    /** Whether the fight is in its second half, which is the only thing that unlocks the brood. */
    public boolean isEnraged() {
        return this.enraged;
    }

    /* THE FIGHT */

    /**
     * A chicken lays an egg on a countdown, and this one must not: its eggs are thrown. The clock is
     * wound back on every tick before the inherited code reads it, which is the same trick
     * {@link CompressedCobblestoneChickenEntity} plays in the other direction.
     */
    @Override
    public void aiStep() {
        this.eggTime = Integer.MAX_VALUE;
        super.aiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.flourish > 0) {
            this.flourish--;
            pourParticles(serverLevel);
        }

        if (this.ramming > 0) {
            tickRam(serverLevel);
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.enraged && this.summonCooldown-- <= 0) {
            this.summonCooldown = SUMMON_INTERVAL;
            summonBrood(serverLevel);
        }

        if (this.ramCooldown-- <= 0 && distanceToSqr(target) <= RAM_RANGE * RAM_RANGE) {
            this.ramCooldown = this.enraged ? RAM_INTERVAL_ENRAGED : RAM_INTERVAL;
            startRam(target);
            return;
        }

        if (this.eggCooldown-- <= 0 && distanceToSqr(target) <= EGG_RANGE * EGG_RANGE
                && hasLineOfSight(target)) {
            this.eggCooldown = this.enraged ? EGG_INTERVAL_ENRAGED : EGG_INTERVAL;
            throwEgg(target);
        }
    }

    /** One egg, led at where the target's middle is rather than at its feet. */
    private void throwEgg(LivingEntity target) {
        CompressedEggEntity egg = new CompressedEggEntity(level(), this);
        double dx = target.getX() - getX();
        double dy = target.getY(0.5) - egg.getY();
        double dz = target.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);

        // The extra tenth of the horizontal distance is the arc: the egg has gravity, so aiming
        // straight at the target lands it short.
        egg.shoot(dx, dy + flat * 0.1, dz, EGG_VELOCITY, 1.0F);
        level().addFreshEntity(egg);
        playSound(SoundEvents.EGG_THROW, 2.0F, 0.4F);
    }

    /**
     * The run-up. It is thrown at the target rather than pathed towards it, which is what makes a
     * ram different from walking over and pecking: it crosses the ground between them in well under
     * a second and hits whatever it passes through.
     */
    private void startRam(LivingEntity target) {
        Vec3 towards = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (towards.lengthSqr() < 1.0E-4) {
            return;
        }

        Vec3 aim = towards.normalize().scale(RAM_SPEED);
        setDeltaMovement(aim.x, RAM_LIFT, aim.z);
        this.hasImpulse = true;
        this.ramming = RAM_TICKS;

        playSound(SoundEvents.CHICKEN_HURT, 4.0F, 0.4F);
    }

    /**
     * One tick of a ram. The horizontal speed is put back on every tick - drag would otherwise eat
     * it before it arrived - and the hit is a <em>swept</em> test rather than a bounding box
     * overlap, because at this speed it covers most of two blocks in a tick and would otherwise pass
     * clean through whoever it was aimed at.
     */
    private void tickRam(ServerLevel level) {
        this.ramming--;

        Vec3 movement = getDeltaMovement();
        Vec3 flat = movement.multiply(1.0, 0.0, 1.0);
        if (flat.lengthSqr() > 1.0E-4) {
            Vec3 held = flat.normalize().scale(RAM_SPEED);
            setDeltaMovement(held.x, movement.y, held.z);
        }

        Vec3 from = new Vec3(this.xo, this.yo, this.zo);
        Vec3 to = position();

        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class,
                Sweep.bounds(from, to, RAM_WIDTH).minmax(getBoundingBox()),
                entity -> entity != this && entity.isAlive() && !isBrood(entity)
                        && !(entity instanceof ArmorStand) && !isAlliedTo(entity)
                        && Sweep.caught(entity, from, to, RAM_WIDTH));

        if (caught.isEmpty()) {
            return;
        }

        for (LivingEntity entity : caught) {
            entity.hurt(damageSources().mobAttack(this), RAM_DAMAGE);

            Vec3 away = entity.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 shove = away.normalize();
                entity.push(shove.x * 1.2, 0.5, shove.z * 1.2);
            }
        }

        // A ram is spent on whatever it connected with; it does not carry on through the far side.
        this.ramming = 0;
        setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 1.8F);
    }

    /**
     * The brood. Ten chickens in the air over the fight, each carrying the infestation, something
     * that hurts it once a second, and enough regeneration to be worth hurting.
     * <p>
     * The order of the three is the whole trick: the infestation does nothing on a tick and
     * everything on being hit, so the freezing is what makes it hatch and the regeneration is what
     * decides how long it goes on doing so. None of them drops anything - they were conjured rather
     * than paid for, which is the rule everywhere else in this mod.
     */
    private void summonBrood(ServerLevel level) {
        for (int i = 0; i < BROOD_SIZE; i++) {
            CompressedCobblestoneChickenEntity chicken =
                    ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get().create(level);
            if (chicken == null) {
                continue;
            }

            double angle = i * (Math.PI * 2.0 / BROOD_SIZE);
            double radius = 2.0 + this.random.nextDouble() * BROOD_SPREAD;
            chicken.moveTo(getX() + Math.cos(angle) * radius,
                    getY() + BROOD_HEIGHT + this.random.nextDouble() * 2.0,
                    getZ() + Math.sin(angle) * radius,
                    this.random.nextFloat() * 360.0F, 0.0F);

            chicken.setConjured(BROOD_HEALTH);
            chicken.addEffect(new MobEffectInstance(ModMobEffects.COMPRESSED_INFESTATION,
                    BROOD_EFFECT_TICKS, 0, false, true, true));
            chicken.addEffect(new MobEffectInstance(ModMobEffects.FREEZING,
                    BROOD_EFFECT_TICKS, BROOD_DAMAGE_AMPLIFIER, false, true, true));
            chicken.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    BROOD_EFFECT_TICKS, BROOD_REGENERATION_AMPLIFIER, false, true, true));

            level.addFreshEntity(chicken);
        }

        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + BROOD_HEIGHT, getZ(),
                80, BROOD_SPREAD, 1.0, BROOD_SPREAD, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.HOSTILE, 4.0F, 0.5F);
    }

    /** The second of light the change is announced with. */
    private void pourParticles(ServerLevel level) {
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + getBbHeight() / 2.0, getZ(),
                24, getBbWidth(), getBbHeight() / 2.0, getBbWidth(), 0.35);
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(),
                4, getBbWidth(), 0.5, getBbWidth(), 0.1);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() / 2.0, getZ(),
                12, getBbWidth(), getBbHeight() / 2.0, getBbWidth(), 0.15);
    }

    /** A chicken flaps down from anything; four times the size of one changes nothing about that. */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /* BOSS BAR */

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
        this.bossEvent.addPlayer(serverPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer serverPlayer) {
        super.stopSeenByPlayer(serverPlayer);
        this.bossEvent.removePlayer(serverPlayer);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(TAG_ENRAGED, this.enraged);
        compound.putInt(TAG_EGG_COOLDOWN, this.eggCooldown);
        compound.putInt(TAG_RAM_COOLDOWN, this.ramCooldown);
        compound.putInt(TAG_SUMMON_COOLDOWN, this.summonCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.enraged = compound.getBoolean(TAG_ENRAGED);
        this.eggCooldown = compound.getInt(TAG_EGG_COOLDOWN);
        this.ramCooldown = compound.getInt(TAG_RAM_COOLDOWN);
        this.summonCooldown = compound.getInt(TAG_SUMMON_COOLDOWN);

        if (this.enraged) {
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        }
    }
}
