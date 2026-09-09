package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A block of TNT in flight, out of a TNT Launcher. It flies as an arrow does and then, wherever it
 * stops, lights the TNT it was fired as and is gone.
 * <p>
 * What that TNT then does is not decided here, and deliberately so. The block is carried along and
 * lit on landing through {@link CompressedTntEffect#lightInPlace}, which is the same
 * put-it-down-light-it-take-it-away path the Random TNT goes through - so every one of this mod's
 * variants does its own thing at the far end, vanilla's TNT explodes, and another mod's TNT does
 * whatever its own primed entity does, with nothing here having to know any of them exist. The fuse
 * is cut to the next tick by that helper, which is what makes a launched charge a shell rather than
 * a thing to run away from.
 * <p>
 * It is an {@link AbstractArrow} for its physics and for nothing else: it does no damage of its own,
 * never sticks and is never left lying about. The blast is the hit, which is why the launcher's
 * Compression Energy buys velocity rather than damage - a harder throw carries further, and how big
 * the crater is was decided when the ammunition was crafted.
 */
public class TntProjectileEntity extends AbstractArrow implements ItemSupplier {
    /**
     * The block it was fired as, synced because the client draws that item tumbling through the
     * air. The lighting is server side, so this is the one thing about it the client needs.
     */
    private static final EntityDataAccessor<ItemStack> DATA_TNT =
            SynchedEntityData.defineId(TntProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final String TAG_TNT = "tnt";

    /**
     * How fast it falls, against an arrow's 0.05. A launched block is heavy and is meant to be
     * lobbed - the arc is what makes aiming a TNT Launcher a different skill from aiming a bow,
     * and it is the same 0.08 a living entity falls at, so the flight reads as a thrown thing
     * rather than as a shot one.
     */
    private static final double GRAVITY = 0.08;

    public TntProjectileEntity(EntityType<? extends TntProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @param launcher the weapon this was fired from, whose enchantments the shell then carries, or
     *                 an empty stack for a shell that was thrown by something with no weapon at all
     */
    public TntProjectileEntity(Level level, LivingEntity shooter, ItemStack tnt, ItemStack launcher) {
        super(ModEntities.TNT_PROJECTILE.get(), shooter, level, tnt, weapon(launcher));
        setTnt(tnt);
    }

    /**
     * An empty weapon is not the same as no weapon, as far as {@code AbstractArrow} is concerned:
     * its constructor throws {@code IllegalArgumentException("Invalid weapon firing an arrow")} on
     * an empty stack and accepts null, which is exactly backwards from how every caller here reads.
     * Both dragons throw charges with their bare claws, so the two are folded together here rather
     * than at each of them.
     */
    @Nullable
    private static ItemStack weapon(ItemStack launcher) {
        return launcher.isEmpty() ? null : launcher;
    }

    /**
     * Never picked up and never dropped. A shell is spent the moment it goes off, which is the one
     * place this differs from an arrow that misses - and it always goes off, since anything it
     * touches lights it.
     */
    @Override
    protected ItemStack getDefaultPickupItem() {
        // Called from AbstractArrow's own constructor, before any field of this class is assigned,
        // so it can only ever answer with a constant - the same trap CompressedArrowEntity and the
        // bolt document. The real block is set immediately afterwards and this is never read.
        return new ItemStack(Items.TNT);
    }

    public void setTnt(ItemStack tnt) {
        this.entityData.set(DATA_TNT, tnt.copyWithCount(1));
    }

    public ItemStack getTnt() {
        return this.entityData.get(DATA_TNT);
    }

    /** What the client draws: the block of TNT itself, tumbling. */
    @Override
    public ItemStack getItem() {
        return getTnt();
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TNT, new ItemStack(Items.TNT));
    }

    /**
     * Ground: it goes off against the face it struck rather than inside it, so a shell fired at a
     * wall is lit in the air beside the wall and not in the block it hit.
     */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        land(result.getBlockPos().relative(result.getDirection()));
    }

    /** Something alive: it goes off where that thing is standing. No arrow damage is dealt first. */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        land(result.getEntity().blockPosition());
    }

    /**
     * Lights the charge and removes the shell. A position outside the loaded world is left alone -
     * every effect in this mod guards that, and a shell that flew past the edge of what is loaded
     * should not drag a chunk in to explode in.
     */
    private void land(BlockPos at) {
        if (level() instanceof ServerLevel serverLevel && serverLevel.isLoaded(at)
                && getTnt().getItem() instanceof BlockItem block) {
            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            CompressedTntEffect.lightInPlace(serverLevel, at, block.getBlock(), owner);
        }

        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put(TAG_TNT, getTnt().saveOptional(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setTnt(ItemStack.parseOptional(registryAccess(), compound.getCompound(TAG_TNT)));
    }
}
