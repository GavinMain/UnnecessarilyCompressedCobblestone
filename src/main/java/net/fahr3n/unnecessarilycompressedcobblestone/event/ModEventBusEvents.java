package net.fahr3n.unnecessarilycompressedcobblestone.event;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedConjurerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowBossTier;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPhantomEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSummonerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.LightningConjurerEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class ModEventBusEvents {
    /** Entities that are not vanilla have to hand their attribute map to the game themselves. */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPRESSED_GOLEM.get(), CompressedGolemEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_CREEPER.get(), CompressedCreeperEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get(), Chicken.createAttributes().build());
        event.put(ModEntities.COMPRESSED_CONJURER.get(), CompressedConjurerEntity.createAttributes().build());
        event.put(ModEntities.LIGHTNING_CONJURER.get(), LightningConjurerEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_PHANTOM.get(), CompressedPhantomEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_SUMMONER.get(), CompressedSummonerEntity.createAttributes().build());
        for (ArrowBossTier tier : ArrowBossTier.values()) {
            event.put(tier.entityType().get(), tier.createAttributes().build());
        }
    }

    /**
     * Where the lesser conjurer is allowed to appear: on solid ground, in the dark, under the same
     * rule vanilla gives every ordinary monster. How often it appears is the biome modifier in
     * {@code data/<modid>/neoforge/biome_modifier/}, which is a hundredth of a skeleton's weight.
     */
    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.LIGHTNING_CONJURER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** What hoppers and item pipes see when they look at the inscriber. */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COMPRESSION_INSCRIBER_BE.get(),
                (blockEntity, side) -> blockEntity.automation);

        // The compressor answers differently per face, so the side is handed straight through.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MATERIAL_COMPRESSOR_BE.get(),
                (blockEntity, side) -> blockEntity.automationFor(side));
    }
}
