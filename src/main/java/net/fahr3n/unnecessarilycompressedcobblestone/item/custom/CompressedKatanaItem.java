package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * The Compressed Katana. A sword in every list, every tag and every enchantment that reads one, and
 * different from one in exactly two numbers: it reaches three blocks further and it swings faster.
 * <p>
 * Both of those are plain attributes on the item rather than behaviour in an event handler, and that
 * is the point of doing it this way. Reach is {@code ENTITY_INTERACTION_RANGE} and
 * {@code BLOCK_INTERACTION_RANGE}, which vanilla reads off whatever is in the main hand, so the
 * extra distance applies in survival and creative alike, shows up in the tooltip, is respected by
 * anything else that reads the attribute, and stacks with the Reach engraving without either
 * knowing about the other. Attack speed is the same attribute a sword already sets, one number
 * further up.
 * <p>
 * Everything else about it is deliberately ordinary. It is in {@code #minecraft:swords}, so an
 * enchanting table hands it Sharpness, Looting, Fire Aspect and the rest with nothing further to do;
 * it is in {@code #c:tools/melee_weapon}, so other mods see what kind of weapon it is; and it is in
 * {@code #ucc:compression_melee_weapon}, so the Compression Inscriber turns its energy into attack
 * damage the way it does for the sword and the mace. Nothing here special-cases it.
 * <p>
 * Like every other tool in this mod it keeps a real durability and never spends it - see
 * {@link CompressedCobblestoneSwordItem} for why the {@code minecraft:unbreakable} component is the
 * wrong way to do that.
 */
public class CompressedKatanaItem extends SwordItem {
    /** How much further than a sword it reaches, in blocks. */
    public static final double REACH_BONUS = 3.0;

    /**
     * Vanilla's sword is -2.4, which is 1.6 swings a second; this is 2.2. The number is the
     * <em>penalty</em> to the player's base attack speed of 4, so less negative is faster.
     */
    public static final float ATTACK_SPEED = -1.8F;

    /** On top of the tier's own bonus, the way every other weapon here states its damage. */
    public static final int ATTACK_DAMAGE = 3;

    private static final ResourceLocation REACH_ENTITY_ID = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "katana_entity_reach");
    private static final ResourceLocation REACH_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "katana_block_reach");

    public CompressedKatanaItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * A sword's own two modifiers with both interaction ranges added to them.
     * <p>
     * {@code SwordItem.createAttributes} returns a finished {@code ItemAttributeModifiers} with no
     * way to add to it, so the two modifiers it would have made are restated here against vanilla's
     * own {@code BASE_ATTACK_DAMAGE_ID} and {@code BASE_ATTACK_SPEED_ID} - using those ids rather
     * than ids of this mod's own is what makes the tooltip read as a weapon's stats instead of as
     * two bonuses stapled to one.
     * <p>
     * Block reach moves with entity reach rather than being left alone. They are separate attributes
     * and vanilla would happily move only one, but a weapon that could hit a creeper six blocks away
     * and not the block it was standing on would feel broken rather than long - and the Reach
     * engraving already sets the precedent of moving the pair together.
     */
    public static ItemAttributeModifiers createAttributes(Tier tier) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                                ATTACK_DAMAGE + tier.getAttackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(REACH_ENTITY_ID, REACH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.BLOCK_INTERACTION_RANGE,
                        new AttributeModifier(REACH_BLOCK_ID, REACH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
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
