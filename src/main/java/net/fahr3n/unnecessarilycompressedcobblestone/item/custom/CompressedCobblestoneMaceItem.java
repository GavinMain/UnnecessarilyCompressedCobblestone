package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TooltipFlag;

/**
 * A mace that never wears out. The smash attack, the fall-distance damage scaling and the
 * knockback ring all come straight from {@link MaceItem}; only the durability is swallowed, the
 * same trick {@link CompressedCobblestoneSwordItem} uses, so the weapon stays damageable on paper
 * and therefore enchantable, repairable and visible to other mods like any vanilla mace.
 */
public class CompressedCobblestoneMaceItem extends MaceItem {
    public CompressedCobblestoneMaceItem(Properties properties) {
        super(properties);
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
