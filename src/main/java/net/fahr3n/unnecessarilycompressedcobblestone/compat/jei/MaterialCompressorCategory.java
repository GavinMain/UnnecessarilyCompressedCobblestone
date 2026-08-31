package net.fahr3n.unnecessarilycompressedcobblestone.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressorTier;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.MaterialCompressorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One entry per {@link CompressorTier}: what that tier eats and the compression level it makes.
 * <p>
 * The tiers are constants rather than datapack recipes, so the display list is read straight off the
 * enum - a new tier appears here as soon as it is added there, with nothing to change in this file.
 */
public class MaterialCompressorCategory implements IRecipeCategory<MaterialCompressorCategory.Display> {
    /** A tier's input and its output, which is all a player needs off the machine. */
    public record Display(CompressorTier tier, ItemStack input, ItemStack output) {
    }

    public static final RecipeType<Display> TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID,
                    "material_compressor"),
            Display.class);

    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID,
            "textures/gui/material_compressor_tier_1/material_compressor_tier_1_gui.png");
    private static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/gui/arrow_progress.png");

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public MaterialCompressorCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(PANEL, 0, 0,
                UCCJeiPlugin.PANEL_WIDTH, UCCJeiPlugin.PANEL_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get()));
        // Its own texture is 24x16, not a 256x256 sheet, so the size has to be said out loud.
        this.arrow = helper.drawableBuilder(ARROW, 0, 0,
                        UCCJeiPlugin.ARROW_WIDTH, UCCJeiPlugin.ARROW_HEIGHT)
                .setTextureSize(UCCJeiPlugin.ARROW_WIDTH, UCCJeiPlugin.ARROW_HEIGHT)
                .buildAnimated(MaterialCompressorBlockEntity.MAX_PROGRESS,
                        IDrawableAnimated.StartDirection.LEFT, false);
    }

    /** One display per tier, in the order the tiers are declared. */
    public static List<Display> displays() {
        List<Display> displays = new ArrayList<>(CompressorTier.values().length);

        for (CompressorTier tier : CompressorTier.values()) {
            displays.add(new Display(tier,
                    new ItemStack(tier.input()),
                    new ItemStack(ModBlocks.byLevel(tier.outputLevel()).get())));
        }

        return displays;
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.unnecessarilycompressedcobblestone.material_compressor");
    }

    // getBackground() is on its way out of the API; a category now says how big it is and draws
    // its own panel in draw().
    @Override
    public int getWidth() {
        return UCCJeiPlugin.PANEL_WIDTH;
    }

    @Override
    public int getHeight() {
        return UCCJeiPlugin.PANEL_HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display display, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, UCCJeiPlugin.INPUT_X, UCCJeiPlugin.SLOT_Y)
                .addItemStack(display.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, UCCJeiPlugin.OUTPUT_X, UCCJeiPlugin.SLOT_Y)
                .addItemStack(display.output());
        // The machine a tier belongs to, so the entry says which of the three to build.
        builder.addSlot(RecipeIngredientRole.CATALYST, UCCJeiPlugin.INPUT_X, UCCJeiPlugin.SLOT_Y - 26)
                .addItemStack(new ItemStack(ModBlocks.compressor(display.tier()).get()));
    }

    @Override
    public void draw(Display display, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        arrow.draw(graphics, UCCJeiPlugin.ARROW_X, UCCJeiPlugin.ARROW_Y);
    }
}
