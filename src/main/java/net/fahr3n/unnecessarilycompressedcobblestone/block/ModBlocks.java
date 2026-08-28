package net.fahr3n.unnecessarilycompressedcobblestone.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressionInscriberBlock;
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

    public static final DeferredBlock<Block> COMPRESSED_COBBLESTONE = registerBlock("compressed_cobblestone",
            () -> new Block(compressedProperties()));

    public static final DeferredBlock<Block> DOUBLE_COMPRESSED_COBBLESTONE = registerBlock("compressed_cobblestone_2",
            () -> new Block(compressedProperties()));

    /**
     * The golem head: carved out of a level 11 block with shears, the way a pumpkin is.
     * Registered outside the level list because it is not a compression level of its own.
     */
    public static final DeferredBlock<CarvedCobblestoneBlock> CARVED_COBBLESTONE_TIER_1 =
            registerBlock("carved_cobblestone_tier_1", () -> new CarvedCobblestoneBlock(compressedProperties()));

    /** The table that moves Compression Energy out of cobblestone and into gear. */
    public static final DeferredBlock<CompressionInscriberBlock> COMPRESSION_INSCRIBER =
            registerBlock("compression_inscriber", () -> new CompressionInscriberBlock(compressedProperties()));

    /**
     * Every compression level in order, so datagen can loop over them.
     * Index 0 is level 1 ({@link #COMPRESSED_COBBLESTONE}), index 254 is level 255.
     * Use {@link #byLevel(int)} rather than indexing this directly.
     */
    public static final List<DeferredBlock<Block>> COMPRESSED_COBBLESTONE_LEVELS = registerRemainingLevels();

    /** Levels 3 and up are all identical, so they are registered in a loop instead of one field each. */
    private static List<DeferredBlock<Block>> registerRemainingLevels() {
        List<DeferredBlock<Block>> levels = new ArrayList<>(MAX_COMPRESSION_LEVEL);
        levels.add(COMPRESSED_COBBLESTONE);
        levels.add(DOUBLE_COMPRESSED_COBBLESTONE);

        for (int level = 3; level <= MAX_COMPRESSION_LEVEL; level++) {
            levels.add(registerBlock("compressed_cobblestone_" + level, () -> new Block(compressedProperties())));
        }

        return List.copyOf(levels);
    }

    /** @param level 1 through {@link #MAX_COMPRESSION_LEVEL} */
    public static DeferredBlock<Block> byLevel(int level) {
        return COMPRESSED_COBBLESTONE_LEVELS.get(level - 1);
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
