package net.fahr3n.unnecessarilycompressedcobblestone.event;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModArmorMaterials;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModPotions;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
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
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class ModEvents {
    private static final ResourceLocation CE_ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_attack_damage");
    private static final ResourceLocation CE_MAX_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_max_health");
    private static final ResourceLocation JUMP_SET_STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_jump_step_height");

    /** Jump Boost V; the amplifier is one less than the numeral. */
    private static final int JUMP_SET_AMPLIFIER = 4;

    /** Long enough that refreshing it is cheap, short enough that it lapses soon after the set comes off. */
    private static final int JUMP_SET_DURATION = 40;

    /**
     * Applies the crafted Compression books, which sit above their enchantment's registered max
     * level and so are the one case the vanilla anvil cannot handle: it clamps every enchantment it
     * merges to {@link Enchantment#getMaxLevel()}, which would quietly grind a crafted level back
     * down. Only merges that involve a crafted level are taken over here; Compression 1 to 3 comes
     * off an enchanting table like any other enchantment and vanilla already does the right thing
     * with it.
     * <p>
     * Super, Hyper and Giga Compression are craft-only, so every level of theirs is a crafted one
     * and every merge involving them lands here. The rules for a crafted level are that it never
     * grows (two Compression IV books do not make a V, and two Super Compression I books do not
     * make a II), never shrinks (a lesser book applied on top leaves it alone), and is charged as
     * if it were a level 1 book however deep the compression is.
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
        if (!hasCraftedCompression(bookEnchantments)
                && !hasCraftedCompression(EnchantmentHelper.getEnchantmentsForCrafting(left))) {
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
        // a book can never trigger, and with crafted Compression levels pinned.
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : bookEnchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            ModEnchantments.Family family = ModEnchantments.family(holder);
            int current = merged.getLevel(holder);
            // Two matching books normally make the next level up. A crafted Compression level is
            // taken as it comes instead, so combining never reaches a level that has no book: the
            // only way up the family is to craft the deeper book. Compression 1 and 2 are still
            // below the table cap, so they level up the way they always have.
            boolean levelsUp = current == entry.getIntValue()
                    && (family == null || !family.isCrafted(entry.getIntValue() + 1));
            int level = levelsUp ? entry.getIntValue() + 1
                    : Math.max(entry.getIntValue(), current);

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
            if (family == null) {
                level = Math.min(level, holder.value().getMaxLevel());
            }
            merged.set(holder, level);

            // Books cost half of an enchantment's anvil cost per level, at least one. Compression
            // is billed for a single level no matter which level is going on.
            int costPerLevel = Math.max(1, holder.value().getAnvilCost() / 2);
            work += costPerLevel * (family == null ? level : 1);
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
     * The brewing stand recipes for the Compression potions: awkward plus a tier 15 block, then
     * redstone to stretch the four minutes out to six.
     * <p>
     * Splash and lingering need nothing here. Gunpowder and dragon's breath are container recipes
     * in vanilla, applied to whatever potion is in the stand rather than to a listed set, so both
     * Compression brews can be thrown the moment they can be brewed at all - and, for the same
     * reason, tipped into arrows.
     */
    @SubscribeEvent
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        builder.addMix(Potions.AWKWARD, ModBlocks.byLevel(15).get().asItem(), ModPotions.COMPRESSION_1);
        builder.addMix(ModPotions.COMPRESSION_1, Items.REDSTONE, ModPotions.LONG_COMPRESSION_1);
    }

    /** Advances any block fill a TNT is still working through, a layer at a time. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            DeferredFill.tick(serverLevel);
        }
    }

    /** A half finished fill must not outlive the world it was filling. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DeferredFill.clear();
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

        // Which stat the energy becomes is decided by tag rather than by class, so a new piece of
        // gear earns its bonus by joining #compression_armor or #compression_melee_weapon and
        // nothing here has to change.
        if (stack.is(ModTags.Items.COMPRESSION_ARMOR) && stack.getItem() instanceof ArmorItem armor) {
            event.addModifier(Attributes.MAX_HEALTH,
                    new AttributeModifier(CE_MAX_HEALTH_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()));
        } else if (stack.is(ModTags.Items.COMPRESSION_MELEE_WEAPON)) {
            event.addModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(CE_ATTACK_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
    }

    /**
     * The Compression Jump set bonus: a step height of one extra block, so the wearer walks up a
     * full block the way a horse does, and Jump Boost III. Neither is a property of any one piece,
     * so this is a tick check on the whole set rather than an attribute on the armour.
     * <p>
     * The effect is re-applied rather than held, and kept short, so taking a piece off lets it lapse
     * within a couple of seconds without this having to track who granted what. A stronger Jump
     * Boost from anywhere else is left alone.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        boolean fullSet = isJumpPiece(player.getItemBySlot(EquipmentSlot.HEAD))
                && isJumpPiece(player.getItemBySlot(EquipmentSlot.CHEST))
                && isJumpPiece(player.getItemBySlot(EquipmentSlot.LEGS))
                && isJumpPiece(player.getItemBySlot(EquipmentSlot.FEET));

        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            boolean applied = stepHeight.getModifier(JUMP_SET_STEP_HEIGHT_ID) != null;
            if (fullSet && !applied) {
                stepHeight.addTransientModifier(
                        new AttributeModifier(JUMP_SET_STEP_HEIGHT_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!fullSet && applied) {
                stepHeight.removeModifier(JUMP_SET_STEP_HEIGHT_ID);
            }
        }

        if (!fullSet) {
            return;
        }

        MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
        if (jump == null || jump.getAmplifier() < JUMP_SET_AMPLIFIER
                || (jump.getAmplifier() == JUMP_SET_AMPLIFIER && jump.getDuration() < JUMP_SET_DURATION / 2)) {
            // Not ambient and not a visible particle cloud, but it still shows in the HUD so the
            // wearer can see the set is doing something.
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_SET_DURATION, JUMP_SET_AMPLIFIER,
                    false, false, true));
        }
    }

    /** Whether {@code stack} is a piece of the Compression Jump set, by the material it is made of. */
    private static boolean isJumpPiece(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor
                && armor.getMaterial().equals(ModArmorMaterials.COMPRESSION_JUMP_ARMOR_MATERIAL);
    }

    /** The energy on a piece of gear, written the only way a 244 digit number can be read. */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        CompressionEnergy.get(event.getItemStack()).ifPresent(log10 -> event.getToolTip().add(
                Component.translatable("tooltip.unnecessarilycompressedcobblestone.compression_energy",
                        CompressionEnergy.format(log10), CompressionEnergy.digits(log10))
                        .withStyle(ChatFormatting.AQUA)));
    }

    /**
     * Whether {@code enchantments} carries a Compression level that only a crafted book can hold -
     * Compression IV and up, or any level of the three craft-only rungs. Those are the merges the
     * vanilla anvil would clamp, and so the ones taken over above.
     */
    private static boolean hasCraftedCompression(ItemEnchantments enchantments) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            ModEnchantments.Family family = ModEnchantments.family(entry.getKey());
            if (family != null && family.isCrafted(entry.getIntValue())) {
                return true;
            }
        }

        return false;
    }
}
