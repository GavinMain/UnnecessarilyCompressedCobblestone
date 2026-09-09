package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedStaffItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;

/** Client-side model predicates. Registered from {@code UCCClient} during client setup. */
public class ModItemProperties {
    public static void addCustomItemProperties() {
        makeCustomBow(ModItems.COMPRESSED_COBBLESTONE_BOW.get(), CompressedCobblestoneBowItem.DRAW_DURATION);
        makeCustomBow(ModItems.BOLT_LAUNCHER.get(), BowItem.MAX_DRAW_DURATION);
        makeCustomBow(ModItems.TNT_LAUNCHER.get(), BowItem.MAX_DRAW_DURATION);
        makeCustomStaff(ModItems.COMPRESSED_LIGHTNING_STAFF.get());
        makeCustomStaff(ModItems.COMPRESSED_ARROW_STAFF.get());
        makeCustomStaff(ModItems.COMPRESSED_ARROW_TNT_STAFF.get());
        makeCustomStaff(ModItems.COMPRESSED_SUMMONING_STAFF.get());
        makeCustomFishingRod(ModItems.COMPRESSED_FISHING_ROD.get());
    }

    /**
     * The one predicate a fishing rod model needs: whether its bobber is in the water, which swaps
     * the model to the bent rod with a line on it. This is vanilla's own test copied rather than
     * shared, because vanilla registers it against {@code Items.FISHING_ROD} by name and there is no
     * way to join that registration - including the offhand rule, which is that a rod in the off
     * hand does not show as cast while the main hand is also holding one, since that is the rod the
     * bobber actually belongs to.
     */
    private static void makeCustomFishingRod(Item item) {
        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("cast"), (stack, level, entity, seed) -> {
            if (!(entity instanceof Player player)) {
                return 0.0F;
            }

            boolean mainHand = player.getMainHandItem() == stack;
            boolean offHand = player.getOffhandItem() == stack
                    && !(player.getMainHandItem().getItem() instanceof FishingRodItem);

            return (mainHand || offHand) && player.fishing != null ? 1.0F : 0.0F;
        });
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
     * 20 ticks, so the animation runs at the speed the bow actually draws - which for the Bolt
     * Launcher happens to be vanilla's, and for the Compressed Cobblestone Bow is four times it.
     */
    private static void makeCustomBow(Item item, int drawDuration) {
        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }

            return entity.getUseItem() != stack ? 0.0F
                    : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks())
                            / (float) drawDuration;
        });

        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
