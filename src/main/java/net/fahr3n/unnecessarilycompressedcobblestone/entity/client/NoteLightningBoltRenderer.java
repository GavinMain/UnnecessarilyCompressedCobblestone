package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.PianoNote;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * Vanilla's lightning, drawn in whatever colour the bolt is carrying.
 * <p>
 * The geometry is {@code LightningBoltRenderer}'s own, copied unchanged: the zigzag is rebuilt from
 * the entity's public {@code seed} the same way, so a bolt drawn here and a bolt drawn by vanilla
 * are the same shape. The single change is that the four channels handed to every quad come from
 * {@link PianoNote#colour} rather than being the pale blue written into that class.
 * <p>
 * A bolt with no note keeps vanilla's colour exactly, so nothing that is not music looks any
 * different from the lightning next to it in a storm.
 */
public class NoteLightningBoltRenderer extends EntityRenderer<VanillaLightningBoltEntity> {
    /** Vanilla's own pale blue, for a bolt that is thunder rather than a key. */
    private static final float PLAIN_RED = 0.45F;
    private static final float PLAIN_GREEN = 0.45F;
    private static final float PLAIN_BLUE = 0.5F;

    public NoteLightningBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VanillaLightningBoltEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float red = PLAIN_RED;
        float green = PLAIN_GREEN;
        float blue = PLAIN_BLUE;

        int note = entity.getNote();
        if (note != VanillaLightningBoltEntity.NO_NOTE) {
            int colour = PianoNote.colour(note);
            red = ((colour >> 16) & 0xFF) / 255.0F;
            green = ((colour >> 8) & 0xFF) / 255.0F;
            blue = (colour & 0xFF) / 255.0F;
        }

        float[] offsetsX = new float[8];
        float[] offsetsZ = new float[8];
        float x = 0.0F;
        float z = 0.0F;
        RandomSource shape = RandomSource.create(entity.seed);

        for (int i = 7; i >= 0; i--) {
            offsetsX[i] = x;
            offsetsZ[i] = z;
            x += shape.nextInt(11) - 5;
            z += shape.nextInt(11) - 5;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (int pass = 0; pass < 4; pass++) {
            RandomSource branch = RandomSource.create(entity.seed);

            for (int fork = 0; fork < 3; fork++) {
                int top = fork > 0 ? 7 - fork : 7;
                int bottom = fork > 0 ? top - 2 : 0;

                float currentX = offsetsX[top] - x;
                float currentZ = offsetsZ[top] - z;

                for (int segment = top; segment >= bottom; segment--) {
                    float previousX = currentX;
                    float previousZ = currentZ;
                    if (fork == 0) {
                        currentX += branch.nextInt(11) - 5;
                        currentZ += branch.nextInt(11) - 5;
                    } else {
                        currentX += branch.nextInt(31) - 15;
                        currentZ += branch.nextInt(31) - 15;
                    }

                    float wideHalf = 0.1F + pass * 0.2F;
                    if (fork == 0) {
                        wideHalf *= segment * 0.1F + 1.0F;
                    }

                    float narrowHalf = 0.1F + pass * 0.2F;
                    if (fork == 0) {
                        narrowHalf *= (segment - 1.0F) * 0.1F + 1.0F;
                    }

                    quad(matrix, consumer, currentX, currentZ, segment, previousX, previousZ,
                            red, green, blue, wideHalf, narrowHalf, false, false, true, false);
                    quad(matrix, consumer, currentX, currentZ, segment, previousX, previousZ,
                            red, green, blue, wideHalf, narrowHalf, true, false, true, true);
                    quad(matrix, consumer, currentX, currentZ, segment, previousX, previousZ,
                            red, green, blue, wideHalf, narrowHalf, true, true, false, true);
                    quad(matrix, consumer, currentX, currentZ, segment, previousX, previousZ,
                            red, green, blue, wideHalf, narrowHalf, false, true, false, false);
                }
            }
        }
    }

    /** {@code LightningBoltRenderer.quad}, unchanged but for taking the colour as an argument. */
    private static void quad(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, int index,
                             float x2, float z2, float red, float green, float blue,
                             float wide, float narrow,
                             boolean farX, boolean farZ, boolean nearX, boolean nearZ) {
        consumer.addVertex(matrix, x1 + (farX ? narrow : -narrow), index * 16.0F, z1 + (farZ ? narrow : -narrow))
                .setColor(red, green, blue, 0.3F);
        consumer.addVertex(matrix, x2 + (farX ? wide : -wide), (index + 1) * 16.0F, z2 + (farZ ? wide : -wide))
                .setColor(red, green, blue, 0.3F);
        consumer.addVertex(matrix, x2 + (nearX ? wide : -wide), (index + 1) * 16.0F, z2 + (nearZ ? wide : -wide))
                .setColor(red, green, blue, 0.3F);
        consumer.addVertex(matrix, x1 + (nearX ? narrow : -narrow), index * 16.0F, z1 + (nearZ ? narrow : -narrow))
                .setColor(red, green, blue, 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(VanillaLightningBoltEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
