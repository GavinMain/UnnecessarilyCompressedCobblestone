package net.fahr3n.unnecessarilycompressedcobblestone.event;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class ModEventBusEvents {
    /** Entities that are not vanilla have to hand their attribute map to the game themselves. */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPRESSED_GOLEM.get(), CompressedGolemEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_CREEPER.get(), CompressedCreeperEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get(), Chicken.createAttributes().build());
    }

    /** What hoppers and item pipes see when they look at the inscriber. */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COMPRESSION_INSCRIBER_BE.get(),
                (blockEntity, side) -> blockEntity.automation);

        // The compressor answers differently per face, so the side is handed straight through.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MATERIAL_COMPRESSOR_TIER_1_BE.get(),
                (blockEntity, side) -> blockEntity.automationFor(side));
    }
}
