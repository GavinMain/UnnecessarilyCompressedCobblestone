package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ScytheWaveEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The crescent in flight: one flat quad carrying a cut-out blade shape.
 * <p>
 * It is billboarded - turned to face the camera every frame - rather than aimed along its travel,
 * and that is a deliberate trade rather than a shortcut. A crescent standing across its own path is
 * edge-on to the person who threw it, so the one player who most needs to see where their swing
 * went would see nothing at all. Facing the camera costs the sense that the blade has a direction
 * and buys the wave being visible from everywhere, which is worth more on a projectile whose whole
 * point is that it is aimed.
 * <p>
 * Drawn at full brightness and both ways round, the way the Arrow Veil's dome is: a cut of light
 * should read the same in a cave as at noon, and a single-sided quad would vanish for half a turn
 * of the camera.
 */
public class ScytheWaveRenderer extends EntityRenderer<ScytheWaveEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/scythe_wave.png");

    private static final int RED = 255;
    private static final int GREEN = 255;
    private static final int BLUE = 255;

    /**
     * Well clear of transparent. The blade shape is carried in the texture's own alpha - see the
     * note in TEXTURE.md - so the tint has no holes to punch and only has to keep the crescent from
     * reading as glass.
     */
    private static final int ALPHA = 230;

    /** How fast the crescent turns about the line of sight, in degrees a tick. */
    private static final float ROLL_DEGREES_PER_TICK = 12.0F;

    public ScytheWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ScytheWaveEntity wave, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // A slow roll, so a wave crossing open ground is alive rather than a decal sliding along.
        poseStack.mulPose(Axis.ZP.rotationDegrees((wave.tickCount + partialTicks) * ROLL_DEGREES_PER_TICK));

        float half = ScytheWaveEntity.SIZE / 2.0F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        quad(consumer, pose, half, false);
        quad(consumer, pose, half, true);

        poseStack.popPose();
        super.render(wave, entityYaw, partialTicks, poseStack, buffer, packedLight);
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
    public ResourceLocation getTextureLocation(ScytheWaveEntity entity) {
        return TEXTURE;
    }
}
