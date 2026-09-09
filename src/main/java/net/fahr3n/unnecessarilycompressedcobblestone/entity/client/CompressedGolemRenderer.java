package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;

/**
 * The golems are shaped like an iron golem, so vanilla's renderer already draws them, cracks and
 * all. Only the skin differs, and which skin that is is the entity's own answer - see
 * {@link CompressedGolemEntity#texture()} - so one renderer covers the whole family and a new golem
 * needs nothing on the client at all.
 */
public class CompressedGolemRenderer extends IronGolemRenderer {
    public CompressedGolemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(IronGolem entity) {
        return entity instanceof CompressedGolemEntity golem ? golem.texture() : super.getTextureLocation(entity);
    }
}
