package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.AbstractCompressedDragonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Vanilla's dragon model, drawn for this mod's dragon.
 * <p>
 * The geometry is not copied: the parts come out of {@code ModelLayers.ENDER_DRAGON}, which vanilla
 * has already registered and baked, so there is no layer definition here and no second copy of two
 * hundred cube declarations to keep in step. What <em>is</em> copied is the posing, because
 * {@code EnderDragonRenderer.DragonModel} is written against {@code EnderDragon} and can only be
 * handed one - and an {@code EnderDragon} subclass is not available either, since that class's
 * constructor pins itself to {@code EntityType.ENDER_DRAGON}.
 * <p>
 * Everything below reads exactly three things off the entity: the wingbeat, the last sixty-four
 * ticks of heading and height, and how far each piece of the neck rides above the last. That is the
 * whole interface a dragon has to its drawing, and {@link AbstractCompressedDragonEntity} answers all three.
 * <p>
 * The neck part is rendered twenty-eight times over - five for the neck, twelve for the tail, and
 * the head once - which is vanilla's own trick and the reason a dragon is one of the cheapest large
 * models in the game.
 */
@OnlyIn(Dist.CLIENT)
public class CompressedDragonModel extends EntityModel<AbstractCompressedDragonEntity> {
    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart jaw;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart leftFrontLeg;
    private final ModelPart leftFrontLegTip;
    private final ModelPart leftFrontFoot;
    private final ModelPart leftRearLeg;
    private final ModelPart leftRearLegTip;
    private final ModelPart leftRearFoot;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;
    private final ModelPart rightFrontLeg;
    private final ModelPart rightFrontLegTip;
    private final ModelPart rightFrontFoot;
    private final ModelPart rightRearLeg;
    private final ModelPart rightRearLegTip;
    private final ModelPart rightRearFoot;

    /**
     * Which dragon is being drawn and how far through the tick, both handed over by
     * {@code prepareMobModel}. {@code renderToBuffer} is given neither, which is why vanilla's model
     * keeps them in fields as well.
     */
    @Nullable
    private AbstractCompressedDragonEntity entity;
    private float partialTick;

    public CompressedDragonModel(ModelPart root) {
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.neck = root.getChild("neck");
        this.body = root.getChild("body");
        this.leftWing = root.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.leftFrontLegTip = this.leftFrontLeg.getChild("left_front_leg_tip");
        this.leftFrontFoot = this.leftFrontLegTip.getChild("left_front_foot");
        this.leftRearLeg = root.getChild("left_hind_leg");
        this.leftRearLegTip = this.leftRearLeg.getChild("left_hind_leg_tip");
        this.leftRearFoot = this.leftRearLegTip.getChild("left_hind_foot");
        this.rightWing = root.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.rightFrontLegTip = this.rightFrontLeg.getChild("right_front_leg_tip");
        this.rightFrontFoot = this.rightFrontLegTip.getChild("right_front_foot");
        this.rightRearLeg = root.getChild("right_hind_leg");
        this.rightRearLegTip = this.rightRearLeg.getChild("right_hind_leg_tip");
        this.rightRearFoot = this.rightRearLegTip.getChild("right_hind_foot");
    }

    @Override
    public void prepareMobModel(AbstractCompressedDragonEntity entity, float limbSwing, float limbSwingAmount,
                               float partialTick) {
        this.entity = entity;
        this.partialTick = partialTick;
    }

