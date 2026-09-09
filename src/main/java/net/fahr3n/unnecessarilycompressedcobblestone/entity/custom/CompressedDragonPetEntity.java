package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * What is left of the Compressed Dragon once the fight is over: the same beast, a tenth of the size
 * of the one that was fought, and something to fly rather than something to fight.
 * <p>
 * It hatches out of the one egg the second phase leaves behind, and it is the only dragon in this
 * mod that is not a {@code Monster} - see {@link AbstractCompressedDragonEntity}, which is
 * deliberately a {@code PathfinderMob} so that this one is not an {@code Enemy} that every iron
 * golem in the game charges at.
 * <p>
 * Left alone it drifts about under its own wings - a flying move control, a flying navigation and
 * one wandering goal, which is the whole of what this mod needs anywhere to make a thing fly. With
 * somebody on it none of that is consulted at all: {@code LivingEntity} routes a ridden mob through
 * {@code travelRidden}, and flying it from there is vanilla's own riding path with the same four
 * hooks the Ghast Mount answers, and for the same reasons:
 * {@link #getControllingPassenger} is what makes {@code LivingEntity} route movement through
 * {@code travelRidden} at all, {@link #getRiddenInput} turns the rider's keys and the pitch of their
 * head into a direction - which is why looking up and pressing forward climbs - and {@link #travel}
 * is overridden because nothing about a {@code PathfinderMob}'s ground movement is any use to a
 * thing with wings.
 * <p>
 * {@link #RIDDEN_ACCELERATION} over {@link #FLIGHT_DRAG} is the speed it actually flies at, and it
 * is quoted that way round on purpose: what a flying thing settles at is its acceleration divided by
 * one minus its drag.
 */
public class CompressedDragonPetEntity extends AbstractCompressedDragonEntity {
    /** Two hundred. It is flown through fights rather than fighting them, and should survive one. */
    public static final float MAX_HEALTH = 200.0F;

    /**
     * How much of a full dragon it is. A tenth: the boss is registered at sixteen blocks across, and
     * a mount that size could not be flown anywhere a player wants to go.
     * <p>
     * {@code SCALE} and nothing else, which is the mod's rule for size: vanilla multiplies the
     * hitbox by it and the renderer multiplies the drawing by it, so the shape on screen and the
     * shape an arrow hits cannot drift apart.
     */
    public static final double PET_SCALE = 0.1;

    /**
     * How much speed the rider adds each tick. With {@link #FLIGHT_DRAG} this settles at about a
     * block a tick - twenty a second, rather faster than the Ghast Mount, because a dragon should
     * be the best way of getting about that this mod hands out.
     */
    public static final float RIDDEN_ACCELERATION = 0.1F;

    /** How much of its speed survives a tick. Vanilla's own flight drag, unchanged. */
    private static final double FLIGHT_DRAG = 0.9;

    /** How much of the forward push a sideways key is worth. Strafing is for aiming, not travelling. */
    private static final double STRAFE_SCALE = 0.5;

    public CompressedDragonPetEntity(EntityType<? extends CompressedDragonPetEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setPersistenceRequired();
        // Always in the air and never through a wall: it is drawn with its wings out, and a mount
        // that ignored walls would be a mount that parked inside them.
        setFlying(true);
        setPhasing(false);

        // Flight when nobody is on it is the same three things it is for anything else in this mod:
        // something that steers in the air, something that paths through it, and no gravity - the
        // third of which the flying flag above already does. Without the first two the pet has a
        // wandering goal it cannot act on, and a dragon that hangs motionless in the air until
        // somebody climbs on it is not a thing that flies.
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setPathfindingMalus(PathType.WATER, -1.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createDragonAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                // What the flying move control steers at when there is no rider. Deliberately far
                // below what a rider gets out of RIDDEN_ACCELERATION: left to itself it drifts.
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.SCALE, PET_SCALE)
                // Being shoved off course by every arrow that hits it is the one thing that would
                // make it unflyable.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * Enough to fly on its own and nothing more. It fights nothing and follows nobody: what these
     * three do is keep it in the air and moving when there is no rider, which is the difference
     * between a mount and a statue that can be sat on.
     * <p>
     * All of them are dropped the moment somebody climbs on, without any of them knowing it:
     * {@code LivingEntity} routes a ridden mob through {@code travelRidden} instead of its goals'
     * navigation, and {@link #travel} then answers to the rider alone.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /** Right-clicking an empty-handed, unoccupied dragon gets on it. Shift gets back off, as ever. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isVehicle() || player.isSecondaryUseActive() || !player.getItemInHand(hand).isEmpty()) {
            return super.mobInteract(player, hand);
        }

        if (!level().isClientSide()) {
            player.startRiding(this);
        }

        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    /**
     * Whoever is sitting on it, which is the single switch that hands movement to the rider:
     * {@code LivingEntity.aiStep} routes through {@code travelRidden} only when this is a player.
     */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    /**
     * The dragon is pointed wherever the rider is looking, and the model follows: the neck and the
     * tail are drawn out of the last sixty-four ticks of this rotation, so a turn written here is a
     * turn the whole beast leans into a moment later.
     */
    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        setYRot(player.getYRot());
        this.yRotO = getYRot();
        // Half the rider's pitch: a dragon that pointed straight down every time somebody looked at
        // their feet would spend the flight on its nose.
        setXRot(player.getXRot() * 0.5F);
        this.yHeadRot = getYRot();
        this.yBodyRot = getYRot();
    }

    /**
     * The rider's keys, turned into a direction in three dimensions.
     * <p>
     * {@code moveRelative} rotates the x and z of this vector by the mount's yaw and leaves the y
     * alone, so splitting the forward key between {@code y} and {@code z} by the rider's pitch is
     * the whole of "you fly where you are looking". There is no separate ascend key and there does
     * not need to be one: pitch covers the full range.
     */
    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        double pitch = Math.toRadians(player.getXRot());
        double forward = player.zza;

        return new Vec3(player.xxa * STRAFE_SCALE, forward * -Math.sin(pitch), forward * Math.cos(pitch));
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return RIDDEN_ACCELERATION;
    }

    /**
     * Flight: the rider's acceleration, a flat drag on the whole delta, and no gravity - which is
     * the base class's, off the flying flag this one never turns off.
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (!isControlledByLocalInstance() || getControllingPassenger() == null) {
            super.travel(travelVector);
            return;
        }

        moveRelative(getSpeed(), travelVector);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(FLIGHT_DRAG));
        calculateEntityAnimation(false);
    }

    /** Hatched rather than paid for, so it leaves nothing behind - the mod's rule for every summon. */
    @Override
    protected boolean shouldDropLoot() {
        return false;
    }
}
