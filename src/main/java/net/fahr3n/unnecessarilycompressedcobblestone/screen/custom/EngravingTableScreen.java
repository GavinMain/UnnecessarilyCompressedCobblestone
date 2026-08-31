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
 * Two boxes and two buttons. The buttons are enabled off the block entity rather than off anything
 * the screen works out for itself - the client's copy of the table is kept current by the block
 * entity's update packet, so it can answer both questions the same way the server does.
 */
public class EngravingTableScreen extends AbstractContainerScreen<EngravingTableMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/gui/engraving_table/engraving_table_gui.png");

    /** The clear column between the two slots, which is where both buttons go. */
    private static final int BUTTON_X = 68;
    private static final int BUTTON_WIDTH = 40;
    private static final int BUTTON_HEIGHT = 16;
    private static final int ENGRAVE_BUTTON_Y = 24;
    private static final int REMOVE_BUTTON_Y = 46;

    private Button engraveButton;
    private Button removeButton;

    public EngravingTableScreen(EngravingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.engraveButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.unnecessarilycompressedcobblestone.engraving_table.engrave"),
                        button -> press(EngravingTableMenu.ENGRAVE_BUTTON))
                .bounds(leftPos + BUTTON_X, topPos + ENGRAVE_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.unnecessarilycompressedcobblestone.engraving_table.engrave.tooltip")))
                .build());

        this.removeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.unnecessarilycompressedcobblestone.engraving_table.remove"),
                        button -> press(EngravingTableMenu.REMOVE_BUTTON))
                .bounds(leftPos + BUTTON_X, topPos + REMOVE_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.unnecessarilycompressedcobblestone.engraving_table.remove.tooltip")))
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

        this.engraveButton.active = menu.blockEntity.canEngrave();
        this.removeButton.active = menu.blockEntity.canRemove();
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
