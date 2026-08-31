package net.fahr3n.unnecessarilycompressedcobblestone.entity;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowVeilEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BossSummonEggEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneChickenEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedConjurerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPhantomEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPrimedTntEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSummonerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSkeletonEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.LightningConjurerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
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
    public static final Supplier<EntityType<CompressedArrowEntity>> COMPRESSED_COBBLESTONE_ARROW =
            ENTITY_TYPES.register("compressed_cobblestone_arrow", () -> arrow("compressed_cobblestone_arrow"));

    /** The same arrow three times over, which is six times a vanilla one. */
    public static final Supplier<EntityType<CompressedArrowEntity>> SUPER_COMPRESSED_ARROW =
            ENTITY_TYPES.register("super_compressed_arrow", () -> arrow("super_compressed_arrow"));

    /**
     * Every strength of compressed arrow is the same entity with a different type; which one it is
     * comes off the type itself, so none of them needs a class or a renderer of its own.
     */
    private static EntityType<CompressedArrowEntity> arrow(String name) {
        return EntityType.Builder.<CompressedArrowEntity>of(CompressedArrowEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
                .build(name);
    }

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
     * The same size vanilla gives {@code minecraft:drowned}, whose model it borrows. Tracked far
     * further than anything else here: it reaches 128 blocks, and a boss bar that vanished while its
     * owner was still being struck would be worse than none.
     */
    public static final Supplier<EntityType<CompressedConjurerEntity>> COMPRESSED_CONJURER =
            ENTITY_TYPES.register("compressed_conjurer",
                    () -> EntityType.Builder.of(CompressedConjurerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(16)
                            .build("compressed_conjurer"));

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

    /** The same size vanilla gives {@code minecraft:skeleton}, tracked as far as it can shoot. */
    public static final Supplier<EntityType<CompressedSkeletonEntity>> COMPRESSED_SKELETON =
            ENTITY_TYPES.register("compressed_skeleton", () -> skeleton("compressed_skeleton"));

    /** The second rung: heavier arrows and a sky full of falling TNT. */
    public static final Supplier<EntityType<CompressedSkeletonEntity>> COMPRESSED_SKELETON_TIER_2 =
            ENTITY_TYPES.register("compressed_skeleton_tier_2", () -> skeleton("compressed_skeleton_tier_2"));

    /** The last rung: every attack the first two have, and the volley on top. */
    public static final Supplier<EntityType<CompressedSkeletonEntity>> COMPRESSED_SKELETON_TIER_3 =
            ENTITY_TYPES.register("compressed_skeleton_tier_3", () -> skeleton("compressed_skeleton_tier_3"));

    /** Every rung of the arrow boss is the same entity; which one it is comes off the type. */
    private static EntityType<CompressedSkeletonEntity> skeleton(String name) {
        return EntityType.Builder.of(CompressedSkeletonEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(10)
                .build(name);
    }

    /**
     * The lesser conjurer, which spawns on its own in the dark. The same drowned size as the boss,
     * tracked at an ordinary mob's range because it only reaches 24 blocks.
     */
    public static final Supplier<EntityType<LightningConjurerEntity>> LIGHTNING_CONJURER =
            ENTITY_TYPES.register("lightning_conjurer",
                    () -> EntityType.Builder.of(LightningConjurerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8)
                            .build("lightning_conjurer"));

    /**
     * The same size vanilla gives a {@code minecraft:phantom} of size 0, whose model it borrows.
     * Tracked further than an ordinary mob: it crosses ground very fast and goes through walls, so
     * it is regularly further from a player than it looks.
     */
    public static final Supplier<EntityType<CompressedPhantomEntity>> COMPRESSED_PHANTOM =
            ENTITY_TYPES.register("compressed_phantom",
                    () -> EntityType.Builder.of(CompressedPhantomEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 0.5F).eyeHeight(0.175F).clientTrackingRange(12)
                            .build("compressed_phantom"));

    /**
     * The same size vanilla gives {@code minecraft:witch}, whose model and renderer it borrows.
     * Tracked far, because it spends the fight deliberately putting distance between itself and
     * whoever it is fighting and its boss bar has to survive that.
     */
    public static final Supplier<EntityType<CompressedSummonerEntity>> COMPRESSED_SUMMONER =
            ENTITY_TYPES.register("compressed_summoner",
                    () -> EntityType.Builder.of(CompressedSummonerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(16)
                            .build("compressed_summoner"));

    /**
     * The Arrow Veil's dome. MISC because it is a rule about projectiles rather than a creature -
     * nothing can hit it, stand on it or push it.
     */
    public static final Supplier<EntityType<ArrowVeilEntity>> ARROW_VEIL =
            ENTITY_TYPES.register("arrow_veil",
                    () -> EntityType.Builder.<ArrowVeilEntity>of(ArrowVeilEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(10)
                            .build("arrow_veil"));

    /**
     * The dragon egg every boss in this mod is called up out of. MISC because it is an animation
     * rather than a creature - it has no attributes, no AI and nothing that can be fought.
     */
    public static final Supplier<EntityType<BossSummonEggEntity>> BOSS_SUMMON_EGG =
            ENTITY_TYPES.register("boss_summon_egg",
                    () -> EntityType.Builder.<BossSummonEggEntity>of(BossSummonEggEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.8F).clientTrackingRange(10)
                            .build("boss_summon_egg"));

    /**
     * Ordinary lightning that can be told how loud to be. The same numbers vanilla gives
     * {@code minecraft:lightning_bolt}, {@code noSave} included: a bolt that outlived a reload would
     * be a bolt that never struck.
     */
    public static final Supplier<EntityType<VanillaLightningBoltEntity>> VANILLA_LIGHTNING_BOLT =
            ENTITY_TYPES.register("vanilla_lightning_bolt",
                    () -> EntityType.Builder.<VanillaLightningBoltEntity>of(VanillaLightningBoltEntity::new, MobCategory.MISC)
                            .noSave().sized(0.0F, 0.0F).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE)
                            .build("vanilla_lightning_bolt"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
