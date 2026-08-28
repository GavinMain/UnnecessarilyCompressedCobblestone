package net.fahr3n.unnecessarilycompressedcobblestone.event;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
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
    }

    /** What hoppers and item pipes see when they look at the inscriber. */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COMPRESSION_INSCRIBER_BE.get(),
                (blockEntity, side) -> blockEntity.automation);
    }
}
