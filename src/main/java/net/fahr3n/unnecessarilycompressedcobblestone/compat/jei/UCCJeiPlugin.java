package net.fahr3n.unnecessarilycompressedcobblestone.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressorTier;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompressionInscriberScreen;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.MaterialCompressorScreen;
import net.minecraft.resources.ResourceLocation;

/**
 * What JEI is told about this mod.
 * <p>
 * The three machines here are not recipe driven the way a furnace is - the Material Compressor's
 * tiers are {@code CompressorTier} constants, and what the Compression Inscriber and the Engraving
 * Table accept are item tags and predicates. So there is nothing to read out of the
 * {@code RecipeManager}: each category builds its own display list by walking the item registry and
 * asking the mod's own code what fits, which is also why another mod's sword shows up in the
 * Engraving Table category with no work here.
 * <p>
 * This class is only ever loaded when JEI is installed - {@link JeiPlugin} is found by JEI's own
 * scan - so the JEI API stays a compile-time-only dependency and the mod runs fine without it.
 */
@JeiPlugin
public class UCCJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
                new MaterialCompressorCategory(helper),
                new CompressionInscriberCategory(helper),
                new EngravingTableCategory(helper),
                new LaserAugmentationTableCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MaterialCompressorCategory.TYPE, MaterialCompressorCategory.displays());
        registration.addRecipes(CompressionInscriberCategory.TYPE, CompressionInscriberCategory.displays());
        registration.addRecipes(EngravingTableCategory.TYPE, EngravingTableCategory.displays());
        registration.addRecipes(LaserAugmentationTableCategory.TYPE, LaserAugmentationTableCategory.displays());

        // What the vanilla anvil category cannot find on its own - see AnvilRecipes for why.
        registration.addRecipes(RecipeTypes.ANVIL, AnvilRecipes.all(registration.getVanillaRecipeFactory()));

        // Everything with no recipe at all, which JEI would otherwise answer with nothing.
        InfoPages.register(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Off the enum, so a sixth tier is a catalyst the day it exists. Listing the blocks by
        // hand is how tiers 4 and 5 went missing from here.
        for (CompressorTier tier : CompressorTier.values()) {
            registration.addRecipeCatalyst(ModBlocks.compressor(tier).get(), MaterialCompressorCategory.TYPE);
        }
        registration.addRecipeCatalysts(CompressionInscriberCategory.TYPE,
                ModBlocks.COMPRESSION_INSCRIBER.get());
        registration.addRecipeCatalysts(EngravingTableCategory.TYPE,
                ModBlocks.ENGRAVING_TABLE.get());
        registration.addRecipeCatalysts(LaserAugmentationTableCategory.TYPE,
                ModBlocks.LASER_AUGMENTATION_TABLE.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // The progress arrow is the click area, the way the furnace's flame is. The Engraving Table
        // gets none: its two buttons sit across the middle of the panel, and a click area over them
        // would eat the clicks that work the table.
        registration.addRecipeClickArea(MaterialCompressorScreen.class,
                ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT, MaterialCompressorCategory.TYPE);
        registration.addRecipeClickArea(CompressionInscriberScreen.class,
                ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT, CompressionInscriberCategory.TYPE);
    }

    static final int ARROW_X = 76;
    static final int ARROW_Y = 35;
    static final int ARROW_WIDTH = 24;
    static final int ARROW_HEIGHT = 16;

    /** Both machines' slots, which the categories place theirs on top of. */
    static final int INPUT_X = 44;
    static final int OUTPUT_X = 116;
    static final int SLOT_Y = 35;

    /** The panel above the player inventory, which is what a category draws as its background. */
    static final int PANEL_WIDTH = 176;
    static final int PANEL_HEIGHT = 85;
}
