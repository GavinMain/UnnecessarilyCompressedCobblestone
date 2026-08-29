package net.fahr3n.unnecessarilycompressedcobblestone.screen;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompressionInscriberMenu;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.MaterialCompressorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, UnnecessarilyCompressedCobblestone.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CompressionInscriberMenu>> COMPRESSION_INSCRIBER_MENU =
            registerMenuType("compression_inscriber_menu", CompressionInscriberMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MaterialCompressorMenu>> MATERIAL_COMPRESSOR_MENU =
            registerMenuType("material_compressor_tier_1_menu", MaterialCompressorMenu::new);

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
