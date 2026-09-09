package net.fahr3n.unnecessarilycompressedcobblestone.entity;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowVeilEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BoltProjectileEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.OmniSlashEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ScytheWaveEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.TntProjectileEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BossSummonEggEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedChickenBossEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneChickenEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedEggEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedConjurerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonBreathEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BossMusicWeatherEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonPetEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonTier2Entity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCreeperTier2Entity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemTier2Entity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGuardianEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedHorseEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedBeeEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedHuskEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGhastEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedOreGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.GhastFireChargeEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.GhastMountEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.GhastPetEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.MiniGhastEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.PlatformColliderEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSnowGolemEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSnowballEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressionBombEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedWolfEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPhantomEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPrimedTntEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSpiritEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSummonerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedComposerEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedWitchEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSilverfishEntity;
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

    /** Five times the Super, and so heavy it is on the ground the moment it is loosed. */
    public static final Supplier<EntityType<CompressedArrowEntity>> HYPER_COMPRESSED_ARROW =
            ENTITY_TYPES.register("hyper_compressed_arrow", () -> arrow("hyper_compressed_arrow"));

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
     * The second creeper. The same size and the same vanilla renderer as the first - it is the same
     * shape of thing, made of deeper stone - and tracked further, because a blast four times as wide
     * reaches well past where an ordinary mob is still being sent to the client.
     */
    public static final Supplier<EntityType<CompressedCreeperTier2Entity>> COMPRESSED_CREEPER_TIER_2 =
            ENTITY_TYPES.register("compressed_creeper_tier_2",
                    () -> EntityType.Builder.of(CompressedCreeperTier2Entity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F).clientTrackingRange(16)
                            .build("compressed_creeper_tier_2"));

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

    /**
     * The chicken boss. The size registered here is a plain chicken's, and that is deliberate: how
     * much bigger than one it is lives in {@code Attributes.SCALE}, which vanilla applies to the
     * hitbox, the drawing and the shadow alike - see
     * {@link CompressedChickenBossEntity#BOSS_SCALE}. Stating a big size here as well would multiply
     * the two together and the hitbox would no longer be the shape on screen.
     * <p>
     * Tracked as far as a boss bar has to reach, since a ram carries it well out of an ordinary
     * mob's range.
     */
    public static final Supplier<EntityType<CompressedChickenBossEntity>> COMPRESSED_CHICKEN_BOSS =
            ENTITY_TYPES.register("compressed_chicken_boss",
                    () -> EntityType.Builder.of(CompressedChickenBossEntity::new, MobCategory.MONSTER)
                            .sized(0.4F, 0.7F).eyeHeight(0.644F)
                            .passengerAttachments(new Vec3(0.0, 0.7, -0.1))
                            .clientTrackingRange(16)
                            .build("compressed_chicken_boss"));

    /**
     * The egg that boss throws. The same size and tracking numbers vanilla gives
     * {@code minecraft:egg}; it is drawn as the item it is thrown as, so it needs no renderer work.
     */
    public static final Supplier<EntityType<CompressedEggEntity>> COMPRESSED_EGG =
            ENTITY_TYPES.register("compressed_egg",
                    () -> EntityType.Builder.<CompressedEggEntity>of(CompressedEggEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                            .build("compressed_egg"));

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
     * The same size vanilla gives {@code minecraft:witch}, whose model and renderer it borrows.
     * Tracked far: it fights at a throwing distance and its boss bar has to survive that.
     */
    public static final Supplier<EntityType<CompressedWitchEntity>> COMPRESSED_WITCH =
            ENTITY_TYPES.register("compressed_witch",
                    () -> EntityType.Builder.of(CompressedWitchEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(16)
                            .build("compressed_witch"));

    /**
     * The same size vanilla gives {@code minecraft:evoker}, whose model and renderer it borrows -
     * the illager's spellcasting pose is a conductor's. Tracked as far as the boss bar has to reach:
     * it puts forty blocks between itself and its target before every performance.
     */
    public static final Supplier<EntityType<CompressedComposerEntity>> COMPRESSED_COMPOSER =
            ENTITY_TYPES.register("compressed_composer",
                    () -> EntityType.Builder.of(CompressedComposerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(16)
                            .build("compressed_composer"));

    /**
     * The same size vanilla gives a {@code minecraft:vex}, whose model and renderer it borrows.
     * Tracked as far as the boss bar has to reach: it crosses ground five times as fast as a vex and
     * goes through walls, so it is regularly much further from a player than it looks.
     */
    public static final Supplier<EntityType<CompressedSpiritEntity>> COMPRESSED_SPIRIT =
            ENTITY_TYPES.register("compressed_spirit",
                    () -> EntityType.Builder.of(CompressedSpiritEntity::new, MobCategory.MONSTER)
                            .sized(0.4F, 0.8F).eyeHeight(0.51875F).clientTrackingRange(16)
                            .build("compressed_spirit"));

    /**
     * The same size vanilla gives {@code minecraft:guardian}, whose model and renderer it borrows.
     * Tracked as far as its beam reaches, since a boss bar that stops before the thing shooting at
     * you does is worse than none.
     */
    /**
     * The Compressed Husk. Vanilla's own husk size, since it is drawn by vanilla's husk renderer,
     * and tracked further than an ordinary mob: it steps twenty blocks out of the fight every five
     * seconds, and a boss bar that stops before the boss does is worse than none.
     */
    /**
     * The Compressed Wolf. A vanilla wolf's size and a vanilla wolf's renderer - what is different
     * about it is four attributes and nothing the client can see.
     */
    /**
     * The Compressed Snow Golem. Vanilla's own snow golem size and renderer; what makes it a boss is
     * that nothing but a fall touches it. Tracked as far as it can throw.
     */
    public static final Supplier<EntityType<CompressedSnowGolemEntity>> COMPRESSED_SNOW_GOLEM =
            ENTITY_TYPES.register("compressed_snow_golem",
                    () -> EntityType.Builder.of(CompressedSnowGolemEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 1.9F).eyeHeight(1.7F).clientTrackingRange(16)
                            .build("compressed_snow_golem"));

    /**
     * What it throws. A snowball as far as the renderer is concerned - it is drawn by
     * {@code ThrownItemRenderer} off the snowball item, so it needs no texture - and a hundred
     * points of damage as far as anything it hits is concerned.
     */
    public static final Supplier<EntityType<CompressedSnowballEntity>> COMPRESSED_SNOWBALL =
            ENTITY_TYPES.register("compressed_snowball",
                    () -> EntityType.Builder.<CompressedSnowballEntity>of(CompressedSnowballEntity::new,
                                    MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                            .build("compressed_snowball"));

    /**
     * A Compression Bomb in flight. Drawn by {@code ThrownItemRenderer} off the item it was thrown
     * as, the way the snowball above it is, so it needs no renderer work and no texture of its own.
     */
    public static final Supplier<EntityType<CompressionBombEntity>> COMPRESSION_BOMB =
            ENTITY_TYPES.register("compression_bomb",
                    () -> EntityType.Builder.<CompressionBombEntity>of(CompressionBombEntity::new,
                                    MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                            .build("compression_bomb"));

    public static final Supplier<EntityType<CompressedWolfEntity>> COMPRESSED_WOLF =
            ENTITY_TYPES.register("compressed_wolf",
                    () -> EntityType.Builder.of(CompressedWolfEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.85F).eyeHeight(0.68F).clientTrackingRange(10)
                            .build("compressed_wolf"));

    /**
     * Registered at a plain horse's own size: the mob is not scaled, only its numbers are, so the
     * hitbox, the model and the shadow all stay where a horse's are.
     */
    public static final Supplier<EntityType<CompressedHorseEntity>> COMPRESSED_HORSE =
            ENTITY_TYPES.register("compressed_horse",
                    () -> EntityType.Builder.of(CompressedHorseEntity::new, MobCategory.CREATURE)
                            .sized(1.3964844F, 1.6F).clientTrackingRange(10)
                            .build("compressed_horse"));

    /**
     * Registered at a plain bee's own size and in the same {@code CREATURE} category, which is what
     * keeps vanilla's {@code BeeRenderer} and every bee-shaped rule in the game working on it.
     */
    public static final Supplier<EntityType<CompressedBeeEntity>> COMPRESSED_BEE =
            ENTITY_TYPES.register("compressed_bee",
                    () -> EntityType.Builder.of(CompressedBeeEntity::new, MobCategory.CREATURE)
                            .sized(0.7F, 0.6F).eyeHeight(0.3F).clientTrackingRange(8)
                            .build("compressed_bee"));

    public static final Supplier<EntityType<CompressedHuskEntity>> COMPRESSED_HUSK =
            ENTITY_TYPES.register("compressed_husk",
                    () -> EntityType.Builder.of(CompressedHuskEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(16)
                            .build("compressed_husk"));

    public static final Supplier<EntityType<CompressedGuardianEntity>> COMPRESSED_GUARDIAN =
            ENTITY_TYPES.register("compressed_guardian",
                    // An elder guardian's own size, not a guardian's: the renderer scales the model
                    // by ELDER_SIZE_SCALE off the two types' registered widths, so a hitbox left at
                    // 0.85 would be a boss drawn two blocks wide and hit at less than one.
                    () -> EntityType.Builder.of(CompressedGuardianEntity::new, MobCategory.MONSTER)
                            .sized(1.9975F, 1.9975F).clientTrackingRange(16)
                            .build("compressed_guardian"));

    /**
     * The second golem. The same size and the same renderer as the first - it is the same shape of
     * thing, built out of deeper stone - and tracked further, because a jump pound carries it well
     * out of an ordinary mob's range.
     */
    public static final Supplier<EntityType<CompressedGolemTier2Entity>> COMPRESSED_GOLEM_TIER_2 =
            ENTITY_TYPES.register("compressed_golem_tier_2",
                    () -> EntityType.Builder.of(CompressedGolemTier2Entity::new, MobCategory.MONSTER)
                            .sized(1.4F, 2.7F).clientTrackingRange(16)
                            .build("compressed_golem_tier_2"));

    /**
     * The Compressed Ore Golem, at an iron golem's own size. It is built the way the other two are,
     * out of level {@code ModBlocks.ORE_GOLEM_LEVEL} stone.
     */
    public static final Supplier<EntityType<CompressedOreGolemEntity>> COMPRESSED_ORE_GOLEM =
            ENTITY_TYPES.register("compressed_ore_golem",
                    () -> EntityType.Builder.of(CompressedOreGolemEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 2.7F).clientTrackingRange(16)
                            .build("compressed_ore_golem"));

    /**
     * The same size vanilla gives {@code minecraft:silverfish}, whose model and renderer it borrows.
     * Tracked further than a vanilla silverfish is: this one flies and crosses ground three times as
     * fast, so it is regularly much further from a player than a silverfish has any business being.
     */
    public static final Supplier<EntityType<CompressedSilverfishEntity>> COMPRESSED_SILVERFISH =
            ENTITY_TYPES.register("compressed_silverfish",
                    () -> EntityType.Builder.of(CompressedSilverfishEntity::new, MobCategory.MONSTER)
                            .sized(0.4F, 0.3F).eyeHeight(0.13F).clientTrackingRange(10)
                            .build("compressed_silverfish"));

    /**
     * A bolt in flight out of a Bolt Launcher. Arrow-sized, and tracked the way an arrow is: it is
     * only ever seen between the string and the ground.
     */
    public static final Supplier<EntityType<BoltProjectileEntity>> BOLT_PROJECTILE =
            ENTITY_TYPES.register("bolt_projectile",
                    () -> EntityType.Builder.<BoltProjectileEntity>of(BoltProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                            .build("bolt_projectile"));

    /**
     * A block of TNT in flight out of a TNT Launcher. Sized as the block it draws itself as rather
     * than as an arrow, since a shell that size is a thing a player has to see coming.
     */
    public static final Supplier<EntityType<TntProjectileEntity>> TNT_PROJECTILE =
            ENTITY_TYPES.register("tnt_projectile",
                    () -> EntityType.Builder.<TntProjectileEntity>of(TntProjectileEntity::new, MobCategory.MISC)
                            .sized(0.98F, 0.98F).clientTrackingRange(4).updateInterval(20)
                            .build("tnt_projectile"));

    /**
     * A scythe's swing, in flight. Tracked far further than an arrow is and updated every other
     * tick, because the whole of the engraving is that a player watches their cut travel to
     * something they could not reach - a crescent that stopped being drawn at four chunks would
     * disappear the moment it became interesting.
     */
    public static final Supplier<EntityType<ScytheWaveEntity>> SCYTHE_WAVE =
            ENTITY_TYPES.register("scythe_wave",
                    () -> EntityType.Builder.<ScytheWaveEntity>of(ScytheWaveEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.8F).clientTrackingRange(32).updateInterval(2)
                            .build("scythe_wave"));

    /**
     * An Omni Slash in flight. Tracked and updated the way the scythe's crescent is, and for the
     * same reason: the whole of the engraving is watching the cut travel, and one that stopped being
     * drawn at four chunks would vanish exactly when it became worth looking at.
     * <p>
     * Registered at a token size. The hitbox is never consulted - the slash passes through
     * everything and does its own swept test against the line it travelled - so a box the shape of
     * the crescent would only be a five block thing for other entities to bump into.
     */
    /**
     * The Compressed Ghast. A ghast's own size, since nothing about the boss is bigger - what is
     * different is what leaves its mouth. Tracked far out, because a fight about volleys crossing
     * sixty blocks is unplayable if the thing throwing them stops being drawn at half that.
     */
    public static final Supplier<EntityType<CompressedGhastEntity>> COMPRESSED_GHAST =
            ENTITY_TYPES.register("compressed_ghast",
                    () -> EntityType.Builder.of(CompressedGhastEntity::new, MobCategory.MONSTER)
                            .sized(4.0F, 4.0F).clientTrackingRange(10).fireImmune()
                            .build("compressed_ghast"));

    /**
     * One of the little ghasts a Ghasted player carries. MISC rather than MONSTER on purpose: it is
     * an effect wearing a mob's model, and putting it in the hostile category would let a flock of
     * them count against the mob cap and stop real mobs spawning.
     * <p>
     * Registered at the unscaled ghast size and shrunk with {@code Attributes.SCALE}, which is the
     * mod's rule - the attribute is what {@code getDimensions}, the renderer and the shadow all read.
     */
    public static final Supplier<EntityType<MiniGhastEntity>> MINI_GHAST =
            ENTITY_TYPES.register("mini_ghast",
                    () -> EntityType.Builder.of(MiniGhastEntity::new, MobCategory.MISC)
                            .sized(4.0F, 4.0F).clientTrackingRange(8).fireImmune()
                            .build("mini_ghast"));

    /**
     * The Ghast Pet. CREATURE rather than MONSTER: it belongs to a player and fights for them, and
     * putting it in the hostile category would have a flock of them count against the mob cap and
     * stop real monsters spawning.
     * <p>
     * Registered at the unscaled ghast size and shrunk with {@code Attributes.SCALE}, which is the
     * mod's rule - the attribute is what {@code getDimensions}, the renderer and the shadow all read.
     */
    public static final Supplier<EntityType<GhastPetEntity>> GHAST_PET =
            ENTITY_TYPES.register("ghast_pet",
                    () -> EntityType.Builder.of(GhastPetEntity::new, MobCategory.CREATURE)
                            .sized(4.0F, 4.0F).clientTrackingRange(10).fireImmune()
                            .build("ghast_pet"));

    /**
     * The Ghast Mount, at a ghast's own full size, since what makes it a mount is that it is big
     * enough to sit on. {@code passengerAttachments} is where the rider is put: three blocks up sits
     * them on the top of the cube rather than inside it, which is as close to a saddle as a creature
     * with no back has.
     */
    public static final Supplier<EntityType<GhastMountEntity>> GHAST_MOUNT =
            ENTITY_TYPES.register("ghast_mount",
                    () -> EntityType.Builder.of(GhastMountEntity::new, MobCategory.CREATURE)
                            .sized(4.0F, 4.0F).passengerAttachments(3.0F)
                            .clientTrackingRange(10).fireImmune()
                            .build("ghast_mount"));

    /**
     * One box of a Ghast Mount's deck: the surface a foot lands on, and nothing else. MISC and
     * sized at nothing, because neither figure is used - {@code PlatformColliderEntity} writes its
     * own bounding box, and it puts itself where its mount is every tick on both sides, so the
     * tracking interval only has to be often enough to notice one appearing.
     */
    public static final Supplier<EntityType<PlatformColliderEntity>> PLATFORM_COLLIDER =
            ENTITY_TYPES.register("platform_collider",
                    () -> EntityType.Builder.<PlatformColliderEntity>of(PlatformColliderEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(20).noSummon()
                            .build("platform_collider"));

    /**
     * What a Compressed Ghast shoots, in both its forms. Drawn as the fire charge item it carries,
     * so it needs no renderer of its own - see {@code GhastFireChargeEntity}.
     */
    public static final Supplier<EntityType<GhastFireChargeEntity>> GHAST_FIRE_CHARGE =
            ENTITY_TYPES.register("ghast_fire_charge",
                    () -> EntityType.Builder.<GhastFireChargeEntity>of(GhastFireChargeEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(10)
                            .build("ghast_fire_charge"));

    public static final Supplier<EntityType<OmniSlashEntity>> OMNI_SLASH =
            ENTITY_TYPES.register("omni_slash",
                    () -> EntityType.Builder.<OmniSlashEntity>of(OmniSlashEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.8F).clientTrackingRange(32).updateInterval(2)
                            .build("omni_slash"));

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
     * <p>
     * The one number that is not vanilla's is the tracking range: 32 chunks rather than 16, because
     * a note bolt's sound is played by the client off its own copy of the entity and is played with
     * attenuation off, so how far a note carries is decided here and nowhere else. 32 is the largest
     * render distance the game offers, and the server's own view distance still caps it.
     */
    public static final Supplier<EntityType<VanillaLightningBoltEntity>> VANILLA_LIGHTNING_BOLT =
            ENTITY_TYPES.register("vanilla_lightning_bolt",
                    () -> EntityType.Builder.<VanillaLightningBoltEntity>of(VanillaLightningBoltEntity::new, MobCategory.MISC)
                            .noSave().sized(0.0F, 0.0F).clientTrackingRange(32).updateInterval(Integer.MAX_VALUE)
                            .build("vanilla_lightning_bolt"));

    /**
     * The Compressed Dragon: the mod's last fight, and the only entity here registered at vanilla's
     * ender dragon size. Sixteen by eight is the whole beast including its wings, and it is a plain
     * hitbox rather than vanilla's eight part entities - there is one thing to hit and it is all of
     * it, which is what lets the fight be about where you are standing rather than about which
     * segment your sword found.
     * <p>
     * Fire immune, because it breathes; tracked as far as the game allows, because it circles
     * eighteen blocks out and thirty up and its boss bar has to survive that.
     */
    public static final Supplier<EntityType<CompressedDragonEntity>> COMPRESSED_DRAGON =
            ENTITY_TYPES.register("compressed_dragon",
                    () -> EntityType.Builder.of(CompressedDragonEntity::new, MobCategory.MONSTER)
                            .sized(16.0F, 8.0F).clientTrackingRange(16).fireImmune()
                            .build("compressed_dragon"));

    /**
     * Its breath. The same size and tracking numbers vanilla gives {@code minecraft:dragon_fireball},
     * whose renderer draws it.
     */
    public static final Supplier<EntityType<CompressedDragonBreathEntity>> COMPRESSED_DRAGON_BREATH =
            ENTITY_TYPES.register("compressed_dragon_breath",
                    () -> EntityType.Builder.<CompressedDragonBreathEntity>of(CompressedDragonBreathEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10)
                            .build("compressed_dragon_breath"));

    /**
     * The second phase: the same class of thing as the first, half again as large, and registered at
     * the same unscaled size for the reason every scaled mob here is - {@code Attributes.SCALE}
     * multiplies the hitbox, the drawing and the shadow together, and stating the size twice would
     * multiply the two.
     */
    public static final Supplier<EntityType<CompressedDragonTier2Entity>> COMPRESSED_DRAGON_TIER_2 =
            ENTITY_TYPES.register("compressed_dragon_tier_2",
                    () -> EntityType.Builder.of(CompressedDragonTier2Entity::new, MobCategory.MONSTER)
                            .sized(16.0F, 8.0F).clientTrackingRange(16).fireImmune()
                            .build("compressed_dragon_tier_2"));

    /**
     * The pet that hatches out of what the fight leaves behind. The same registered size as the
     * bosses, shrunk to a tenth by {@code Attributes.SCALE} - so a rider sits on something about a
     * block and a half across rather than on sixteen blocks of dragon.
     */
    public static final Supplier<EntityType<CompressedDragonPetEntity>> COMPRESSED_DRAGON_PET =
            ENTITY_TYPES.register("compressed_dragon_pet",
                    () -> EntityType.Builder.of(CompressedDragonPetEntity::new, MobCategory.CREATURE)
                            .sized(16.0F, 8.0F).clientTrackingRange(10).fireImmune()
                            .build("compressed_dragon_pet"));

    /**
     * The storm the second phase fights inside, which is a piece of music rather than a creature:
     * MISC, no attributes, no AI, and nothing that can be hit. It is drawn by nothing at all - the
     * bolts are the only thing there is to see.
     */
    public static final Supplier<EntityType<BossMusicWeatherEntity>> BOSS_MUSIC_WEATHER =
            ENTITY_TYPES.register("boss_music_weather",
                    () -> EntityType.Builder.<BossMusicWeatherEntity>of(BossMusicWeatherEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE)
                            .build("boss_music_weather"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
