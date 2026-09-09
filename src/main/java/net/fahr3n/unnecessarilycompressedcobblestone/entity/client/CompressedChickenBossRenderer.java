package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Vanilla's chicken renderer with a different skin on it. The model, the flapping and the head
 * bobbing are all inherited, and so is how large it is drawn: the boss is a chicken and nothing
 * else, several times over.
 * <p>
 * There is deliberately no scaling here. {@code LivingEntityRenderer#render} already multiplies the
 * model by {@code entity.getScale()} and {@code getShadowRadius} already multiplies the shadow by
 * it, so
 * {@link net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedChickenBossEntity#BOSS_SCALE}
 * is the one number that moves the drawing, the shadow and the hitbox together. Scaling the pose
 * stack here as well would draw a body the hitbox does not match, which is the one thing a boss this
 * large must not do - every arrow at it would miss by a body's width.
 */
public class CompressedChickenBossRenderer extends ChickenRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/compressed_chicken_boss.png");

    public CompressedChickenBossRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Chicken entity) {
        return TEXTURE;
    }
}
