package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowTier;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The item side of a compressed arrow. Both paths hand off to {@link CompressedArrowEntity}, which
 * is where the base damage lives, so an arrow hits the same whether a player shot it or a dispenser
 * did.
 */
public class CompressedArrowItem extends ArrowItem {
    private final CompressedArrowTier tier;

    public CompressedArrowItem(CompressedArrowTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new CompressedArrowEntity(this.tier, level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        CompressedArrowEntity arrow = new CompressedArrowEntity(this.tier, level, pos.x(), pos.y(), pos.z(),
                stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;

        return arrow;
    }
}
