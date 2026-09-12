package net.fahr3n.unnecessarilycompressedcobblestone;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.component.ModDataComponents;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantmentEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModArmorMaterials;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModCreativeModeTabs;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModPotions;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.sound.ModSounds;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModAttributeCeilings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(UnnecessarilyCompressedCobblestone.MOD_ID)
public class UnnecessarilyCompressedCobblestone {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "unnecessarilycompressedcobblestone";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public UnnecessarilyCompressedCobblestone(IEventBus modEventBus, ModContainer modContainer) {
        // First, and it has to be first: an entity's attributes are clamped to the ceiling standing
        // when its supplier is built, so a boss registered before this line keeps vanilla's 1024
        // health however high the ceiling goes afterwards.
        ModAttributeCeilings.raise();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModEnchantmentEffects.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModSounds.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Dispensers fire every bolt as a projectile, the way they fire an arrow. Walked off the
        // registry rather than listed, so all eighty-eight note bolts and every later bolt are
        // covered. The registry map is not thread safe, hence enqueueWork.
        event.enqueueWork(() -> BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof BoltItem)
                .forEach(DispenserBlock::registerProjectileBehavior));
    }

    // Adds this mod's items to vanilla creative tabs
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}
