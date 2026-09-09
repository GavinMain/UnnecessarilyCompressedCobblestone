package net.fahr3n.unnecessarilycompressedcobblestone.event;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.network.SetTeleportationGatePayload;
import net.fahr3n.unnecessarilycompressedcobblestone.network.PlatformSyncPayload;
import net.fahr3n.unnecessarilycompressedcobblestone.network.SwingScythePayload;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedChickenBossEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedConjurerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperTier2Entity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowBossTier;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGuardianEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedHuskEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSnowGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedBeeEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGhastEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedOreGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.GhastMountEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.GhastPetEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.MiniGhastEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedHorseEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedWolfEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemTier2Entity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPhantomEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSpiritEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSummonerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSilverfishEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedComposerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonPetEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonTier2Entity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedWitchEntity;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class ModEventBusEvents {
    /** Entities that are not vanilla have to hand their attribute map to the game themselves. */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPRESSED_GOLEM.get(), CompressedGolemEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_GOLEM_TIER_2.get(), CompressedGolemTier2Entity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_CREEPER.get(), CompressedCreeperEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_CREEPER_TIER_2.get(), CompressedCreeperTier2Entity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get(), Chicken.createAttributes().build());
        event.put(ModEntities.COMPRESSED_CHICKEN_BOSS.get(), CompressedChickenBossEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_CONJURER.get(), CompressedConjurerEntity.createAttributes().build());
        event.put(ModEntities.LIGHTNING_CONJURER.get(), LightningConjurerEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_PHANTOM.get(), CompressedPhantomEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_SUMMONER.get(), CompressedSummonerEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_SPIRIT.get(), CompressedSpiritEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_WITCH.get(), CompressedWitchEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_COMPOSER.get(), CompressedComposerEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_SILVERFISH.get(), CompressedSilverfishEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_GUARDIAN.get(), CompressedGuardianEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_HUSK.get(), CompressedHuskEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_WOLF.get(), CompressedWolfEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_HORSE.get(), CompressedHorseEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_BEE.get(), CompressedBeeEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_ORE_GOLEM.get(), CompressedOreGolemEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_GHAST.get(), CompressedGhastEntity.createAttributes().build());
        event.put(ModEntities.MINI_GHAST.get(), MiniGhastEntity.createAttributes().build());
        event.put(ModEntities.GHAST_PET.get(), GhastPetEntity.createAttributes().build());
        event.put(ModEntities.GHAST_MOUNT.get(), GhastMountEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_SNOW_GOLEM.get(), CompressedSnowGolemEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_DRAGON.get(), CompressedDragonEntity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_DRAGON_TIER_2.get(), CompressedDragonTier2Entity.createAttributes().build());
        event.put(ModEntities.COMPRESSED_DRAGON_PET.get(), CompressedDragonPetEntity.createAttributes().build());
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

    /**
     * The mod's one packet: the two names a player typed into a Teleportation Gate's screen. Version
     * "1" is the channel's own, and it is not optional - a client without this mod cannot open the
     * screen that sends it in the first place.
     */
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SetTeleportationGatePayload.TYPE,
                SetTeleportationGatePayload.STREAM_CODEC, SetTeleportationGatePayload::handle);
        event.registrar("1").playToServer(SwingScythePayload.TYPE,
                SwingScythePayload.STREAM_CODEC, SwingScythePayload::handle);
        // The one payload that goes the other way: a Ghast Mount's deck is a map rather than a
        // value, so there is no synched-data serializer for it.
        event.registrar("1").playToClient(PlatformSyncPayload.TYPE,
                PlatformSyncPayload.STREAM_CODEC, PlatformSyncPayload::handle);
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
