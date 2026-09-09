package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The dev sword: a weapon that claims to hit for 1 x 10^255, and hits for the most the game can
 * actually carry.
 * <p>
 * The claim cannot be met and it is worth being exact about why, since three separate ceilings sit
 * under it and each one is lower than the last. {@code Attributes.ATTACK_DAMAGE} is a
 * {@code RangedAttribute} capped at <b>2048</b>, so no modifier, enchantment or inscription can put
 * a larger number on a weapon - a bigger one is clamped on the way in with nothing saying so. Damage
 * is carried as a {@code float} from end to end, so the largest blow that can exist at all is
 * {@link Float#MAX_VALUE}, about 3.4 x 10^38. And health is a float too, capped at 1024 by
 * {@code Attributes.MAX_HEALTH}, so everything past about a thousand is the same blow: dead.
 * <p>
 * So the sword does both halves. Its attribute is set to the attribute ceiling, and
 * {@code ModEvents.onDevSwordDamage} raises the blow to {@code Float.MAX_VALUE} after every
 * reduction has had its say - which is the honest maximum, and is why armour, Protection and even
 * Resistance V cannot save anything from it.
 * <p>
 * What it does <b>not</b> get past is a boss that has stated a rule of its own about what may hurt
 * it. The blow is an ordinary {@code minecraft:player_attack} in every respect but its size, which
 * is exactly how each of those rules reads it: the Compressed Ore Golem wants a pickaxe, the Snow
 * Golem wants a fall, the Spirit wants lightning, the Composer wants a note, and the Compressed
 * Dragon's second phase throws away any hit this large for being this large. Against those five the
 * swing lands, flashes, knocks them about and costs them nothing - {@code SelectiveImmunity} is that
 * shape, and its handler runs at {@code LOWEST} so it always has the last word here. That is the
 * right answer rather than a limitation: this kills anything a number can kill, and those five are
 * not fights about numbers. The real figure is hidden with
 * {@link ItemAttributeModifiers#withTooltip} and the printed one is a lang line, so the tooltip
 * reads as asked. It is a lie, and it is a lie on a creative-only item, which is the only place a
 * lie of that shape belongs.
 * <p>
 * It is a {@link SwordItem} rather than a plain {@code Item} so that everything that reads a sword
 * still reads this one - the sword tags, enchanting, the anvil and every other mod's weapon rule -
 * and it refuses durability the same way the rest of this mod's gear does, keeping a real damage
 * bar so that it stays enchantable and repairable.
 */
public class DevSwordItem extends SwordItem {
    /**
     * What it says it does. Only ever printed, never used as a number: it does not fit in a float,
     * a double would round it, and nothing downstream would accept it.
     */
    public static final String CLAIMED_DAMAGE = "1 x 10^255";

    /** What it really does before {@code ModEvents} takes over: the attribute's own ceiling. */
    public static final double ATTACK_DAMAGE = 2048.0;

    /** A sword's swing, unchanged. The penalty is against the player's base 4. */
    private static final double ATTACK_SPEED = -2.4;

    public DevSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * The two modifiers a sword would have had, restated against vanilla's own ids so the numbers
     * read as the weapon's stats, and then hidden outright - the whole point being that the tooltip
     * says {@link #CLAIMED_DAMAGE} and nothing contradicts it two lines further down.
     */
    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, ATTACK_DAMAGE,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build()
                .withTooltip(false);
    }

    /** Swallows every point of durability damage before vanilla can apply it. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /** The line the hidden attributes were hidden for, drawn the green vanilla draws a stat in. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.unnecessarilycompressedcobblestone.dev_sword.damage",
                CLAIMED_DAMAGE).withStyle(ChatFormatting.DARK_GREEN));
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
