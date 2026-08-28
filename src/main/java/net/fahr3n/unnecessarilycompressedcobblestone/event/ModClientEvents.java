package net.fahr3n.unnecessarilycompressedcobblestone.event;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {
    /**
     * The zoom that comes with drawing a bow. Vanilla hardcodes it to {@code minecraft:bow}, so a
     * modded bow has to apply its own; this is the same curve over this bow's longer draw.
     */
    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (!event.getPlayer().isUsingItem() || !event.getPlayer().getUseItem().is(ModItems.COMPRESSED_COBBLESTONE_BOW.get())) {
            return;
        }

        float drawn = (float) event.getPlayer().getTicksUsingItem() / (float) CompressedCobblestoneBowItem.DRAW_DURATION;
        drawn = drawn > 1.0F ? 1.0F : drawn * drawn;

        event.setNewFovModifier(event.getNewFovModifier() * (1.0F - drawn * 0.15F));
    }
}
