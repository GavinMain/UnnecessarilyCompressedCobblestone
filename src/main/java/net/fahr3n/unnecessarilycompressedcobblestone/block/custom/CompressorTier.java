package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * What one tier of the Material Compressor eats and what it turns it into. Every tier is a constant
 * here rather than a class of its own: the block, the block entity, the menu and the screen are all
 * shared, and a new tier is a constant, a block registration, a recipe and five textures.
 * <p>
 * Each tier's output is the same compression level its own recipe is built out of, so a machine
 * always produces exactly the stone it was made from.
 */
public enum CompressorTier {
    /** A block of coal into a level 16 block. */
    TIER_1(Items.COAL_BLOCK, 16),

    /** A block of iron into a level 50 block. */
    TIER_2(Items.IRON_BLOCK, 50),

    /** A block of gold into a level 90 block. */
    TIER_3(Items.GOLD_BLOCK, 90),

    /** A diamond - the stone, not a block of them - into a level 123 block. */
    TIER_4(Items.DIAMOND, 123),

    /**
     * A netherite ingot into a level 187 block. Like the diamond above it this eats a bare item
     * rather than a block of them, which is the whole of what makes an ingot worth feeding it - and
     * it is the deepest stone any machine makes, which is why it is built around the Compressed
     * Guardian's heart and so waits on that fight.
     */
    TIER_5(Items.NETHERITE_INGOT, 187);

    private final Item input;
    private final int outputLevel;

    CompressorTier(Item input, int outputLevel) {
        this.input = input;
        this.outputLevel = outputLevel;
    }

    /** The only item this tier will accept. */
    public Item input() {
        return this.input;
    }

    /** The compression level it produces. */
    public int outputLevel() {
        return this.outputLevel;
    }

    /** The block id and lang key this tier is registered under. */
    public String blockName() {
        return "material_compressor_" + name().toLowerCase(java.util.Locale.ROOT);
    }
}
