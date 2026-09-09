package net.fahr3n.unnecessarilycompressedcobblestone.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.GhastMountEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.MountPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Ghast Mount, and the deck built on its back.
 * <p>
 * The ghast itself is {@link FriendlyGhastRenderer} unchanged. The deck is drawn on top of it with
 * the game's own block renderer, one cell at a time, which is what makes another mod's block look
 * exactly as it does in the world without this knowing anything about it.
 * <p>
 * The cells are drawn <em>unrotated</em>, in the pose the dispatcher hands over before the model's
 * own rotations are applied. That is deliberate: the mount turns to follow whoever is flying it, and
 * a floor that rolled with the creature under it would be unstandable - it also keeps the deck's
 * collision boxes axis-aligned, which is the only kind an entity can have.
 * <p>
 * Light is read from the world position each cell happens to be over rather than from the entity, so
 * a deck flown into a cave darkens with the mount instead of staying lit from wherever it was built.
 */
public class GhastMountRenderer extends FriendlyGhastRenderer<GhastMountEntity> {
    public GhastMountRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GhastMountEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);

        MountPlatform platform = entity.platform();
        if (platform.isEmpty()) {
            return;
        }

        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        Level level = entity.level();
        Vec3 origin = MountPlatform.origin(entity.getPosition(partialTick));

        platform.forEach((x, y, z, block) -> {
            poseStack.pushPose();
            poseStack.translate(x - 0.5, y + MountPlatform.FLOOR_OFFSET, z - 0.5);
            BlockPos lit = BlockPos.containing(origin.x + x + 0.5, origin.y + y + 0.5, origin.z + z + 0.5);
            blocks.renderSingleBlock(block.defaultBlockState(), poseStack, buffers,
                    LevelRenderer.getLightColor(level, lit), OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        });
    }
}
