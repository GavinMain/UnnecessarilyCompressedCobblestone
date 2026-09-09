package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * One augment, as the item a player crafts and carries. Like an engraving it does nothing at all in
 * an inventory - the whole of its worth is what a Laser Augmentation Table fits to a Ray of Laser
 * with it - so all it carries is which augment it is and a line saying what that augment does.
 */
public class AugmentItem extends Item {
    private final Augment augment;

    public AugmentItem(Augment augment, Properties properties) {
        super(properties);
        this.augment = augment;
    }

    public Augment augment() {
        return this.augment;
    }

    /** The augment {@code stack} is, or null if it is not an augment at all. */
    public static Augment of(ItemStack stack) {
        return stack.getItem() instanceof AugmentItem item ? item.augment : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "tooltip.unnecessarilycompressedcobblestone.augment." + this.augment.getSerializedName())
                .withStyle(ChatFormatting.GRAY));
    }
}
