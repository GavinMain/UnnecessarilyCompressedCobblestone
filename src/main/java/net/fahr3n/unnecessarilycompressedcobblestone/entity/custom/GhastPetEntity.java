package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.EnumSet;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.util.PetAi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Ghast Pet: a quarter-sized ghast that follows whoever hatched it and throws the Compressed
 * Ghast's own fire charges at whatever is threatening them.
 * <p>
 * It is the third thing built out of that fight, and it is deliberately the mildest. The boss throws
 * nine charges at five hundred each and hangs a screaming flock on whatever they hit; this throws
 * one at a time, for {@link #CHARGE_DAMAGE}, and presses no Ghasted at all - see
 * {@code GhastFireChargeEntity.NO_GHASTED}, which exists for exactly this. A pet that could do what
 * the boss does would be worth more than beating the boss was.
 * <p>
 * Everything about who it fights is {@code PetAi} and nothing of its own: it joins whatever fight
 * its owner is in, hunts hostile mobs on sight, never targets its owner, and leaves other people
 * alone unless they are already swinging at somebody. That is the same rule the summoned phantom
 * and the summoned silverfish follow, written once.
 * <p>
 * It is built on {@link AbstractFriendlyGhastEntity} rather than on {@code Ghast}, and that is not
 * a detail: {@code Ghast} implements {@code Enemy}, which is what an iron golem's targeting goal
 * looks for, so a pet built on it would be attacked on sight by every golem in every village. See
 * that class. What is left to do here is the flying - a {@link FlyingMoveControl} and a
 * {@link FlyingPathNavigation}, because the follow goal paths and vanilla's ghast does not - and
 * {@link #travel}, because {@code FlyingMob}'s own flight cannot be tuned at all.
 */
public class GhastPetEntity extends AbstractFriendlyGhastEntity implements PetAi.Adoptable {
    /** A quarter of a ghast, the same as the Ghasted flock: about a player's height. */
    public static final double SCALE = 0.25;

    /** Forty. It is a pet rather than a wall, and it is meant to be worth protecting. */
    public static final float MAX_HEALTH = 40.0F;

    /** What one of its charges is worth: a fortieth of the boss's, and about a diamond sword's. */
    public static final float CHARGE_DAMAGE = 12.0F;

    /** How far it looks for something to fight, and how far a charge is thrown. */
    public static final double SEARCH_RADIUS = 24.0;

    /** How long between charges. Three seconds, of which the last {@link #WIND_UP} is the mouth. */
    private static final int SHOOT_INTERVAL = 60;

    /** How long the mouth is open before a charge leaves it - a ghast's own telegraph, shortened. */
    private static final int WIND_UP = 20;

    /** How far in front a charge is born, scaled to something a quarter of a ghast's size. */
    private static final double MUZZLE = 1.0;

    /**
     * How much speed it adds each tick, and how much survives one. It settles at the acceleration
     * over one minus the drag - half a block a tick, or ten a second, which is comfortably above a
     * sprint and is what a pet has to make to be a pet rather than a thing you keep waiting for.
     * <p>
     * They exist because {@link net.minecraft.world.entity.FlyingMob}'s own flight cannot be tuned
     * at all. It hard-codes an acceleration of 0.02 and {@code moveRelative} normalises any input
     * longer than one block before scaling by it, so past {@code FLYING_SPEED} of 1 a ghast's
     * terminal speed is pinned at 0.02 over 0.09 - about four blocks a second - whatever any
     * attribute says. Multiplying the acceleration is the mod's rule for exactly this reason;
     * scaling the delta afterwards would feed straight back into the move control.
     */
    private static final float FLIGHT_ACCELERATION = 0.05F;
    private static final double FLIGHT_DRAG = 0.9;

    /** How close its owner has to get before it sets off, stops, and is put back by hand. */
    private static final double FOLLOW_START = 8.0;
    private static final double FOLLOW_STOP = 3.0;
    private static final double FOLLOW_TELEPORT = 24.0;

    private static final String TAG_OWNER = "pet_owner";

    @Nullable
    private UUID owner;

    private int lastOwnerFight;

    public GhastPetEntity(EntityType<? extends GhastPetEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setPersistenceRequired();
    }

    /**
     * A ghast's own attributes, shrunk and given a pet's health. {@code FOLLOW_RANGE} is stated
     * because the targeting goal reads it and vanilla's ghast leaves it at a hundred, which on
     * something that fights for a player is a pet that flies off after a skeleton three chunks away.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFriendlyGhastEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS)
                .add(Attributes.SCALE, SCALE)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                // Not decoration and not optional: FlyingMoveControl reads FLYING_SPEED every tick
                // it is steering, and a ghast has no such attribute of its own. One is the largest
                // figure worth stating - the control writes it into zza, and moveRelative normalises
                // anything longer than a block, so a larger number would be thrown away.
                .add(Attributes.FLYING_SPEED, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new ChargeGoal(this));
        // No wandering goal to go with the follow, and there is nothing to add one to: vanilla's
        // flying wander is written against a PathfinderMob and a ghast is a FlyingMob. It is no loss
        // - a pet that is not following and not fighting should be hanging next to the person it
        // belongs to, which is exactly what a follow goal with nothing else in its slot produces.
        this.goalSelector.addGoal(6,
                new PetAi.FollowOwnerGoal<>(this, FOLLOW_START, FOLLOW_STOP, FOLLOW_TELEPORT));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // Its owner's fight comes first, so a pet already chasing something drops it the moment the
        // person it belongs to swings at something else.
        this.targetSelector.addGoal(1, new PetAi.OwnerCombatTargetGoal<>(this));
        // No HurtByTargetGoal: that one is written against a PathfinderMob and a ghast is a
        // FlyingMob, so retaliation comes out of the same predicate as everything else - anything
        // hostile within reach is already a target.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, entity -> PetAi.isEnemyOf(this, this, entity)));
    }

    /**
     * Flight, with the acceleration {@link net.minecraft.world.entity.FlyingMob} hard-codes replaced
     * by the pet's own - see {@link #FLIGHT_ACCELERATION}. Water and lava are left to that class,
     * which already has its own figures for both.
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (!isControlledByLocalInstance() || isInWater() || isInLava()) {
            super.travel(travelVector);
            return;
        }

        moveRelative(FLIGHT_ACCELERATION, travelVector);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(FLIGHT_DRAG));
        calculateEntityAnimation(false);
    }

    /** Nothing spawns one of these; every one was hatched by somebody and belongs to them. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * The peaceful branch of {@code Mob.checkDespawn} runs before the persistence flag is looked at,
     * so it is a separate answer from {@link #removeWhenFarAway}. {@code Mob}'s own is already false
     * - it is {@code Ghast} that overrides it to true, and this is no longer a ghast - so this is
     * stated rather than needed, and is here so that nothing about the base class can quietly start
     * deleting these when the difficulty changes.
     */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** Conjured rather than paid for, so it leaves nothing behind - the mod's rule for every pet. */
    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    /* WHAT MAKES IT A PET - see PetAi, which owns both goals written against these four. */

    @Nullable
    @Override
    public UUID petOwner() {
        return this.owner;
    }

    @Override
    public void setPetOwner(@Nullable Player owner) {
        this.owner = owner == null ? null : owner.getUUID();
    }

    @Override
    public int lastOwnerFight() {
        return this.lastOwnerFight;
    }

    @Override
    public void setLastOwnerFight(int timestamp) {
        this.lastOwnerFight = timestamp;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.owner != null) {
            compound.putUUID(TAG_OWNER, this.owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.owner = compound.hasUUID(TAG_OWNER) ? compound.getUUID(TAG_OWNER) : null;
    }

    /**
     * One charge every {@link #SHOOT_INTERVAL} ticks at whatever it is fighting, with the mouth open
     * for the last {@link #WIND_UP} of them.
     * <p>
     * The charge steers, which is the boss's homing volley rather than its scatter, and that is the
     * right half to give a pet: a scatter shot is answered by not standing in the cone, and a pet
     * has no way of knowing that its owner is standing in it.
     */
    static class ChargeGoal extends Goal {
        private final GhastPetEntity ghast;
        private int charge;

        ChargeGoal(GhastPetEntity ghast) {
            this.ghast = ghast;
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.ghast.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.charge = 0;
        }

        @Override
        public void stop() {
            this.ghast.setCharging(false);
        }

        @Override
        public void tick() {
            LivingEntity target = this.ghast.getTarget();
            if (target == null) {
                return;
            }

            this.ghast.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Out of range or behind a wall costs the wind-up rather than being shot through it,
            // and the counter unwinds instead of resetting - the same rule the boss's volley uses.
            if (this.ghast.distanceToSqr(target) > SEARCH_RADIUS * SEARCH_RADIUS
                    || !this.ghast.hasLineOfSight(target)) {
                if (this.charge > 0) {
                    this.charge--;
                }

                this.ghast.setCharging(false);
                return;
            }

            this.charge++;
            this.ghast.setCharging(this.charge >= SHOOT_INTERVAL - WIND_UP);

            if (this.charge >= SHOOT_INTERVAL) {
                fire(target);
                this.charge = 0;
            }
        }

        private void fire(LivingEntity target) {
            Level level = this.ghast.level();
            if (!this.ghast.isSilent()) {
                // 1016 is vanilla's ghast shot, so a pet firing sounds like the thing it is.
                level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
            }

            Vec3 muzzle = this.ghast.position().add(0.0, this.ghast.getBbHeight() * 0.5, 0.0);
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                    .subtract(muzzle).normalize();

            GhastFireChargeEntity charge = new GhastFireChargeEntity(level, this.ghast, aim);
            charge.setPos(muzzle.add(aim.scale(MUZZLE)));
            charge.setDamage(CHARGE_DAMAGE);
            charge.setGhastedAmplifier(GhastFireChargeEntity.NO_GHASTED);
            charge.makeHoming(target);
            level.addFreshEntity(charge);
        }
    }
}
