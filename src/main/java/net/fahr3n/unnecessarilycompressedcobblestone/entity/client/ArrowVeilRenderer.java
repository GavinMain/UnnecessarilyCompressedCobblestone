package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowVeilEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The dome itself: a translucent sphere built here rather than loaded from a model, because a model
 * would have to be a box and the veil is round.
 * <p>
 * Every quad is emitted twice, wound both ways, so the dome is drawn from inside as well as out -
 * the owner is standing in the middle of it, and a single-sided sphere would be invisible to the one
 * person it is protecting.
 */
public class ArrowVeilRenderer extends EntityRenderer<ArrowVeilEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/arrow_veil.png");

    /** How finely the sphere is cut. Twelve by twenty-four is smooth at four blocks across. */
    private static final int RINGS = 12;
    private static final int SEGMENTS = 24;

    private static final int RED = 150;
    private static final int GREEN = 210;
    private static final int BLUE = 255;
    private static final int ALPHA = 90;

    public ArrowVeilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ArrowVeilEntity veil, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float radius = ArrowVeilEntity.RADIUS * veil.openness(partialTicks);
        if (radius <= 0.0F) {
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        for (int ring = 0; ring < RINGS; ring++) {
            double phi0 = Math.PI * ring / RINGS;
            double phi1 = Math.PI * (ring + 1) / RINGS;

            for (int segment = 0; segment < SEGMENTS; segment++) {
                double theta0 = 2.0 * Math.PI * segment / SEGMENTS;
                double theta1 = 2.0 * Math.PI * (segment + 1) / SEGMENTS;

                float[] a = point(phi0, theta0, radius);
                float[] b = point(phi1, theta0, radius);
                float[] c = point(phi1, theta1, radius);
                float[] d = point(phi0, theta1, radius);

                float u0 = (float) segment / SEGMENTS;
                float u1 = (float) (segment + 1) / SEGMENTS;
                float v0 = (float) ring / RINGS;
                float v1 = (float) (ring + 1) / RINGS;

                quad(consumer, pose, packedLight, a, b, c, d, u0, v0, u1, v1);
                // The same face wound the other way, so it is solid from the inside too.
                quad(consumer, pose, packedLight, d, c, b, a, u1, v0, u0, v1);
            }
        }
    }

    private static float[] point(double phi, double theta, float radius) {
        float sinPhi = (float) Math.sin(phi);
        return new float[] {
                radius * sinPhi * (float) Math.cos(theta),
                radius * (float) Math.cos(phi),
                radius * sinPhi * (float) Math.sin(theta)};
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                             float[] a, float[] b, float[] c, float[] d,
                             float u0, float v0, float u1, float v1) {
        vertex(consumer, pose, packedLight, a, u0, v0);
        vertex(consumer, pose, packedLight, b, u0, v1);
        vertex(consumer, pose, packedLight, c, u1, v1);
        vertex(consumer, pose, packedLight, d, u1, v0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                               float[] position, float u, float v) {
        // Full brightness: the veil is lit from within, so it reads the same in a cave as at noon.
        consumer.addVertex(pose, position[0], position[1], position[2])
                .setColor(RED, GREEN, BLUE, ALPHA)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, position[0], position[1], position[2]);
    }

    @Override
    public ResourceLocation getTextureLocation(ArrowVeilEntity entity) {
        return TEXTURE;
    }
}
