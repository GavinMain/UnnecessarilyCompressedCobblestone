package net.fahr3n.unnecessarilycompressedcobblestone.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.fahr3n.unnecessarilycompressedcobblestone.util.FullEnchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The same ceiling again, in the builder that writes the levels.
 * <p>
 * {@code ItemEnchantments.Mutable} does not throw on a level over the ceiling, it silently takes the
 * minimum - so with only {@link ItemEnchantmentsMixin} applied, asking for 999 would have produced
 * 255 with nothing complaining, which is the same failure vanilla's max health clamp made and just
 * as quiet. Both of its writing methods clamp, and both are here: {@code set}, which is what this
 * mod stamps its two full-enchantment items with, and {@code upgrade}, which is what an anvil uses.
 */
@Mixin(ItemEnchantments.Mutable.class)
public class ItemEnchantmentsMutableMixin {
    @ModifyConstant(method = "set", constant = @Constant(intValue = 255))
    private int ucc$raiseSetCeiling(int ceiling) {
        return FullEnchantment.CEILING;
    }

    @ModifyConstant(method = "upgrade", constant = @Constant(intValue = 255))
    private int ucc$raiseUpgradeCeiling(int ceiling) {
        return FullEnchantment.CEILING;
    }
}
