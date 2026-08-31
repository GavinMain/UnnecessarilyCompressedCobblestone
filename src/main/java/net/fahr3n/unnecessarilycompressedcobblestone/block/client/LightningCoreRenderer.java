package net.fahr3n.unnecessarilycompressedcobblestone.block.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.LightningCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the bolt hovering over the core, turning slowly and bobbing, the way an enchanting table
 * draws its book. There is no model of its own: it is the bolt item's own icon rendered in the
 * world, so a bolt added later is drawn correctly without touching this.
 */
public class LightningCoreRenderer implements BlockEntityRenderer<LightningCoreBlockEntity> {
    /** A full turn every four seconds. */
    private static final float DEGREES_PER_TICK = 4.5F;

    /** How far the bob carries it, in blocks. */
    private static final float BOB_HEIGHT = 0.06F;

    private final ItemRenderer itemRenderer;

    public LightningCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LightningCoreBlockEntity core, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack bolt = core.getBolt();
        if (bolt.isEmpty() || core.getLevel() == null) {
            return;
        }

        float age = core.getLevel().getGameTime() + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.25 + Mth.sin(age / 12.0F) * BOB_HEIGHT, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * DEGREES_PER_TICK));
        poseStack.scale(0.6F, 0.6F, 0.6F);

        this.itemRenderer.renderStatic(bolt, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, core.getLevel(), 0);
        poseStack.popPose();
    }
}
