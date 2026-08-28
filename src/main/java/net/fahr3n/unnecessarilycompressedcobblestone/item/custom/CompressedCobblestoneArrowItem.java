package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneArrowEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The item side of the arrow. Both paths hand off to {@link CompressedCobblestoneArrowEntity},
 * which is where the base damage lives, so an arrow hits the same whether a player shot it or a
 * dispenser did.
 */
public class CompressedCobblestoneArrowItem extends ArrowItem {
    public CompressedCobblestoneArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new CompressedCobblestoneArrowEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        CompressedCobblestoneArrowEntity arrow =
                new CompressedCobblestoneArrowEntity(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;

        return arrow;
    }
}