    /** Nothing: every angle this model has is set in {@code renderToBuffer}, as vanilla's is. */
    @Override
    public void setupAnim(AbstractCompressedDragonEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        if (this.entity == null) {
            return;
        }

        poseStack.pushPose();
        float beat = Mth.lerp(this.partialTick, this.entity.oFlapTime, this.entity.flapTime);
        this.jaw.xRot = (float) (Math.sin(beat * (float) (Math.PI * 2)) + 1.0) * 0.2F;
        float rise = (float) (Math.sin(beat * (float) (Math.PI * 2) - 1.0F) + 1.0);
        rise = (rise * rise + rise * 2.0F) * 0.05F;
        poseStack.translate(0.0F, rise - 2.0F, -3.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(rise * 2.0F));
        float x = 0.0F;
        float y = 20.0F;
        float z = -12.0F;
        double[] spineEnd = this.entity.getLatencyPos(6, this.partialTick);
        float roll = Mth.wrapDegrees((float) (this.entity.getLatencyPos(5, this.partialTick)[0]
                - this.entity.getLatencyPos(10, this.partialTick)[0]));
        float lean = Mth.wrapDegrees((float) (this.entity.getLatencyPos(5, this.partialTick)[0]
                + (double) (roll / 2.0F)));
        float phase = beat * (float) (Math.PI * 2);

        for (int i = 0; i < 5; i++) {
            double[] here = this.entity.getLatencyPos(5 - i, this.partialTick);
            float ripple = (float) Math.cos((float) i * 0.45F + phase) * 0.15F;
            this.neck.yRot = Mth.wrapDegrees((float) (here[0] - spineEnd[0])) * (float) (Math.PI / 180.0) * 1.5F;
            this.neck.xRot = ripple + this.entity.getHeadPartYOffset(i, spineEnd, here)
                    * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
            this.neck.zRot = -Mth.wrapDegrees((float) (here[0] - (double) lean)) * (float) (Math.PI / 180.0) * 1.5F;
            this.neck.y = y;
            this.neck.z = z;
            this.neck.x = x;
            y += Mth.sin(this.neck.xRot) * 10.0F;
            z -= Mth.cos(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            x -= Mth.sin(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            this.neck.render(poseStack, buffer, packedLight, packedOverlay, color);
        }

        this.head.y = y;
        this.head.z = z;
        this.head.x = x;
        double[] headPos = this.entity.getLatencyPos(0, this.partialTick);
        this.head.yRot = Mth.wrapDegrees((float) (headPos[0] - spineEnd[0])) * (float) (Math.PI / 180.0);
        this.head.xRot = Mth.wrapDegrees(this.entity.getHeadPartYOffset(6, spineEnd, headPos))
                * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
        this.head.zRot = -Mth.wrapDegrees((float) (headPos[0] - (double) lean)) * (float) (Math.PI / 180.0);
        this.head.render(poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-roll * 1.5F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        this.body.zRot = 0.0F;
        this.body.render(poseStack, buffer, packedLight, packedOverlay, color);
        float wing = beat * (float) (Math.PI * 2);
        this.leftWing.xRot = 0.125F - (float) Math.cos(wing) * 0.2F;
        this.leftWing.yRot = -0.25F;
        this.leftWing.zRot = -((float) (Math.sin(wing) + 0.125)) * 0.8F;
        this.leftWingTip.zRot = (float) (Math.sin(wing + 2.0F) + 0.5) * 0.75F;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.zRot = -this.leftWing.zRot;
        this.rightWingTip.zRot = -this.leftWingTip.zRot;
        renderSide(poseStack, buffer, packedLight, packedOverlay, rise, this.leftWing, this.leftFrontLeg,
                this.leftFrontLegTip, this.leftFrontFoot, this.leftRearLeg, this.leftRearLegTip,
                this.leftRearFoot, color);
        renderSide(poseStack, buffer, packedLight, packedOverlay, rise, this.rightWing, this.rightFrontLeg,
                this.rightFrontLegTip, this.rightFrontFoot, this.rightRearLeg, this.rightRearLegTip,
                this.rightRearFoot, color);
        poseStack.popPose();

        float ripple = 0.0F;
        phase = beat * (float) (Math.PI * 2);
        y = 10.0F;
        z = 60.0F;
        x = 0.0F;
        spineEnd = this.entity.getLatencyPos(11, this.partialTick);

        for (int i = 0; i < 12; i++) {
            headPos = this.entity.getLatencyPos(12 + i, this.partialTick);
            ripple += Mth.sin((float) i * 0.45F + phase) * 0.05F;
            this.neck.yRot = (Mth.wrapDegrees((float) (headPos[0] - spineEnd[0])) * 1.5F + 180.0F)
                    * (float) (Math.PI / 180.0);
            this.neck.xRot = ripple + (float) (headPos[1] - spineEnd[1]) * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
            this.neck.zRot = Mth.wrapDegrees((float) (headPos[0] - (double) lean)) * (float) (Math.PI / 180.0) * 1.5F;
            this.neck.y = y;
            this.neck.z = z;
            this.neck.x = x;
            y += Mth.sin(this.neck.xRot) * 10.0F;
            z -= Mth.cos(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            x -= Mth.sin(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            this.neck.render(poseStack, buffer, packedLight, packedOverlay, color);
        }

        poseStack.popPose();
    }

    /** One wing and the two legs under it. Both sides are the same parts at mirrored angles. */
    private static void renderSide(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                                   float rise, ModelPart wing, ModelPart frontLeg, ModelPart frontLegTip,
                                   ModelPart frontFoot, ModelPart rearLeg, ModelPart rearLegTip,
                                   ModelPart rearFoot, int color) {
        rearLeg.xRot = 1.0F + rise * 0.1F;
        rearLegTip.xRot = 0.5F + rise * 0.1F;
        rearFoot.xRot = 0.75F + rise * 0.1F;
        frontLeg.xRot = 1.3F + rise * 0.1F;
        frontLegTip.xRot = -0.5F - rise * 0.1F;
        frontFoot.xRot = 0.75F + rise * 0.1F;
        wing.render(poseStack, buffer, packedLight, packedOverlay, color);
        frontLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
        rearLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
