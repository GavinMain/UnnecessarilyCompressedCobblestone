package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;


import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.FullEnchantment;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * The only tool that gets into compressed cobblestone at {@code ModBlocks.HARDENED_LEVEL_TIER_2} and
 * above - the second floor, which is closed even to the pickaxe that opens every hardened level
 * below it. It opens those too: it is in {@code #ucc:hardened_mining} as well as
 * {@code #ucc:hardened_mining_tier_2}, since a tool that reached the deeper stone and not the
 * shallower one would be a strange thing to hold.
 * <p>
 * It is a {@link PickaxeItem} and nothing more exotic, so everything that keys off a pickaxe treats
 * it as one, and it inherits the one-point-of-mining-speed-per-digit that Compression Energy buys
 * every tool here from {@link CompressedCobblestonePickaxeItem}.
 * <p>
 * Two things are its own. It reaches {@link #REACH_BONUS} blocks further, stated as the two
 * interaction-range attributes on the item the way the Compressed Katana states them - so the extra
 * distance applies in survival and creative alike, shows in the tooltip, and stacks with the Reach
 * engraving without either knowing about the other. And it carries every enchantment in the game at
 * once, curses excepted, worked out from the registry when it is held rather than written into its
 * recipe: see {@link FullEnchantment} for why that has to happen at runtime, and why the level is
 * 255 rather than any larger number that might be asked for. All of them are listed in the tooltip
 * one line at a time, which is a screenful and is meant to be.
 */
public class CompressedCobblestonePickaxeTier2Item extends CompressedCobblestonePickaxeItem {
    /** How much further than an ordinary tool it reaches, in blocks. */
    public static final double REACH_BONUS = 4.0;

    /** Vanilla's own pickaxe numbers, restated because {@code createAttributes} cannot be added to. */
    public static final float ATTACK_DAMAGE = 1.0F;
    public static final float ATTACK_SPEED = -2.8F;

    /** How often the pickaxe checks whether the enchantment registry has grown under it, in ticks. */
    private static final int TOP_UP_INTERVAL = 100;

    private static final ResourceLocation REACH_ENTITY_ID = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "pickaxe_tier_2_entity_reach");
    private static final ResourceLocation REACH_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "pickaxe_tier_2_block_reach");

    public CompressedCobblestonePickaxeTier2Item(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * A pickaxe's own two modifiers with both interaction ranges added to them.
     * <p>
     * {@code PickaxeItem.createAttributes} returns a finished {@code ItemAttributeModifiers} with no
     * way to add to it, so the two modifiers it would have made are restated here against vanilla's
     * own {@code BASE_ATTACK_DAMAGE_ID} and {@code BASE_ATTACK_SPEED_ID} - using those ids rather
     * than ids of this mod's own is what makes the tooltip read as a tool's stats instead of as two
     * bonuses stapled to one. Block reach moves with entity reach for the reason the katana gives:
     * a tool that could hit a creeper eight blocks away and not the block it was standing on would
     * feel broken rather than long, and on a pickaxe the block half is the half that matters.
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

    /**
     * Fills in anything the stack is missing. Called from the tick below and when the pickaxe is
     * built for the creative tab, so a copy is fully enchanted before anyone ever holds it.
     */
    public static void enchantFully(ItemStack stack, HolderLookup.Provider registries) {
        FullEnchantment.apply(stack, registries, false);
    }

    /**
     * Puts back anything missing: a pickaxe handed out by a command, taken from the creative tab
     * before this ran, or held while a datapack reload added an enchantment that was not there when
     * it was made.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || entity.tickCount % TOP_UP_INTERVAL != 0) {
            return;
        }

        if (FullEnchantment.needsTopUp(stack, level.registryAccess(), false)) {
            enchantFully(stack, level.registryAccess());
        }
    }
}
