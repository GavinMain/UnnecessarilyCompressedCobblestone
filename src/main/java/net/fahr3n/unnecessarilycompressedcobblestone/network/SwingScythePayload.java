package net.fahr3n.unnecessarilycompressedcobblestone.network;

import io.netty.buffer.ByteBuf;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.event.ModEvents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "I swung at nothing", on its way to the server, so that a Scythe Wave can be loosed from it.
 * <p>
 * It exists because the server is never told about that swing. Left-clicking an entity arrives as
 * {@code ServerboundInteractPacket} and posts {@code AttackEntityEvent}, which is where the
 * engraving catches an ordinary attack - but left-clicking empty air produces only a swing
 * animation, and the one event for it, {@code PlayerInteractEvent.LeftClickEmpty}, is fired on the
 * client and nowhere else. Since the whole of this engraving is hitting things that are <em>not</em>
 * in reach, that is the case that matters most, and a packet is the only way across.
 * <p>
 * It carries nothing at all, and it is trusted for nothing: the handler reads what is in the
 * sender's own hand and re-checks the engraving, so the packet can only ever mean "this player
 * swung" and never "this player swung with that, over there".
 */
public record SwingScythePayload() implements CustomPacketPayload {
    public static final SwingScythePayload INSTANCE = new SwingScythePayload();

    public static final Type<SwingScythePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "swing_scythe"));

    public static final StreamCodec<ByteBuf, SwingScythePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwingScythePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ModEvents.swingScytheWave(player);
        }
    }
}
