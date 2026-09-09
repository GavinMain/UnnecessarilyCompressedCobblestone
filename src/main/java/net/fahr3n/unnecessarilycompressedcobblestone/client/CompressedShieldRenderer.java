package net.fahr3n.unnecessarilycompressedcobblestone.client;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the Compressed Shield in hand and in the inventory.
 * <p>
 * A shield needs a renderer of its own and there is no way round it: vanilla's
 * {@code BlockEntityWithoutLevelRenderer#renderByItem} tests {@code stack.is(Items.SHIELD)} by
 * identity, so any other shield in the game is simply not drawn at all - it is invisible in the
 * hand, not merely flat. This is that branch again, minus the banner patterns, which the Compressed
 * Shield does not take.
 * <p>
 * The sprite is stitched onto vanilla's own shield atlas without any registration here. That atlas
 * is built from {@code assets/minecraft/atlases/shield_patterns.json}, whose third source is a
 * {@code directory} lister over {@code textures/entity/shield}, and a directory lister walks
 * <em>every</em> loaded namespace rather than only {@code minecraft} - so a file dropped at
 * {@code assets/unnecessarilycompressedcobblestone/textures/entity/shield/compressed_shield.png}
 * arrives on the atlas under that name and nothing else has to be told about it.
 */
public class CompressedShieldRenderer extends BlockEntityWithoutLevelRenderer {
    /** The face, on vanilla's shield atlas. See the class note for why no registration is needed. */
    public static final Material MATERIAL = new Material(Sheets.SHIELD_SHEET,
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID,
                    "entity/shield/compressed_shield"));

    private ShieldModel model;

    public CompressedShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (this.model == null) {
            this.model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        }

        // Vanilla's own flip, which is what puts the model the right way up in every display
        // context at once rather than needing one transform per context.
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        var consumer = MATERIAL.sprite().wrap(ItemRenderer.getFoilBufferDirect(
                buffer, this.model.renderType(MATERIAL.atlasLocation()), true, stack.hasFoil()));
        this.model.handle().render(poseStack, consumer, packedLight, packedOverlay);
        this.model.plate().render(poseStack, consumer, packedLight, packedOverlay);

        poseStack.popPose();
    }
}
