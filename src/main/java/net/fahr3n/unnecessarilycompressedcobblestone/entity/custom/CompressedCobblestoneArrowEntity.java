package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The arrow in flight. Base damage belongs to the projectile rather than to the bow that fired it,
 * so it is set here and every shot of this arrow carries it, whatever it was fired from.
 * <p>
 * Everything else is stock {@link AbstractArrow}: it arcs, sticks in blocks, can be picked back up,
 * and takes the firing weapon's enchantments into account on impact.
 */
public class CompressedCobblestoneArrowEntity extends AbstractArrow {
    /** Against the 2.0 every vanilla arrow carries. */
    public static final double BASE_DAMAGE = 4.0;

    /** Used when the client and the save file recreate the entity; the saved damage overwrites this. */
    public CompressedCobblestoneArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
        this.setBaseDamage(BASE_DAMAGE);
    }

    /** Fired from a bow. */
    public CompressedCobblestoneArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.COMPRESSED_COBBLESTONE_ARROW.get(), shooter, level, pickupItem, weapon);
        this.setBaseDamage(BASE_DAMAGE + CompressionEnergy.bonus(pickupItem));
    }

    /** Fired from a dispenser. */
    public CompressedCobblestoneArrowEntity(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.COMPRESSED_COBBLESTONE_ARROW.get(), x, y, z, level, pickupItem, weapon);
        this.setBaseDamage(BASE_DAMAGE + CompressionEnergy.bonus(pickupItem));
    }

    /** What the arrow turns back into when it is picked up. */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.COMPRESSED_COBBLESTONE_ARROW.get());
    }
}
