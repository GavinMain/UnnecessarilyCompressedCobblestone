package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedCobblestoneArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Vanilla's {@link ArrowRenderer} already draws the arrow shape and handles the wobble on impact,
 * so all a custom arrow has to supply is the texture. It is laid out like vanilla's: a 16x5 strip
 * of the arrow seen from the side in the top left, and a 5x5 cap for the flights below it.
 */
public class CompressedCobblestoneArrowRenderer extends ArrowRenderer<CompressedCobblestoneArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "textures/entity/projectiles/compressed_cobblestone_arrow.png");

    public CompressedCobblestoneArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CompressedCobblestoneArrowEntity entity) {
        return TEXTURE;
    }
}
