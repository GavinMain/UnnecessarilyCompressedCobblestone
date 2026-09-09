package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.network.PlatformSyncPayload;
import net.fahr3n.unnecessarilycompressedcobblestone.util.MountPlatform;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Ghast Mount: a full-sized ghast that will be sat on, flown, and built on.
 * <p>
 * It is the one thing out of the Compressed Ghast fight that does not fight at all. It has no goals
 * whatsoever, so left alone it hangs exactly where it was put and everything it does is done by
 * whoever is sitting on it.
 * <p>
 * Like the pet, it is built on {@link AbstractFriendlyGhastEntity} rather than on {@code Ghast},
 * because {@code Ghast} implements {@code Enemy} and a mount that every iron golem in the game
 * charges at is not a mount. See that class.
 * <p>
 * Flying it is vanilla's own riding path with three of its four hooks answered.
 * {@link #getControllingPassenger} is what makes {@code LivingEntity} route movement through
 * {@code travelRidden} at all; {@link #getRiddenInput} turns the rider's keys and the pitch of their
 * head into a direction, which is why looking up and pressing forward climbs; and {@link #travel} is
 * overridden because {@link net.minecraft.world.entity.FlyingMob}'s own hard-codes an acceleration
 * of 0.02 that no attribute can reach, and 0.02 against its 0.91 of drag settles at a fifth of a
 * block a tick - slower than walking. {@link #RIDDEN_ACCELERATION} over {@link #FLIGHT_DRAG} is the
 * speed it actually flies at, and it is quoted that way round on purpose: what a flying thing
 * settles at is its acceleration divided by one minus its drag, exactly as a ghast's own fireball
 * is.
 * <p>
 * There is no gravity to answer and no fall damage to spare anybody: {@code FlyingMob} already
 * writes neither, so a rider who dismounts three hundred blocks up falls on their own account and a
 * mount that is left there stays there.
 * <p>
 * <b>The deck.</b> Its back is a nine-by-nine grid that can be built on - the platform saddle, and
 * the reason to own one that a pet ghast does not answer. The blocks are {@link MountPlatform}
 * cells rather than world blocks, so they travel with the creature instead of being rebuilt under
 * it; {@link PlatformColliderEntity} is what a foot actually lands on, since an entity's box is the
 * only surface a mod can put in the air; and {@link #carry} is what stops everything standing on
 * the deck being left behind the moment it moves, because nothing in vanilla carries a passenger
 * that is not sitting down. Right-click the deck with a full block to lay one, hit it to take it
 * back. Three things follow from the deck being the mount's own and not the world's: no chunk is
 * touched, nothing on it can hold an inventory (there is nowhere to keep one), and every cell comes
 * back as an item when the mount dies.
 */
public class GhastMountEntity extends AbstractFriendlyGhastEntity implements IEntityWithComplexSpawn {
    /** Sixty. It is ridden through fights rather than fighting, and is meant to survive one. */
    public static final float MAX_HEALTH = 60.0F;

    /**
     * How much speed the rider adds each tick. With {@link #FLIGHT_DRAG} this settles at eight
     * tenths of a block a tick - about sixteen a second, which is a little over a sprint and about
     * what an elytra makes on a gentle glide.
     */
    public static final float RIDDEN_ACCELERATION = 0.08F;

    /** How much of its speed survives a tick. Vanilla's own flight drag, unchanged. */
    private static final double FLIGHT_DRAG = 0.9;

    /** How much of the forward push a sideways key is worth. Strafing is for aiming, not travelling. */
    private static final double STRAFE_SCALE = 0.5;

    /** Where the pilot sits with nothing built: on the creature's own back, as it always was. */
    private static final double BARE_SEAT = 3.0;

    /**
     * Where the pilot sits once there is a deck: standing on the floor layer, which is one block
     * above where that layer starts. A pilot left at {@link #BARE_SEAT} would be inside the floor.
     */
    private static final double DECK_SEAT = MountPlatform.FLOOR_OFFSET + 1.0;

    /** How far above the deck something may be and still be carried by it. Room for a jump. */
    private static final double CARRY_HEADROOM = 3.0;

    /** What has been built on its back. Replaced wholesale on the client by a sync payload. */
    private MountPlatform platform = new MountPlatform();

    /** The collider entities covering the deck, by id. Server side only; they are never saved. */
    private final List<Integer> colliders = new ArrayList<>();

    /** The cell boxes those colliders were cut for, to tell an edit that moves one from an edit that does not. */
    private List<int[]> colliderBoxes = List.of();

    /** Whether {@link #colliders} has been built for the deck as it now stands. */
    private boolean collidersBuilt;

    /** Where it was at the end of the last tick, which is the whole of how far the deck moved. */
    private Vec3 previousPosition = Vec3.ZERO;

    public GhastMountEntity(EntityType<? extends GhastMountEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    /**
     * A ghast's own attributes with a mount's health and full knockback resistance - being shoved
     * off course by every arrow that hits it is the one thing that would make it unflyable.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFriendlyGhastEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * A block in hand builds; an empty hand rides. There is no button to swap between the two and
     * there does not need to be one - the two things a player holds while doing them are different.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (MountPlatform.buildable(player.getItemInHand(hand)) != null) {
            return placeLookedAt(player, hand);
        }

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

    /** The pilot stands on the deck once there is one, and sits on the creature until there is. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0, this.platform.isEmpty() ? BARE_SEAT : DECK_SEAT, 0.0);
    }

    /**
     * The mount is pointed wherever the rider is looking. Both rotations are written, and the body
     * and head with them, so the model turns as one thing rather than craning after the camera.
     */
    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        setYRot(player.getYRot());
        this.yRotO = getYRot();
        // Half the rider's pitch: a ghast that pointed straight down every time somebody looked at
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
     * not need to be one: pitch covers the full range, and a jump button would be a second way of
     * saying the same thing.
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
     * Flight, with the acceleration that {@link net.minecraft.world.entity.FlyingMob} hard-codes
     * replaced by the mount's own. Everything else about that class's flight is kept: no gravity, no
     * ground friction, and a flat {@link #FLIGHT_DRAG} on the whole delta.
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

    /* THE DECK */

    public MountPlatform platform() {
        return this.platform;
    }

    /**
     * A mount does not collide with its own deck. The floor sits exactly on top of a ghast, so
     * without this the first thing a rider would find is that climbing is impossible: the creature
     * would be stopped by the boards on its own back. Everybody else's deck is still a floor.
     */
    @Override
    public boolean canCollideWith(Entity entity) {
        return !(entity instanceof PlatformColliderEntity collider && collider.belongsTo(this))
                && super.canCollideWith(entity);
    }

    /**
     * The deck reaches four blocks past the creature in every direction, so the creature's own box
     * is not what decides whether it is on screen: a mount culled by its body takes its floor with
     * it, and the floor is the half a player is standing on.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(MountPlatform.RADIUS + 1.0);
    }

    /** What a sync payload hands over: the deck as the server has it, replacing whatever was drawn. */
    public void setPlatform(MountPlatform platform) {
        this.platform = platform;
    }

    /**
     * Keep the colliders honest and carry whatever is standing on the deck.
     * <p>
     * The carry is not an extra: nothing in vanilla moves an entity that is merely standing on
     * another one, so without this a player on the deck is left hanging in the air the instant the
     * mount moves out from under them. It is done on whichever side owns the entity being moved -
     * the server for everything, the client for its own player - because a player's position is
     * their own client's to decide and a server that moved them would be corrected a tick later.
     */
    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide() && !this.collidersBuilt) {
            rebuildColliders();
        }

        // The upper bound is what a tick of flight can possibly be worth, and it is there so that
        // the first tick of all (where there is no previous position) and a teleport carry nobody.
        Vec3 moved = position().subtract(this.previousPosition);
        if (!this.platform.isEmpty() && moved.lengthSqr() > 1.0E-8 && moved.lengthSqr() < 64.0) {
            carry(moved);
        }

        this.previousPosition = position();
    }

    private void carry(Vec3 moved) {
        Vec3 origin = MountPlatform.origin(position());
        AABB deck = this.platform.bounds(origin);
        if (deck == null) {
            return;
        }

        boolean client = level().isClientSide();
        Predicate<Entity> mine = entity -> entity instanceof Player player ? player.isLocalPlayer() : !client;

        for (Entity passenger : level().getEntities(this, deck.expandTowards(0.0, CARRY_HEADROOM, 0.0),
                entity -> !(entity instanceof PlatformColliderEntity) && entity.getVehicle() == null && mine.test(entity))) {
            if (this.platform.supports(origin, passenger.getBoundingBox())) {
                passenger.move(MoverType.SELF, moved);
                passenger.resetFallDistance();
            }
        }
    }

    /**
     * Lay one cell where the player is looking, which is either the face of a cell they can see or -
     * with an empty deck, where there is nothing to aim at - wherever their line of sight crosses
     * the floor layer. That second case is the only way the first block is ever placed.
     */
    public InteractionResult placeLookedAt(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Block block = MountPlatform.buildable(stack);
        if (block == null) {
            return InteractionResult.PASS;
        }

        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Vec3 origin = MountPlatform.origin(position());
        Vec3 from = player.getEyePosition();
        Vec3 to = reachEnd(player, from);

        MountPlatform.Hit hit = this.platform.clip(origin, from, to);
        int x;
        int y;
        int z;
        if (hit != null) {
            Direction face = hit.face();
            x = hit.x() + face.getStepX();
            y = hit.y() + face.getStepY();
            z = hit.z() + face.getStepZ();
        } else {
            MountPlatform.Hit floor = MountPlatform.clipFloor(origin, from, to);
            if (floor == null) {
                return InteractionResult.PASS;
            }

            x = floor.x();
            y = floor.y();
            z = floor.z();
        }

        if (!MountPlatform.inBounds(x, y, z) || this.platform.get(x, y, z) != null || this.platform.isFull()
                || !unobstructed(MountPlatform.cellBox(origin, x, y, z))) {
            return InteractionResult.CONSUME;
        }

        this.platform.set(x, y, z, block);
        deckChanged();

        SoundType sound = block.defaultBlockState().getSoundType();
        level().playSound(null, getX(), getY() + MountPlatform.FLOOR_OFFSET, getZ(), sound.getPlaceSound(),
                SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Take back the cell the player is looking at. The block goes into their inventory rather than
     * onto the ground: the ground, on a mount, is usually two hundred blocks down.
     */
    public void breakLookedAt(Player player) {
        Vec3 origin = MountPlatform.origin(position());
        Vec3 from = player.getEyePosition();
        MountPlatform.Hit hit = this.platform.clip(origin, from, reachEnd(player, from));
        if (hit == null) {
            return;
        }

        Block block = this.platform.remove(hit.x(), hit.y(), hit.z());
        if (block == null) {
            return;
        }

        deckChanged();

        SoundType sound = block.defaultBlockState().getSoundType();
        Vec3 at = MountPlatform.cellBox(origin, hit.x(), hit.y(), hit.z()).getCenter();
        level().playSound(null, at.x, at.y, at.z, sound.getBreakSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (!player.getAbilities().instabuild) {
            ItemStack drop = new ItemStack(block);
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
    }

    /** How far a player may reach, as a point: the block range, since the deck is built with blocks. */
    private Vec3 reachEnd(Player player, Vec3 from) {
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
        return from.add(player.getViewVector(1.0F).scale(reach));
    }

    /**
     * Whether a cell may be laid: vanilla's own rule for building inside something, read off
     * {@code blocksBuilding} so that it is every entity the game already thinks is in the way. The
     * colliders are excluded because they <em>are</em> the deck, and the mount because the deck is
     * on its back by design.
     */
    private boolean unobstructed(AABB cell) {
        return level().getEntities(this, cell,
                entity -> entity.blocksBuilding && !(entity instanceof PlatformColliderEntity)).isEmpty();
    }

    /** Anything that changes the deck: resend it, and re-cut the boxes that are stood on. */
    private void deckChanged() {
        if (!level().isClientSide()) {
            rebuildColliders();
            PacketDistributor.sendToPlayersTrackingEntity(this, new PlatformSyncPayload(getId(), this.platform));
        }
    }

    /**
     * Throw the collider entities away and cut new ones. It is done wholesale rather than edited
     * because the merge in {@link MountPlatform#boxes} is global - one cell placed in the middle of
     * a floor can change every box in that layer - and because there are at most a handful of them.
     */
    private void rebuildColliders() {
        List<int[]> boxes = this.platform.boxes();
        if (this.collidersBuilt && sameBoxes(boxes)) {
            return;
        }

        this.colliderBoxes = boxes;

        for (int id : this.colliders) {
            if (level().getEntity(id) instanceof PlatformColliderEntity collider) {
                collider.discard();
            }
        }

        this.colliders.clear();
        this.collidersBuilt = true;

        Vec3 origin = MountPlatform.origin(position());
        for (int[] box : boxes) {
            PlatformColliderEntity collider = ModEntities.PLATFORM_COLLIDER.get().create(level());
            if (collider == null) {
                continue;
            }

            collider.setBox(getId(), PlatformColliderEntity.packBox(box[0], box[1], box[2], box[3], box[4], box[5]));
            collider.setPos(origin.x + box[0], origin.y + box[1], origin.z + box[2]);
            level().addFreshEntity(collider);
            this.colliders.add(collider.getId());
        }
    }

    /**
     * Whether the deck still cuts into the same boxes it did. Most edits do not change them all -
     * a block added to a wall leaves the floor's box exactly as it was - and a collider that is
     * thrown away and remade is a floor that is not there for a tick, which is felt by whoever is
     * standing on it. So the set is only remade when the shape it covers has actually moved.
     */
    private boolean sameBoxes(List<int[]> boxes) {
        if (boxes.size() != this.colliderBoxes.size()) {
            return false;
        }

        for (int i = 0; i < boxes.size(); i++) {
            if (!Arrays.equals(boxes.get(i), this.colliderBoxes.get(i))) {
                return false;
            }
        }

        return true;
    }

    /** A deck is worth what was built into it, so a mount that dies hands all of it back. */
    @Override
    public void die(DamageSource damageSource) {
        if (!level().isClientSide()) {
            Vec3 origin = MountPlatform.origin(position());
            this.platform.forEach((x, y, z, block) -> {
                Vec3 at = MountPlatform.cellBox(origin, x, y, z).getCenter();
                ItemEntity drop = new ItemEntity(level(), at.x, at.y, at.z, new ItemStack(block));
                drop.setDefaultPickUpDelay();
                level().addFreshEntity(drop);
            });

            this.platform.clear();
            deckChanged();
        }

        super.die(damageSource);
    }

    /** The colliders are the deck's shadow and have no life of their own once the mount has gone. */
    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            for (int id : this.colliders) {
                if (level().getEntity(id) instanceof PlatformColliderEntity collider) {
                    collider.discard();
                }
            }

            this.colliders.clear();
        }

        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.platform.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.platform.load(tag);
        this.collidersBuilt = false;
    }

    /** The deck goes out with the entity, so a client never sees the mount before what is on it. */
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        this.platform.write(buffer);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.platform.read(buffer);
    }

    /** Nothing spawns one; every one was hatched by somebody and is somebody's way of getting about. */
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

    /** Conjured rather than paid for, so it leaves nothing behind - the mod's rule for every summon. */
    @Override
    protected boolean shouldDropLoot() {
        return false;
    }
}
