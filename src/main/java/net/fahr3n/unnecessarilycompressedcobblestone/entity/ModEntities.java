package net.fahr3n.unnecessarilycompressedcobblestone.entity;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
