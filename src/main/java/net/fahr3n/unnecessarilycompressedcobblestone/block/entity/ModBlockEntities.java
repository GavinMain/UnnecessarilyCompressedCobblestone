package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, UnnecessarilyCompressedCobblestone.MOD_ID);

    public static final Supplier<BlockEntityType<CompressionInscriberBlockEntity>> COMPRESSION_INSCRIBER_BE =
            BLOCK_ENTITIES.register("compression_inscriber_be", () -> BlockEntityType.Builder.of(
                    CompressionInscriberBlockEntity::new, ModBlocks.COMPRESSION_INSCRIBER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
