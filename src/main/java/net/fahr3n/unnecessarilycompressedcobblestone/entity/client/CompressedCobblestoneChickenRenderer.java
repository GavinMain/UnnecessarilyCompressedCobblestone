package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Vanilla's chicken renderer with a different skin on it. The model, the flapping and the head
 * bobbing are all inherited; only the texture says this one is made of cobblestone.
 */
public class CompressedCobblestoneChickenRenderer extends ChickenRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/compressed_cobblestone_chicken.png");

    public CompressedCobblestoneChickenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Chicken entity) {
        return TEXTURE;
    }
}
