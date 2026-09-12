package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BlackHoleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The one thing a black hole answers to. It does nothing on its own: {@link BlackHoleEntity} checks
 * for it in the main hand when it is hit or used on, and collapses, taking one of these with it.
 * <p>
 * It is a tool rather than gear, so it is deliberately not enchantable and not inscribable - there
 * is nothing about "ends a black hole" that a level could make more of.
 */
public class BlackHoleStopperItem extends Item {
    public BlackHoleStopperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.unnecessarilycompressedcobblestone.black_hole_stopper")
                .withStyle(ChatFormatting.GRAY));
    }
}
