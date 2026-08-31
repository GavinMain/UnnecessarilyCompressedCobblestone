package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedStaffItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Client-side model predicates. Registered from {@code UCCClient} during client setup. */
public class ModItemProperties {
    public static void addCustomItemProperties() {
        makeCustomBow(ModItems.COMPRESSED_COBBLESTONE_BOW.get());
        makeCustomStaff(ModItems.COMPRESSED_LIGHTNING_STAFF.get());
        makeCustomStaff(ModItems.COMPRESSED_ARROW_STAFF.get());
        makeCustomStaff(ModItems.COMPRESSED_ARROW_TNT_STAFF.get());
        makeCustomStaff(ModItems.COMPRESSED_SUMMONING_STAFF.get());
    }

    /**
     * One predicate: whether the cast has finished. The staff swaps to its charged model the moment
     * it is ready to release and stays there for as long as the button is held, so the texture is
     * the only thing telling the caster the four seconds are up.
     */
    private static void makeCustomStaff(Item item) {
        ItemProperties.register(item,
                ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "cast"),
                (stack, level, entity, seed) ->
                        CompressedStaffItem.isCharged(stack, level, entity) ? 1.0F : 0.0F);
    }

    /**
     * The two predicates every bow model needs: {@code pulling} for whether it is being drawn and
     * {@code pull} for how far. The divisor is the bow's own draw duration rather than vanilla's
     * 20 ticks, so the animation runs at the speed the bow actually draws.
     */
    private static void makeCustomBow(Item item) {
        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }

            return entity.getUseItem() != stack ? 0.0F
                    : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks())
                            / (float) CompressedCobblestoneBowItem.DRAW_DURATION;
        });

        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
