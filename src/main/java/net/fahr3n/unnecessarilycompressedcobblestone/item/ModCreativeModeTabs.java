package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.List;
import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BrokenCompressedSwordItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestonePickaxeTier2Item;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModPotions;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
                        output.accept(ModBlocks.DEV_COMPRESSED_COBBLESTONE);

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

                        output.accept(ModItems.COMPRESSION_MAGIC_HELMET);
                        output.accept(ModItems.COMPRESSION_MAGIC_CHESTPLATE);
                        output.accept(ModItems.COMPRESSION_MAGIC_LEGGINGS);
                        output.accept(ModItems.COMPRESSION_MAGIC_BOOTS);
                        output.accept(ModItems.ULTIMATE_COMPRESSED_HELMET);
                        output.accept(ModItems.ULTIMATE_COMPRESSED_CHESTPLATE);
                        output.accept(ModItems.ULTIMATE_COMPRESSED_LEGGINGS);
                        output.accept(ModItems.ULTIMATE_COMPRESSED_BOOTS);

                        output.accept(ModItems.COMPRESSION_RAIN_HELMET);
                        output.accept(ModItems.COMPRESSION_RAIN_CHESTPLATE);
                        output.accept(ModItems.COMPRESSION_RAIN_LEGGINGS);
                        output.accept(ModItems.COMPRESSION_RAIN_BOOTS);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_PICKAXE);

                        // Built as a stack rather than accepted as an item, for the reason the
                        // broken sword below is: its enchantments are read off the registry, and
                        // the tab is the one place a copy is handed out without ever being ticked.
                        ItemStack pickaxeTier2 = new ItemStack(ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get());
                        CompressedCobblestonePickaxeTier2Item.enchantFully(pickaxeTier2,
                                itemDisplayParameters.holders());
                        output.accept(pickaxeTier2);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_SWORD);
                        output.accept(ModItems.COMPRESSED_KATANA);
                        output.accept(ModItems.COMPRESSED_SCYTHE);
                        output.accept(ModItems.COMPRESSED_SPEAR);

                        // Built as a stack rather than accepted as an item: the enchantments are
                        // read off the registry, and the tab is the one place a copy is handed out
                        // without ever being ticked, so it would otherwise sit here bare.
                        ItemStack brokenSword = new ItemStack(ModItems.BROKEN_COMPRESSED_SWORD.get());
                        BrokenCompressedSwordItem.enchantFully(brokenSword, itemDisplayParameters.holders());
                        output.accept(brokenSword);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_MACE);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_BOW);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_ARROW);
                        output.accept(ModItems.SUPER_COMPRESSED_ARROW);
                        output.accept(ModItems.HYPER_COMPRESSED_ARROW);
                        output.accept(ModItems.COMPRESSED_LIGHTNING_STAFF);
                        output.accept(ModItems.COMPRESSED_ARROW_STAFF);
                        output.accept(ModItems.COMPRESSED_ARROW_TNT_STAFF);
                        output.accept(ModItems.COMPRESSED_HEALING_STAFF);
                        output.accept(ModItems.COMPRESSED_SUMMONING_STAFF);
                        output.accept(ModItems.ARROW_VEIL);
                        output.accept(ModItems.BLACK_HOLE_STOPPER);
                        output.accept(ModItems.COMPRESSED_FIREWORK);
                        output.accept(ModItems.COMPRESSED_FISHING_ROD);
                        output.accept(ModItems.COMPRESSION_BOMB);
                        output.accept(ModItems.COMPRESSED_TOTEM_OF_UNDYING);

                        output.accept(ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE);
                        output.accept(ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE);
                        output.accept(ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE);
                        output.accept(ModItems.TIER_4_COMPRESSED_COBBLESTONE_APPLE);

                        output.accept(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES);
                        output.accept(ModBlocks.COMPRESSED_COBBLESTONE_SAPLING);
                        output.accept(ModBlocks.REGEN_WEB);
                        output.accept(ModBlocks.DAMAGE_WEB);
                        output.accept(ModBlocks.SLIPPERY_ICE);

                        output.accept(ModBlocks.CARVED_COBBLESTONE_TIER_1);
                        output.accept(ModBlocks.CARVED_COBBLESTONE_TIER_2);
                        output.accept(ModBlocks.CARVED_COBBLESTONE_TIER_3);
                        output.accept(ModBlocks.LIGHTNING_CORE);
                        output.accept(ModItems.COMPRESSED_VANILLA_BOLT);
                        ModItems.NOTE_BOLTS.forEach(output::accept);
                        output.accept(ModItems.MOONLIGHT_BOLT);
                        output.accept(ModItems.COMPOSITION_BOLT);
                        output.accept(ModItems.REST_BOLT);
                        output.accept(ModItems.LONG_REST_BOLT);
                        output.accept(ModItems.BOLT_LAUNCHER);
                        output.accept(ModItems.TNT_LAUNCHER);
                        output.accept(ModBlocks.COMPOSITION_TABLE);

                        output.accept(ModBlocks.COMPRESSION_INSCRIBER);
                        output.accept(ModBlocks.ENGRAVING_TABLE);
                        ModItems.ENGRAVINGS.values().forEach(output::accept);
                        output.accept(ModItems.COMPRESSED_SHIELD);
                        output.accept(ModItems.COMPRESSED_BONE);
                        output.accept(ModItems.COMPRESSED_SADDLE);
                        output.accept(ModItems.RAY_OF_LASER);
                        output.accept(ModBlocks.LASER_AUGMENTATION_TABLE);
                        ModItems.AUGMENTS.values().forEach(output::accept);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_1);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_2);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_3);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_4);
                        output.accept(ModBlocks.MATERIAL_COMPRESSOR_TIER_5);
                        output.accept(ModBlocks.TELEPORTATION_GATE);

                        ModBlocks.TNTS.forEach(output::accept);
                        output.accept(ModItems.TIER_1_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_2_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_3_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_4_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_5_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_6_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_7_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_8_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_9_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_10_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_11_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_12_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_13_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_14_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_15_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_16_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_17_COMPRESSED_HEART);
                        output.accept(ModItems.TIER_18_COMPRESSED_HEART);
                        output.accept(ModItems.COMPRESSED_CREEPER_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_CONJURER_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SKELETON_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SKELETON_TIER_2_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SKELETON_TIER_3_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SUMMONER_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SPIRIT_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_WITCH_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_CHICKEN_BOSS_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_CREEPER_TIER_2_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_COMPOSER_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_GUARDIAN_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_HUSK_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_SNOW_GOLEM_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_GHAST_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_DRAGON_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_DRAGON_TIER_2_SPAWN_EGG);
                        output.accept(ModItems.COMPRESSED_DRAGON_EGG);
                        output.accept(ModItems.GHAST_PET_SPAWN_EGG);
                        output.accept(ModItems.GHAST_MOUNT_SPAWN_EGG);

                        // Vanilla's own tabs already list these, since they are built by walking the
                        // potion registry rather than a fixed list. Repeating them keeps everything
                        // the mod adds in one place, in the same four forms vanilla shows.
                        List<Holder<Potion>> brews = new java.util.ArrayList<>(
                                List.of(ModPotions.COMPRESSION_1, ModPotions.LONG_COMPRESSION_1));
                        for (ModPotions.Brew brew : ModPotions.BREWS) {
                            brews.add(brew.base());
                            brews.add(brew.extended());
                            brews.add(brew.strong());
                        }

                        for (Holder<Potion> potion : brews) {
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

                        // The craft-only singles, which no table will ever offer either.
                        for (ModEnchantments.CraftedEnchantment crafted : ModEnchantments.CRAFTED_ENCHANTMENTS) {
                            for (int level = 1; level <= crafted.maxLevel(); level++) {
                                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                                        itemDisplayParameters.holders().lookupOrThrow(Registries.ENCHANTMENT)
                                                .getOrThrow(crafted.key()),
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

                        // The dev pair: no recipe anywhere, so a creative tab is the only way to
                        // either of them. They sit at the end of the mod's own tab rather than in a
                        // tab of their own, so that everything the mod adds is in one place.
                        output.accept(ModItems.DEV_SWORD);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
