package net.fahr3n.unnecessarilycompressedcobblestone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RayOfLaserItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The Ray of Laser's beam, drawn exactly as a guardian's is.
 * <p>
 * The geometry is vanilla's own - two twisted quads down the length of the beam and a cap facing the
 * target, on the same {@code guardian_beam} texture - because the weapon is a guardian's attack and
 * should look like nothing else. What is not vanilla's is where it comes from: a guardian's beam is
 * drawn by that mob's renderer off its own {@code getActiveAttackTarget}, and a player has no such
 * field, so this hangs off the level render instead.
 * <p>
 * Nothing about the beam is sent to the client. Who is using an item, which item, and how long they
 * have held it are already synced for every player a client can see, and where the beam ends is the
 * same line of sight the server casts when it fires - so the picture is derived on both sides from
 * the same three facts and no packet of this mod's own exists. The one thing that differs is the
 * origin: the server hurts whatever the eye line meets, while the beam is drawn from the hand, so
 * that a laser held in first person is visible at all.
 */
@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID, value = Dist.CLIENT)
public class LaserBeamRenderer {
    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/guardian_beam.png");
    private static final RenderType BEAM_RENDER_TYPE = RenderType.entityCutoutNoCull(BEAM_TEXTURE);

    /** How far in front of the eye the beam starts, so a first-person view is not inside it. */
    private static final double FORWARD_OFFSET = 0.6;

    /** How far below and to the side of the eye it starts, which is roughly where the hand is. */
    private static final double DOWN_OFFSET = 0.25;
    private static final double SIDE_OFFSET = 0.35;

    /** Vanilla's own beam figures: the width of the shaft, the cap, and how fast the twist runs. */
    private static final float SHAFT_RADIUS = 0.2F;
    private static final float CAP_RADIUS = 0.282F;
    private static final float TWIST_RATE = -0.075F;

    private LaserBeamRenderer() {
    }

    /**
     * Drawn after the entities, which is where a guardian's own beam would have been drawn: the beam
     * is a cutout rather than a translucent surface, so it has to be laid down after everything solid
     * and before the water and the clouds.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        for (Player player : level.players()) {
            ItemStack stack = player.getUseItem();
            if (!player.isUsingItem() || !(stack.getItem() instanceof RayOfLaserItem)) {
                continue;
            }

            renderBeam(poseStack, buffer, level, player, stack, partialTick);
        }

        buffer.endBatch(BEAM_RENDER_TYPE);
        poseStack.popPose();
    }

    /** One beam, from the holder's hand to wherever the line of sight ends. */
    private static void renderBeam(PoseStack poseStack, MultiBufferSource buffer, ClientLevel level,
                                   Player player, ItemStack stack, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 side = look.cross(up).normalize();

        Vec3 eye = new Vec3(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight(),
                Mth.lerp(partialTick, player.zo, player.getZ()));

        // The hand rather than the eye, so the beam reads as coming out of the weapon. Which hand it
        // is comes off the one holding the laser, so a laser in the off hand fires from that side.
        double sideways = player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? SIDE_OFFSET : -SIDE_OFFSET;
        if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT) {
            sideways = -sideways;
        }

        Vec3 origin = eye.add(look.scale(FORWARD_OFFSET))
                .add(side.scale(sideways))
                .subtract(0.0, DOWN_OFFSET, 0.0);

        // Where the beam ends is worked out the same way the server works out what it hits, so what
        // is drawn and what is hurt are the same line.
        Vec3 end = RayOfLaserItem.beamEnd(level, player);
        Vec3 along = end.subtract(origin);
        float length = (float) along.length();
        if (length < 0.01F) {
            return;
        }

        Vec3 direction = along.normalize();
        float pitch = (float) Math.acos(direction.y);
        float yaw = (float) Math.atan2(direction.z, direction.x);

        // How far into the charge this beam is, which is what brightens it: a guardian's ray starts
        // thin and pale and is at its brightest on the tick it lands, and so does this one.
        float charge = RayOfLaserItem.chargeProgress(stack, player);
        float age = (player.tickCount + partialTick);

