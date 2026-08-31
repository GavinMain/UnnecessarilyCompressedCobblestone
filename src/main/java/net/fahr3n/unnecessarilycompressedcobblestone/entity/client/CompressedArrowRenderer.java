package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Vanilla's {@link ArrowRenderer} already draws the arrow shape and handles the wobble on impact, so
 * all a custom arrow has to supply is the texture - and every strength of compressed arrow supplies
 * its own, so one renderer serves all of them.
 */
public class CompressedArrowRenderer extends ArrowRenderer<CompressedArrowEntity> {
    public CompressedArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CompressedArrowEntity entity) {
        return entity.tier().texture();
    }
}
