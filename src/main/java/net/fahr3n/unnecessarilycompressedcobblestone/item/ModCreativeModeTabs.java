package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
