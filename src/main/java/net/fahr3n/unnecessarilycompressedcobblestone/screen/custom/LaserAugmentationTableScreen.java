package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Two boxes, a list and two buttons.
 * <p>
 * The list is the whole reason this screen is not the Engraving Table's: a laser carries up to eight
 * augments at once and any one of them may be taken off, so they are all shown and one is picked,
 * rather than the last one being handed back. What is drawn comes off the block entity - the client's
 * copy of the table is kept current by its update packet, and the laser stack in it carries its own
 * augment component - so the screen can answer every question the same way the server does and
 * neither button has to be trusted.
 * <p>
 * The selection is client side and nothing else knows about it: the Remove button sends the chosen
 * augment by name, so a stale selection is refused by the block entity rather than removing the
 * wrong thing.
 */
public class LaserAugmentationTableScreen extends AbstractContainerScreen<LaserAugmentationTableMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID,
            "textures/gui/laser_augmentation_table/laser_augmentation_table_gui.png");

    /** The panel, which is taller than the usual 166 to make room for the list. */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 186;

    /** The list: five rows of twelve pixels, with a scrollbar down its right-hand edge. */
    private static final int LIST_X = 44;
    private static final int LIST_Y = 16;
    private static final int LIST_WIDTH = 118;
    private static final int ROW_HEIGHT = 12;
    private static final int VISIBLE_ROWS = 5;
    private static final int SCROLLBAR_X = LIST_X + LIST_WIDTH + 2;
    private static final int SCROLLBAR_WIDTH = 6;

    private static final int BUTTON_Y = 80;
    private static final int BUTTON_HEIGHT = 16;
    private static final int APPLY_BUTTON_X = 44;
    private static final int REMOVE_BUTTON_X = 108;
    private static final int BUTTON_WIDTH = 60;

    /** Row colours: the selected row, the text on it, and the text on every other row. */
    private static final int SELECTED_BACKGROUND = 0xFF5B7FB0;
    private static final int SELECTED_TEXT = 0xFFFFFF;
    private static final int TEXT = 0x404040;
    private static final int EMPTY_TEXT = 0x808080;
    private static final int SCROLLBAR_TRACK = 0xFF8B8B8B;
    private static final int SCROLLBAR_THUMB = 0xFFC6C6C6;

    private Button applyButton;
    private Button removeButton;

    /** The augment the player has picked out of the list, or null if none is picked. */
    private Augment selected;

    /** How far down the list is scrolled, in whole rows. */
    private int scroll;

    public LaserAugmentationTableScreen(LaserAugmentationTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        // The panel is twenty pixels taller than a standard one, so the inventory label has to come
        // down with the inventory itself; vanilla's default is written against 166.
        this.inventoryLabelY = PANEL_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.applyButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.unnecessarilycompressedcobblestone.laser_augmentation_table.apply"),
                        button -> press(LaserAugmentationTableMenu.APPLY_BUTTON))
                .bounds(leftPos + APPLY_BUTTON_X, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.unnecessarilycompressedcobblestone.laser_augmentation_table.apply.tooltip")))
                .build());

        this.removeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.unnecessarilycompressedcobblestone.laser_augmentation_table.remove"),
                        button -> removeSelected())
                .bounds(leftPos + REMOVE_BUTTON_X, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.unnecessarilycompressedcobblestone.laser_augmentation_table.remove.tooltip")))
                .build());
    }

    /** The chosen augment is sent by name, as {@code REMOVE_BUTTON_BASE} plus its ordinal. */
    private void removeSelected() {
        if (this.selected != null) {
            press(LaserAugmentationTableMenu.REMOVE_BUTTON_BASE + this.selected.ordinal());
        }
    }

    /** Both buttons go through the vanilla button-click packet, which lands in the menu. */
    private void press(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        List<Augment> fitted = menu.blockEntity.fitted();

        // The laser can be pulled off the table, or an augment taken off it by somebody else at the
        // same table, so a selection is only kept while it is still there to be selected.
        if (this.selected != null && !fitted.contains(this.selected)) {
            this.selected = null;
        }

        this.scroll = Math.max(0, Math.min(this.scroll, fitted.size() - VISIBLE_ROWS));

        this.applyButton.active = menu.blockEntity.canApply();
        this.removeButton.active = menu.blockEntity.canRemove(this.selected);
    }

    /** A click inside the list picks a row; anything else is the container's business. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Augment> fitted = menu.blockEntity.fitted();
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;

        if (x >= LIST_X && x < LIST_X + LIST_WIDTH
                && y >= LIST_Y && y < LIST_Y + VISIBLE_ROWS * ROW_HEIGHT) {
            int row = this.scroll + (y - LIST_Y) / ROW_HEIGHT;
            if (row >= 0 && row < fitted.size()) {
                Augment clicked = fitted.get(row);
                // Clicking the selected row again clears the selection, so there is a way back to
                // having nothing chosen without having to pull the laser off the table.
                this.selected = clicked == this.selected ? null : clicked;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int hidden = menu.blockEntity.fitted().size() - VISIBLE_ROWS;
        if (hidden > 0) {
            this.scroll = Math.max(0, Math.min(hidden, this.scroll - (int) Math.signum(scrollY)));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        renderList(guiGraphics);
    }

    /** The fitted augments, five at a time, with the chosen one picked out. */
    private void renderList(GuiGraphics guiGraphics) {
        List<Augment> fitted = menu.blockEntity.fitted();

        if (fitted.isEmpty()) {
            Component empty = Component.translatable(
                    menu.blockEntity.laser().isEmpty()
                            ? "gui.unnecessarilycompressedcobblestone.laser_augmentation_table.no_laser"
                            : "gui.unnecessarilycompressedcobblestone.laser_augmentation_table.none");
            guiGraphics.drawString(this.font, empty, leftPos + LIST_X + 2, topPos + LIST_Y + 2,
                    EMPTY_TEXT, false);
            return;
        }

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.scroll + row;
            if (index >= fitted.size()) {
                break;
            }

            Augment augment = fitted.get(index);
            int y = topPos + LIST_Y + row * ROW_HEIGHT;
            boolean picked = augment == this.selected;

            if (picked) {
                guiGraphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH, y + ROW_HEIGHT,
                        SELECTED_BACKGROUND);
            }

            guiGraphics.drawString(this.font, augment.displayName(), leftPos + LIST_X + 3, y + 2,
                    picked ? SELECTED_TEXT : TEXT, false);
        }

        renderScrollbar(guiGraphics, fitted.size());
    }

    /** A track and a thumb, drawn only when there is more in the list than the five rows show. */
    private void renderScrollbar(GuiGraphics guiGraphics, int rows) {
        int hidden = rows - VISIBLE_ROWS;
        if (hidden <= 0) {
            return;
        }

        int top = topPos + LIST_Y;
        int height = VISIBLE_ROWS * ROW_HEIGHT;
        int thumbHeight = Math.max(8, height * VISIBLE_ROWS / rows);
        int thumbTop = top + (height - thumbHeight) * this.scroll / hidden;

        guiGraphics.fill(leftPos + SCROLLBAR_X, top, leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH,
                top + height, SCROLLBAR_TRACK);
        guiGraphics.fill(leftPos + SCROLLBAR_X, thumbTop, leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH,
                thumbTop + thumbHeight, SCROLLBAR_THUMB);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
