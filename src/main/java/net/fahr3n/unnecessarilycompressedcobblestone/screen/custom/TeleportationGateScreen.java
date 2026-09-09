package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.TeleportationGateBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.network.SetTeleportationGatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Two lines of text and a button: what this gate is called, and the name of the gate it sends to.
 * <p>
 * Both boxes start from the block entity's own copy, which the client has because the gate puts both
 * names in its update tag - so opening a gate shows what it actually says rather than what this
 * screen last typed. The button is the only thing that sends anything, and it is greyed out until
 * one of the two boxes differs from the gate, so there is no doubt about whether a change took.
 */
public class TeleportationGateScreen extends AbstractContainerScreen<TeleportationGateMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/gui/teleportation_gate/teleportation_gate_gui.png");

    private static final int BOX_X = 8;
    private static final int BOX_WIDTH = 160;
    private static final int BOX_HEIGHT = 18;
    private static final int NAME_Y = 27;
    private static final int DESTINATION_Y = 60;

    private static final int BUTTON_X = 58;
    private static final int BUTTON_Y = 84;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 18;

    /** Vanilla's escape key, which closes the screen even while a box has the keyboard. */
    private static final int ESCAPE = 256;

    private EditBox nameBox;
    private EditBox destinationBox;
    private Button saveButton;

    public TeleportationGateScreen(TeleportationGateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 110;
        this.inventoryLabelY = this.imageHeight;
    }

    @Override
    protected void init() {
        super.init();

        this.nameBox = box(NAME_Y, "gui.unnecessarilycompressedcobblestone.teleportation_gate.name",
                menu.blockEntity.gateName());
        this.destinationBox = box(DESTINATION_Y, "gui.unnecessarilycompressedcobblestone.teleportation_gate.destination",
                menu.blockEntity.destination());

        this.saveButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.unnecessarilycompressedcobblestone.teleportation_gate.save"),
                        button -> save())
                .bounds(leftPos + BUTTON_X, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.unnecessarilycompressedcobblestone.teleportation_gate.save.tooltip")))
                .build());

        setInitialFocus(this.nameBox);
    }

    private EditBox box(int y, String hint, String value) {
        EditBox box = new EditBox(font, leftPos + BOX_X, topPos + y, BOX_WIDTH, BOX_HEIGHT,
                Component.translatable(hint));
        box.setMaxLength(TeleportationGateBlockEntity.MAX_NAME_LENGTH);
        box.setValue(value);

        return addRenderableWidget(box);
    }

    /**
     * The one thing that changes anything. The server re-checks that this player has this gate's
     * menu open before it believes a word of it - see {@link SetTeleportationGatePayload}.
     */
    private void save() {
        PacketDistributor.sendToServer(new SetTeleportationGatePayload(menu.blockEntity.getBlockPos(),
                nameBox.getValue().trim(), destinationBox.getValue().trim()));
    }

    /** Greyed out while both boxes still say what the gate says. */
    @Override
    protected void containerTick() {
        super.containerTick();

        this.saveButton.active = !nameBox.getValue().trim().equals(menu.blockEntity.gateName())
                || !destinationBox.getValue().trim().equals(menu.blockEntity.destination());
    }

    /**
     * While a box has the keyboard, the inventory key is a letter rather than a way out - which is
     * vanilla's own arrangement on the anvil, and is why this is not left to the superclass.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == ESCAPE && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
            return true;
        }

        if (nameBox.keyPressed(keyCode, scanCode, modifiers) || nameBox.canConsumeInput()
                || destinationBox.keyPressed(keyCode, scanCode, modifiers) || destinationBox.canConsumeInput()) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** The gate has no slots, so the player's inventory label would name a chest that is not there. */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font,
                Component.translatable("gui.unnecessarilycompressedcobblestone.teleportation_gate.name"),
                BOX_X, NAME_Y - 10, 0x404040, false);
        guiGraphics.drawString(font,
                Component.translatable("gui.unnecessarilycompressedcobblestone.teleportation_gate.destination"),
                BOX_X, DESTINATION_Y - 10, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
