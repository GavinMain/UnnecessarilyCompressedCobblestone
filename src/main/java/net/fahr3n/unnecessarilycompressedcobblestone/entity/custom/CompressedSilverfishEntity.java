package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.util.PetAi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * A silverfish that flies, moves several times as fast as one that does not, and leaves
 * {@link ModMobEffects#COMPRESSED_INFESTATION} in whatever it bites.
 * <p>
 * It is a {@link Silverfish} subclass so that vanilla's model, renderer and sounds come free.
 * {@code super.registerGoals()} is deliberately not called: two of the goals it adds are about being
 * a silverfish on the ground - hiding inside stone and calling the neighbours out of it - and neither
 * means anything to something in the air. The one field that costs is {@code friendsGoal}, which is
 * private and stays null; vanilla's own {@code hurt} null-checks it, so nothing breaks.
 * <p>
 * Its bite is the point of it. One point of damage sounds like nothing and is not, because it goes
 * through {@link ModDamageTypes#COMPRESSED_SILVERFISH}, which is in every {@code bypasses_*} tag that
 * means "protection does not apply" - so the deepest armour in this mod answers it exactly as well as
 * no armour at all, and the only thing between a player and a swarm is killing them. What the swarm
 * costs is the effect it leaves behind, not the point of damage.
 */
public class CompressedSilverfishEntity extends Silverfish implements PetAi.Pet {
    /** What the bite is worth. It is small on purpose: nothing can reduce it. */
    private static final float TRUE_DAMAGE = 1.0F;

    /** How long the infestation lasts on whatever was bitten. */
    private static final int INFESTATION_TICKS = 400;

    /** NBT keys. */
    private static final String BROODLING_TAG = "Broodling";
    private static final String OWNER_TAG = "Owner";
    private static final String DAMAGE_TAG = "TrueDamage";

    /** How close a summoned one stays to whoever called it up, and how far it will be left behind. */
    private static final double FOLLOW_START = 10.0;
    private static final double FOLLOW_STOP = 4.0;
    private static final double FOLLOW_TELEPORT = 24.0;

    /**
     * Whether this one hatched out of an infestation rather than being placed in the world.
     * <p>
     * It is the whole answer to a loop that would otherwise print blocks: the effect spawns
     * silverfish when its holder is hurt, those silverfish bite and re-apply the effect, and every
     * one of them that dies would drop a block of the deepest stone the mod has. A brood member
     * drops nothing at all, so the loop still costs a fight and yields no stone - and a silverfish
     * out of a Silverfish TNT, which was paid for with that stone, drops as it should.
     */
    private boolean broodling;

    /**
     * Who summoned it, if anyone. A wild one is null and hunts players like any other monster; one
     * out of a Summoning Staff carrying the Silverfish engraving belongs to the caster and fights
     * for them instead - which is the whole of what that engraving buys.
     */
    @Nullable
    private UUID owner;

    /**
     * What its bite is worth. A field rather than the constant because a summoned one carries the
     * staff's Compression Energy and Surge; a wild one is always {@link #TRUE_DAMAGE}.
     */
    private float trueDamage = TRUE_DAMAGE;

    /** The owner's last fight this one has already joined, so a single swing is acted on once. */
    private int lastOwnerFight;

    public CompressedSilverfishEntity(EntityType<? extends Silverfish> entityType, Level level) {
        super(entityType, level);
        // Flight is three things and all of them are needed: something that steers in the air,
        // something that paths through it, and no gravity pulling the result back down.
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    /**
     * A vanilla silverfish's numbers with the speed trebled and flying speed to match. Attack damage
     * is left where vanilla has it and is never actually read - {@link #doHurtTarget} states its own
     * figure, since the damage type is the point rather than the number.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.75)
                .add(Attributes.FLYING_SPEED, 0.75)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    /**
     * Its own goals rather than vanilla's. Hiding in stone and waking the neighbours are both about
     * a silverfish standing on the ground, so what is left is: swim, chase, wander and look.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        // Between fights a summoned one stays with its owner; a wild one has no owner, the goal
        // never starts, and the wandering goal below has the slot to itself.
        this.goalSelector.addGoal(6, new PetAi.FollowOwnerGoal<>(this, FOLLOW_START, FOLLOW_STOP, FOLLOW_TELEPORT));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // Whatever its owner is fighting comes first, so a summoned one drops what it is chasing
        // the moment the person who called it up swings at something else.
        this.targetSelector.addGoal(1, new PetAi.OwnerCombatTargetGoal<>(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers());
        // One predicate covers both lives: a wild one hunts players, a summoned one hunts monsters.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isEnemyOf));
    }

    /**
     * Who this one will go after, which is {@link PetAi#isEnemyOf} plus one exclusion of its own.
     * <p>
     * With no owner it is the ordinary hostile rule this project asks for: the player, and whoever
     * hit it, which {@code HurtByTargetGoal} covers. With an owner it is the wolf's rule instead -
     * any hostile mob, and a person only while that person is already in a fight - so a summoned one
     * never turns on the caster, and two summoned from different staves ignore each other.
     * <p>
     * The chicken is the exclusion, and it is this mob's alone: the Compressed Cobblestone Chicken
     * is livestock the mod hands out by the field, and a swarm that ate it would make the Chicken
     * TNT and the Silverfish TNT unusable in the same base.
     */
    private boolean isEnemyOf(LivingEntity entity) {
        return !(entity instanceof Chicken) && PetAi.isEnemyOf(this, this, entity);
    }

    /* WHAT MAKES IT A PET - see PetAi, which owns both goals written against these three. */

    @Nullable
    @Override
    public UUID petOwner() {
        return this.owner;
    }

    @Override
    public int lastOwnerFight() {
        return this.lastOwnerFight;
    }

    @Override
    public void setLastOwnerFight(int timestamp) {
        this.lastOwnerFight = timestamp;
    }

    /**
     * The bite. It replaces vanilla's melee entirely rather than adding to it, because everything
     * vanilla's does - reading {@code ATTACK_DAMAGE}, applying knockback, running the weapon's
     * enchantments - is about a hit that armour is allowed to answer, and this one is not.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = target.hurt(
                this.damageSources().source(ModDamageTypes.COMPRESSED_SILVERFISH, this), this.trueDamage);

        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModMobEffects.COMPRESSED_INFESTATION,
                    INFESTATION_TICKS, 0, false, true, true), this);
        }

        return hurt;
    }

    /** Its own bite cannot infest it, and neither can another one of its kind. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(ModMobEffects.COMPRESSED_INFESTATION) && super.canBeAffected(effect);
    }

    /** One of its own kind's bite does nothing to it at all. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.getEntity() instanceof CompressedSilverfishEntity || super.isInvulnerableTo(source);
    }

    /**
     * It will not fight a chicken, whoever hatched it.
     * <p>
     * This is the other half of the Compressed Chicken Boss's second phase: the brood it calls up
     * carries the infestation so that these hatch out of it, and a swarm that turned on the
     * hatchery the moment one of the boss's own eggs clipped it would undo the whole ability. It is
     * stated as an alliance rather than inside {@link #isEnemyOf} because alliance is what
     * {@code TargetingConditions} consults before <em>every</em> acquisition - so it covers
     * {@code HurtByTargetGoal} as well, which is the goal that would actually have turned it round.
     */
    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof Chicken || super.isAlliedTo(entity);
    }

    /** Marks this one as having hatched or been summoned rather than paid for; see {@link #broodling}. */
    public void setBroodling() {
        this.broodling = true;
    }

    /** Who summoned it, which is what turns it from a monster into an ally. */
    public void setOwner(@Nullable Player owner) {
        this.owner = owner == null ? null : owner.getUUID();
    }

    /**
     * What the Compression Energy and Surge on a Summoning Staff buy. Health is an attribute; the
     * bite is not, because it is not {@code ATTACK_DAMAGE} - it goes through the mod's own damage
     * type, so the figure lives on the entity and {@link #doHurtTarget} states it.
     */
    public void setStats(float maxHealth, float trueDamage) {
        this.trueDamage = trueDamage;

        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(maxHealth);
        }

        setHealth(getMaxHealth());
    }

    /** A summoned one belongs to somebody and has no business being cleaned up as scenery. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.owner == null;
    }

    /** The peaceful check runs before the persistence one, so it is a third, separate override. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return this.owner == null;
    }

    @Override
    protected boolean shouldDropLoot() {
        return !this.broodling && super.shouldDropLoot();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(BROODLING_TAG, this.broodling);
        compound.putFloat(DAMAGE_TAG, this.trueDamage);
        if (this.owner != null) {
            compound.putUUID(OWNER_TAG, this.owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.broodling = compound.getBoolean(BROODLING_TAG);
        this.trueDamage = compound.contains(DAMAGE_TAG) ? compound.getFloat(DAMAGE_TAG) : TRUE_DAMAGE;
        this.owner = compound.hasUUID(OWNER_TAG) ? compound.getUUID(OWNER_TAG) : null;
    }
}
