package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltItem;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bolt in flight, out of a Bolt Launcher. It flies as an arrow does and then, wherever it stops,
 * calls its own lightning down and is gone.
 * <p>
 * What that lightning is is not decided here. The bolt it was fired as is carried along and asked
 * on landing - {@link BoltItem#strike} - so a note bolt plays its note where it lands, a composition
 * bolt plays its sheet there, and a plain one is a storm bolt. Nothing had to be taught what the
 * eighty-eight keys are.
 * <p>
 * It is an {@link AbstractArrow} for its physics and for nothing else: it never sticks, is never
 * picked up, and does no damage of its own - the strike is the damage, which is why an inscribed
 * launcher makes the <em>lightning</em> harder rather than the projectile.
 */
public class BoltProjectileEntity extends AbstractArrow implements ItemSupplier {
    /**
     * The bolt it was fired as, synced because the client draws that item spinning through the air.
     * The strike itself is server side, so this is the one thing about it the client needs.
     */
    private static final EntityDataAccessor<ItemStack> DATA_BOLT =
            SynchedEntityData.defineId(BoltProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final String TAG_BOLT = "bolt";
    private static final String TAG_BONUS_DAMAGE = "bonus_damage";
    private static final String TAG_SIGNAL = "signal";

    /** Added to whatever the strike would have done: the launcher's Compression Energy and Power. */
    private float bonusDamage;

    /** How hard the strike is called, which is what a bolt turns into volume. A full one by default. */
    private int signal = BoltItem.MAX_SIGNAL;

    public BoltProjectileEntity(EntityType<? extends BoltProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public BoltProjectileEntity(Level level, LivingEntity shooter, ItemStack bolt, ItemStack launcher) {
        super(ModEntities.BOLT_PROJECTILE.get(), shooter, level, launcher, null);
        setBolt(bolt);
    }

    /**
     * Never picked up and never dropped, whatever it lands on. A bolt is spent the moment its
     * lightning falls, which is the one place this differs from an arrow that misses.
     */
    @Override
    protected ItemStack getDefaultPickupItem() {
        // Called from AbstractArrow's own constructor, before any field of this class is assigned,
        // so it can only ever answer with a constant - see the note on CompressedArrowEntity for
        // the same trap. The real bolt is set immediately afterwards and this is never read.
        return new ItemStack(ModItems.COMPRESSED_VANILLA_BOLT.get());
    }

    public void setBolt(ItemStack bolt) {
        this.entityData.set(DATA_BOLT, bolt.copyWithCount(1));
    }

    public ItemStack getBolt() {
        return this.entityData.get(DATA_BOLT);
    }

    public void setBonusDamage(float bonusDamage) {
        this.bonusDamage = bonusDamage;
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

    /** What the client draws: the bolt itself, tumbling. */
    @Override
    public ItemStack getItem() {
        return getBolt();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOLT, new ItemStack(ModItems.COMPRESSED_VANILLA_BOLT.get()));
    }

    /** Ground: the strike lands on the block face it stopped against, and the bolt is spent. */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        land(result.getLocation());
    }

    /**
     * Something alive: the strike lands on it rather than on the ground under it, so a bolt that
     * hits a bird hits the bird. No arrow damage is dealt on the way - the lightning is the hit.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        land(result.getEntity().position());
    }

    /**
     * Calls the strike down and removes the bolt.
     * <p>
     * A block landing is nudged up out of whatever it struck: {@code onHitBlock} leaves the arrow
     * inside the block face, and a lightning bolt spawned there would be starting a block below the
     * ground it should be hitting.
     */
    private void land(Vec3 at) {
        if (level() instanceof ServerLevel serverLevel && getBolt().getItem() instanceof BoltItem bolt) {
            bolt.strike(serverLevel, BlockPos.containing(at.x, at.y + 0.5, at.z), getBolt(),
                    this.signal, this.bonusDamage);
        }

        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put(TAG_BOLT, getBolt().saveOptional(registryAccess()));
        compound.putFloat(TAG_BONUS_DAMAGE, this.bonusDamage);
        compound.putInt(TAG_SIGNAL, this.signal);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setBolt(ItemStack.parseOptional(registryAccess(), compound.getCompound(TAG_BOLT)));
        this.bonusDamage = compound.getFloat(TAG_BONUS_DAMAGE);
        this.signal = compound.contains(TAG_SIGNAL) ? compound.getInt(TAG_SIGNAL) : BoltItem.MAX_SIGNAL;
    }
}
