package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.OmniSlashEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Omni Slash in flight: one quad five blocks tall, carrying a cut-out blade shape.
 * <p>
 * It is billboarded about the vertical axis only, which is the difference between this and the
 * Scythe Wave's crescent and is the whole of what a five block blade needs. The wave is small enough
 * that turning it fully to face the camera reads as a coin; a slash this tall wants to stand
 * upright, because standing upright is the shape of the tunnel it is about to open and a player
 * needs to see how tall that is before it reaches them. Turning it in yaw alone keeps it upright and
 * still stops it ever being edge-on - which a crescent aimed along its own path would be to exactly
 * the person who threw it.
 * <p>
 * Drawn at full brightness and both ways round, the way the Scythe Wave and the Arrow Veil's dome
 * are: a cut of light should read the same in a cave as at noon, and a single-sided quad would
 * vanish for half a turn of the camera.
 */
public class OmniSlashRenderer extends EntityRenderer<OmniSlashEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/omni_slash.png");

    private static final int RED = 255;
    private static final int GREEN = 255;
    private static final int BLUE = 255;

    /**
     * Well clear of transparent. The blade shape is carried in the texture's own alpha - see the
     * note in TEXTURE.md - so the tint has no holes to punch and only has to keep the crescent from
     * reading as glass.
     */
    private static final int ALPHA = 235;

    public OmniSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(OmniSlashEntity slash, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Yaw only, off the camera rather than off the entity: upright is the point, and the blade
        // should never present its edge to whoever is watching it.
        poseStack.mulPose(Axis.YP.rotationDegrees(
                -this.entityRenderDispatcher.camera.getYRot()));

        float half = OmniSlashEntity.HEIGHT / 2.0F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        quad(consumer, pose, half, false);
        quad(consumer, pose, half, true);

        poseStack.popPose();
        super.render(slash, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One face of the quad. {@code flip} winds it the other way, so the back is solid too. */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float half, boolean flip) {
        float[][] corners = {
                {-half, -half, 0.0F, 1.0F},
                {half, -half, 1.0F, 1.0F},
                {half, half, 1.0F, 0.0F},
                {-half, half, 0.0F, 0.0F}};

        for (int i = 0; i < 4; i++) {
            float[] corner = corners[flip ? 3 - i : i];
            consumer.addVertex(pose, corner[0], corner[1], 0.0F)
                    .setColor(RED, GREEN, BLUE, ALPHA)
                    .setUv(corner[2], corner[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, 0.0F, 0.0F, flip ? -1.0F : 1.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(OmniSlashEntity entity) {
        return TEXTURE;
    }
}
