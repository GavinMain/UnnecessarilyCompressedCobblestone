package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.MaterialCompressorBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.MaterialCompressorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Squeezes blocks of coal into deeply compressed cobblestone: one block of coal in, one tier
 * {@link #OUTPUT_LEVEL} block out. The first tier of the machine only knows this one conversion.
 */
public class MaterialCompressorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    /** What a block of coal is worth here. */
    public static final int OUTPUT_LEVEL = 16;

    /** Ten seconds a block, the same pace the inscriber works at. */
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
            // Nothing may be put into the output slot from outside - not by a player, not by a
            // pipe. The machine fills it with setStackInSlot, which does not ask.
            return slot == INPUT_SLOT && stack.is(Items.COAL_BLOCK);
        }
    };

    /** The two views handed out by {@link #automationFor}, built once rather than per query. */
    private final IItemHandler inputFace = new FaceHandler(this.itemHandler, INPUT_SLOT, -1);
    private final IItemHandler outputFace = new FaceHandler(this.itemHandler, -1, OUTPUT_SLOT);

    protected final ContainerData data;
    private int progress = 0;

    public MaterialCompressorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATERIAL_COMPRESSOR_TIER_1_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MaterialCompressorBlockEntity.this.progress;
                    case 1 -> MAX_PROGRESS;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    MaterialCompressorBlockEntity.this.progress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    /**
     * What a hopper or a pipe on {@code side} is allowed to do. Coal goes in through the top or the
     * back and cobblestone comes out of the bottom or the front, which is what the block's textures
     * advertise; the two remaining sides are solid casing and hand back nothing at all.
     * <p>
     * A hopper above pushes down into the top and a hopper below pulls out of the bottom, so the
     * vanilla pair works without either of them needing to know about the side faces.
     *
     * @param side the face being asked about, or null for the block's own internal view
     */
    @Nullable
    public IItemHandler automationFor(@Nullable Direction side) {
        if (side == null) {
            return this.itemHandler;
        }

        // The state is read rather than stored so that rotating the block re-aims its ports. During
        // removal the block entity can outlive its block for a tick, so the property is checked
        // rather than assumed.
        BlockState state = getBlockState();
        if (!state.hasProperty(MaterialCompressorBlock.FACING)) {
            return null;
        }

        Direction facing = state.getValue(MaterialCompressorBlock.FACING);
        if (side == Direction.UP || side == facing.getOpposite()) {
            return this.inputFace;
        }

        if (side == Direction.DOWN || side == facing) {
            return this.outputFace;
        }

        return null;
    }

    /**
     * One face's view of the machine: it may push into {@code insertSlot} and pull out of
     * {@code extractSlot}, and a slot of -1 means that direction is closed off entirely. The slots
     * still read back through, so a pipe can see what is inside without being able to move it.
     */
    private record FaceHandler(ItemStackHandler backing, int insertSlot, int extractSlot) implements IItemHandler {
        @Override
        public int getSlots() {
            return this.backing.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.backing.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == this.insertSlot ? this.backing.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == this.extractSlot ? this.backing.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.backing.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == this.insertSlot && this.backing.isItemValid(slot, stack);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.material_compressor_tier_1");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MaterialCompressorMenu(containerId, inventory, this, this.data);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (!canCompress()) {
            resetProgress();
            return;
        }

        this.progress++;
        setChanged(level, blockPos, blockState);

        if (this.progress >= MAX_PROGRESS) {
            compress();
            resetProgress();
        }
    }

    /** Coal is waiting and the finished block will have somewhere to go. */
    private boolean canCompress() {
        if (!itemHandler.getStackInSlot(INPUT_SLOT).is(Items.COAL_BLOCK)) {
            return false;
        }

        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        return output.isEmpty() || (output.is(result().getItem()) && output.getCount() < output.getMaxStackSize());
    }

    /** One block of coal is spent and one compressed block is added to whatever is already waiting. */
    private void compress() {
        if (!canCompress()) {
            return;
        }

        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result());
        } else {
            output.grow(1);
            itemHandler.setStackInSlot(OUTPUT_SLOT, output);
        }

        itemHandler.extractItem(INPUT_SLOT, 1, false);
        setChanged();
    }

    private static ItemStack result() {
        return new ItemStack(ModBlocks.byLevel(OUTPUT_LEVEL).get());
    }

    private void resetProgress() {
        this.progress = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("material_compressor.progress", progress);

        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("material_compressor.progress");
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
