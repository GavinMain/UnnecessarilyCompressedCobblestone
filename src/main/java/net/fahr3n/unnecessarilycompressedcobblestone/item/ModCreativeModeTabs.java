package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.List;
import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModPotions;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UnnecessarilyCompressedCobblestone.MOD_ID);

    public static final Supplier<CreativeModeTab> UCC_TAB = CREATIVE_MODE_TAB.register("ucc_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.byLevel(1).get()))
                    .title(Component.translatable("creativetab.unnecessarilycompressedcobblestone.ucc"))
                    .displayItems((itemDisplayParameters, output) -> {
                        ModBlocks.COMPRESSED_COBBLESTONE_LEVELS.forEach(output::accept);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_HELMET);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_CHESTPLATE);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_LEGGINGS);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_BOOTS);

                        output.accept(ModItems.COMPRESSION_JUMP_HELMET);
                        output.accept(ModItems.COMPRESSION_JUMP_CHESTPLATE);
                        output.accept(ModItems.COMPRESSION_JUMP_LEGGINGS);
                        output.accept(ModItems.COMPRESSION_JUMP_BOOTS);

                        output.accept(ModItems.COMPRESSION_LIGHTNING_HELMET);
                        output.accept(ModItems.COMPRESSION_LIGHTNING_CHESTPLATE);
                        output.accept(ModItems.COMPRESSION_LIGHTNING_LEGGINGS);
                        output.accept(ModItems.COMPRESSION_LIGHTNING_BOOTS);

                        output.accept(ModItems.COMPRESSION_ARROW_HELMET);
                        output.accept(ModItems.COMPRESSION_ARROW_CHESTPLATE);
                        output.accept(ModItems.COMPRESSION_ARROW_LEGGINGS);
                        output.accept(ModItems.COMPRESSION_ARROW_BOOTS);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_PICKAXE);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_SWORD);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_MACE);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_BOW);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_ARROW);
                        output.accept(ModItems.SUPER_COMPRESSED_ARROW);
                        output.accept(ModItems.COMPRESSED_LIGHTNING_STAFF);
                        output.accept(ModItems.COMPRESSED_ARROW_STAFF);
                        output.accept(ModItems.COMPRESSED_ARROW_TNT_STAFF);
                        output.accept(ModItems.COMPRESSED_SUMMONING_STAFF);
                        output.accept(ModItems.ARROW_VEIL);

                        output.accept(ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE);
                        output.accept(ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE);
                        output.accept(ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE);

                        output.accept(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES);
                        output.accept(ModBlocks.COMPRESSED_COBBLESTONE_SAPLING);

                        output.accept(ModBlocks.CARVED_COBBLESTONE_TIER_1);
                        output.accept(ModBlocks.LIGHTNING_CORE);
                        output.accept(ModItems.COMPRESSED_VANILLA_BOLT);

                        output.accept(ModBlocks.COMPRESSION_INSCRIBER);
                        output.accept(ModBlocks.ENGRAVING_TABLE);
                        ModItems.ENGRAVINGS.values().forEach(output::accept);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_1);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_2);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_3);

                        ModBlocks.TNTS.forEach(output::accept);
                        output.accept(ModItems.TIER_1_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_2_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_3_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_4_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_5_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_6_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_7_COMPRESSED_HEART);
                        output.accept(ModItems.COMPRESSED_CREEPER_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_CONJURER_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SKELETON_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SKELETON_TIER_2_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SKELETON_TIER_3_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SUMMONER_SPAWN_EGG);

                        // Vanilla's own tabs already list these, since they are built by walking the
                        // potion registry rather than a fixed list. Repeating them keeps everything
                        // the mod adds in one place, in the same four forms vanilla shows.
                        for (Holder<Potion> potion : List.of(ModPotions.COMPRESSION_1, ModPotions.LONG_COMPRESSION_1)) {
                            for (Item bottle : List.of(Items.POTION, Items.SPLASH_POTION,
                                    Items.LINGERING_POTION, Items.TIPPED_ARROW)) {
                                output.accept(PotionContents.createItemStack(bottle, potion));
                            }
                        }

                        // Compression 1 to 3 already shows up as books in the ingredients tab like
                        // any other enchantment; the crafted levels are what vanilla knows nothing
                        // about, and the three craft-only rungs are crafted all the way down.
                        // The rungs of each staff enchantment that no table will ever offer.
                        for (ModEnchantments.StaffEnchantment staff : ModEnchantments.STAFF_ENCHANTMENTS) {
                            for (int level = staff.firstCraftedLevel(); level <= staff.maxLevel(); level++) {
                                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                                        itemDisplayParameters.holders().lookupOrThrow(Registries.ENCHANTMENT)
                                                .getOrThrow(staff.key()),
                                        level)));
                            }
                        }

                        for (ModEnchantments.Family family : ModEnchantments.FAMILIES) {
                            for (int level = family.firstCraftedLevel(); level <= family.maxLevel(); level++) {
                                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                                        itemDisplayParameters.holders().lookupOrThrow(Registries.ENCHANTMENT)
                                                .getOrThrow(family.key()),
                                        level)));
                            }
                        }
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
