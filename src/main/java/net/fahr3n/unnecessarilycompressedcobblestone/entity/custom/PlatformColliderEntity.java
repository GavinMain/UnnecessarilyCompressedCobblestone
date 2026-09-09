package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.util.MountPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One box of a Ghast Mount's deck, and the only reason the deck can be stood on.
 * <p>
 * A {@link MountPlatform} cell is not a block in the world, so nothing in the game would ever
 * collide with it. What <em>is</em> collided with is an entity: {@code Entity.collide} asks the
 * level for the boxes of everything answering {@link #canBeCollidedWith}, which is exactly how a
 * boat and a shulker are stood on, and it is the only surface a mod can offer without a mixin. So
 * every deck is covered by a handful of these - one per box out of {@link MountPlatform#boxes},
 * which is one for a plain floor however wide it is - and they are invisible, unhurtable and
 * unpushable, being a shape and nothing else.
 * <p>
 * They are not synced by position. Each carries the id of the mount it belongs to and its own cell
 * offsets, and puts itself where those say every tick on both sides, so the client's copy sits
 * exactly where the client's copy of the mount is and no amount of network lag can leave a floor
 * behind the creature carrying it.
 * <p>
 * {@link #makeBoundingBox} is overridden rather than the entity being sized: {@code EntityDimensions}
 * is square in x and z, and a deck merged into boxes is not. That method is what {@code setPos}
 * writes the box from, so overriding it is the whole of having an oblong hitbox.
 */
public class PlatformColliderEntity extends Entity {
    /** The mount this belongs to, by entity id, so both sides can find it in their own level. */
    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(PlatformColliderEntity.class, EntityDataSerializers.INT);

    /**
     * Which cells this box covers, packed: the corner cell and the size in each axis. It is one
     * accessor rather than six because six synched integers on up to a dozen of these per mount is
     * a dozen watchers of six fields each, and the whole of it fits in eighteen bits.
     */
    private static final EntityDataAccessor<Integer> DATA_BOX =
            SynchedEntityData.defineId(PlatformColliderEntity.class, EntityDataSerializers.INT);

    public PlatformColliderEntity(EntityType<? extends PlatformColliderEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
        setNoGravity(true);
    }

    /** Packs a box of the mount's own cells. x and z are offset by the deck radius to be positive. */
    public static int packBox(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        return (x + MountPlatform.RADIUS)
                | (y << 4)
                | ((z + MountPlatform.RADIUS) << 7)
                | (sizeX << 11)
                | (sizeY << 15)
                | (sizeZ << 18);
    }

    public void setBox(int owner, int box) {
        this.entityData.set(DATA_OWNER, owner);
        this.entityData.set(DATA_BOX, box);
    }

    /** Whether this is part of that mount's own deck, which is how a mount ignores its own floor. */
    public boolean belongsTo(Entity entity) {
        return this.entityData.get(DATA_OWNER) == entity.getId();
    }

    private int box() {
        return this.entityData.get(DATA_BOX);
    }

    private double cellX() {
        return (box() & 0xF) - MountPlatform.RADIUS;
    }

    private double cellY() {
        return box() >> 4 & 0x7;
    }

    private double cellZ() {
        return (box() >> 7 & 0xF) - MountPlatform.RADIUS;
    }

    private double sizeX() {
        return Math.max(1, box() >> 11 & 0xF);
    }

    private double sizeY() {
        return Math.max(1, box() >> 15 & 0x7);
    }

    private double sizeZ() {
        return Math.max(1, box() >> 18 & 0xF);
    }

    @Nullable
    private GhastMountEntity owner() {
        return level().getEntity(this.entityData.get(DATA_OWNER)) instanceof GhastMountEntity mount ? mount : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_BOX, 0);
    }

    /**
     * Follow the mount, or stop existing. There is no movement of its own to do: the box is where
     * the mount is plus the offsets it was given, worked out identically on both sides.
     * <p>
     * A collider whose mount has gone is a floor hanging in the air, so on the server it is
     * discarded. On the client it is left alone - the mount may simply be out of tracking range -
     * and the server's own removal will arrive in its own time.
     */
    @Override
    public void tick() {
        GhastMountEntity mount = owner();
        if (mount == null || !mount.isAlive()) {
            if (!level().isClientSide()) {
                discard();
            }

            return;
        }

        Vec3 origin = MountPlatform.origin(mount.position());
        setPos(origin.x + cellX(), origin.y + cellY(), origin.z + cellZ());
    }

    /** The box is read off the synched offsets, so a client that has just been sent them re-makes it. */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_BOX.equals(key)) {
            setPos(getX(), getY(), getZ());
        }
    }

    /** The position is the box's lowest corner, not its centre: cells are corners, not centres. */
    @Override
    protected AABB makeBoundingBox() {
        return new AABB(getX(), getY(), getZ(), getX() + sizeX(), getY() + sizeY(), getZ() + sizeZ());
    }

    /** Only ever asked for culling and never for the hitbox, which {@link #makeBoundingBox} owns. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed((float) Math.max(sizeX(), sizeZ()), (float) sizeY());
    }

    /* WHAT IT IS: A SURFACE, AND NOTHING ELSE */

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void push(Entity entity) {
    }

    /** Pickable so that a player may aim at the deck; unhurtable so that aiming at it cannot kill it. */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    /**
     * Hitting the deck breaks the cell that was looked at, and never damages anything. Returning
     * true is what stops {@code Player.attack} in its tracks, so no attack cooldown is spent, no
     * sweep goes off and nothing is knocked back by a player mining their own floor.
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        GhastMountEntity mount = owner();
        if (mount != null && !level().isClientSide() && attacker instanceof Player player) {
            mount.breakLookedAt(player);
        }

        return true;
    }

    /** Right-clicking the deck builds on it, which is the mount's business rather than this box's. */
    @Override
    public InteractionResult interactAt(Player player, Vec3 at, InteractionHand hand) {
        GhastMountEntity mount = owner();
        return mount == null ? InteractionResult.PASS : mount.placeLookedAt(player, hand);
    }

    /* IT IS NEVER SAVED: THE MOUNT REBUILDS THE WHOLE SET WHENEVER THE DECK CHANGES */

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
