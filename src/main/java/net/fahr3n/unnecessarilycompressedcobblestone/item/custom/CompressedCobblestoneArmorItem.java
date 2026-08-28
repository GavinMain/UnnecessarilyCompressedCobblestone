package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Armor that never wears out, but is otherwise an ordinary damageable piece of gear.
 * <p>
 * The obvious way to make something unbreakable is the {@code minecraft:unbreakable} data
 * component, but that makes {@link ItemStack#isDamageableItem()} false, and that in turn is
 * what enchanting tables ({@link Item#isEnchantable}), anvils and most mods key off. Instead
 * the piece keeps its durability and simply refuses to spend any of it, so everything that
 * works on a leather helmet works here too.
 */
public class CompressedCobblestoneArmorItem extends ArmorItem {
    public CompressedCobblestoneArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    /** Swallows every point of durability damage before vanilla can apply it. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /** The durability is never spent, so say so the way an unbreakable item does. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
