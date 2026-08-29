package net.fahr3n.unnecessarilycompressedcobblestone;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedCobblestoneArrowRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedCobblestoneChickenRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedGolemRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompressionInscriberScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.MaterialCompressorScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModItemProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = UnnecessarilyCompressedCobblestone.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID, value = Dist.CLIENT)
public class UCCClient {
    public UCCClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        UnnecessarilyCompressedCobblestone.LOGGER.info("HELLO FROM CLIENT SETUP");
        UnnecessarilyCompressedCobblestone.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        event.enqueueWork(ModItemProperties::addCustomItemProperties);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPRESSED_COBBLESTONE_ARROW.get(), CompressedCobblestoneArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_GOLEM.get(), CompressedGolemRenderer::new);

        // A vanilla creeper in every respect the renderer cares about; the wind-up it draws comes
        // from the entity's own getSwelling override.
        event.registerEntityRenderer(ModEntities.COMPRESSED_CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get(),
                CompressedCobblestoneChickenRenderer::new);

        // TntRenderer draws whichever block state the primed entity is carrying, so both
        // Compressed TNTs render as themselves with no renderer of our own.
        event.registerEntityRenderer(ModEntities.COMPRESSED_PRIMED_TNT.get(), TntRenderer::new);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMPRESSION_INSCRIBER_MENU.get(), CompressionInscriberScreen::new);
        event.register(ModMenuTypes.MATERIAL_COMPRESSOR_MENU.get(), MaterialCompressorScreen::new);
    }
}
