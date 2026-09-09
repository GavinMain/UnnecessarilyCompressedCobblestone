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

    public static final Supplier<BlockEntityType<EngravingTableBlockEntity>> ENGRAVING_TABLE_BE =
            BLOCK_ENTITIES.register("engraving_table_be", () -> BlockEntityType.Builder.of(
                    EngravingTableBlockEntity::new, ModBlocks.ENGRAVING_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<LaserAugmentationTableBlockEntity>> LASER_AUGMENTATION_TABLE_BE =
            BLOCK_ENTITIES.register("laser_augmentation_table_be", () -> BlockEntityType.Builder.of(
                    LaserAugmentationTableBlockEntity::new, ModBlocks.LASER_AUGMENTATION_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<CompositionTableBlockEntity>> COMPOSITION_TABLE_BE =
            BLOCK_ENTITIES.register("composition_table_be", () -> BlockEntityType.Builder.of(
                    CompositionTableBlockEntity::new, ModBlocks.COMPOSITION_TABLE.get()).build(null));

    /**
     * One per gate, and only on its lower half - the upper half of a gate has no block entity at
     * all, which is what keeps a two-block doorway one gate rather than two.
     */
    public static final Supplier<BlockEntityType<TeleportationGateBlockEntity>> TELEPORTATION_GATE_BE =
            BLOCK_ENTITIES.register("teleportation_gate_be", () -> BlockEntityType.Builder.of(
                    TeleportationGateBlockEntity::new, ModBlocks.TELEPORTATION_GATE.get()).build(null));

    /** What a Lightning Core is holding, and the strikes it makes while it is powered. */
    public static final Supplier<BlockEntityType<LightningCoreBlockEntity>> LIGHTNING_CORE_BE =
            BLOCK_ENTITIES.register("lightning_core_be", () -> BlockEntityType.Builder.of(
                    LightningCoreBlockEntity::new, ModBlocks.LIGHTNING_CORE.get()).build(null));

    /**
     * One type for every tier of the machine - they share a block entity, and which tier a given one
     * is comes off its block. The registered id still says tier 1 because it is what every machine
     * already placed in a world is saved under, and changing it would delete them.
     */
    public static final Supplier<BlockEntityType<MaterialCompressorBlockEntity>> MATERIAL_COMPRESSOR_BE =
            BLOCK_ENTITIES.register("material_compressor_tier_1_be", () -> BlockEntityType.Builder.of(
                    MaterialCompressorBlockEntity::new,
                    ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get(),
                    ModBlocks.MATERIAL_COMPRESSOR_TIER_2.get(),
                    ModBlocks.MATERIAL_COMPRESSOR_TIER_3.get(),
                    ModBlocks.MATERIAL_COMPRESSOR_TIER_4.get(),
                    ModBlocks.MATERIAL_COMPRESSOR_TIER_5.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
