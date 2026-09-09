package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.EnumSet;
import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * The Compressed Guardian. An <em>elder</em> guardian in every respect that is drawn - the same
 * model, the same renderer, the same beam winding up out of its one eye - and nothing like one in
 * what the beam carries when it lands.
 * <p>
 * It is an {@link ElderGuardian} rather than a plain {@code Guardian} because the elder is what a
 * boss-sized guardian is: two blocks across rather than five-sixths of one, the grey shell rather
 * than the green, and its own set of sounds. Two things come with that class and both are wanted.
 * Its {@code customServerAiStep} presses vanilla's own curse - Mining Fatigue III for five minutes
 * on players within fifty blocks - which is a wider and far weaker thing than the aura below and
 * reads as the announcement it is in vanilla; and it restricts itself to sixteen blocks of wherever
 * it woke up, which is what {@code MoveTowardsRestrictionGoal} in {@link #registerGoals} has always
 * been steering towards and which keeps the fight where it started.
 * <p>
 * Two things make the fight. The first is {@link #BEAM_DAMAGE}: twenty hearts through a full set of
 * netherite with Protection IV on every piece, worked back through vanilla's two reductions, which
 * is enough that a player standing in the open trades three beams for their life. The second is the
 * aura, which turns the water it is fought in against whoever is standing in it - every
 * {@link #AURA_INTERVAL} ticks it presses Mining Fatigue and Slowness, both at amplifier
 * {@value #AURA_AMPLIFIER}, onto everything alive within {@link #AURA_RADIUS} blocks. At that depth
 * Slowness is not a slow: {@code MOVEMENT_SPEED} is a ranged attribute with a floor of zero and ten
 * levels of Slowness take the multiplier past -1, so anything caught in the aura is not slowed but
 * stopped, and the beam is then aimed at something that cannot walk out of the way.
 * <p>
 * That is why the aura is re-applied on a short duration rather than held on a long one, the same
 * shape as the armour sets' bonuses: an elder guardian's Mining Fatigue lasts five minutes and is a
 * nuisance, whereas five minutes of being unable to move at all would not be a fight. It lapses
 * within {@link #AURA_DURATION} ticks of leaving the radius, so getting out of range is the whole of
 * the counterplay - and a Compression Rain set, which refuses harmful effects outright while it is
 * raining, is the other.
 * <p>
 * Vanilla's own attack goal is not kept. Its damage figures are hardcoded inside it, and it lands
 * the beam as two separate hits on one tick - a small {@code indirectMagic} hit and then a swing -
 * of which the invulnerability window would discard most of the second. {@link BeamGoal} below is
 * that goal with its two hits summed into one, which is the rule this mod uses everywhere damage is
 * dealt more than once in a tick.
 */
public class CompressedGuardianEntity extends ElderGuardian {
    public static final float MAX_HEALTH = 5000.0F;

    /**
     * Twenty hearts through a full set of netherite with Protection IV on all four pieces.
     * <p>
     * The beam is a mob attack rather than magic, so armour, Protection and Resistance all apply to
     * it for free and this figure is a raw one. Worked through vanilla: a hit this large is well
     * past the point where {@code CombatRules} clamps armour's contribution down to its floor of
     * {@code armour * 0.2}, so netherite's twenty armour leaves {@code 1 - 4/25 = 0.84} of it, and
     * sixteen points of Protection leave {@code 1 - 16/25 = 0.36} of that. 132 x 0.84 x 0.36 is
     * 39.9, which is the twenty hearts asked for. Unarmoured it is the whole 132.
     */
    public static final float BEAM_DAMAGE = 132.0F;

    /** Deliberately light: a boss with five thousand health does not also need plate. */
    public static final double ARMOR = 8.0;
    public static final double ARMOR_TOUGHNESS = 4.0;

    /** How far it looks for something to fight, which is also how far the beam reaches. */
    private static final double SEARCH_RADIUS = 32.0;

    /** How long the beam winds up before it lands. Vanilla's guardian takes 80 ticks, an elder 60. */
    private static final int BEAM_WIND_UP = 70;

    /**
     * A guardian cannot beam anything inside three blocks of it - vanilla's own rule, kept here, and
     * the reason a player who closes the distance is out of the beam and only in the aura.
     */
    private static final double BEAM_MIN_DISTANCE_SQR = 9.0;

    /** Mining Fatigue X and Slowness X: amplifier 9, an effect's level being its amplifier plus one. */
    private static final int AURA_AMPLIFIER = 9;

    /** How far the aura reaches, how often it is pressed, and how long one dose of it lasts. */
    private static final double AURA_RADIUS = 24.0;
    private static final int AURA_INTERVAL = 20;
    private static final int AURA_DURATION = 60;

    /**
     * How long the rain a summon brings on lasts, in ticks - ten minutes, which is long enough to
     * cover the whole of a five thousand health fight. Vanilla's own {@code /weather rain} is half
     * that; this is longer because the weather is not decoration here, it is the counterplay.
     */
    private static final int SUMMON_RAIN_TICKS = 12000;

    /**
     * How often the curse flash is sent, which is deliberately not how often the aura is pressed:
     * the effect is refreshed once a second so it lapses quickly on leaving, but a screen going dark
     * once a second would be unwatchable.
     */
    private static final int AURA_FLASH_INTERVAL = 200;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_guardian"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);

    public CompressedGuardianEntity(EntityType<? extends ElderGuardian> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 750;
        setPersistenceRequired();
    }

    /**
     * Monster numbers with the health, armour and reach the fight is written around. Attack damage
     * is stated at {@link #BEAM_DAMAGE} rather than left at a guardian's six, so the beam can read
     * the attribute rather than name a figure of its own - which is the rule the rest of this mod
     * follows, and which keeps the beam correctly priced if anything ever modifies the attribute.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BEAM_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS);
    }

    /**
     * A guardian's own goals with two changes, so {@code super} is deliberately not called.
     * <p>
     * The attack goal is {@link BeamGoal} rather than vanilla's, for the reason on this class; and
     * the targeting is this mod's boss rule - everything alive and in reach, rather than vanilla's
     * players, squid and axolotls - excluding its own kind and armour stands. Vanilla's three-block
     * floor is kept in the predicate, because a target it cannot beam is a target it would lock onto
     * and then do nothing about.
     */
    @Override
    protected void registerGoals() {
        this.randomStrollGoal = new RandomStrollGoal(this, 1.0, 80);
        MoveTowardsRestrictionGoal towardsRestriction = new MoveTowardsRestrictionGoal(this, 1.0);

        this.goalSelector.addGoal(4, new BeamGoal(this));
        this.goalSelector.addGoal(5, towardsRestriction);
        this.goalSelector.addGoal(7, this.randomStrollGoal);
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.randomStrollGoal.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        towardsRestriction.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> !isOwnKind(target) && !(target instanceof ArmorStand)
                        && target.distanceToSqr(this) > BEAM_MIN_DISTANCE_SQR));
    }

    /**
     * Summoning one brings the rain on.
     * <p>
     * The aura is a wall of harmful effects and the Compression Rain set is the thing that refuses
     * them - but only while it is raining, which is weather a player has no way to call up. So the
     * boss calls it up itself, and the set that answers this fight is wearable the moment the fight
     * starts rather than whenever the sky happens to agree.
     * <p>
     * This is {@code finalizeSpawn} rather than the constructor, so it fires once when the guardian
     * is actually summoned - out of a Boss Spawn Egg, a spawner or {@code /summon} alike - and not
     * every time the chunk it is standing in is loaded. Weather already in progress is left alone:
     * setting it here would turn somebody's thunderstorm into plain rain, and a storm is already
     * raining as far as the set is concerned. Note this only reaches dimensions that have weather at
     * all - fought in the Nether or the End, nothing brings the rain on and the set does nothing.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        if (!level.getLevel().isRaining()) {
            level.getLevel().setWeatherParameters(0, SUMMON_RAIN_TICKS, true, false);
        }

        return data;
    }

    /** How long the beam winds up for, which is what the renderer draws the charge over. */
    @Override
    public int getAttackDuration() {
        return BEAM_WIND_UP;
    }

    /** Its own kind, which is the one living thing it will not fight. */
    private boolean isOwnKind(LivingEntity entity) {
        return entity instanceof CompressedGuardianEntity;
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_GUARDIAN.get() && type != EntityType.ARMOR_STAND;
    }

    /**
     * Peaceful is a separate branch of {@code Mob.checkDespawn} that runs before the persistence
     * flag is even looked at, so a boss has to answer this as well as being persistent.
     */
    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    /**
     * The aura, pressed once a second onto everything alive in reach.
     * <p>
     * It reaches every living thing rather than players alone - an elder guardian's version is for
     * players only, but this mod's bosses are hostile to everything, and anything else fighting
     * alongside the guardian would otherwise be untouched by the thing that defines the fight. Its
     * own kind and armour stands are the two exceptions its targeting makes, and they are kept here.
     * <p>
     * {@code addEffect} goes through {@code canBeAffected}, and so through NeoForge's applicable
     * event, which is exactly how a Compression Rain set standing in the rain refuses this without
     * either side having to know the other exists.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel) || this.tickCount % AURA_INTERVAL != 0) {
            return;
        }

        List<LivingEntity> caught = serverLevel.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(AURA_RADIUS),
                entity -> entity != this && entity.isAlive() && !isOwnKind(entity)
                        && !(entity instanceof ArmorStand) && !isAlliedTo(entity)
                        && distanceToSqr(entity) <= AURA_RADIUS * AURA_RADIUS);

        boolean flash = this.tickCount % AURA_FLASH_INTERVAL == 0;
        for (LivingEntity entity : caught) {
            boolean took = entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, AURA_DURATION,
                    AURA_AMPLIFIER, false, true, true), this);
            took |= entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AURA_DURATION,
                    AURA_AMPLIFIER, false, true, true), this);

            // The screen-darkening flash an elder guardian sends with its curse, which is what makes
            // an effect arriving out of nowhere read as the mob's doing rather than as a bug. It is
            // sent only to a player the effect actually landed on, so a Compression Rain set that
            // refused it is not told it was cursed - and only every few seconds, because the aura is
            // pressed once a second and a flash that often would be unwatchable.
            if (flash && took && entity instanceof ServerPlayer player) {
                player.connection.send(new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, isSilent() ? 0.0F : 1.0F));
            }
        }
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

    /**
     * Vanilla's guardian attack goal with one change: the beam lands as a single hit.
     * <p>
     * Everything else here is vanilla's - the counter starting at -10 so there is a beat before the
     * ray appears, the line-of-sight check that drops the target if it breaks, the entity event 21
     * that plays the charge-up sound, and clearing the active target on stop so the ray stops being
     * drawn. The damage is one {@code mobAttack} of the guardian's whole {@code ATTACK_DAMAGE}
     * rather than a small magic hit followed by a swing, because a living entity ignores a second
     * hit inside twenty ticks of the first unless it is larger: vanilla's pair only works at
     * vanilla's figures, and at these the first hit would eat most of the second.
     */
    static class BeamGoal extends Goal {
        private final CompressedGuardianEntity guardian;
        private int attackTime;

        BeamGoal(CompressedGuardianEntity guardian) {
            this.guardian = guardian;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.guardian.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.guardian.getTarget();
            return super.canContinueToUse() && target != null
                    && this.guardian.distanceToSqr(target) > BEAM_MIN_DISTANCE_SQR;
        }

        @Override
        public void start() {
            this.attackTime = -10;
            this.guardian.getNavigation().stop();

            LivingEntity target = this.guardian.getTarget();
            if (target != null) {
                this.guardian.getLookControl().setLookAt(target, 90.0F, 90.0F);
            }

            this.guardian.hasImpulse = true;
        }

        @Override
        public void stop() {
            this.guardian.setActiveAttackTarget(0);
            this.guardian.setTarget(null);
            this.guardian.randomStrollGoal.trigger();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.guardian.getTarget();
            if (target == null) {
                return;
            }

            this.guardian.getNavigation().stop();
            this.guardian.getLookControl().setLookAt(target, 90.0F, 90.0F);

            if (!this.guardian.hasLineOfSight(target)) {
                this.guardian.setTarget(null);
                return;
            }

            this.attackTime++;
            if (this.attackTime == 0) {
                this.guardian.setActiveAttackTarget(target.getId());
                if (!this.guardian.isSilent()) {
                    this.guardian.level().broadcastEntityEvent(this.guardian, (byte) 21);
                }
            } else if (this.attackTime >= this.guardian.getAttackDuration()) {
                target.hurt(this.guardian.damageSources().mobAttack(this.guardian),
                        (float) this.guardian.getAttributeValue(Attributes.ATTACK_DAMAGE));
                this.guardian.setTarget(null);
            }

            super.tick();
        }
    }
}
