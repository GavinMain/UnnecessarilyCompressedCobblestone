package net.fahr3n.unnecessarilycompressedcobblestone.entity;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneChickenEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPrimedTntEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, UnnecessarilyCompressedCobblestone.MOD_ID);

    /** The same size and tracking numbers vanilla gives {@code minecraft:arrow}. */
    public static final Supplier<EntityType<CompressedCobblestoneArrowEntity>> COMPRESSED_COBBLESTONE_ARROW =
            ENTITY_TYPES.register("compressed_cobblestone_arrow",
                    () -> EntityType.Builder.<CompressedCobblestoneArrowEntity>of(CompressedCobblestoneArrowEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
                            .build("compressed_cobblestone_arrow"));

    /** The same size and category vanilla gives {@code minecraft:iron_golem}. */
    public static final Supplier<EntityType<CompressedGolemEntity>> COMPRESSED_GOLEM =
            ENTITY_TYPES.register("compressed_golem",
                    () -> EntityType.Builder.of(CompressedGolemEntity::new, MobCategory.MISC)
                            .sized(1.4F, 2.7F).clientTrackingRange(10)
                            .build("compressed_golem"));

    /** The same size vanilla gives {@code minecraft:creeper}, tracked as far as a boss should be. */
    public static final Supplier<EntityType<CompressedCreeperEntity>> COMPRESSED_CREEPER =
            ENTITY_TYPES.register("compressed_creeper",
                    () -> EntityType.Builder.of(CompressedCreeperEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F).clientTrackingRange(10)
                            .build("compressed_creeper"));

    /**
     * The same size and tracking numbers vanilla gives {@code minecraft:tnt}. Both Compressed TNTs
     * light this one type; how hard it goes off travels on the entity.
     */
    public static final Supplier<EntityType<CompressedPrimedTntEntity>> COMPRESSED_PRIMED_TNT =
            ENTITY_TYPES.register("compressed_primed_tnt",
                    () -> EntityType.Builder.<CompressedPrimedTntEntity>of(CompressedPrimedTntEntity::new, MobCategory.MISC)
                            .fireImmune().sized(0.98F, 0.98F).eyeHeight(0.15F).clientTrackingRange(10).updateInterval(10)
                            .build("compressed_primed_tnt"));

    /** The same size, eye height and rider seat vanilla gives {@code minecraft:chicken}. */
    public static final Supplier<EntityType<CompressedCobblestoneChickenEntity>> COMPRESSED_COBBLESTONE_CHICKEN =
            ENTITY_TYPES.register("compressed_cobblestone_chicken",
                    () -> EntityType.Builder.of(CompressedCobblestoneChickenEntity::new, MobCategory.CREATURE)
                            .sized(0.4F, 0.7F).eyeHeight(0.644F)
                            .passengerAttachments(new Vec3(0.0, 0.7, -0.1))
                            .clientTrackingRange(10)
                            .build("compressed_cobblestone_chicken"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
