package net.fahr3n.unnecessarilycompressedcobblestone.event;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class ModEvents {
    private static final ResourceLocation CE_ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_attack_damage");
    private static final ResourceLocation CE_MAX_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_max_health");

    /**
     * Applies the crafted Compression books, which sit above the enchantment's registered max level
     * and so are the one case the vanilla anvil cannot handle: it clamps every enchantment it merges
     * to {@link Enchantment#getMaxLevel()}, which would quietly grind a crafted level back down to
     * 3. Only merges that involve a crafted level are taken over here; levels 1 to 3 are ordinary
     * enchantments and vanilla already does the right thing with them.
     * <p>
     * The rules for a crafted level are that it never grows (two level 4 books do not make a level
     * 5, and two level 5s do not make a 6), never shrinks (a lesser book applied on top leaves it
     * alone), and is charged as if it were a level 1 book however deep the compression is.
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // A crafted level only ever travels on an enchanted book. Anything else - repairing such a
        // weapon with material, renaming it, merging two weapons - either never touches the
        // enchantment or is left to vanilla.
        if (!right.has(DataComponents.STORED_ENCHANTMENTS)) {
            return;
        }

        ItemEnchantments bookEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(right);
        int bookLevel = compressionLevel(bookEnchantments);
        int itemLevel = compressionLevel(EnchantmentHelper.getEnchantmentsForCrafting(left));
        if (bookLevel == 0 || Math.max(bookLevel, itemLevel) < ModEnchantments.FIRST_CRAFTED_LEVEL) {
            return;
        }

        if (!EnchantmentHelper.canStoreEnchantments(left) || !left.isBookEnchantable(right)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack output = left.copy();
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(output));
        int work = 0;
        boolean anyApplied = false;

        // The same merge vanilla performs in AnvilMenu#createResult, minus the repair handling that
        // a book can never trigger, and with Compression pinned to level 4.
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : bookEnchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            boolean compression = holder.is(ModEnchantments.COMPRESSION);
            int current = merged.getLevel(holder);
            // Crafted Compression is taken as it comes rather than levelled up, so combining never
            // reaches a level that has no book.
            int level = compression ? Math.max(current, entry.getIntValue())
                    : (current == entry.getIntValue() ? entry.getIntValue() + 1 : Math.max(entry.getIntValue(), current));

            boolean supported = left.supportsEnchantment(holder) || player.hasInfiniteMaterials();
            for (Holder<Enchantment> other : merged.keySet()) {
                if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
                    supported = false;
                    work++;
                }
            }

            if (!supported) {
                continue;
            }

            anyApplied = true;
            if (!compression) {
                level = Math.min(level, holder.value().getMaxLevel());
            }
            merged.set(holder, level);

            // Books cost half of an enchantment's anvil cost per level, at least one. Compression is
            // billed for a single level no matter which level is going on.
            int costPerLevel = Math.max(1, holder.value().getAnvilCost() / 2);
            work += costPerLevel * (compression ? 1 : level);
            if (left.getCount() > 1) {
                work = 40;
            }
        }

        if (!anyApplied) {
            return;
        }

        String name = event.getName();
        if (name != null && !StringUtil.isBlank(name)) {
            if (!name.equals(left.getHoverName().getString())) {
                work++;
                output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        } else if (left.has(DataComponents.CUSTOM_NAME)) {
            work++;
            output.remove(DataComponents.CUSTOM_NAME);
        }

        int leftPriorWork = left.getOrDefault(DataComponents.REPAIR_COST, 0);
        int rightPriorWork = right.getOrDefault(DataComponents.REPAIR_COST, 0);
        output.set(DataComponents.REPAIR_COST,
                AnvilMenu.calculateIncreasedRepairCost(Math.max(leftPriorWork, rightPriorWork)));
        EnchantmentHelper.setEnchantments(output, merged.toImmutable());

        event.setOutput(output);
        // Prior work can push this past the 40 levels vanilla refuses to go beyond; going through
        // the event means the anvil asks for the levels instead of giving up, which is the lesser
        // evil - cancelling the event here would leave a stale stack in the output slot.
        event.setCost((long) leftPriorWork + rightPriorWork + work);
        event.setMaterialCost(1);
    }

    /**
     * Carving a level 11 block with shears, the way a pumpkin becomes a carved pumpkin. This lives
     * on the event bus rather than on the block because every compression level is registered as
     * the same plain block in a loop, and only this one level behaves differently.
     * <p>
     * Setting the block runs the carved block's own placement check, so carving in place can
     * finish a golem exactly as placing the head does.
     */
    @SubscribeEvent
    public static void onCarveCobblestone(PlayerInteractEvent.RightClickBlock event) {
        ItemStack tool = event.getItemStack();
        if (!tool.canPerformAction(ItemAbilities.SHEARS_CARVE)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(ModBlocks.byLevel(CarvedCobblestoneBlock.CARVED_FROM_LEVEL).get())) {
            return;
        }

        Player player = event.getEntity();
        if (!level.isClientSide) {
            Direction face = event.getFace();
            Direction facing = face == null || face.getAxis() == Direction.Axis.Y ? player.getDirection().getOpposite() : face;

            level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, ModBlocks.CARVED_COBBLESTONE_TIER_1.get().defaultBlockState()
                    .setValue(CarvedCobblestoneBlock.FACING, facing), 11);
            tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            level.gameEvent(player, GameEvent.SHEAR, pos);
            player.awardStat(Stats.ITEM_USED.get(Items.SHEARS));
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    /**
     * The armour and damage a piece of gear has earned from its Compression Energy. Adding the
     * modifiers here rather than writing them onto the stack keeps the energy component the only
     * thing that has to be right: change the energy and the stats follow on their own.
     * <p>
     * Each piece scales exactly one stat. Armour takes max health, one point per digit: vanilla
     * caps the armour attribute itself at 30, where deep energy would be wasted, while max health
     * runs to 1024, which a full set of the deepest gear very nearly reaches. The hearts are part
     * of the wearer while the armour is on rather than a pool that drains away. Attack damage has
     * no ceiling worth worrying about.
     */
    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        int bonus = CompressionEnergy.bonus(stack);
        if (bonus <= 0) {
            return;
        }

        if (stack.getItem() instanceof CompressedCobblestoneArmorItem armor) {
            event.addModifier(Attributes.MAX_HEALTH,
                    new AttributeModifier(CE_MAX_HEALTH_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()));
        } else if (stack.is(ModItems.COMPRESSED_COBBLESTONE_SWORD.get())) {
            event.addModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(CE_ATTACK_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
    }

    /** The energy on a piece of gear, written the only way a 244 digit number can be read. */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        CompressionEnergy.get(event.getItemStack()).ifPresent(log10 -> event.getToolTip().add(
                Component.translatable("tooltip.unnecessarilycompressedcobblestone.compression_energy",
                        CompressionEnergy.format(log10), CompressionEnergy.digits(log10))
                        .withStyle(ChatFormatting.AQUA)));
    }

    /** The level of Compression in {@code enchantments}, or 0 if it is not there. */
    private static int compressionLevel(ItemEnchantments enchantments) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(ModEnchantments.COMPRESSION)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }
}
