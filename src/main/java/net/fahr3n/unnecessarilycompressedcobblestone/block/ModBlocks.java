package net.fahr3n.unnecessarilycompressedcobblestone.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressionInscriberBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.EngravingTableBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressorTier;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.HardenedCompressedBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.HardenedLeavesBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.LightningCoreBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.worldgen.ModTreeGrowers;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.MaterialCompressorBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedTntEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
            registerBlock("carved_cobblestone_tier_1", () -> new CarvedCobblestoneBlock(compressedProperties()));

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

    /** Every TNT in the mod, so datagen can walk them instead of listing them again. */
    public static final List<DeferredBlock<CompressedTntBlock>> TNTS = List.of(
            COMPRESSED_TNT, SUPER_COMPRESSED_TNT, FLOWER_TNT, GUARDIAN_TNT,
            CHUNK_TNT, GLASS_TNT, CHICKEN_TNT, GOLEM_TNT, APPLE_TNT, RANDOM_TNT,
            TRIDENT_ENCHANT_TNT, BINDING_TNT, GLASS_TNT_TIER_2, ANVIL_TNT, LEVITATION_TNT, CHICKEN_TNT_TIER_2,
            LIGHTNING_SONG_TNT,
            TREENT_TNT,
            ARROW_TNT, BREEDING_TNT, EFFECT_TNT, ARROW_SPIRAL_TNT, FLASH_TNT);

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

    /** The block that runs a given tier, so anything looping over the enum can reach its block. */
    public static DeferredBlock<MaterialCompressorBlock> compressor(CompressorTier tier) {
        return switch (tier) {
            case TIER_1 -> MATERIAL_COMPRESSOR_TIER_1;
            case TIER_2 -> MATERIAL_COMPRESSOR_TIER_2;
            case TIER_3 -> MATERIAL_COMPRESSOR_TIER_3;
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
     * A pedestal for lightning: load it with a bolt, give it a redstone signal, and it calls that
     * bolt down on itself once a second for as long as the signal lasts. Empty, it does nothing.
     */
    public static final DeferredBlock<LightningCoreBlock> LIGHTNING_CORE =
            registerBlock("lightning_core", () -> new LightningCoreBlock(compressedProperties()));

    /** The table that moves Compression Energy out of cobblestone and into gear. */
    public static final DeferredBlock<CompressionInscriberBlock> COMPRESSION_INSCRIBER =
            registerBlock("compression_inscriber", () -> new CompressionInscriberBlock(compressedProperties()));

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
     * Every level is the same block - only the name and the texture differ - so all 255 are
     * registered in one loop rather than as a field each. The only thing that changes with depth is
     * that {@link #HARDENED_LEVEL} and above are {@link HardenedCompressedBlock}, which is what makes
     * them unbreakable by anything but the pickaxe.
     */
    private static List<DeferredBlock<Block>> registerLevels() {
        List<DeferredBlock<Block>> levels = new ArrayList<>(MAX_COMPRESSION_LEVEL);

        for (int level = 1; level <= MAX_COMPRESSION_LEVEL; level++) {
            levels.add(level < HARDENED_LEVEL
                    ? registerBlock(levelName(level), () -> new Block(compressedProperties()))
                    : registerBlock(levelName(level), () -> new HardenedCompressedBlock(hardenedProperties())));
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

    /** @param level 1 through {@link #MAX_COMPRESSION_LEVEL} */
    public static DeferredBlock<Block> byLevel(int level) {
        // The levels are one-based and the list is zero-based, so level 1 is the block at index 0.
        // This is the only place that conversion happens, which is why the list is not indexed
        // directly anywhere else.
        return COMPRESSED_COBBLESTONE_LEVELS.get(level - 1);
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
