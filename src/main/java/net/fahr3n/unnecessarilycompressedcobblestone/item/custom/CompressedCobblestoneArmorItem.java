package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
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

    /**
     * Whether this piece glides, which is the whole of the Elytra engraving.
     * <p>
     * It has to live here rather than in {@code ModEvents} with the other engravings' behaviour,
     * because vanilla asks the <em>chestplate</em>: {@code LivingEntity.updateFallFlying} clears the
     * flying flag every tick unless the stack in the chest slot answers this, so nothing an event
     * does afterwards can hold a player in the air. It is also why the engraving is offered on this
     * mod's chestplates and no others - see {@link Engraving#ELYTRA}.
     */
    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return this.type == Type.CHESTPLATE && Engravings.has(stack, Engraving.ELYTRA);
    }

    /**
     * And it goes on gliding for as long as it is worn. An elytra spends a point of durability every
     * twenty ticks of flight, which is what NeoForge's default does here; none of this armour ever
     * takes damage, so there is nothing to spend and nothing to break.
     */
    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        return canElytraFly(stack, entity);
    }
}
