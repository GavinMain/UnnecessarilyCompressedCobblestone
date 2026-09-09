package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * One engraving, as the item a player crafts and carries. It does nothing at all in an inventory -
 * the whole of its worth is what an Engraving Table cuts into a piece of gear with it - so all it
 * carries is which engraving it is and a line saying what that engraving fits.
 */
public class EngravingItem extends Item {
    private final Engraving engraving;

    public EngravingItem(Engraving engraving, Properties properties) {
        super(properties);
        this.engraving = engraving;
    }

    public Engraving engraving() {
        return this.engraving;
    }

    /** The engraving {@code stack} is, or null if it is not an engraving at all. */
    public static Engraving of(ItemStack stack) {
        return stack.getItem() instanceof EngravingItem item ? item.engraving : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "tooltip.unnecessarilycompressedcobblestone.engraving." + this.engraving.getSerializedName())
                .withStyle(ChatFormatting.GRAY));

        // A Potion Engraving is the one that carries something, and vanilla only writes a potion's
        // effects into a tooltip for its own bottles - so it is written here, in vanilla's own
        // format, for the engraving and for whatever bow it has been cut into.
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            contents.addPotionTooltip(tooltipComponents::add, 1.0F, context.tickRate());
        }
    }
}
