package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.EnumSet;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A phantom cut out of compressed stone: it goes through walls, flies far faster than the thing it
 * is built on, and hits like a diamond sword through netherite armour.
 * <p>
 * Two of them exist in practice and they are the same entity. The Summoner calls up flocks of them
 * with no owner, and those hunt players the way an ordinary phantom does. The Summoning Staff calls
 * up one with an owner, and that one is an ally: it goes after hostile mobs and leaves people alone
 * unless they are already fighting - see {@link #isEnemyOf}.
 * <p>
 * Three things about {@link Phantom} have to be worked around. Its move control is package private,
 * so the flight speed cannot be set and is scaled after the fact in {@link #tick} instead; its
 * size accessor rewrites {@code ATTACK_DAMAGE} from the phantom size every time that syncs, so the
 * real damage is re-applied in {@link #onSyncedDataUpdated}; and its anchor point is package private
 * and starts at the world origin, so anything spawning one of these must run {@code finalizeSpawn}
 * or it will fly off towards 0, 0 and never come back.
 */
public class CompressedPhantomEntity extends Phantom {
    /** Fifty, against a vanilla phantom's twenty. */
    public static final float MAX_HEALTH = 50.0F;

    /** A full set of netherite: twenty armour and twelve toughness. */
    public static final double ARMOR = 20.0;
    public static final double ARMOR_TOUGHNESS = 12.0;

    /** Ten, against the six a vanilla phantom of size 0 does. */
    public static final float ATTACK_DAMAGE = 10.0F;

    /**
     * How far it looks for something to fight.
     * <p>
     * This is not decoration. Vanilla's phantom finds its target through {@code
     * PhantomAttackPlayerTargetGoal}, which scans sixty-four blocks on its own and ignores the
     * follow range entirely; that goal is thrown away in {@link #registerGoals}, so the replacement
     * runs on the attribute instead - and {@code Monster.createMonsterAttributes} leaves it at the
     * sixteen block default. A phantom is spawned twenty blocks up and circles up to fifteen out
     * from its anchor, so sixteen blocks is regularly less than the distance to the thing it is
     * supposed to be hunting, and a phantom that never acquires a target never moves off its anchor.
     */
    public static final double FOLLOW_RANGE = 64.0;

    /**
     * How much of its own speed it adds each tick. Vanilla's {@code PhantomMoveControl} is package
     * private and its 1.8 ceiling cannot be reached from here, so the delta it produces is scaled
     * up afterwards instead. The cap is what makes that safe: the control blends its next delta from
     * the current one, so an uncapped multiplier every tick compounds and runs away.
     */
    private static final double SPEED_MULTIPLIER = 1.8;

    /** The ceiling that keeps the scaling above from diverging, in blocks a tick. */
    private static final double MAX_SPEED = 2.4;

    /**
     * How recently a player must have hit something, or been hit, to count as in combat. Vanilla's
     * own combat tracker forgets a fight after five seconds of nothing, and this is that number -
     * its {@code inCombat} flag is private with no getter, so this reads the same idea off the two
     * public timestamps instead.
     */
    private static final int COMBAT_MEMORY_TICKS = 100;

    private static final String TAG_OWNER = "owner";
    private static final String TAG_ATTACK_DAMAGE = "compressed_attack_damage";
    private static final String TAG_MAX_HEALTH = "compressed_max_health";

    /** Who called it up, if anyone. Null for the Summoner's flocks, which belong to nobody. */
    @Nullable
    private UUID owner;

    /**
     * What it actually hits for. Kept as a field rather than read off the attribute because
     * {@link Phantom} overwrites that attribute from the phantom size whenever the size syncs, so
     * this is the value that has to be put back.
     */
    private float attackDamage = ATTACK_DAMAGE;

    /**
     * The timestamp of the owner's last fight this phantom has already joined, so one swing is acted
     * on once. Lives on the entity rather than in the goal only because the goal is rebuilt whenever
     * the entity is reloaded and this should not be.
     */
    private int lastOwnerFight;

    public CompressedPhantomEntity(EntityType<? extends Phantom> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 20;
        setPersistenceRequired();
    }

    /**
     * Nothing spawns one of these naturally - every compressed phantom is either a Summoning Staff's
     * or one of the Summoner's flock - so none of them has any business being cleaned up as scenery.
     * Vanilla would do it anyway, and quickly: {@code Mob.checkDespawn} discards an unpersisted mob
     * outright past a hundred and twenty-eight blocks, and this one covers ground at more than two
     * blocks a tick, so a flock left circling while the fight moved on was gone within seconds.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * The peaceful check runs <em>before</em> the persistence check, so it is the one thing
     * {@link #removeWhenFarAway} above does not cover. A flock of the Summoner's is a monster and
     * goes with the rest of them; one belonging to a player is that player's, and stays.
     */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return this.owner == null;
    }

    /**
     * Vanilla gives a phantom nothing but {@code Monster.createMonsterAttributes} - there is no
     * {@code Phantom.createAttributes} to build on - so every number this one cares about is stated
     * here. {@code ATTACK_DAMAGE} is only the starting value: {@link Phantom} rewrites it from the
     * phantom size whenever that syncs, which is what {@link #applyAttackDamage} exists to undo.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    /**
     * Vanilla's three flight goals are kept and its targeting is thrown away: the phantom's own
     * target goal hunts the nearest player, which would have a summoned one turn on the person who
     * summoned it. What it hunts instead is {@link #isEnemyOf}, which answers differently depending
     * on whether it has an owner.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(goal -> true);
        // What its owner is fighting comes first, so a phantom already hunting some skeleton drops
        // it the moment the person who summoned it swings at something else.
        this.targetSelector.addGoal(1, new OwnerCombatTargetGoal(this));
        // No HurtByTargetGoal to go with it: that one needs a PathfinderMob and a phantom is a
        // FlyingMob, so retaliation has to come out of the same predicate as everything else.
        //
        // Line of sight is not required, and that is the point of this entity: it goes through
        // walls, so a target it cannot see is still a target it can reach. Vanilla's own phantom
        // goal did not check sight either.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, false,
                this::isEnemyOf));
    }

    /** The player who summoned it, if they are still in this world. */
    @Nullable
    private Player ownerPlayer() {
        return this.owner == null ? null : level().getPlayerByUUID(this.owner);
    }

    /**
     * A wolf's pair of owner goals, in one: it goes after whatever its owner last hit, and after
     * whoever last hit its owner.
     * <p>
     * Vanilla's {@code OwnerHurtTargetGoal} and {@code OwnerHurtByTargetGoal} cannot be reused
     * because both are written against {@code TamableAnimal}, which a phantom is not. The timestamps
     * are the working part and they are public on any {@code LivingEntity}: each fight is acted on
     * once, so a target that is given up on, or fled from, is not immediately picked up again.
     */
    private static final class OwnerCombatTargetGoal extends TargetGoal {
        private final CompressedPhantomEntity phantom;
        @Nullable
        private LivingEntity found;
        private int timestamp;

        private OwnerCombatTargetGoal(CompressedPhantomEntity phantom) {
            super(phantom, false);
            this.phantom = phantom;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = this.phantom.ownerPlayer();
            if (owner == null) {
                return false;
            }

            // Whichever of the two happened later is the fight it joins.
            LivingEntity struck = owner.getLastHurtMob();
            LivingEntity attacker = owner.getLastHurtByMob();
            int struckAt = owner.getLastHurtMobTimestamp();
            int attackedAt = owner.getLastHurtByMobTimestamp();

            if (attacker != null && (struck == null || attackedAt >= struckAt)) {
                this.found = attacker;
                this.timestamp = attackedAt;
            } else if (struck != null) {
                this.found = struck;
                this.timestamp = struckAt;
            } else {
                return false;
            }

            return this.timestamp != this.phantom.lastOwnerFight
                    && this.found != this.phantom
                    && !(this.found instanceof CompressedPhantomEntity)
                    && this.found != owner
                    && canAttack(this.found, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.found);
            this.phantom.lastOwnerFight = this.timestamp;
            super.start();
        }
    }

    /**
     * Who this phantom will go after.
     * <p>
     * With no owner it is one of the Summoner's, and it hunts players, which is what an ordinary
     * phantom does and what the mod's rule for a non-boss hostile asks for. Anything else that
     * attacks it is picked up by the {@link Enemy} arm of the owned case being false here - an
     * unowned phantom simply ignores other monsters rather than fighting them.
     * <p>
     * With an owner it is an ally, and the rule is: any hostile mob, and a person only if that
     * person is already in a fight. Its owner is never a target, and neither is another compressed
     * phantom, so two flocks from different sources will ignore each other rather than tangle.
     */
    private boolean isEnemyOf(LivingEntity entity) {
        if (entity instanceof CompressedPhantomEntity || entity instanceof ArmorStand || !entity.canBeSeenAsEnemy()) {
            return false;
        }

        if (this.owner == null) {
            return entity instanceof Player;
        }

        if (entity instanceof Player player) {
            return !player.getUUID().equals(this.owner) && isInCombat(player);
        }

        return entity instanceof Enemy;
    }

    /**
     * Whether {@code entity} has hit something, or been hit, recently enough to still be fighting.
     * {@code CombatTracker} knows this exactly but keeps it to itself, so this reads the two public
     * timestamps {@code LivingEntity} does expose.
     */
    private static boolean isInCombat(LivingEntity entity) {
        int now = entity.tickCount;

        return (entity.getLastHurtByMob() != null && now - entity.getLastHurtByMobTimestamp() <= COMBAT_MEMORY_TICKS)
                || (entity.getLastHurtMob() != null && now - entity.getLastHurtMobTimestamp() <= COMBAT_MEMORY_TICKS);
    }

    /** Who summoned it, which is what turns it from a monster into an ally. */
    public void setOwner(@Nullable Player owner) {
        this.owner = owner == null ? null : owner.getUUID();
    }

    /**
     * What the Compression Energy on a Summoning Staff buys: a point of health and a quarter point of
     * damage per digit, both applied here rather than on the staff, since the phantom outlives the
     * cast that made it.
     */
    public void setStats(float maxHealth, float attackDamage) {
        this.attackDamage = attackDamage;

        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(maxHealth);
        }

        applyAttackDamage();
        setHealth(getMaxHealth());
    }

    /**
     * The one place the damage is put back. {@link Phantom} recalculates {@code ATTACK_DAMAGE} from
     * the phantom size every time that syncs - including once during {@code finalizeSpawn} - so
     * anything set before then would be quietly replaced by vanilla's {@code 6 + size}.
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        applyAttackDamage();
    }

    private void applyAttackDamage() {
        AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && damage.getBaseValue() != this.attackDamage) {
            damage.setBaseValue(this.attackDamage);
        }
    }

    /**
     * Walls are no obstacle, the way they are none to a vex: physics are switched off for the tick
     * in which movement happens and back on afterwards, so nothing else that reads the flag - the
     * renderer, anything riding it - sees a phantom that is permanently intangible.
     * <p>
     * The speed scaling rides along here for a reason of ordering: the move control and {@code travel}
     * have both run by the time {@code super.tick} returns, so this is scaling the delta the phantom
     * actually ended the tick with rather than a number something downstream is about to overwrite.
     */
    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;

        if (!level().isClientSide()) {
            Vec3 movement = getDeltaMovement();
            double speed = movement.length();
            if (speed > 1.0E-4) {
                setDeltaMovement(movement.scale(Math.min(speed * SPEED_MULTIPLIER, MAX_SPEED) / speed));
            }
        }
    }

    /** Stone does not burn off in the morning, and a summoned ally that did would be worthless. */
    @Override
    public boolean isSunBurnTick() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.owner != null) {
            compound.putUUID(TAG_OWNER, this.owner);
        }

        compound.putFloat(TAG_ATTACK_DAMAGE, this.attackDamage);
        compound.putFloat(TAG_MAX_HEALTH, getMaxHealth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.owner = compound.hasUUID(TAG_OWNER) ? compound.getUUID(TAG_OWNER) : null;

        if (compound.contains(TAG_ATTACK_DAMAGE)) {
            // Read back through setStats rather than set directly, so the damage is put back on the
            // attribute at the same time. setStats fills the health to the new maximum, which is
            // right when a staff has just made one and wrong here - vanilla read the saved health in
            // the super call above - so it is taken before and put back after.
            float health = getHealth();
            setStats(compound.getFloat(TAG_MAX_HEALTH), compound.getFloat(TAG_ATTACK_DAMAGE));
            setHealth(health);
        }
    }
}
