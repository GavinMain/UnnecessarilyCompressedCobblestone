package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import java.util.OptionalDouble;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompressionInscriberMenu;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CompressionInscriberBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int GEAR_SLOT = 1;

    /** Ten seconds to move one block's worth of energy across. */
    public static final int MAX_PROGRESS = 200;

    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT -> CompressionEnergy.sourceValue(stack).isPresent();
                case GEAR_SLOT -> CompressionEnergy.canHoldEnergy(stack);
                default -> false;
            };
        }
    };

    /**
     * What hoppers and pipes are given. Cobblestone can be pushed into the left slot and finished
     * gear can be pulled out of the right one; nothing can reach into the left slot to take the
     * cobblestone back out, or drop gear in from the outside.
     */
    public final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT_SLOT ? itemHandler.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == GEAR_SLOT ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT && itemHandler.isItemValid(slot, stack);
        }
    };

    protected final ContainerData data;
    private int progress = 0;

    public CompressionInscriberBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMPRESSION_INSCRIBER_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> CompressionInscriberBlockEntity.this.progress;
                    case 1 -> MAX_PROGRESS;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    CompressionInscriberBlockEntity.this.progress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.compression_inscriber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CompressionInscriberMenu(containerId, inventory, this, this.data);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (!canInscribe()) {
            resetProgress();
            return;
        }

        this.progress++;
        setChanged(level, blockPos, blockState);

        if (this.progress >= MAX_PROGRESS) {
            inscribe();
            resetProgress();
        }
    }

    /** Both slots hold something the machine knows what to do with. */
    private boolean canInscribe() {
        return CompressionEnergy.sourceValue(itemHandler.getStackInSlot(INPUT_SLOT)).isPresent()
                && CompressionEnergy.canHoldEnergy(itemHandler.getStackInSlot(GEAR_SLOT));
    }

    /** One block's energy goes into the gear and the block itself is spent. */
    private void inscribe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        OptionalDouble value = CompressionEnergy.sourceValue(input);
        ItemStack gear = itemHandler.getStackInSlot(GEAR_SLOT);
        if (value.isEmpty() || !CompressionEnergy.canHoldEnergy(gear)) {
            return;
        }

        CompressionEnergy.add(gear, value.getAsDouble());
        itemHandler.extractItem(INPUT_SLOT, 1, false);
        setChanged();
    }

    private void resetProgress() {
        this.progress = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("compression_inscriber.progress", progress);

        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("compression_inscriber.progress");
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
