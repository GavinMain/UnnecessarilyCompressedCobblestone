package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;

/**
 * The golem is shaped like an iron golem, so vanilla's renderer already draws it, cracks and all.
 * Only the skin differs.
 */
public class CompressedGolemRenderer extends IronGolemRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/compressed_golem/compressed_golem.png");

    public CompressedGolemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(IronGolem entity) {
        return TEXTURE;
    }
}
