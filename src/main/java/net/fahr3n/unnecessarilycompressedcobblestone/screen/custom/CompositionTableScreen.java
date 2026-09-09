package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
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
 * Two staves of squares, a bolt, and two buttons. Both buttons are enabled off the block entity
 * rather than off anything the screen works out for itself - the client's copy of the table is kept
 * current by the block entity's update packet, so it can answer both questions the same way the
 * server does.
 */
public class CompositionTableScreen extends AbstractContainerScreen<CompositionTableMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/gui/composition_table/composition_table_gui.png");

    /** The row the bolt sits on, to the right of it. */
    private static final int BUTTON_Y = 61;
    private static final int COMPOSE_BUTTON_X = 32;
    private static final int COMPOSE_BUTTON_WIDTH = 62;
    private static final int ERASE_BUTTON_X = 100;
    private static final int ERASE_BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 16;

    private Button composeButton;
    private Button eraseButton;

    public CompositionTableScreen(CompositionTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.composeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.unnecessarilycompressedcobblestone.composition_table.compose"),
                        button -> press(CompositionTableMenu.COMPOSE_BUTTON))
                .bounds(leftPos + COMPOSE_BUTTON_X, topPos + BUTTON_Y, COMPOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.unnecessarilycompressedcobblestone.composition_table.compose.tooltip")))
                .build());

        this.eraseButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.unnecessarilycompressedcobblestone.composition_table.erase"),
                        button -> press(CompositionTableMenu.ERASE_BUTTON))
                .bounds(leftPos + ERASE_BUTTON_X, topPos + BUTTON_Y, ERASE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.unnecessarilycompressedcobblestone.composition_table.erase.tooltip")))
                .build());
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

        this.composeButton.active = menu.blockEntity.canCompose();
        this.eraseButton.active = menu.blockEntity.canErase();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        guiGraphics.blit(GUI_TEXTURE, (width - imageWidth) / 2, (height - imageHeight) / 2, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
