package net.fahr3n.unnecessarilycompressedcobblestone.network;

import io.netty.buffer.ByteBuf;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.TeleportationGateBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.TeleportationGateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What a player typed into a Teleportation Gate's screen, on its way to the server.
 * <p>
 * This is the mod's one packet, and it exists because the Engraving Table's trick does not stretch
 * to it: {@code clickMenuButton} carries an integer, which is enough for "engrave" and "remove" but
 * not for a name. Everything else the screen needs already arrives on its own - the gate's current
 * two names come down in the block entity's update tag, the way the Composition Table's sheet does.
 * <p>
 * Nothing in here is trusted. The handler re-checks that the sender has that gate's menu open and
 * that the menu is still valid, so the position in the packet can only ever name a gate the player
 * is already standing at, and the strings are cut to length by the block entity itself.
 */
public record SetTeleportationGatePayload(BlockPos pos, String name, String destination)
        implements CustomPacketPayload {
    public static final Type<SetTeleportationGatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "set_teleportation_gate"));

    /**
     * The length cap is the packet's own guard rather than a repeat of the block entity's: a client
     * that sends a megabyte of name is disconnected by the codec before anything reads it.
     */
    public static final StreamCodec<ByteBuf, SetTeleportationGatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetTeleportationGatePayload::pos,
            ByteBufCodecs.stringUtf8(TeleportationGateBlockEntity.MAX_NAME_LENGTH), SetTeleportationGatePayload::name,
            ByteBufCodecs.stringUtf8(TeleportationGateBlockEntity.MAX_NAME_LENGTH),
            SetTeleportationGatePayload::destination,
            SetTeleportationGatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Applies the change, having checked the sender is entitled to make it: they must have this
     * gate's menu open, and that menu must still be valid - which is vanilla's own "are you still
     * standing at the block" test.
     */
    public static void handle(SetTeleportationGatePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.containerMenu instanceof TeleportationGateMenu menu
                && menu.blockEntity.getBlockPos().equals(payload.pos())
                && menu.stillValid(player)) {
            menu.blockEntity.configure(payload.name(), payload.destination());
        }
    }
}
