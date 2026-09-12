package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BlackHoleEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A black hole: a pure black sphere the size of its core, inside a faint violet halo that is drawn
 * from inside as well as out. Built here rather than from a model for the same reason the Arrow
 * Veil's dome is - a model is a box and this is round.
 * <p>
 * The texture is plain white and every colour is the vertex colour, so the core is black under any
 * light and needs no drawing of its own.
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/black_hole.png");

    private static final int RINGS = 16;
    private static final int SEGMENTS = 32;

    /** How much wider than the core the halo is. */
    private static final float HALO_SCALE = 1.35F;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BlackHoleEntity hole, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float core = (float) hole.coreRadius();
        PoseStack.Pose pose = poseStack.last();

        float flash = hole.flashProgress(partialTicks);
        if (flash >= 0.0F) {
            renderFlash(buffer, pose, core, flash);
            return;
        }

        // Both render types skip back-face culling, so one winding draws from inside and out.
        sphere(buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), pose, core, 0, 0, 0, 255);
        sphere(buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)), pose, core * HALO_SCALE,
                90, 30, 160, 70);
    }

    /**
     * A Singularity's flash: the black core is gone and a white sphere swells out of where it was.
     * It reaches full size in the first {@value #FLASH_SWELL} of the flash, holds at full brightness
     * until {@value #FLASH_HOLD}, and only then fades - the hold is what makes it linger rather than
     * blink. Two shells, an inner near-opaque one and a wider soft one, because none of these render
     * types is additive and brightness has to be built out of an opaque middle for the glow to sit in
     * front of. Both are emissive, so the flash is as bright in a cave as at noon.
     */
    private static void renderFlash(MultiBufferSource buffer, PoseStack.Pose pose, float core, float flash) {
        float swell = Math.min(1.0F, flash / FLASH_SWELL);
        float eased = 1.0F - (1.0F - swell) * (1.0F - swell);
        float radius = core + (BlackHoleEntity.FLASH_RADIUS - core) * eased;
        float fade = flash <= FLASH_HOLD ? 1.0F : 1.0F - (flash - FLASH_HOLD) / (1.0F - FLASH_HOLD);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        sphere(consumer, pose, radius * 0.6F, 255, 255, 255, (int) (240 * fade));
        sphere(consumer, pose, radius, 255, 250, 235, (int) (130 * fade));
    }

    /** Where the flash reaches full size, and where it starts to fade, as fractions of the flash. */
    private static final float FLASH_SWELL = 0.2F;
    private static final float FLASH_HOLD = 0.5F;

    private static void sphere(VertexConsumer consumer, PoseStack.Pose pose, float radius,
                               int red, int green, int blue, int alpha) {
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

                quad(consumer, pose, a, b, c, d, red, green, blue, alpha);
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

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float[] a, float[] b, float[] c, float[] d,
                             int red, int green, int blue, int alpha) {
        vertex(consumer, pose, a, 0.0F, 0.0F, red, green, blue, alpha);
        vertex(consumer, pose, b, 0.0F, 1.0F, red, green, blue, alpha);
        vertex(consumer, pose, c, 1.0F, 1.0F, red, green, blue, alpha);
        vertex(consumer, pose, d, 1.0F, 0.0F, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float[] position, float u, float v,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, position[0], position[1], position[2])
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, position[0], position[1], position[2]);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return TEXTURE;
    }
}
