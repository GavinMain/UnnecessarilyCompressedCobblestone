package net.fahr3n.unnecessarilycompressedcobblestone.network;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.GhastMountEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.MountPlatform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A Ghast Mount's deck, on its way to everyone watching it.
 * <p>
 * The deck cannot be synched data: it is a map rather than a value, and there is no serializer for
 * one. The whole of it is sent rather than the cell that changed, which is the cheaper thing to get
 * right - {@link MountPlatform#MAX_BLOCKS} cells is a couple of hundred bytes and it only goes out
 * when somebody places or breaks something, whereas a stream of edits has to be replayed in order
 * onto a client that may have joined halfway through. The first copy a client gets is not this at
 * all but {@code GhastMountEntity.writeSpawnData}, which sends the same bytes with the entity.
 */
public record PlatformSyncPayload(int entityId, MountPlatform platform) implements CustomPacketPayload {
    public static final Type<PlatformSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "platform_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlatformSyncPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.entityId());
                payload.platform().write(buffer);
            }, buffer -> {
                int entityId = buffer.readVarInt();
                MountPlatform platform = new MountPlatform();
                platform.read(buffer);
                return new PlatformSyncPayload(entityId, platform);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Nothing in the payload is trusted beyond the id: the platform has already capped and
     * bounds-checked every cell as it was read, and an id naming something that is not a mount is
     * simply dropped.
     */
    public static void handle(PlatformSyncPayload payload, IPayloadContext context) {
        Entity entity = context.player().level().getEntity(payload.entityId());
        if (entity instanceof GhastMountEntity mount) {
            mount.setPlatform(payload.platform());
        }
    }
}
