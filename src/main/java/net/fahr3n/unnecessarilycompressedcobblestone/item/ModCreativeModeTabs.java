package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UnnecessarilyCompressedCobblestone.MOD_ID);

    public static final Supplier<CreativeModeTab> UCC_TAB = CREATIVE_MODE_TAB.register("ucc_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.COMPRESSED_COBBLESTONE.get()))
                    .title(Component.translatable("creativetab.unnecessarilycompressedcobblestone.ucc"))
                    .displayItems((itemDisplayParameters, output) -> {
                        ModBlocks.COMPRESSED_COBBLESTONE_LEVELS.forEach(output::accept);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_HELMET);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_CHESTPLATE);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_LEGGINGS);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_BOOTS);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_SWORD);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_BOW);
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_ARROW);

                        output.accept(ModItems.COMPRESSED_COBBLESTONE_APPLE);

                        output.accept(ModBlocks.CARVED_COBBLESTONE_TIER_1);
                        output.accept(ModBlocks.COMPRESSION_INSCRIBER);
                        output.accept(ModItems.TIER_1_COMPRESSED_HEART);

                        // Levels 1 to 3 already show up as books in the ingredients tab like any
                        // other enchantment; the crafted ones are what vanilla knows nothing about.
                        for (int level = ModEnchantments.FIRST_CRAFTED_LEVEL; level <= ModEnchantments.LAST_CRAFTED_LEVEL; level++) {
                            output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                                    itemDisplayParameters.holders().lookupOrThrow(Registries.ENCHANTMENT)
                                            .getOrThrow(ModEnchantments.COMPRESSION),
                                    level)));
                        }
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
