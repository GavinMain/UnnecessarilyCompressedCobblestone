package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The Compressed Husk. A husk in everything the renderer draws, and a fight built out of three
 * things a husk does not have: no reach, no knockback, and no staying anywhere.
 * <p>
 * The reach is the whole of it. {@link #ATTACK_REACH} is a twentieth of a block where vanilla's is
 * {@code sqrt(2.04) - 0.6}, about five sixths of one, so the two hitboxes have to be all but
 * touching before a swing lands - which turns a mob that would otherwise be fought from a step back
 * into one that has to be let into your face to be hit at all. Everything else the boss does is
 * about that distance. It cannot be knocked out of it ({@code KNOCKBACK_RESISTANCE} 1.0, the
 * ceiling of the attribute), it blinds whatever it touches so the distance cannot be judged, and it
 * takes the distance away itself every {@link #TELEPORT_INTERVAL} ticks by stepping
 * {@link #TELEPORT_DISTANCE} blocks out of the fight, so closing it is something that has to be
 * done over and over rather than once.
 * <p>
 * Which is why it brings its own floor. A fight decided by exactly how far apart two things are
 * cannot be fought on terrain, so the boss flattens {@link #ARENA_RADIUS} blocks of it when it
 * arrives - see {@code DeferredFill.queueArena}, which lays it down over a second rather than on
 * the tick the boss lands.
 * <p>
 * The three self-buffs are held the way the armour sets' bonuses are: re-applied every second on a
 * short duration rather than kept on a long one, so they show in the boss's own particles and stop
 * with the boss rather than lingering on whatever it turned into.
 */
public class CompressedHuskEntity extends Husk {
    public static final float MAX_HEALTH = 2000.0F;

    /**
     * Twenty and eighteen, which is the shape of a heavily armoured boss rather than an unkillable
     * one. Both are well inside the hundred that {@code CombatRules} breaks down at, and armour's
     * contribution tops out at 80% however deep it goes - what these two actually buy is the floor,
     * {@code armour * 0.2}, which is what stops a single enormous hit going straight through.
     */
    public static final double ARMOR = 20.0;
    public static final double ARMOR_TOUGHNESS = 18.0;

    /**
     * Raw, before its own armour and Resistance - a husk's three, taken to what a boss standing in
     * your face for a whole fight should be worth against a player who has been through this mod.
     * A hundred is priced against roughly six hundred health rather than against twenty: through a
     * full netherite set with Protection IV it lands at about thirty, so a player who lets it stay
     * in their face has twenty swings in them, and the blindness and the slowness are what decide
     * how many of those they actually get to spend walking away.
     */
    public static final float ATTACK_DAMAGE = 100.0F;

    /**
     * How far past its own hitbox a swing reaches, in blocks. Vanilla's {@code DEFAULT_ATTACK_REACH}
     * is {@code sqrt(2.04) - 0.6}, about 0.828; this is a twentieth of a block, so the boss and its
     * target are all but overlapping before anything lands.
     */
    private static final double ATTACK_REACH = 0.05;

    /** How far it looks for something to fight. Well inside the arena it flattens. */
    private static final double SEARCH_RADIUS = 48.0;

    /** Blindness and Slowness V for ten seconds, on everything it touches. */
    private static final int TOUCH_DURATION = 200;

    /**
     * Slowness V, an effect's level being its amplifier plus one - and deliberately the deepest
     * level that is still a slow. The effect is {@code multiply_total -0.15} per level onto
     * {@code MOVEMENT_SPEED}, which is a ranged attribute with a floor of zero, so amplifier 6 and
     * up clamp it to nothing and the target cannot move at all. Amplifier 4 leaves a quarter of the
     * player's speed, which is slow enough that the boss can hold the distance it needs and fast
     * enough that walking away is still something a player can do.
     */
    private static final int SLOWNESS_AMPLIFIER = 4;

    /** The three it holds on itself: Regeneration II, Resistance II and Speed X. */
    private static final int REGENERATION_AMPLIFIER = 1;
    private static final int RESISTANCE_AMPLIFIER = 1;
    private static final int SPEED_AMPLIFIER = 9;

    /** How often the three are pressed back on, and how long one dose lasts. */
    private static final int BUFF_INTERVAL = 20;
    private static final int BUFF_DURATION = 60;

    /** Every five seconds, twenty blocks. */
    private static final int TELEPORT_INTERVAL = 100;
    private static final int TELEPORT_DISTANCE = 20;

    /** How many directions are tried before a teleport gives up and waits for the next one. */
    private static final int TELEPORT_ATTEMPTS = 16;

    /** How wide the floor it brings with it is. */
    private static final int ARENA_RADIUS = 50;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_husk"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    public CompressedHuskEntity(EntityType<? extends Husk> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1000;
        setPersistenceRequired();
    }

    /**
     * The numbers the fight is written around. Movement speed is left at a husk's own: the boss
     * carries Speed X, which is a {@code multiply_total} on top of this, and stating a large base as
     * well would put it past anything a player could walk away from.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS);
    }

    /**
     * A zombie's goals minus everything about being a zombie in a village - the turtle eggs, the
     * doors, the villagers, the iron golems - so {@code super} is deliberately not called. What is
     * left is the attack, the stroll and this mod's boss targeting: everything alive and in reach,
     * players first, except its own kind and the things that only stand there.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> BossTargeting.isValid(this, target, SEARCH_RADIUS, CompressedHuskEntity::isOwnKind)));
    }

    /**
     * Arriving flattens the ground.
     * <p>
     * This is {@code finalizeSpawn} rather than the constructor for the reason the guardian's rain
     * is: it fires once, when the boss is actually called up - out of a Boss Spawn Egg, a spawner or
     * {@code /summon} alike - and not every time the chunk it is standing in is loaded back.
     * <p>
     * Two of vanilla's own spawn rolls are overruled here rather than prevented, since both happen
     * inside {@code super}. A zombie may be spawned as a baby, and a baby boss is a boss with a
     * different hitbox and a different speed; and every zombie rolls a chance to call reinforcements
     * when it is hurt, which on a two thousand health boss would be a zombie horde rather than a
     * boss fight.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        setBaby(false);

        AttributeInstance reinforcements = getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.setBaseValue(0.0);
        }

        DeferredFill.queueArena(level.getLevel(), blockPosition(), ARENA_RADIUS);

        return data;
    }

    /**
     * The reach, and the whole of the fight. Vanilla's is the bounding box inflated by about five
     * sixths of a block on both horizontal axes; this is the same box inflated by a twentieth, so
     * the boss has to be all but inside its target to hit it.
     * <p>
     * Overriding the box rather than a distance is what makes it hold everywhere: {@code Mob} tests
     * melee range as this box against the target's hitbox, so the attack goal and anything else that
     * asks whether a swing would land are all answered by the one figure.
     */
    @Override
    protected AABB getAttackBoundingBox() {
        return getBoundingBox().inflate(ATTACK_REACH, 0.0, ATTACK_REACH);
    }

    /**
     * The blow. It is {@code Mob.doHurtTarget} with the knockback taken out and two effects put in,
     * so neither {@code super} is called: a husk's own adds the Hunger this boss does not want, and
     * a mob's own adds the shove it must not have.
     * <p>
     * <b>No knockback.</b> Not "less" - the push is not the attacker's to give. What a player feels
     * when a mob connects is applied by {@code LivingEntity#hurt} to whatever was hit, gated on
     * {@code #minecraft:no_knockback}, so the only way to state it is in the damage type - hence
     * {@link ModDamageTypes#COMPRESSED_HUSK}, which is vanilla's {@code mob_attack} in that one tag.
     * ({@code Attributes.ATTACK_KNOCKBACK} is the extra on top and is already zero here.) It matters
     * more on this boss than it would on any other: a hit that threw the player clear would be a hit
     * that undid the boss's whole problem for it, and the fight is about a distance that has to be
     * closed and held.
     * <p>
     * <b>The two effects.</b> Ten seconds of Blindness and Slowness V, which are the reach again from
     * the other side. Blindness takes away the ability to judge exactly how far away something is,
     * which is the one thing that matters against a boss with no reach, and Slowness takes away the
     * ability to get back out of that distance once it has been closed.
     */
    @Override
    public boolean doHurtTarget(Entity entity) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = damageSources().source(ModDamageTypes.COMPRESSED_HUSK, this);

        if (level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, getWeaponItem(), entity, source, damage);
        }

        boolean hurt = entity.hurt(source, damage);
        if (!hurt) {
            return false;
        }

        if (level() instanceof ServerLevel serverLevel) {
            EnchantmentHelper.doPostAttackEffects(serverLevel, entity, source);
        }

        setLastHurtMob(entity);
        playAttackSound();

        if (entity instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TOUCH_DURATION, 0, false, true, true), this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TOUCH_DURATION,
                    SLOWNESS_AMPLIFIER, false, true, true), this);
        }

        return true;
    }

    /** The three it carries, pressed back on once a second, and the step out of the fight. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.tickCount % BUFF_INTERVAL == 0) {
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_DURATION,
                    REGENERATION_AMPLIFIER, false, true, true));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION,
                    RESISTANCE_AMPLIFIER, false, true, true));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_DURATION,
                    SPEED_AMPLIFIER, false, true, true));
        }

        if (this.tickCount % TELEPORT_INTERVAL == 0) {
            stepAway();
        }
    }

    /**
     * Twenty blocks in a direction of its own choosing.
     * <p>
     * The direction is drawn fresh and the distance is exact, so what the player is given back is
     * always the same amount of ground to cross and never the same way across it. Each try scans
     * down for the first thing that would hold the boss up - the same walk an enderman's teleport
     * does - and then hands the landing to {@code randomTeleport}, which is what actually refuses a
     * spot the boss would not fit in. If none of {@link #TELEPORT_ATTEMPTS} directions works the
     * boss simply stays where it is until the next one comes round: a boss stuck in a wall is worse
     * than a boss that did not move.
     */
    private void stepAway() {
        if (!(level() instanceof ServerLevel)) {
            return;
        }

        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double x = getX() + Math.cos(angle) * TELEPORT_DISTANCE;
            double z = getZ() + Math.sin(angle) * TELEPORT_DISTANCE;

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, getY() + 4.0, z);
            while (pos.getY() > level().getMinBuildHeight() && !level().getBlockState(pos).blocksMotion()) {
                pos.move(0, -1, 0);
            }

            BlockState landing = level().getBlockState(pos);
            if (!landing.blocksMotion() || !landing.getFluidState().isEmpty()) {
                continue;
            }

            double fromX = getX();
            double fromY = getY();
            double fromZ = getZ();

            if (randomTeleport(x, pos.getY() + 1, z, true)) {
                level().playSound(null, fromX, fromY, fromZ, SoundEvents.ENDERMAN_TELEPORT,
                        getSoundSource(), 1.0F, 1.0F);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                return;
            }
        }
    }

    /** Its own kind, which is the one living thing it will not fight. */
    private static boolean isOwnKind(LivingEntity entity) {
        return entity instanceof CompressedHuskEntity;
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_HUSK.get() && type != EntityType.ARMOR_STAND;
    }

    /**
     * A husk turns into a drowned if it is left standing in water, which for a boss is a way of
     * deleting it that costs a bucket. It stays a husk wherever it is standing.
     */
    @Override
    protected boolean convertsInWater() {
        return false;
    }

    /**
     * Peaceful is a separate branch of {@code Mob.checkDespawn} that runs before the persistence
     * flag is even looked at, so a boss has to answer this as well as being persistent.
     */
    @Override
    public boolean shouldDespawnInPeaceful() {
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
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }
}
