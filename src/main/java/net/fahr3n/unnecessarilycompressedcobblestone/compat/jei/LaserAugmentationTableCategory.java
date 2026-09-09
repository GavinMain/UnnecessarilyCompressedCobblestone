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
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.LaserAugmentationTableMenu;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One entry per {@link Augment}: the augment, and the weapon it goes on.
 * <p>
 * Unlike the Engraving Table's category there is nothing to look up - every augment fits exactly one
 * item, the Ray of Laser - so the gear side of each entry is that item and no registry walk is
 * needed. What the entries are for is the same thing: somewhere to read what exists, in a game where
 * the only other way to find out is to craft one.
 * <p>
 * Like the Engraving Table this shows the fitting direction and says the other is free.
 */
public class LaserAugmentationTableCategory implements IRecipeCategory<LaserAugmentationTableCategory.Display> {
    /** One augment and the laser it is fitted to. */
    public record Display(Augment augment) {
    }

    public static final RecipeType<Display> TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID,
                    "laser_augmentation_table"),
            Display.class);

    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID,
            "textures/gui/laser_augmentation_table/laser_augmentation_table_gui.png");

    /** The top of the table's panel, which is taller than the other three machines'. */
    private static final int HEIGHT = 100;

    private final IDrawableStatic background;
    private final IDrawable icon;

    public LaserAugmentationTableCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(PANEL, 0, 0, UCCJeiPlugin.PANEL_WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.LASER_AUGMENTATION_TABLE.get()));
    }

    /** One display per augment. */
    public static List<Display> displays() {
        List<Display> displays = new ArrayList<>(Augment.values().length);
        for (Augment augment : Augment.values()) {
            displays.add(new Display(augment));
        }

        return displays;
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.laser_augmentation_table");
    }

    @Override
    public int getWidth() {
        return UCCJeiPlugin.PANEL_WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display display, IFocusGroup focuses) {
        ItemStack laser = new ItemStack(ModItems.RAY_OF_LASER.get());

        builder.addSlot(RecipeIngredientRole.INPUT,
                        LaserAugmentationTableMenu.LASER_SLOT_X, LaserAugmentationTableMenu.LASER_SLOT_Y)
                .addItemStack(laser);
        builder.addSlot(RecipeIngredientRole.INPUT,
                        LaserAugmentationTableMenu.AUGMENT_SLOT_X, LaserAugmentationTableMenu.AUGMENT_SLOT_Y)
                .addItemStack(new ItemStack(display.augment().item()));

        // The augmented laser leaves in the slot it arrived in, the way the Engraving Table's gear
        // does, so "recipes for this item" has to be told about it separately.
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(laser);
    }

    @Override
    public void draw(Display display, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        var font = Minecraft.getInstance().font;

        Component name = display.augment().displayName();
        graphics.drawString(font, name, 48, 20, 0x404040, false);

        // The description is a sentence rather than a label, so it is wrapped to what is left of the
        // panel beside the two slots rather than being allowed to run off the edge of it.
        Component line = Component.translatable(
                "tooltip.unnecessarilycompressedcobblestone.augment." + display.augment().getSerializedName());
        int y = 34;
        for (var wrapped : font.split(line, UCCJeiPlugin.PANEL_WIDTH - 56)) {
            graphics.drawString(font, wrapped, 48, y, 0x404040, false);
            y += font.lineHeight;
        }

        Component free = Component.translatable(
                "jei.unnecessarilycompressedcobblestone.laser_augmentation_table.info");
        graphics.drawString(font, free, 8, 84, 0x404040, false);
    }
}