        poseStack.pushPose();
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(((float) (Math.PI / 2) - yaw) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * Mth.RAD_TO_DEG));

        float scroll = age * 0.5F % 1.0F;
        float twist = age * TWIST_RATE;
        float brightness = charge * charge;
        int red = 64 + (int) (brightness * 191.0F);
        int green = 32 + (int) (brightness * 191.0F);
        int blue = 128 - (int) (brightness * 64.0F);

        float capA = Mth.cos(twist + (float) (Math.PI * 3.0 / 4.0)) * CAP_RADIUS;
        float capB = Mth.sin(twist + (float) (Math.PI * 3.0 / 4.0)) * CAP_RADIUS;
        float capC = Mth.cos(twist + (float) (Math.PI / 4)) * CAP_RADIUS;
        float capD = Mth.sin(twist + (float) (Math.PI / 4)) * CAP_RADIUS;
        float capE = Mth.cos(twist + (float) Math.PI * 5.0F / 4.0F) * CAP_RADIUS;
        float capF = Mth.sin(twist + (float) Math.PI * 5.0F / 4.0F) * CAP_RADIUS;
        float capG = Mth.cos(twist + (float) Math.PI * 7.0F / 4.0F) * CAP_RADIUS;
        float capH = Mth.sin(twist + (float) Math.PI * 7.0F / 4.0F) * CAP_RADIUS;

        float shaftA = Mth.cos(twist + (float) Math.PI) * SHAFT_RADIUS;
        float shaftB = Mth.sin(twist + (float) Math.PI) * SHAFT_RADIUS;
        float shaftC = Mth.cos(twist) * SHAFT_RADIUS;
        float shaftD = Mth.sin(twist) * SHAFT_RADIUS;
        float shaftE = Mth.cos(twist + (float) (Math.PI / 2)) * SHAFT_RADIUS;
        float shaftF = Mth.sin(twist + (float) (Math.PI / 2)) * SHAFT_RADIUS;
        float shaftG = Mth.cos(twist + (float) (Math.PI * 3.0 / 2.0)) * SHAFT_RADIUS;
        float shaftH = Mth.sin(twist + (float) (Math.PI * 3.0 / 2.0)) * SHAFT_RADIUS;

        float vStart = -1.0F + scroll;
        float vEnd = length * 2.5F + vStart;

        VertexConsumer consumer = buffer.getBuffer(BEAM_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();

        vertex(consumer, pose, shaftA, length, shaftB, red, green, blue, 0.4999F, vEnd);
        vertex(consumer, pose, shaftA, 0.0F, shaftB, red, green, blue, 0.4999F, vStart);
        vertex(consumer, pose, shaftC, 0.0F, shaftD, red, green, blue, 0.0F, vStart);
        vertex(consumer, pose, shaftC, length, shaftD, red, green, blue, 0.0F, vEnd);
        vertex(consumer, pose, shaftE, length, shaftF, red, green, blue, 0.4999F, vEnd);
        vertex(consumer, pose, shaftE, 0.0F, shaftF, red, green, blue, 0.4999F, vStart);
        vertex(consumer, pose, shaftG, 0.0F, shaftH, red, green, blue, 0.0F, vStart);
        vertex(consumer, pose, shaftG, length, shaftH, red, green, blue, 0.0F, vEnd);

        float capV = player.tickCount % 2 == 0 ? 0.5F : 0.0F;
        vertex(consumer, pose, capA, length, capB, red, green, blue, 0.5F, capV + 0.5F);
        vertex(consumer, pose, capC, length, capD, red, green, blue, 1.0F, capV + 0.5F);
        vertex(consumer, pose, capG, length, capH, red, green, blue, 1.0F, capV);
        vertex(consumer, pose, capE, length, capF, red, green, blue, 0.5F, capV);

        poseStack.popPose();
    }

    /** Vanilla's own beam vertex: full bright, no overlay, normal straight up the beam. */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               int red, int green, int blue, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
