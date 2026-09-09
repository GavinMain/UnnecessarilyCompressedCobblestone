package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.AbstractCompressedDragonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Draws the Compressed Dragon: vanilla's dragon geometry, posed by {@link CompressedDragonModel}.
 * <p>
 * It is an {@code EntityRenderer} rather than a {@code MobRenderer} for the same reason vanilla's
 * dragon renderer is: the body's rotation is not the entity's {@code yRot} but where its head was
 * seven ticks ago, and the whole model is pitched by how fast it is climbing. A living-entity
 * renderer would apply its own body rotation on top of that and the dragon would draw twisted.
 * <p>
 * The crystal beam and the death rays vanilla draws here are gone - there are no end crystals in
 * this fight, and the finale is four lines of text rather than a light show. What is kept is the
 * cutout pass and the glowing eyes, which is what makes a dragon read as a dragon in the dark.
 */
@OnlyIn(Dist.CLIENT)
public class CompressedDragonRenderer extends EntityRenderer<AbstractCompressedDragonEntity> {
    /**
     * Vanilla's own dragon skin, deliberately: the fight is drawn as an ender dragon until someone
     * draws a compressed one - see TEXTURE.md. Both are read as the vanilla paths rather than copied
     * into this mod's assets, so there is nothing to keep in step if vanilla's art changes.
     */
    private static final ResourceLocation DRAGON =
            ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");

    private static final RenderType BODY = RenderType.entityCutoutNoCull(DRAGON);
    private static final RenderType EYES = RenderType.eyes(EYES_TEXTURE);

    private final CompressedDragonModel model;

    public CompressedDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        // Vanilla's baked dragon layer. Nothing is registered for this mod: the parts and their
        // cubes are already in the game, and a second copy of them could only drift.
        this.model = new CompressedDragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON));
    }

    @Override
    public void render(AbstractCompressedDragonEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // How large this dragon is. An EntityRenderer is not a LivingEntityRenderer and so does not
        // apply Attributes.SCALE for us, and it has to be applied here rather than left out: the
        // second phase is half again as large and the pet a tenth of one, and both of those figures
        // are that attribute. Everything below is in the model's own units, so the scale goes on
        // first and the shadow with it.
        float scale = entity.getScale();
        poseStack.scale(scale, scale, scale);

        // Where it was pointing seven ticks ago, and how much it has climbed over the last five:
        // the body follows the neck rather than the other way round, which is what makes a dragon's
        // turn read as a bank.
        float heading = (float) entity.getLatencyPos(7, partialTicks)[0];
        float climb = (float) (entity.getLatencyPos(5, partialTicks)[1]
                - entity.getLatencyPos(10, partialTicks)[1]);

        // 180 minus the heading, which is where this parts company with vanilla's dragon renderer
        // and is a bug fix rather than a flourish. Vanilla rotates by minus the heading alone, which
        // draws the model facing the *opposite* way to the yaw - and that is right for vanilla,
        // because an ender dragon's own tickPart puts its head at (sin, -cos), which is the reverse
        // of what a yaw means for every other entity in the game. Nothing here uses that convention:
        // `face` sets the yaw to the standard heading of where the dragon is travelling, and the
        // pet's `travel` hands its rider's input to `moveRelative`, which reads the yaw the standard
        // way too. Without the 180 the whole family was drawn tail first - which on the boss reads
        // as a dragon flying oddly, and on the pet reads as sitting on it backwards.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - heading));
        poseStack.mulPose(Axis.XP.rotationDegrees(climb * 10.0F));
        poseStack.translate(0.0F, 0.0F, 1.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        boolean hurt = entity.hurtTime > 0;
        this.model.prepareMobModel(entity, 0.0F, 0.0F, partialTicks);

        VertexConsumer body = buffer.getBuffer(BODY);
        this.model.renderToBuffer(poseStack, body, packedLight, OverlayTexture.pack(0.0F, hurt));

        VertexConsumer eyes = buffer.getBuffer(EYES);
        this.model.renderToBuffer(poseStack, eyes, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The shadow grows with the beast, exactly as a living entity renderer's would. */
    @Override
    protected float getShadowRadius(AbstractCompressedDragonEntity entity) {
        return this.shadowRadius * entity.getScale();
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractCompressedDragonEntity entity) {
        return DRAGON;
    }
}
