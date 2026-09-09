package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.AbstractFriendlyGhastEntity;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Vanilla's {@code GhastRenderer}, written out for a mob that is not a {@code Ghast}.
 * <p>
 * It has to be written out rather than reused because that class is declared
 * {@code MobRenderer<Ghast, GhastModel<Ghast>>} and the friendly ghasts deliberately are not
 * ghasts - see {@link AbstractFriendlyGhastEntity} for why. Nothing else about it changes:
 * {@code GhastModel} is generic over {@code Entity} rather than over {@code Ghast}, so the model,
 * the shadow, the 4.5 scale and both of vanilla's textures come across unchanged, and the charging
 * flag still picks the open-mouthed one.
 */
public class FriendlyGhastRenderer<T extends AbstractFriendlyGhastEntity> extends MobRenderer<T, GhastModel<T>> {
    private static final ResourceLocation GHAST =
            ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast.png");
    private static final ResourceLocation GHAST_SHOOTING =
            ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast_shooting.png");

    public FriendlyGhastRenderer(EntityRendererProvider.Context context) {
        super(context, new GhastModel<>(context.bakeLayer(ModelLayers.GHAST)), 1.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.isCharging() ? GHAST_SHOOTING : GHAST;
    }

    /** Vanilla's own figure: the ghast model is built at a ninth of its size and scaled up here. */
    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(4.5F, 4.5F, 4.5F);
    }
}
