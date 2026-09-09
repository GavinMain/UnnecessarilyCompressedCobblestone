package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * What the Compressed Chicken Boss throws. It is an egg in the two ways that matter - it arcs, and
 * it breaks into shell where it lands - and in no other: it hatches nothing, and it hits for
 * {@link CompressedChickenBossEntity#EGG_DAMAGE}.
 * <p>
 * It needs neither a renderer nor a texture of its own, since it is drawn as the item it is thrown
 * as: {@code ThrownItemRenderer} plus {@code getDefaultItem}, the same one line that covers every
 * bolt in flight.
 */
public class CompressedEggEntity extends ThrowableItemProjectile {
    public CompressedEggEntity(EntityType<? extends CompressedEggEntity> entityType, Level level) {
        super(entityType, level);
    }

    public CompressedEggEntity(Level level, LivingEntity thrower) {
        super(ModEntities.COMPRESSED_EGG.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EGG;
    }

    /**
     * The damage goes through the thrower rather than through the egg, so the kill is credited to
     * the boss and everything a player wears answers it exactly as it answers any other projectile.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(damageSources().thrown(this, getOwner()),
                CompressedChickenBossEntity.EGG_DAMAGE);
    }

    /**
     * Spent wherever it stops, on a creature or on the ground, and shell either way.
     * <p>
     * The shell goes out <em>before</em> the call up, not after it: {@code ThrowableProjectile#onHit}
     * discards the projectile, and an entity event broadcast for something already removed reaches
     * nobody.
     */
    @Override
    protected void onHit(HitResult result) {
        if (!level().isClientSide()) {
            // Vanilla's own break particles for a thrown item; the client turns 3 into a burst of
            // the item this projectile is drawn as.
            level().broadcastEntityEvent(this, (byte) 3);
        }

        super.onHit(result);
    }
}
