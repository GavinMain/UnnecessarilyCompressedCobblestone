package net.fahr3n.unnecessarilycompressedcobblestone.block;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.DamageWebBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.RegenWebBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.SlipperyIceBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressionInscriberBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompositionTableBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.EngravingTableBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.LaserAugmentationTableBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressorTier;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.HardenedCompressedBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.HardenedCompressedBlockTier2;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.TeleportationGateBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.HardenedLeavesBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.LightningCoreBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.worldgen.ModTreeGrowers;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.MaterialCompressorBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedTntEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UnnecessarilyCompressedCobblestone.MOD_ID);

    /** Highest compression level; each level is nine of the level below it. */
    public static final int MAX_COMPRESSION_LEVEL = 255;

    /**
     * The golem head: carved out of a level 11 block with shears, the way a pumpkin is.
     * Registered outside the level list because it is not a compression level of its own.
     */
    public static final DeferredBlock<CarvedCobblestoneBlock> CARVED_COBBLESTONE_TIER_1 =
            registerBlock("carved_cobblestone_tier_1", () -> new CarvedCobblestoneBlock(compressedProperties(),
                    CarvedCobblestoneBlock.CARVED_FROM_LEVEL, CarvedCobblestoneBlock.GOLEM_BODY_LEVEL,
                    ModEntities.COMPRESSED_GOLEM));

    /** The stone the second golem is built out of, head and body alike. */
    public static final int GOLEM_TIER_2_LEVEL = 115;

    /**
     * The second golem's head, carved out of level 115 with shears the way the first is out of level
     * 11 - and its body is that same level, so the whole thing is four blocks of one stone.
     */
    public static final DeferredBlock<CarvedCobblestoneBlock> CARVED_COBBLESTONE_TIER_2 =
            registerBlock("carved_cobblestone_tier_2", () -> new CarvedCobblestoneBlock(compressedProperties(),
                    GOLEM_TIER_2_LEVEL, GOLEM_TIER_2_LEVEL, ModEntities.COMPRESSED_GOLEM_TIER_2));

    /** The stone the Compressed Ore Golem is built out of, head and body alike. */
    public static final int ORE_GOLEM_LEVEL = 222;

    /**
     * The Ore Golem's head, carved out of level 222 with shears the way the other two are, and its
     * body is that same level - so the whole boss is four blocks of one stone, exactly as the second
     * golem is.
     */
    public static final DeferredBlock<CarvedCobblestoneBlock> CARVED_COBBLESTONE_TIER_3 =
            registerBlock("carved_cobblestone_tier_3", () -> new CarvedCobblestoneBlock(compressedProperties(),
                    ORE_GOLEM_LEVEL, ORE_GOLEM_LEVEL, ModEntities.COMPRESSED_ORE_GOLEM));

    /** Every golem head, so the carving handler and datagen can walk them. */
    public static final List<DeferredBlock<CarvedCobblestoneBlock>> CARVED_HEADS =
            List.of(CARVED_COBBLESTONE_TIER_1, CARVED_COBBLESTONE_TIER_2, CARVED_COBBLESTONE_TIER_3);

    public static final DeferredBlock<CompressedTntBlock> COMPRESSED_TNT =
            registerTnt("compressed_tnt", CompressedTntEffect.BLAST_5X);

    public static final DeferredBlock<CompressedTntBlock> SUPER_COMPRESSED_TNT =
            registerTnt("super_compressed_tnt", CompressedTntEffect.BLAST_20X);

    /** Breaks nothing; throws a hundred flowers and dyes into the air. */
    public static final DeferredBlock<CompressedTntBlock> FLOWER_TNT =
            registerTnt("flower_tnt", CompressedTntEffect.FLOWERS);

    /** Turns on the rain and beaches a shoal of guardians in it. */
    public static final DeferredBlock<CompressedTntBlock> GUARDIAN_TNT =
            registerTnt("guardian_tnt", CompressedTntEffect.GUARDIANS);

    /** Packs its whole chunk solid with level 29 stone, bedrock to sky. */
    public static final DeferredBlock<CompressedTntBlock> CHUNK_TNT =
            registerTnt("chunk_tnt", CompressedTntEffect.CHUNK_FILL);

    /** Scatters glass across a wide, shallow disc, thinning towards the rim. */
    public static final DeferredBlock<CompressedTntBlock> GLASS_TNT =
            registerTnt("glass_tnt", CompressedTntEffect.GLASS_SCATTER);

    /** A flock of Compressed Cobblestone Chickens. */
    public static final DeferredBlock<CompressedTntBlock> CHICKEN_TNT =
            registerTnt("chicken_tnt", CompressedTntEffect.CHICKENS);

    /** A hundred iron golems and twenty compressed ones, all put down on the surface. */
    public static final DeferredBlock<CompressedTntBlock> GOLEM_TNT =
            registerTnt("golem_tnt", CompressedTntEffect.GOLEMS);

    /** Throws a hundred apples of every kind the game has into the air. */
    public static final DeferredBlock<CompressedTntBlock> APPLE_TNT =
            registerTnt("apple_tnt", CompressedTntEffect.APPLES);

    /** Goes off as some other TNT, drawn at random from {@code #unnecessarilycompressedcobblestone:tnt}. */
    public static final DeferredBlock<CompressedTntBlock> RANDOM_TNT =
            registerTnt("random_tnt", CompressedTntEffect.RANDOM);

    /** Trident Enchant TNT. */
    public static final DeferredBlock<CompressedTntBlock> TRIDENT_ENCHANT_TNT =
            registerTnt("trident_enchant_tnt", CompressedTntEffect.TRIDENT_ENCHANT);

    /** Binding TNT. */
    public static final DeferredBlock<CompressedTntBlock> BINDING_TNT =
            registerTnt("binding_tnt", CompressedTntEffect.BINDING);

    /** Glass TNT Tier 2. */
    public static final DeferredBlock<CompressedTntBlock> GLASS_TNT_TIER_2 =
            registerTnt("glass_tnt_tier_2", CompressedTntEffect.GLASS_SCATTER_2);

    /** Anvil TNT. */
    public static final DeferredBlock<CompressedTntBlock> ANVIL_TNT =
            registerTnt("anvil_tnt", CompressedTntEffect.ANVIL_RAIN);

    /** Levitation TNT. */
    public static final DeferredBlock<CompressedTntBlock> LEVITATION_TNT =
            registerTnt("levitation_tnt", CompressedTntEffect.LEVITATION);

    /** Chicken TNT Tier 2. */
    public static final DeferredBlock<CompressedTntBlock> CHICKEN_TNT_TIER_2 =
            registerTnt("chicken_tnt_tier_2", CompressedTntEffect.CHICKENS_2);

    /** Lays down soil and grows a grove of compressed cobblestone trees on it. */
    public static final DeferredBlock<CompressedTntBlock> TREENT_TNT =
            registerTnt("treent_tnt", CompressedTntEffect.TREENT);

    /** Arrow TNT. */
    public static final DeferredBlock<CompressedTntBlock> ARROW_TNT =
            registerTnt("arrow_tnt", CompressedTntEffect.ARROW_BURST);

    /** Breeding TNT. */
    public static final DeferredBlock<CompressedTntBlock> BREEDING_TNT =
            registerTnt("breeding_tnt", CompressedTntEffect.BREEDING);

    /** Effect TNT. */
    public static final DeferredBlock<CompressedTntBlock> EFFECT_TNT =
            registerTnt("effect_tnt", CompressedTntEffect.EFFECTS);

    /** Arrow Spiral TNT. */
    public static final DeferredBlock<CompressedTntBlock> ARROW_SPIRAL_TNT =
            registerTnt("arrow_spiral_tnt", CompressedTntEffect.ARROW_SPIRAL);

    /** Flash TNT. */
    public static final DeferredBlock<CompressedTntBlock> FLASH_TNT =
            registerTnt("flash_tnt", CompressedTntEffect.FLASH);

    /** Plays a song in lightning, and goes on playing it whether or not anyone is left to hear. */
    public static final DeferredBlock<CompressedTntBlock> LIGHTNING_SONG_TNT =
            registerTnt("lightning_song_tnt", CompressedTntEffect.LIGHTNING_SONG);

    /** Thirty seconds of wind bursts, which knock whatever is caught in them from one to the next. */
    public static final DeferredBlock<CompressedTntBlock> PINBALL_TNT =
            registerTnt("pinball_tnt", CompressedTntEffect.PINBALL);

    /** Breaks nothing; throws two dozen bolts drawn at random from every bolt there is. */
    public static final DeferredBlock<CompressedTntBlock> BOLT_TNT =
            registerTnt("bolt_tnt", CompressedTntEffect.BOLT_DROP);

    /** The opening of the Moonlight Sonata, played in lightning. */
    public static final DeferredBlock<CompressedTntBlock> MOONLIGHT_TNT =
            registerTnt("moonlight_tnt", CompressedTntEffect.MOONLIGHT);

    /** The Lightning TNT's song again, played on the note bolts rather than on one note. */
    public static final DeferredBlock<CompressedTntBlock> LIGHTNING_SONG_TNT_2 =
            registerTnt("lightning_song_tnt_2", CompressedTntEffect.LIGHTNING_SONG_2);

    /** Digs a bowl three blocks across and fills it with water. */
    public static final DeferredBlock<CompressedTntBlock> POOL_TNT =
            registerTnt("pool_tnt", CompressedTntEffect.POOL);

    /** Speed X and an extra block of step height on everything nearby; breaks nothing. */
    public static final DeferredBlock<CompressedTntBlock> ZOOM_TNT =
            registerTnt("zoom_tnt", CompressedTntEffect.ZOOM);

    /** Half a minute of cobwebs scattered thinly out to the edge of what anyone can see. */
    public static final DeferredBlock<CompressedTntBlock> WEB_TNT =
            registerTnt("web_tnt", CompressedTntEffect.WEBS);

    /** A nether portal, and a barrage of wither skulls fired straight into it. */
    public static final DeferredBlock<CompressedTntBlock> PORTAL_TNT =
            registerTnt("portal_tnt", CompressedTntEffect.PORTAL);

    /** A spawner and a fistful of vanilla spawn eggs, thrown into the air. */
    public static final DeferredBlock<CompressedTntBlock> SPAWNER_TNT =
            registerTnt("spawner_tnt", CompressedTntEffect.SPAWNER);

    /** Deep snow across the ground, and the cold that goes with it on everything standing in it. */
    public static final DeferredBlock<CompressedTntBlock> SNOW_TNT =
            registerTnt("snow_tnt", CompressedTntEffect.SNOW);

    /** A Sierpinski carpet of ancient debris overhead, on a floor turned to Slippery Ice. */
    public static final DeferredBlock<CompressedTntBlock> DEBRIS_TNT =
            registerTnt("debris_tnt", CompressedTntEffect.DEBRIS);

    /** A nest of ordinary silverfish. */
    public static final DeferredBlock<CompressedTntBlock> SILVERFISH_TNT =
            registerTnt("silverfish_tnt", CompressedTntEffect.SILVERFISH);

    /** The same nest of the ones that fly, bite through armour and breed out of whatever they bit. */
    public static final DeferredBlock<CompressedTntBlock> SILVERFISH_TNT_TIER_2 =
            registerTnt("silverfish_tnt_tier_2", CompressedTntEffect.SILVERFISH_2);

    /** A spout of water one block wide, climbing forty blocks out of the ground. */
    public static final DeferredBlock<CompressedTntBlock> GEYSER_TNT =
            registerTnt("geyser_tnt", CompressedTntEffect.GEYSER);

    /** The same spout in lava, shorter because lava stands far longer than water does. */
    public static final DeferredBlock<CompressedTntBlock> GEYSER_TNT_TIER_2 =
            registerTnt("geyser_tnt_tier_2", CompressedTntEffect.GEYSER_2);

    /** A basin of water with a coral reef grown in it. */
    public static final DeferredBlock<CompressedTntBlock> CORAL_TNT =
            registerTnt("coral_tnt", CompressedTntEffect.CORAL);

    /** The flock again, ten times over, under half a minute of the Pinball TNT's wind. */
    public static final DeferredBlock<CompressedTntBlock> CHICKEN_TNT_TIER_3 =
            registerTnt("chicken_tnt_tier_3", CompressedTntEffect.CHICKENS_3);

    /** The arrow burst again, with a random effect on every arrow in it. */
    public static final DeferredBlock<CompressedTntBlock> ARROW_TNT_TIER_2 =
            registerTnt("arrow_tnt_tier_2", CompressedTntEffect.ARROW_BURST_2);

    /** Ten seconds of a weak black hole: everything loose nearby, and the ground itself, pulled in. */
    public static final DeferredBlock<CompressedTntBlock> SUCC_TNT =
            registerTnt("succ_tnt", CompressedTntEffect.SUCC);

    /** Builds a vanilla village of whatever kind the biome asks for. */
    public static final DeferredBlock<CompressedTntBlock> VILLAGE_TNT =
            registerTnt("village_tnt", CompressedTntEffect.VILLAGE);

    /** Drops a woodland mansion, its illagers included. */
    public static final DeferredBlock<CompressedTntBlock> MANSION_TNT =
            registerTnt("mansion_tnt", CompressedTntEffect.MANSION);

    /** Drops an ocean monument, wherever it went off - water or not. */
    public static final DeferredBlock<CompressedTntBlock> PYRAMID_TNT =
            registerTnt("pyramid_tnt", CompressedTntEffect.MONUMENT);

    /** Raises full-height pillars of level 213 stone through the chunks around it. */
    public static final DeferredBlock<CompressedTntBlock> PILLAR_TNT_TIER_2 =
            registerTnt("pillar_tnt_tier_2", CompressedTntEffect.PILLARS_2);

    /** Every TNT in the mod, so datagen can walk them instead of listing them again. */
    /** Throws a swarm of aggressive, corroding bees into the air. */
    public static final DeferredBlock<CompressedTntBlock> BEE_NT =
            registerTnt("bee_nt", CompressedTntEffect.BEES);

    /**
     * Levels a thirty block disc: a floor of tier 229 stone laid across it and everything above
     * that floor taken away, all the way to the world's ceiling.
     */
    public static final DeferredBlock<CompressedTntBlock> FLAT_TNT =
            registerTnt("flat_tnt", CompressedTntEffect.FLATTEN);

    /** Fills a three block sphere with Damage Webs. A trap rather than a landscape. */
    public static final DeferredBlock<CompressedTntBlock> DAMAGE_WEB_TNT =
            registerTnt("damage_web_tnt", CompressedTntEffect.DAMAGE_WEBS);

    /** One beam out of a Ray of Laser carrying every augment there is, at whatever is nearest. */
    public static final DeferredBlock<CompressedTntBlock> LASER_TNT =
            registerTnt("laser_tnt", CompressedTntEffect.LASER);

    /** Builds a dome of tier 241 stone, ring by ring, around wherever it went off. */
    public static final DeferredBlock<CompressedTntBlock> DOME_TNT =
            registerTnt("dome_tnt", CompressedTntEffect.DOME);

    /** Leaves a two-hour black hole that only the Black Hole Stopper can end. */
    public static final DeferredBlock<CompressedTntBlock> BLACKHOLE_TNT =
            registerTnt("blackhole_tnt", CompressedTntEffect.BLACKHOLE);

    /** The black hole in one minute, then a flash, then one blast of ten thousand TNT. */
    public static final DeferredBlock<CompressedTntBlock> SINGULARITY_TNT =
            registerTnt("singularity_tnt", CompressedTntEffect.SINGULARITY);

    public static final List<DeferredBlock<CompressedTntBlock>> TNTS = List.of(
            COMPRESSED_TNT, SUPER_COMPRESSED_TNT, FLOWER_TNT, GUARDIAN_TNT,
            CHUNK_TNT, GLASS_TNT, CHICKEN_TNT, GOLEM_TNT, APPLE_TNT, RANDOM_TNT,
            TRIDENT_ENCHANT_TNT, BINDING_TNT, GLASS_TNT_TIER_2, ANVIL_TNT, LEVITATION_TNT, CHICKEN_TNT_TIER_2,
            LIGHTNING_SONG_TNT,
            TREENT_TNT,
            ARROW_TNT, BREEDING_TNT, EFFECT_TNT, ARROW_SPIRAL_TNT, FLASH_TNT,
            PINBALL_TNT, BOLT_TNT, MOONLIGHT_TNT, LIGHTNING_SONG_TNT_2, POOL_TNT,
            ZOOM_TNT, WEB_TNT, CHICKEN_TNT_TIER_3,
            PORTAL_TNT, SPAWNER_TNT, SNOW_TNT, DEBRIS_TNT,
            SILVERFISH_TNT, SILVERFISH_TNT_TIER_2,
            GEYSER_TNT, GEYSER_TNT_TIER_2, CORAL_TNT,
            ARROW_TNT_TIER_2, SUCC_TNT,
            VILLAGE_TNT, MANSION_TNT, PYRAMID_TNT, PILLAR_TNT_TIER_2,
            BEE_NT,
            FLAT_TNT, DAMAGE_WEB_TNT, LASER_TNT, DOME_TNT, BLACKHOLE_TNT, SINGULARITY_TNT);

    /** Turns blocks of coal into deeply compressed cobblestone. */
    public static final DeferredBlock<MaterialCompressorBlock> MATERIAL_COMPRESSOR_TIER_1 =
            registerBlock(CompressorTier.TIER_1.blockName(),
                    () -> new MaterialCompressorBlock(compressedProperties(), CompressorTier.TIER_1));

    /** Blocks of iron into level 50 stone. */
    public static final DeferredBlock<MaterialCompressorBlock> MATERIAL_COMPRESSOR_TIER_2 =
            registerBlock(CompressorTier.TIER_2.blockName(),
                    () -> new MaterialCompressorBlock(compressedProperties(), CompressorTier.TIER_2));

    /** Blocks of gold into level 90 stone. */
    public static final DeferredBlock<MaterialCompressorBlock> MATERIAL_COMPRESSOR_TIER_3 =
            registerBlock(CompressorTier.TIER_3.blockName(),
                    () -> new MaterialCompressorBlock(compressedProperties(), CompressorTier.TIER_3));

    /** Diamonds into level 123 stone. */
    public static final DeferredBlock<MaterialCompressorBlock> MATERIAL_COMPRESSOR_TIER_4 =
            registerBlock(CompressorTier.TIER_4.blockName(),
                    () -> new MaterialCompressorBlock(compressedProperties(), CompressorTier.TIER_4));

    /** Netherite ingots into level 187 stone, the deepest any machine makes. */
    public static final DeferredBlock<MaterialCompressorBlock> MATERIAL_COMPRESSOR_TIER_5 =
            registerBlock(CompressorTier.TIER_5.blockName(),
                    () -> new MaterialCompressorBlock(compressedProperties(), CompressorTier.TIER_5));

    /** The block that runs a given tier, so anything looping over the enum can reach its block. */
    public static DeferredBlock<MaterialCompressorBlock> compressor(CompressorTier tier) {
        return switch (tier) {
            case TIER_1 -> MATERIAL_COMPRESSOR_TIER_1;
            case TIER_2 -> MATERIAL_COMPRESSOR_TIER_2;
            case TIER_3 -> MATERIAL_COMPRESSOR_TIER_3;
            case TIER_4 -> MATERIAL_COMPRESSOR_TIER_4;
            case TIER_5 -> MATERIAL_COMPRESSOR_TIER_5;
        };
    }

    /**
     * The leaves of a compressed cobblestone tree: unbreakable like the stone they grow on, and
     * cleared either with the pickaxe or by waiting for them to decay. What they drop is in
     * {@code ModBlockLootTableProvider}, and decay drops it too.
     */
    public static final DeferredBlock<HardenedLeavesBlock> COMPRESSED_COBBLESTONE_LEAVES =
            registerBlock("compressed_cobblestone_leaves",
                    () -> new HardenedLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                            .strength(-1.0F, 3600000.0F)));

    /** Plant it on dirt and it grows the same tree worldgen puts down. */
    public static final DeferredBlock<SaplingBlock> COMPRESSED_COBBLESTONE_SAPLING =
            registerBlock("compressed_cobblestone_sapling",
                    () -> new SaplingBlock(ModTreeGrowers.COMPRESSED_COBBLESTONE,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    /**
     * A cobweb that heals whatever it holds. It is a real web - it catches and slows exactly as
     * vanilla's does - and puts Instant Health on anything caught in it every tick, which the undead
     * feel the other way round. Vanilla's cobweb needs shears for its drop; this one does not, so a
     * block cut from the deepest stone in the mod is never lost to breaking it with the wrong thing.
     */
    public static final DeferredBlock<RegenWebBlock> REGEN_WEB = registerBlock("regen_web",
            () -> new RegenWebBlock(webProperties()));

    /**
     * The same web, the other way round: it holds whatever walks into it and puts Instant Damage II
     * on it every tick it is held, which the undead feel as healing. See {@link DamageWebBlock}.
     */
    public static final DeferredBlock<DamageWebBlock> DAMAGE_WEB = registerBlock("damage_web",
            () -> new DamageWebBlock(webProperties()));

    /**
     * The dev block: a compressed cobblestone block that deals 999 points of true damage every time
     * a player hits something with it. Creative-only, with no recipe, and an ordinary block in every
     * other respect - it places, breaks and drops itself like the stone it looks like.
     * <p>
     * All of the behaviour is on the <em>item</em> and none of it is here, because a block that is
     * being swung is not in the world at all. It lives in {@code ModEvents.onDevBlockAttack}, which
     * catches the swing itself rather than the weapon's {@code hurtEnemy}: that hook only fires when
     * the punch underneath it landed, and a punch every five ticks is swallowed by the
     * twenty-tick invulnerability window, so half the hits would be worth nothing. See
     * {@code ModDamageTypes.DEV_STRIKE} for what makes the 999 true.
     */
    public static final DeferredBlock<Block> DEV_COMPRESSED_COBBLESTONE =
            registerBlock("dev_compressed_cobblestone", () -> new Block(compressedProperties()));

    /**
     * Ice with three times blue ice's slide on it. Vanilla's ice properties minus the melting, plus
     * {@link SlipperyIceBlock#FRICTION}; the Debris TNT lays a field of it under everything it drops.
     */
    public static final DeferredBlock<SlipperyIceBlock> SLIPPERY_ICE = registerBlock("slippery_ice",
            () -> new SlipperyIceBlock(slipperyIceProperties()));

    /**
     * A pedestal for lightning: load it with a bolt, give it a redstone signal, and it calls that
     * bolt down on itself once a second for as long as the signal lasts. Empty, it does nothing.
     */
    public static final DeferredBlock<LightningCoreBlock> LIGHTNING_CORE =
            registerBlock("lightning_core", () -> new LightningCoreBlock(compressedProperties()));

    /** The table that moves Compression Energy out of cobblestone and into gear. */
    public static final DeferredBlock<CompressionInscriberBlock> COMPRESSION_INSCRIBER =
            registerBlock("compression_inscriber", () -> new CompressionInscriberBlock(compressedProperties()));

    /**
     * The table that fits augments to a Ray of Laser, and takes them back off again. It is the
     * Engraving Table's opposite number for the one weapon the engravings have nothing to say about.
     */
    public static final DeferredBlock<LaserAugmentationTableBlock> LASER_AUGMENTATION_TABLE =
            registerBlock("laser_augmentation_table",
                    () -> new LaserAugmentationTableBlock(compressedProperties()));

    /** The table a song is written at, and written onto a Composition Bolt. */
    public static final DeferredBlock<CompositionTableBlock> COMPOSITION_TABLE =
            registerBlock("composition_table", () -> new CompositionTableBlock(compressedProperties()));

    /**
     * A doorway two blocks tall that sends whatever walks into it to the gate it names. It is not a
     * full block, so it is built on the compressed stone properties with the occlusion dropped and a
     * light of its own; the two halves and everything else about it are
     * {@link TeleportationGateBlock}.
     */
    public static final DeferredBlock<TeleportationGateBlock> TELEPORTATION_GATE =
            registerBlock("teleportation_gate", () -> new TeleportationGateBlock(compressedProperties()
                    .noOcclusion()
                    .lightLevel(state -> 11)));

    /** The table that fits engravings to gear, and takes them back off again. */
    public static final DeferredBlock<EngravingTableBlock> ENGRAVING_TABLE =
            registerBlock("engraving_table", () -> new EngravingTableBlock(compressedProperties()));

    /**
     * Every compression level in order, so datagen and the creative tab can walk them. That is all
     * this is public for - reach a single level through {@link #byLevel(int)} rather than indexing
     * here, because the list is zero-based while the levels are one-based.
     */
    public static final List<DeferredBlock<Block>> COMPRESSED_COBBLESTONE_LEVELS = registerLevels();

    /**
     * The first compression level that ordinary tools cannot touch. From here up the stone is
     * unbreakable to everything except a tool in {@code #hardened_mining}, and no explosion in the
     * game will move it.
     */
    public static final int HARDENED_LEVEL = 57;

    /**
     * The second floor. From here up the stone is closed even to the pickaxe that opens every level
     * between {@link #HARDENED_LEVEL} and this one, and the only way through is a tool in
     * {@code #hardened_mining_tier_2} - which today is the Compressed Cobblestone Pickaxe Tier 2
     * and nothing else in the game.
     */
    public static final int HARDENED_LEVEL_TIER_2 = 207;

    /**
     * Every level is the same block - only the name and the texture differ - so all 255 are
     * registered in one loop rather than as a field each. The only thing that changes with depth is
     * which class a level gets: {@link HardenedCompressedBlock} from {@link #HARDENED_LEVEL} up,
     * which is what makes it unbreakable by anything but the pickaxe, and
     * {@link HardenedCompressedBlockTier2} from {@link #HARDENED_LEVEL_TIER_2} up, which closes it
     * again to everything but the tier 2 pickaxe.
     */
    private static List<DeferredBlock<Block>> registerLevels() {
        List<DeferredBlock<Block>> levels = new ArrayList<>(MAX_COMPRESSION_LEVEL);

        for (int level = 1; level <= MAX_COMPRESSION_LEVEL; level++) {
            if (level < HARDENED_LEVEL) {
                levels.add(registerBlock(levelName(level), () -> new Block(compressedProperties())));
            } else if (level < HARDENED_LEVEL_TIER_2) {
                levels.add(registerBlock(levelName(level), () -> new HardenedCompressedBlock(hardenedProperties())));
            } else {
                levels.add(registerBlock(levelName(level),
                        () -> new HardenedCompressedBlockTier2(hardenedProperties())));
            }
        }

        return List.copyOf(levels);
    }

    /**
     * Level 1 is plain {@code compressed_cobblestone} and every level above it carries its number.
     * These ids are what the block textures, the lang file, the recipes and any existing world are
     * keyed on, so the unnumbered first level has to stay unnumbered.
     */
    private static String levelName(int level) {
        return level == 1 ? "compressed_cobblestone" : "compressed_cobblestone_" + level;
    }

    /**
     * The compression level {@code block} is, or null if it is not compressed cobblestone at all.
     * <p>
     * The inverse of {@link #byLevel}, and the one thing that lets code ask "how deep is this?"
     * about a block it is holding - which is what the Exploding Sword engraving needs to decide
     * whether a blast reaches it. Built on first use rather than at class init, because the blocks
     * are still registering while this class is being loaded and resolving one here would be a
     * registry asked for something it has not finished making.
     */
    @Nullable
    public static Integer levelOf(Block block) {
        if (LEVELS_BY_BLOCK == null) {
            Map<Block, Integer> built = new IdentityHashMap<>(MAX_COMPRESSION_LEVEL);
            for (int level = 1; level <= MAX_COMPRESSION_LEVEL; level++) {
                built.put(byLevel(level).get(), level);
            }

            LEVELS_BY_BLOCK = built;
        }

        return LEVELS_BY_BLOCK.get(block);
    }

    @Nullable
    private static Map<Block, Integer> LEVELS_BY_BLOCK;

    /** @param level 1 through {@link #MAX_COMPRESSION_LEVEL} */
    public static DeferredBlock<Block> byLevel(int level) {
        // The levels are one-based and the list is zero-based, so level 1 is the block at index 0.
        // This is the only place that conversion happens, which is why the list is not indexed
        // directly anywhere else.
        return COMPRESSED_COBBLESTONE_LEVELS.get(level - 1);
    }

    /**
     * Vanilla cobweb's own properties with one line left out. Copying the block wholesale would
     * bring {@code requiresCorrectToolForDrops} with it, and vanilla answers that with a loot table
     * conditioned on shears or a sword; this block drops itself to anything, so the flag has to go
     * rather than the loot table having to grow a condition.
     */
    private static BlockBehaviour.Properties webProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL)
                .sound(SoundType.COBWEB)
                .forceSolidOn()
                .noCollission()
                .strength(4.0F)
                .pushReaction(PushReaction.DESTROY);
    }

    /**
     * Vanilla ice's properties with the melting left out and the friction turned up. {@code
     * randomTicks} is what drives ice's melting, so dropping it is the whole of that change; the
     * spawn, conductor and view-blocking rules are ice's own, since a block that looks like ice
     * should behave like it for everything that is not sliding on it.
     */
    private static BlockBehaviour.Properties slipperyIceProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .friction(SlipperyIceBlock.FRICTION)
                .strength(0.5F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> type == EntityType.POLAR_BEAR)
                .isRedstoneConductor((state, level, pos) -> false);
    }

    /** Everything vanilla TNT is - instantly broken, lit by lava, not a redstone conductor. */
    private static DeferredBlock<CompressedTntBlock> registerTnt(String name, CompressedTntEffect effect) {
        return registerBlock(name, () -> new CompressedTntBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.TNT), effect));
    }

    private static BlockBehaviour.Properties compressedProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).strength(2.0F, 8.0F);
    }

    /**
     * Bedrock's own numbers: a hardness of -1, which is what every tool and every piece of code in
     * the game reads as "cannot be broken", and an explosion resistance nothing reaches. The one way
     * back through is {@link HardenedCompressedBlock#getDestroyProgress}.
     */
    private static BlockBehaviour.Properties hardenedProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).strength(-1.0F, 3600000.0F);
    }

    /** Registers the block and its matching BlockItem in one go. */
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
