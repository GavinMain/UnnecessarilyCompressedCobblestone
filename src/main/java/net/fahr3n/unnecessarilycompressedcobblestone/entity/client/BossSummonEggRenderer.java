package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BossSummonEggEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * Draws the summoning egg as a spinning dragon egg. There is no model and no texture of its own: it
 * is the vanilla dragon egg block, rendered through the block dispatcher the way
 * {@code TntRenderer} draws its TNT.
 * <p>
 * The glow is light rather than a shader - the block is handed full brightness regardless of where
 * it actually is, so it reads as lit from within even at the bottom of a cave, which is the same
 * trick that makes an end crystal look like it is burning.
 */
public class BossSummonEggRenderer extends EntityRenderer<BossSummonEggEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public BossSummonEggRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(BossSummonEggEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);

        // Swells slightly as it climbs, so the last second before the burst is visibly the biggest.
        float scale = 1.0F + entity.riseProgress(partialTicks) * 0.35F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(BossSummonEggEntity.spinDegrees(entity.tickCount, partialTicks)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        this.blockRenderer.renderSingleBlock(Blocks.DRAGON_EGG.defaultBlockState(), poseStack, buffer,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Lit from within: the block above is what the shadow and any lighting pass would read. */
    @Override
    protected int getBlockLightLevel(BossSummonEggEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(BossSummonEggEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
