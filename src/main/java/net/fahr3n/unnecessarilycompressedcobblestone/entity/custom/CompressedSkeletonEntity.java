package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * The arrow boss, at whichever rung {@link ArrowBossTier} says it is. It fights the way a skeleton
 * does - a bow, at range, backing away - and then it does things a skeleton cannot.
 * <p>
 * There are four attacks between the three rungs, and which of them a given boss knows is entirely
 * the tier's business:
 * <ul>
 * <li>the plain shot, which every rung has;
 * <li>the stream, ten seconds of arrows poured down one line, fast enough that the only answer is a
 *     wall - and every arrow that misses lies where it lands for five minutes;
 * <li>the TNT rain, five seconds of Arrow and Arrow Spiral TNT falling out of the sky at random over
 *     the fight, which breaks nothing and hurts everything;
 * <li>the volley, which tears up every spent arrow lying nearby and puts the lot through its target
 *     at once - the attack that makes those five-minute arrows matter, and the one only the last rung
 *     has.
 * </ul>
 * It extends {@link Skeleton} for its model, renderer and the bow in its hand; none of the vanilla
 * bow AI is used, which is why {@link #reassessWeaponGoal} is nailed shut.
 */
public class CompressedSkeletonEntity extends Skeleton {
    /** A full diamond set: twenty points of armour behind eight of toughness. */
    public static final double ARMOR = 20.0;
    public static final double ARMOR_TOUGHNESS = 8.0;

    /** How far it will reach for a target, in blocks. */
    public static final double ATTACK_RADIUS = 48.0;

    /** Ten seconds of stream, an arrow every other tick. */
    private static final int STREAM_TICKS = 200;
    private static final int STREAM_INTERVAL = 2;

    /** How long a streamed arrow lies where it lands: five minutes, against vanilla's one. */
    private static final int STREAM_ARROW_DESPAWN = 6000;

    /**
     * How far above the skeleton the stream is conjured. Eight blocks puts a useful angle on it at
     * the range this fight is usually held at, so misses land in front of the target rather than
     * flying flat until the world runs out.
     */
    private static final double STREAM_ELEVATION = 8.0;

    /**
     * What the stream fires at: a Compressed Cobblestone Bow at a full draw carrying a hundred digits
     * of Compression Energy. The inscriber pays a point of velocity per digit, so that is the bow's
     * own nine plus a hundred.
     * <p>
     * At this speed an arrow crosses a hundred and nine blocks a tick, which is past the point where
     * flight time means anything - it arrives on the tick it is fired. Nothing tunnels, because a
     * projectile ray-casts the whole segment it moved through rather than testing where it landed, so
     * blocks and bodies in the way are still hit.
     */
    private static final int STREAM_DIGITS = 100;
    private static final float STREAM_VELOCITY = CompressedCobblestoneBowItem.MAX_VELOCITY + STREAM_DIGITS;

    /** What an ordinary shot is fired at, which is a full draw and nothing more. */
    private static final float SHOT_VELOCITY = CompressedCobblestoneBowItem.MAX_VELOCITY;

    /** How far the volley reaches for arrows to pull up, and how many it will take. */
    private static final double VOLLEY_RADIUS = 32.0;
    private static final int VOLLEY_MAX_ARROWS = 30;

    /** The TNT rain: five seconds of it, falling anywhere within this of the fight. */
    private static final int TNT_RAIN_TICKS = 100;
    private static final int TNT_RAIN_RADIUS = 12;

    /**
     * Exactly what one cast of the rain is made of: two Arrow TNT and one Arrow Spiral TNT, no more
     * and no fewer. The counts are exact rather than a draw from the two kinds, because the spiral
     * pours arrows for as long as it runs and two of them at once is a different attack from one.
     */
    private static final int TNT_RAIN_ARROW_COUNT = 2;
    private static final int TNT_RAIN_SPIRAL_COUNT = 1;

    /** Ticks between attacks, and the pause after one of the long ones. */
    private static final int ATTACK_INTERVAL = 40;
    private static final int LONG_ATTACK_RECOVERY = 60;

    /** How often it looks for something new to shoot at while it has nothing. */
    private static final int SEARCH_INTERVAL = 20;

    private final ArrowBossTier tier;
    private final ServerBossEvent bossEvent;

    private int attackCooldown = ATTACK_INTERVAL;
    private int streamTicks;
    private int searchCooldown;

    public CompressedSkeletonEntity(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
        this.tier = ArrowBossTier.of(entityType);
        this.bossEvent = new ServerBossEvent(entityType.getDescription(),
                BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 150;
        setPersistenceRequired();
        arm();
    }

    public ArrowBossTier tier() {
        return this.tier;
    }

    /**
     * Movement only. What it attacks with and when is decided in {@link #customServerAiStep}, because
     * none of the attacks is something a vanilla goal can express - and vanilla's bow goal would be
     * firing its own shots underneath all of it.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.0, (float) ATTACK_RADIUS));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Vanilla swaps a skeleton between a bow goal and a melee one. This one uses neither. */
    @Override
    public void reassessWeaponGoal() {
    }

    /** The bow it is drawn holding. It never drops it, and it is never actually drawn from. */
    private void arm() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.COMPRESSED_COBBLESTONE_BOW.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        arm();
        return data;
    }

    /** Daylight is not an answer to a boss. */
    @Override
    public boolean isSunBurnTick() {
        return false;
    }

    /**
     * Its own arrows never hurt it. The deeper rungs go further and are immune to arrows and blasts
     * outright - both of which are things they throw by the dozen, so a fight with one cannot be won
     * by turning its own weapons back on it.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.getEntity() == this) {
            return true;
        }

        if (this.tier.immuneToItsOwnWeapons()
                && (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypes.ARROW))) {
            return true;
        }

        return super.isInvulnerableTo(source);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !BossTargeting.isValid(this, target, ATTACK_RADIUS, this::isOwnKind)) {
            setTarget(null);
            this.streamTicks = 0;

            if (this.searchCooldown-- <= 0) {
                this.searchCooldown = SEARCH_INTERVAL;
                setTarget(BossTargeting.choose(serverLevel, this, ATTACK_RADIUS, this::isOwnKind));
            }

            return;
        }

        getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.streamTicks > 0) {
            stream(serverLevel, target);
            return;
        }

        if (this.attackCooldown-- > 0) {
            return;
        }

        this.attackCooldown = ATTACK_INTERVAL;
        chooseAttack(serverLevel, target);
    }

    /**
     * Rolls one attack out of the ones this rung has. The weights are the tier's, and a weight of
     * zero means the attack is simply not in the draw.
     */
    private void chooseAttack(ServerLevel level, LivingEntity target) {
        int total = this.tier.shotWeight() + this.tier.streamWeight()
                + this.tier.tntRainWeight() + this.tier.volleyWeight();
        int roll = this.random.nextInt(Math.max(1, total));

        if ((roll -= this.tier.volleyWeight()) < 0) {
            volley(level, target);
            return;
        }

        if ((roll -= this.tier.tntRainWeight()) < 0) {
            tntRain(level, target);
            return;
        }

        if (roll - this.tier.streamWeight() < 0) {
            this.streamTicks = STREAM_TICKS;
            playSound(SoundEvents.SKELETON_CONVERTED_TO_STRAY, 4.0F, 0.6F);
            return;
        }

        shoot(level, target, SHOT_VELOCITY, false, 0.0);
    }

    /**
     * One arrow, at a full draw. {@code elevation} lifts where it starts from without moving the
     * skeleton: the stream is fired from well overhead so that it comes down at an angle, and a shot
     * that misses drives into the ground near its target instead of leaving for the horizon. At a
     * hundred and nine blocks a tick a level shot would be gone over the edge of the world before it
     * ever landed, and the volley would have nothing left to pick up.
     */
    private void shoot(ServerLevel level, LivingEntity target, float velocity, boolean persistent, double elevation) {
        CompressedArrowTier arrowTier = this.tier.arrowTier();
        CompressedArrowEntity arrow = new CompressedArrowEntity(arrowTier, level, this,
                new ItemStack(arrowTier.item().get()), getMainHandItem());

        if (persistent) {
            arrow.setDespawnDelay(STREAM_ARROW_DESPAWN);
        }

        arrow.setPos(getX(), getEyeY() + elevation, getZ());

        // Aimed at the middle of the target rather than its feet, and with no inaccuracy at all:
        // this is a boss, and the stream in particular is meant to be broken by cover, not by luck.
        Vec3 aim = target.getEyePosition().subtract(0.0, 0.4, 0.0).subtract(arrow.position());
        arrow.shoot(aim.x, aim.y, aim.z, velocity, 0.0F);
        level.addFreshEntity(arrow);

        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    /**
     * Ten seconds of arrows, one every other tick, each one aimed where the target is now. It stands
     * still for the whole of it: the stream is a line, and a line is something a player walks out of.
     */
    private void stream(ServerLevel level, LivingEntity target) {
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        getNavigation().stop();

        if (this.streamTicks % STREAM_INTERVAL == 0) {
            shoot(level, target, STREAM_VELOCITY, true, STREAM_ELEVATION);
        }

        if (--this.streamTicks <= 0) {
            this.attackCooldown = LONG_ATTACK_RECOVERY;
        }
    }

    /**
     * Five seconds of TNT falling out of the sky over the fight: two Arrow TNT and one Arrow Spiral
     * TNT, in a random order and at random spots. Where each one appears is random and none of them
     * breaks anything, so what it does is fill the air with arrows and leave the ground exactly where
     * it was. Each one lands before it goes off - an Arrow TNT that bursts on the way down throws its
     * arrows out of the sky, over an area far too wide to be an attack.
     */
    private void tntRain(ServerLevel level, LivingEntity target) {
        List<Block> drops = new ArrayList<>(TNT_RAIN_ARROW_COUNT + TNT_RAIN_SPIRAL_COUNT);
        for (int index = 0; index < TNT_RAIN_ARROW_COUNT; index++) {
            drops.add(ModBlocks.ARROW_TNT.get());
        }

        for (int index = 0; index < TNT_RAIN_SPIRAL_COUNT; index++) {
            drops.add(ModBlocks.ARROW_SPIRAL_TNT.get());
        }

        DeferredFill.queueTntRain(level, target.blockPosition(), TNT_RAIN_RADIUS, TNT_RAIN_TICKS,
                drops, this, 1.0F);

        playSound(SoundEvents.SKELETON_SHOOT, 4.0F, 0.5F);
        this.attackCooldown = LONG_ATTACK_RECOVERY;
    }

    /**
     * The volley: every arrow lying on the ground nearby is torn up and put through the target at
     * once. It is dealt as a single hit rather than as one per arrow, because a second hit inside the
     * same invulnerability window is simply discarded - a hundred arrows would otherwise land as one.
     * <p>
     * The arrows themselves are removed rather than re-fired. Vanilla arrows carry their damage in
     * their speed, and there is no honest speed to give an arrow that starts a block from its target.
     */
    private void volley(ServerLevel level, LivingEntity target) {
        List<AbstractArrow> grounded = level.getEntitiesOfClass(AbstractArrow.class,
                getBoundingBox().inflate(VOLLEY_RADIUS), arrow -> arrow.isAlive() && arrow.onGround());
        if (grounded.isEmpty()) {
            // Nothing to throw: it falls back on an ordinary shot rather than wasting the turn.
            shoot(level, target, SHOT_VELOCITY, false, 0.0);
            return;
        }

        float damage = 0.0F;
        int taken = 0;

        for (AbstractArrow arrow : grounded) {
            if (taken++ >= VOLLEY_MAX_ARROWS) {
                break;
            }

            damage += (float) arrow.getBaseDamage();
            level.sendParticles(ParticleTypes.CRIT, arrow.getX(), arrow.getY(), arrow.getZ(), 6, 0.1, 0.1, 0.1, 0.1);
            arrow.discard();
        }

        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2.0,
                target.getZ(), 60, 0.6, 0.8, 0.6, 0.4);
        level.playSound(null, target.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 3.0F, 0.6F);

        target.hurt(level.damageSources().source(DamageTypes.ARROW, this, this), damage);
        this.attackCooldown = LONG_ATTACK_RECOVERY;
    }

    private boolean isOwnKind(LivingEntity entity) {
        return entity instanceof CompressedSkeletonEntity;
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
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }
}
