package net.fahr3n.unnecessarilycompressedcobblestone;

import net.fahr3n.unnecessarilycompressedcobblestone.block.client.LightningCoreRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.ArrowVeilRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.BossSummonEggRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedArrowRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedCobblestoneChickenRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedGolemRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompressionInscriberScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.EngravingTableScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.MaterialCompressorScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModItemProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
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
        event.registerEntityRenderer(ModEntities.COMPRESSED_COBBLESTONE_ARROW.get(), CompressedArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.SUPER_COMPRESSED_ARROW.get(), CompressedArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_GOLEM.get(), CompressedGolemRenderer::new);

        // A vanilla creeper in every respect the renderer cares about; the wind-up it draws comes
        // from the entity's own getSwelling override.
        event.registerEntityRenderer(ModEntities.COMPRESSED_CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get(),
                CompressedCobblestoneChickenRenderer::new);

        // A vanilla drowned in every respect the renderer cares about, trident and all.
        event.registerEntityRenderer(ModEntities.COMPRESSED_CONJURER.get(), DrownedRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_CONJURER.get(), DrownedRenderer::new);

        // A vanilla skeleton in every respect the renderer cares about, bow and all.
        event.registerEntityRenderer(ModEntities.COMPRESSED_SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SKELETON_TIER_2.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SKELETON_TIER_3.get(), SkeletonRenderer::new);

        // A vanilla phantom and a vanilla witch in every respect the renderers care about.
        event.registerEntityRenderer(ModEntities.COMPRESSED_PHANTOM.get(), PhantomRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SUMMONER.get(), WitchRenderer::new);

        event.registerEntityRenderer(ModEntities.BOSS_SUMMON_EGG.get(), BossSummonEggRenderer::new);
        event.registerEntityRenderer(ModEntities.ARROW_VEIL.get(), ArrowVeilRenderer::new);

        // Vanilla's own renderer: it reads nothing off the bolt but the public seed, so lightning
        // that only differs in how far it is heard needs no renderer of its own.
        event.registerEntityRenderer(ModEntities.VANILLA_LIGHTNING_BOLT.get(), LightningBoltRenderer::new);

        // TntRenderer draws whichever block state the primed entity is carrying, so both
        // Compressed TNTs render as themselves with no renderer of our own.
        event.registerEntityRenderer(ModEntities.COMPRESSED_PRIMED_TNT.get(), TntRenderer::new);
    }

    /** The bolt a Lightning Core is holding is drawn hovering over it, so the core needs a renderer. */
    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LIGHTNING_CORE_BE.get(), LightningCoreRenderer::new);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMPRESSION_INSCRIBER_MENU.get(), CompressionInscriberScreen::new);
        event.register(ModMenuTypes.MATERIAL_COMPRESSOR_MENU.get(), MaterialCompressorScreen::new);
        event.register(ModMenuTypes.ENGRAVING_TABLE_MENU.get(), EngravingTableScreen::new);
    }
}
