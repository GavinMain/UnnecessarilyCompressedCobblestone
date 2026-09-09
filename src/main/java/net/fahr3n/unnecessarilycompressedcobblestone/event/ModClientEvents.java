package net.fahr3n.unnecessarilycompressedcobblestone.event;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.fahr3n.unnecessarilycompressedcobblestone.network.SwingScythePayload;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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

    /**
     * A swing at nothing, with a scythe that turns swings into crescents, told to the server.
     * <p>
     * This is the only way the server hears about it. A left click that lands on an entity arrives
     * as an interact packet and posts {@code AttackEntityEvent}, which {@code ModEvents} catches -
     * but a click on empty air produces a swing animation and nothing else, and this event is
     * client side only. Since a Scythe Wave exists to hit things far outside reach, empty air is
     * what the player is nearly always clicking.
     * <p>
     * The packet says only that a swing happened. Everything it is worth is read on the server off
     * the sender's own hand.
     */
    @SubscribeEvent
    public static void onScytheSwingAtNothing(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getHand() == InteractionHand.MAIN_HAND
                && Engravings.has(event.getItemStack(), Engraving.SCYTHE_WAVE)) {
            PacketDistributor.sendToServer(SwingScythePayload.INSTANCE);
        }
    }
}
