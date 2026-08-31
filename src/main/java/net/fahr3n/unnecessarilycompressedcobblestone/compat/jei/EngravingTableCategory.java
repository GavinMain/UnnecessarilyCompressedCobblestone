package net.fahr3n.unnecessarilycompressedcobblestone.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One entry per {@link Engraving}: the engraving, and everything it will go on.
 * <p>
 * What a given engraving fits is a predicate written against item tags wherever it can be, so the
 * gear list is found by asking {@link Engraving#canApplyTo} about every item in the registry rather
 * than by listing anything here. Another mod's helmet appears under Vision on its own.
 * <p>
 * The table takes an engraving back off again as cleanly as it puts one on, and both directions go
 * through the same two slots, so this shows the applying direction and says the other is free.
 */
public class EngravingTableCategory implements IRecipeCategory<EngravingTableCategory.Display> {
    /** One engraving and every piece of gear it has somewhere to go on. */
    public record Display(Engraving engraving, List<ItemStack> gear) {
    }

    public static final RecipeType<Display> TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID,
                    "engraving_table"),
            Display.class);

    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID,
            "textures/gui/engraving_table/engraving_table_gui.png");

    private final IDrawableStatic background;
    private final IDrawable icon;

    public EngravingTableCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(PANEL, 0, 0,
                UCCJeiPlugin.PANEL_WIDTH, UCCJeiPlugin.PANEL_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ENGRAVING_TABLE.get()));
    }

    /** One display per engraving, each carrying the gear that engraving fits. */
    public static List<Display> displays() {
        List<Display> displays = new ArrayList<>(Engraving.values().length);

        for (Engraving engraving : Engraving.values()) {
            List<ItemStack> gear = new ArrayList<>();

            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (engraving.canApplyTo(stack)) {
                    gear.add(stack);
                }
            }

            if (!gear.isEmpty()) {
                displays.add(new Display(engraving, gear));
            }
        }

        return displays;
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.engraving_table");
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
                .addItemStacks(display.gear());
        builder.addSlot(RecipeIngredientRole.INPUT, UCCJeiPlugin.OUTPUT_X, UCCJeiPlugin.SLOT_Y)
                .addItemStack(new ItemStack(display.engraving().item()));

        // The engraved piece leaves in the slot it arrived in, the way the inscriber's gear does.
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(display.gear());
    }

    @Override
    public void draw(Display display, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        var font = Minecraft.getInstance().font;

        Component name = display.engraving().displayName();
        graphics.drawString(font, name, (UCCJeiPlugin.PANEL_WIDTH - font.width(name)) / 2, 16,
                0x404040, false);

        Component line = Component.translatable("jei.unnecessarilycompressedcobblestone.engraving_table.info");
        graphics.drawString(font, line, (UCCJeiPlugin.PANEL_WIDTH - font.width(line)) / 2,
                UCCJeiPlugin.SLOT_Y + 26, 0x404040, false);
    }
}
