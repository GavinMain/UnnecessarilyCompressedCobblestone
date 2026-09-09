package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.TeleportationGateBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.TeleportationGateMenu;
import net.fahr3n.unnecessarilycompressedcobblestone.util.TeleporterNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * One end of a teleportation link. It holds two strings and nothing else: what this gate is called,
 * and the name of the gate anything walking into it should come out of.
 * <p>
 * Only the lower half of a gate has one of these - the upper half is the same block with no block
 * entity, and everything that happens to it is passed down. The name is the gate's identity in
 * {@link TeleporterNetwork}, which is what lets a destination in an unloaded chunk, or an unloaded
 * dimension, be found without walking every gate in the world.
 */
public class TeleportationGateBlockEntity extends BlockEntity implements MenuProvider {
    /** As long a name as the screen has room for, and as long as the packet will carry. */
    public static final int MAX_NAME_LENGTH = 32;

    private static final String NAME = "gate_name";
    private static final String DESTINATION = "destination";

    /**
     * How long a stay counts as one visit. An entity standing in the gate is inside it on every one
     * of these ticks; a gap of more than a tick is what "left and came back" means, and is the only
     * thing that lets it be sent again.
     */
    private static final int VISIT_GAP_TICKS = 1;

    /** When it stops being worth keeping every passer-by and is worth sweeping the map. */
    private static final int TOUCH_SWEEP_SIZE = 32;

    private String gateName = "";
    private String destination = "";

    /**
     * The last tick each entity was inside this gate, which is the whole of the "do not send it
     * again until it leaves and comes back" rule.
     * <p>
     * It is deliberately not saved. A gate that has just been loaded has nobody standing in it as
     * far as it knows, and the worst that can happen is that something already inside is sent once
     * on the tick after the chunk comes back - which is what walking into it would have done anyway.
     */
    private final Map<UUID, Long> lastTouch = new HashMap<>();

