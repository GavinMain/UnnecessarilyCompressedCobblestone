package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;


import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;

/**
 * The Compressed Creeper. It blinks to whoever it is hunting, winds up for three seconds and goes
 * off, over and over - the blast never kills it, so the fight is a series of detonations the player
 * has to be somewhere else for.
 * <p>
 * The cycle is fixed and never varies: blink onto the player, stand perfectly still for the whole
 * three second wind-up, go off, wait half a second, blink again. It does not chase and it does not
 * re-position once the wind-up has started, so the blast always lands where it arrived - the player
 * is meant to read the landing spot and leave it, not to outrun a moving target.
 * <p>
 * It extends {@link Creeper} purely to inherit the model and renderer; none of the vanilla fuse is
 * used. Vanilla's swell is driven by {@code swellDir}, which nothing here ever sets to 1, so the
 * inherited tick leaves it pinned at zero and {@link #getSwelling} is overridden to report this
 * entity's own wind-up instead. The two vanilla paths that could set that fuse going behind our back
 * - flint and steel, and a long fall - are both closed off below, because either would run vanilla's
 * {@code explodeCreeper} and quietly delete the boss.
 */
public class CompressedCreeperEntity extends Creeper {
    private static final EntityDataAccessor<Integer> DATA_CHARGE =
            SynchedEntityData.defineId(CompressedCreeperEntity.class, EntityDataSerializers.INT);

    /**
     * How high Jump Boost V carries a player, by simulation of vanilla's jump: a starting velocity
     * of {@code 0.42 + 0.1 * 5}, then {@code v = (v - 0.08) * 0.98} each tick until it turns over.
     */
    public static final float JUMP_BOOST_5_HEIGHT = 5.1F;

    /** Half a Jump Boost V jump, as asked. Vanilla creepers reach 3. */
    public static final float EXPLOSION_RADIUS = JUMP_BOOST_5_HEIGHT / 2.0F;

    /**
     * Five smash hits from a 26 digit inscribed mace, swung after a Jump Boost V jump.
     * <p>
     * A 5.1 block fall gives vanilla's {@code 12 + 2 * (f - 3)} = 16.2 bonus damage, on top of 5
     * base mace damage, 26 from the inscriber and the player's own 1. Density V adds another
     * {@code 2.5 * 5.1} = 12.75, for 60.96 raw, of which armour 30 lets 46.33 through; Breach IV
     * instead cancels the armour outright and lands 48.21. Five of either is 232 or 241, so the
     * round 250 is a shade above what five hits do: a Breach build needs six, and a Density build
     * six, unless the swing comes from higher than a standing jump. A 26 digit Sharpness V sword
     * needs fifteen.
     */
    public static final float MAX_HEALTH = 250.0F;

    /** Three seconds of wind-up, then it goes off. */
    private static final int CHARGE_TICKS = 60;

    /** Half a second of standing spent after a blast, and then it is on top of the player again. */
    private static final int RECOVERY_TICKS = 10;

    /**
     * The blast hits far harder than its size suggests. Vanilla ties the two together - the damage
     * an explosion does is worked out from its radius, so the only way to hit harder is normally to
     * reach further - and this pulls them apart by scaling the damage after the fact and leaving the
     * radius, and so the crater and the reach, exactly where it was.
     * <p>
     * At point blank the stock formula gives about 37 for a radius of {@link #EXPLOSION_RADIUS};
     * {@link #DAMAGE_MULTIPLIER} takes that to roughly 110 before armour, which a player in deeply
     * inscribed gear survives and an unprepared one does not. Everything about how blocks are
     * resisted and destroyed is inherited untouched.
     */
    private static final float DAMAGE_MULTIPLIER = 3.0F;

    private static final ExplosionDamageCalculator BLAST = new ExplosionDamageCalculator() {
        @Override
        public float getEntityDamageAmount(Explosion explosion, Entity entity) {
            return super.getEntityDamageAmount(explosion, entity) * DAMAGE_MULTIPLIER;
        }
    };

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_creeper"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);

    private int recovery;

    /** Set when the pause ends, spent on the blink that opens the next wind-up. */
    private boolean readyToBlink = true;
    private int clientCharge;
    private int clientOldCharge;

