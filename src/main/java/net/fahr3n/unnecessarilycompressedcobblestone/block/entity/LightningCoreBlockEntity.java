package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the bolt a Lightning Core is loaded with, and calls it down while the core is powered.
 * <p>
 * The bolt is the whole of the block's behaviour: an empty core is inert, and what a loaded one
 * throws is decided by the {@link BoltItem} sitting on it rather than by anything here. The stack is
 * synced to clients because it is what the renderer draws hovering over the block, the way an
 * enchanting table's book is drawn.
 */
public class LightningCoreBlockEntity extends BlockEntity {
    /** A strike a second for as long as the redstone holds. */
    public static final int STRIKE_INTERVAL = 20;

    private static final String TAG_BOLT = "bolt";

    private ItemStack bolt = ItemStack.EMPTY;
    private int cooldown;

    public LightningCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LIGHTNING_CORE_BE.get(), pos, blockState);
    }

    public ItemStack getBolt() {
        return this.bolt;
    }

    public boolean isEmpty() {
        return this.bolt.isEmpty();
    }

    /** Puts one bolt on the core. Taking it out of a hand is the caller's business. */
    public void setBolt(ItemStack stack) {
        this.bolt = stack;
        this.cooldown = 0;
        sync();
    }

    /** Takes the bolt back off; an empty stack if there was none. */
    public ItemStack removeBolt() {
        ItemStack taken = this.bolt;
        this.bolt = ItemStack.EMPTY;
        sync();
        return taken;
    }

    /**
     * A strike every {@link #STRIKE_INTERVAL} ticks, but only while the core holds a bolt and
     * something is powering it. A button is a single strike; a lever is as long as you leave it.
     * <p>
     * How hard it is powered is passed on rather than thrown away: the strongest signal it can see
     * is what the bolt is struck at, so a comparator or a length of dust is a dial on the strike.
     * What a given bolt does with that is its own business - see {@link BoltItem#strike}.
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || this.bolt.isEmpty()) {
            return;
        }

        int signal = level.getBestNeighborSignal(pos);
        if (signal <= 0) {
            // Held ready rather than left counting down, so the first strike lands the moment the
            // signal arrives instead of up to a second later.
            this.cooldown = 0;
            return;
        }

        if (this.cooldown-- > 0) {
            return;
        }

        this.cooldown = STRIKE_INTERVAL;
        if (this.bolt.getItem() instanceof BoltItem boltItem) {
            // One block up, so what the strike hits is whatever is standing on the core.
            boltItem.strike(serverLevel, pos.above(), this.bolt, signal, 0.0F);
        }
    }

    /** Whatever is on the core falls off when it is broken. */
    public void drops() {
        if (!this.bolt.isEmpty() && this.level != null) {
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(),
                    this.worldPosition.getZ(), this.bolt);
            this.bolt = ItemStack.EMPTY;
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * The key is written even when the core is empty, and this is not tidiness: an empty core would
     * otherwise save nothing at all, and {@link ClientboundBlockEntityDataPacket} turns an empty
     * update tag into a null one, which the client is told to ignore. The bolt would come off the
     * core on the server and go on being drawn on every client until something else made them
     * reload the chunk.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_BOLT, this.bolt.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.bolt = ItemStack.parseOptional(registries, tag.getCompound(TAG_BOLT));
    }

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
