package net.fahr3n.unnecessarilycompressedcobblestone;

import net.fahr3n.unnecessarilycompressedcobblestone.block.client.LightningCoreRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.client.CompressedShieldRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.NoteLightningBoltRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.ArrowVeilRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.BossSummonEggRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedArrowRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedChickenBossRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedCobblestoneChickenRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedDragonRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.CompressedGolemRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.OmniSlashRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.ScytheWaveRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompressionInscriberScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompositionTableScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.EngravingTableScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.LaserAugmentationTableScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.MaterialCompressorScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.TeleportationGateScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModItemProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.DragonFireballRenderer;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.FriendlyGhastRenderer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.client.GhastMountRenderer;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
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
        event.registerEntityRenderer(ModEntities.HYPER_COMPRESSED_ARROW.get(), CompressedArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_GOLEM.get(), CompressedGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_GOLEM_TIER_2.get(), CompressedGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_ORE_GOLEM.get(), CompressedGolemRenderer::new);

        // A vanilla creeper in every respect the renderer cares about; the wind-up it draws comes
        // from the entity's own getSwelling override.
        event.registerEntityRenderer(ModEntities.COMPRESSED_CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_CREEPER_TIER_2.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get(),
                CompressedCobblestoneChickenRenderer::new);

        // The boss is the same chicken model scaled by four, and its eggs are drawn as eggs.
        event.registerEntityRenderer(ModEntities.COMPRESSED_CHICKEN_BOSS.get(),
                CompressedChickenBossRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_EGG.get(), ThrownItemRenderer::new);

        // A vanilla drowned in every respect the renderer cares about, trident and all.
        event.registerEntityRenderer(ModEntities.COMPRESSED_CONJURER.get(), DrownedRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_CONJURER.get(), DrownedRenderer::new);

        // A vanilla skeleton in every respect the renderer cares about, bow and all.
        event.registerEntityRenderer(ModEntities.COMPRESSED_SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SKELETON_TIER_2.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SKELETON_TIER_3.get(), SkeletonRenderer::new);

        // A vanilla phantom and a vanilla witch in every respect the renderers care about.
        event.registerEntityRenderer(ModEntities.COMPRESSED_PHANTOM.get(), PhantomRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SPIRIT.get(), VexRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_WITCH.get(), WitchRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_COMPOSER.get(), EvokerRenderer::new);

        // A vanilla guardian in every respect the renderer cares about, beam and all: the ray is
        // drawn off getActiveAttackTarget, which the mod's own attack goal sets exactly as vanilla's
        // does.
        event.registerEntityRenderer(ModEntities.COMPRESSED_GUARDIAN.get(), ElderGuardianRenderer::new);

        // A vanilla husk in every respect the renderer cares about: what makes it a boss is its
        // reach, its floor and where it stands, none of which the model knows about.
        event.registerEntityRenderer(ModEntities.COMPRESSED_HUSK.get(), HuskRenderer::new);

        // A vanilla wolf in every respect the renderer cares about, collar and variant included -
        // both are copied across from the wolf that was fed, so a compressed one looks like the
        // animal it used to be.
        event.registerEntityRenderer(ModEntities.COMPRESSED_WOLF.get(), WolfRenderer::new);
        // Vanilla's own horse renderer draws it, coat and markings and all - there is nothing
        // about a Compressed Horse to see that is not a horse.
        event.registerEntityRenderer(ModEntities.COMPRESSED_HORSE.get(), HorseRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_BEE.get(), BeeRenderer::new);

        // A vanilla snow golem, pumpkin and all. And its snowballs are drawn as snowball items,
        // which is the one renderer line that needs no texture and no model.
        event.registerEntityRenderer(ModEntities.COMPRESSED_SNOW_GOLEM.get(), SnowGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SNOWBALL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSION_BOMB.get(), ThrownItemRenderer::new);

        // A vanilla silverfish in every respect the renderer cares about. That it is in the air
        // rather than on the ground is the move control's doing and nothing the model knows about.
        event.registerEntityRenderer(ModEntities.COMPRESSED_SILVERFISH.get(), SilverfishRenderer::new);

        // A bolt in flight is drawn as the item it was fired as, tumbling - which is the one
        // renderer that needs no work at all for eighty-eight different projectiles.
        event.registerEntityRenderer(ModEntities.BOLT_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.TNT_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_SUMMONER.get(), WitchRenderer::new);

        event.registerEntityRenderer(ModEntities.BOSS_SUMMON_EGG.get(), BossSummonEggRenderer::new);
        event.registerEntityRenderer(ModEntities.ARROW_VEIL.get(), ArrowVeilRenderer::new);
        event.registerEntityRenderer(ModEntities.SCYTHE_WAVE.get(), ScytheWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.OMNI_SLASH.get(), OmniSlashRenderer::new);
        // Both ghasts use vanilla's own renderer: they are ghasts, and the mini one is shrunk by
        // Attributes.SCALE, which LivingEntityRenderer already multiplies by.
        event.registerEntityRenderer(ModEntities.COMPRESSED_GHAST.get(), GhastRenderer::new);
        event.registerEntityRenderer(ModEntities.MINI_GHAST.get(), GhastRenderer::new);
        // Their own renderer rather than vanilla's: neither is a Ghast, so GhastRenderer cannot
        // be handed one. It draws the same model off the same two textures - see
        // FriendlyGhastRenderer.
        event.registerEntityRenderer(ModEntities.GHAST_PET.get(), FriendlyGhastRenderer::new);
        // The mount is the one that is built on, so it draws its deck on top of the ghast.
        event.registerEntityRenderer(ModEntities.GHAST_MOUNT.get(), GhastMountRenderer::new);
        // The deck's collision boxes are a shape and not a sight: the blocks they stand in for are
        // drawn by the mount, so drawing these as well would draw everything twice.
        event.registerEntityRenderer(ModEntities.PLATFORM_COLLIDER.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GHAST_FIRE_CHARGE.get(),
                context -> new ThrownItemRenderer<>(context, 0.75F, true));

        // Vanilla's own renderer: it reads nothing off the bolt but the public seed, so lightning
        // that only differs in how far it is heard needs no renderer of its own.
        // Vanilla's renderer, but for the colour: a note bolt is drawn in its own key's hue.
        event.registerEntityRenderer(ModEntities.VANILLA_LIGHTNING_BOLT.get(), NoteLightningBoltRenderer::new);

        // The dragon is the one mob here with a model posed by this mod rather than by vanilla:
        // the geometry is vanilla's baked ender dragon layer, and the posing is a copy, because
        // EnderDragonRenderer's model can only be handed an actual EnderDragon. Its breath is
        // vanilla's own dragon fireball renderer, which needs nothing from us.
        event.registerEntityRenderer(ModEntities.COMPRESSED_DRAGON.get(), CompressedDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_DRAGON_TIER_2.get(), CompressedDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_DRAGON_PET.get(), CompressedDragonRenderer::new);

        // The storm is drawn by nothing: it is a marker that plays music, and its lightning is the
        // only thing there is to see. NoopRenderer is vanilla's own answer for exactly that.
        event.registerEntityRenderer(ModEntities.BOSS_MUSIC_WEATHER.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.COMPRESSED_DRAGON_BREATH.get(), DragonFireballRenderer::new);

        // TntRenderer draws whichever block state the primed entity is carrying, so both
        // Compressed TNTs render as themselves with no renderer of our own.
        event.registerEntityRenderer(ModEntities.COMPRESSED_PRIMED_TNT.get(), TntRenderer::new);
    }

    /** The bolt a Lightning Core is holding is drawn hovering over it, so the core needs a renderer. */
    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LIGHTNING_CORE_BE.get(), LightningCoreRenderer::new);
    }

    /**
     * The Compressed Shield is drawn by a renderer of its own, because vanilla{@literal '}s
     * BlockEntityWithoutLevelRenderer tests {@code stack.is(Items.SHIELD)} by identity - so any
     * other shield in the game is drawn as nothing at all, not merely as something flat. See
     * {@link CompressedShieldRenderer}.
     */
    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private CompressedShieldRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new CompressedShieldRenderer();
                }

                return this.renderer;
            }
        }, ModItems.COMPRESSED_SHIELD.get());
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMPRESSION_INSCRIBER_MENU.get(), CompressionInscriberScreen::new);
        event.register(ModMenuTypes.MATERIAL_COMPRESSOR_MENU.get(), MaterialCompressorScreen::new);
        event.register(ModMenuTypes.COMPOSITION_TABLE_MENU.get(), CompositionTableScreen::new);
        event.register(ModMenuTypes.ENGRAVING_TABLE_MENU.get(), EngravingTableScreen::new);
        event.register(ModMenuTypes.LASER_AUGMENTATION_TABLE_MENU.get(), LaserAugmentationTableScreen::new);
        event.register(ModMenuTypes.TELEPORTATION_GATE_MENU.get(), TeleportationGateScreen::new);
    }
}
