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
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.CompressionInscriberBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What the Compression Inscriber will take, one entry per piece of gear it will charge.
 * <p>
 * There are no recipes behind this - the inscriber asks {@link CompressionEnergy} whether a stack is
 * a source and whether a stack can hold energy, and both answers come off item tags. So the display
 * list is built by asking those same two questions of every item in the registry, which means
 * another mod's cobblestone shows up in the left slot and another mod's gear shows up as its own
 * entry with nothing to add here.
 * <p>
 * The gear slot is both the input and the output: the piece stays where it is and the energy is
 * poured into it. The output is registered invisibly so that looking a piece of gear up in JEI
 * still finds this category.
 */
public class CompressionInscriberCategory implements IRecipeCategory<CompressionInscriberCategory.Display> {
    /** One piece of gear, and everything the inscriber will burn into it. */
    public record Display(ItemStack gear, List<ItemStack> sources) {
    }

    public static final RecipeType<Display> TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID,
                    "compression_inscriber"),
            Display.class);

    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID,
            "textures/gui/compression_inscriber/compression_inscriber_gui.png");
    private static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/gui/arrow_progress.png");

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public CompressionInscriberCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(PANEL, 0, 0,
                UCCJeiPlugin.PANEL_WIDTH, UCCJeiPlugin.PANEL_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.COMPRESSION_INSCRIBER.get()));
        this.arrow = helper.drawableBuilder(ARROW, 0, 0,
                        UCCJeiPlugin.ARROW_WIDTH, UCCJeiPlugin.ARROW_HEIGHT)
                .setTextureSize(UCCJeiPlugin.ARROW_WIDTH, UCCJeiPlugin.ARROW_HEIGHT)
                .buildAnimated(CompressionInscriberBlockEntity.MAX_PROGRESS,
                        IDrawableAnimated.StartDirection.LEFT, false);
    }

    /**
     * One display per inscribable item, each carrying every source the inscriber accepts. Both lists
     * are read out of the registry rather than written down, so this follows the tags.
     */
    public static List<Display> displays() {
        List<ItemStack> sources = new ArrayList<>();
        List<ItemStack> gear = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            if (CompressionEnergy.sourceValue(stack).isPresent()) {
                sources.add(stack);
            }
            if (CompressionEnergy.canHoldEnergy(stack)) {
                gear.add(stack);
            }
        }

        List<Display> displays = new ArrayList<>(gear.size());
        for (ItemStack piece : gear) {
            displays.add(new Display(piece, sources));
        }

        return displays;
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.compression_inscriber");
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
                .addItemStacks(display.sources());
        builder.addSlot(RecipeIngredientRole.INPUT, UCCJeiPlugin.OUTPUT_X, UCCJeiPlugin.SLOT_Y)
                .addItemStack(display.gear());

        // The gear comes back out of the slot it went into, so there is no third slot to show it in.
        // Registering it invisibly is what makes "recipes for this item" find the inscriber.
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(display.gear());
    }

    @Override
    public void draw(Display display, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        arrow.draw(graphics, UCCJeiPlugin.ARROW_X, UCCJeiPlugin.ARROW_Y);

        Component line = Component.translatable("jei.unnecessarilycompressedcobblestone.compression_inscriber.info");
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, line,
                (UCCJeiPlugin.PANEL_WIDTH - font.width(line)) / 2, UCCJeiPlugin.SLOT_Y + 26,
                0x404040, false);
    }
}