    public CompressedCreeperEntity(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 100;
    }

    /**
     * Armour 30 with no toughness at all. The pairing is the point: vanilla's
     * {@code armour - damage / (2 + toughness / 4)} means a big hit eats through far more of the
     * armour than a small one, and toughness is the stat that would undo that. At 30 and 0, a ten
     * damage swing loses 80% and a sixty damage smash loses 24%, so the same raw damage is worth
     * about four times as much delivered in one blow.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                // Not the full 1.0: the mace's smash knocks things back, and a boss that shrugged
                // that off entirely would be ignoring half of the weapon it is built around.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    /**
     * Deliberately does not call {@code super}: vanilla's creeper goals include the SwellGoal that
     * drives its fuse, and this one's wind-up is its own.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // No movement goal at all: it closes distance by blinking and is rooted the rest of the
        // time, so anything that tried to walk it would only fight the freeze below.
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGE, 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.hasEffect(MobEffects.REGENERATION)) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION,
                    1, false, false, true));
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setCharge(0);
            this.readyToBlink = true;
            return;
        }

        // Spent, and standing there. At the end of the pause it lines up the next blink.
        if (this.recovery > 0) {
            this.recovery--;
            if (this.recovery == 0) {
                this.readyToBlink = true;
            }

            return;
        }

        // One blink per cycle, taken the moment the pause ends, and then it is committed: from here
        // to the blast it does not move again, so where it lands is where it goes off.
        if (this.readyToBlink) {
            teleportNear(target);
            this.readyToBlink = false;
            playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
        }

        // Rooted for the whole wind-up. The goals would otherwise keep walking it at the player,
        // and the point of the pattern is that it commits to a spot and the player leaves it.
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));

        setCharge(getCharge() + 1);
        if (getCharge() >= CHARGE_TICKS) {
            detonate();
            setCharge(0);
            this.recovery = RECOVERY_TICKS;
        }
    }

    /** Blinks to somewhere near {@code target}, the way an enderman does. */
    private boolean teleportNear(LivingEntity target) {
        double x = target.getX() + (this.random.nextDouble() - 0.5) * 4.0;
        double y = target.getY() + this.random.nextInt(3) - 1;
        double z = target.getZ() + (this.random.nextDouble() - 0.5) * 4.0;

        if (!randomTeleport(x, y, z, true)) {
            return false;
        }

        level().playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.0F, 1.0F);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }

    /**
     * Hurts whatever is standing nearby and tears up the ground, but never the creeper: it is immune
     * to explosion damage, so it walks out of its own blast and starts winding up again.
     */
    private void detonate() {
        level().explode(this, null, BLAST, getX(), getY(), getZ(), EXPLOSION_RADIUS, false,
                Level.ExplosionInteraction.MOB);
    }

    /** Its own blast cannot hurt it - and neither can anyone else's, so it cannot be cheesed with TNT. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_EXPLOSION) || super.isInvulnerableTo(source);
    }

    /**
     * Never takes fall damage. This is not generosity: vanilla's creeper adds fall distance straight
     * onto its fuse, and since it teleports around a lot, a bad landing would otherwise set off the
     * inherited fuse and destroy the boss outright.
     */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /** Flint and steel would light the inherited fuse and delete the boss, so it does nothing here. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /** The other door into the inherited fuse, nailed shut for the same reason. */
    @Override
    public void ignite() {
    }

    private int getCharge() {
        return this.entityData.get(DATA_CHARGE);
    }

    private void setCharge(int charge) {
        this.entityData.set(DATA_CHARGE, charge);
    }

    /**
     * What the renderer draws: the creeper swells and flashes white as this climbs. Vanilla reads
     * its own fuse here, which is pinned at zero, so this reports the wind-up instead.
     */
    @Override
    public float getSwelling(float partialTicks) {
        return Mth.clamp(Mth.lerp(partialTicks, this.clientOldCharge, this.clientCharge) / CHARGE_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            this.clientOldCharge = this.clientCharge;
            this.clientCharge = getCharge();
        }

        super.tick();
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
