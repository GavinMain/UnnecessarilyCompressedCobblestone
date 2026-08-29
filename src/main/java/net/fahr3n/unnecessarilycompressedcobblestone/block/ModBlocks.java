package net.fahr3n.unnecessarilycompressedcobblestone.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressionInscriberBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.MaterialCompressorBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedTntEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    /** Every TNT in the mod, so datagen can walk them instead of listing them again. */
    public static final List<DeferredBlock<CompressedTntBlock>> TNTS = List.of(
            COMPRESSED_TNT, SUPER_COMPRESSED_TNT, FLOWER_TNT, GUARDIAN_TNT,
            CHUNK_TNT, GLASS_TNT, CHICKEN_TNT, GOLEM_TNT);

    /** Turns blocks of coal into deeply compressed cobblestone. */
    public static final DeferredBlock<MaterialCompressorBlock> MATERIAL_COMPRESSOR_TIER_1 =
            registerBlock("material_compressor_tier_1", () -> new MaterialCompressorBlock(compressedProperties()));

    /** The table that moves Compression Energy out of cobblestone and into gear. */
    public static final DeferredBlock<CompressionInscriberBlock> COMPRESSION_INSCRIBER =
            registerBlock("compression_inscriber", () -> new CompressionInscriberBlock(compressedProperties()));

    /**
     * Every compression level in order, so datagen and the creative tab can walk them. That is all
     * this is public for - reach a single level through {@link #byLevel(int)} rather than indexing
     * here, because the list is zero-based while the levels are one-based.
     */
    public static final List<DeferredBlock<Block>> COMPRESSED_COBBLESTONE_LEVELS = registerLevels();

    /**
     * Every level is the same plain block - only the name and the texture differ - so all 255 are
     * registered in one loop rather than as a field each.
     */
    private static List<DeferredBlock<Block>> registerLevels() {
        List<DeferredBlock<Block>> levels = new ArrayList<>(MAX_COMPRESSION_LEVEL);

        for (int level = 1; level <= MAX_COMPRESSION_LEVEL; level++) {
            levels.add(registerBlock(levelName(level), () -> new Block(compressedProperties())));
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