    public TeleportationGateBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TELEPORTATION_GATE_BE.get(), pos, blockState);
    }

    public String gateName() {
        return gateName;
    }

    public String destination() {
        return destination;
    }

    /**
     * Renames this gate and re-aims it, moving its entry in the network to match.
     * <p>
     * The old name is taken out of the book before the new one goes in, and only if it still pointed
     * here - see {@link TeleporterNetwork#unbind}. The client's copy is caught up by the block
     * update at the end.
     */
    public void configure(String name, String destination) {
        String trimmedName = trim(name);
        String trimmedDestination = trim(destination);
        if (trimmedName.equals(gateName) && trimmedDestination.equals(this.destination)) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            TeleporterNetwork network = TeleporterNetwork.get(serverLevel);
            network.unbind(gateName, worldPosition);
            network.bind(trimmedName, serverLevel.dimension(), worldPosition);
        }

        this.gateName = trimmedName;
        this.destination = trimmedDestination;
        setChanged();

        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Puts this gate back in the book when its chunk comes back, but never over a gate that has
     * since taken its name - the one holding the name is the one that answers to it, and a chunk
     * loading is not a claim.
     */
    @Override
    public void onLoad() {
        super.onLoad();

        if (level instanceof ServerLevel serverLevel && !gateName.isEmpty()) {
            TeleporterNetwork network = TeleporterNetwork.get(serverLevel);
            if (!network.claimedByOther(gateName, serverLevel.dimension(), worldPosition)) {
                network.bind(gateName, serverLevel.dimension(), worldPosition);
            }
        }
    }

    /** Called from the block when it is actually broken, not when its chunk is merely unloaded. */
    public void unbind() {
        if (level instanceof ServerLevel serverLevel) {
            TeleporterNetwork.get(serverLevel).unbind(gateName, worldPosition);
        }
    }

    /**
     * Sends {@code entity} to this gate's destination, if it has one and if this is not the same
     * visit it was already sent on.
     * <p>
     * The guard is a tick stamp rather than a cooldown, because the rule wanted is "not again until
     * it leaves and comes back" rather than "not again for N seconds". An entity standing in the
     * gate is reported inside on every tick, so a stamp that is still current means the same visit;
     * a gap means it left and came back.
     */
    public void accept(Entity entity) {
        if (!(level instanceof ServerLevel serverLevel) || destination.isEmpty()) {
            return;
        }

        long now = serverLevel.getGameTime();
        Long last = lastTouch.put(entity.getUUID(), now);
        sweep(now);

        if (last != null && last >= now - VISIT_GAP_TICKS) {
            return;
        }

        // A passenger is carried by whatever it is riding, and sending half of a stack of entities
        // through would leave the other half behind; vanilla's portals dismount for the same reason.
        if (entity.isPassenger() || entity.isVehicle()) {
            return;
        }

        Optional<TeleporterNetwork.Gate> target = TeleporterNetwork.get(serverLevel).find(destination);
        if (target.isEmpty()) {
            return;
        }

        TeleporterNetwork.Gate gate = target.get();
        if (gate.pos().equals(worldPosition) && gate.dimension().equals(serverLevel.dimension())) {
            return;
        }

        ServerLevel destinationLevel = serverLevel.getServer().getLevel(gate.dimension());
        if (destinationLevel == null) {
            return;
        }

        // Deferred to the end of the tick rather than done here. This is reached from inside
        // Entity#move, and moving an entity between dimensions there means removing it and building
        // a copy halfway through its own movement; the server's task queue is the first moment after
        // the tick that is safe, and is where vanilla's own portals end up too.
        serverLevel.getServer().execute(() -> {
            if (!entity.isRemoved() && entity.level() == serverLevel) {
                send(entity, serverLevel, destinationLevel, gate.pos());
            }
        });
    }

    /**
     * Puts {@code entity} down in front of the far gate, facing away from it, and marks it as having
     * arrived there so the far gate does not send it straight back.
     * <p>
     * {@code changeDimension} is used for the same-dimension case as well as the cross-dimension
     * one rather than only the second: it is the one call that moves a player's own client as well
     * as the server's copy, and vanilla's own implementation already reduces to a plain teleport
     * when the two levels are the same.
     */
    private void send(Entity entity, ServerLevel from, ServerLevel to, BlockPos gatePos) {
        BlockState gateState = to.getBlockState(gatePos);
        Direction facing = gateState.hasProperty(TeleportationGateBlock.FACING)
                ? gateState.getValue(TeleportationGateBlock.FACING)
                : Direction.NORTH;

        Vec3 landing = TeleportationGateBlock.landingSpot(to, gatePos, facing);

        from.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);

        // The far gate is told the entity is already there, so the tick it lands on is part of a
        // visit it has already made rather than a new one. It only matters where the landing spot
        // is close enough for the far gate to catch it, but it costs one map write to be sure.
        if (to.getBlockEntity(gatePos) instanceof TeleportationGateBlockEntity far) {
            far.lastTouch.put(entity.getUUID(), to.getGameTime());
        }

        Entity moved = entity.changeDimension(new DimensionTransition(to, landing, Vec3.ZERO,
                facing.toYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));

        if (moved != null) {
            to.playSound(null, BlockPos.containing(landing), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Drops everything that is no longer inside, so the map does not grow with every passer-by. */
    private void sweep(long now) {
        if (lastTouch.size() > TOUCH_SWEEP_SIZE) {
            lastTouch.values().removeIf(tick -> tick < now - VISIT_GAP_TICKS);
        }
    }

    private static String trim(String name) {
        String trimmed = name.trim();
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.teleportation_gate");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TeleportationGateMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString(NAME, gateName);
        tag.putString(DESTINATION, destination);

        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        gateName = tag.getString(NAME);
        destination = tag.getString(DESTINATION);
    }

    /** The screen reads both names off the client's copy, so both have to be in the update tag. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
