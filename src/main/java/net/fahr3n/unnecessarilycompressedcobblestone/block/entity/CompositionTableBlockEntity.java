package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompositionBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.CompositionTableMenu;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Composition;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A sheet of music with a bolt beside it.
 * <p>
 * The sheet is {@link Composition#SHEET_SIZE} squares laid out left to right, and a square holds one
 * note bolt or one rest. There is no separate palette of notes to drag from: laying a note in a
 * square <em>is</em> writing it, and the player's own inventory is where the notes come from - so
 * the whole table is the row of squares and the bolt they are written onto.
 * <p>
 * Composing is a button rather than something that happens on its own, for the same reason the
 * Engraving Table's is: the notes are spent, and nothing should spend a player's notes because they
 * put one down to look at it.
 */
public class CompositionTableBlockEntity extends BlockEntity implements MenuProvider {
    /** The bolt being written to, and then the sheet. */
    public static final int BOLT_SLOT = 0;
    public static final int FIRST_SHEET_SLOT = 1;
    public static final int SLOT_COUNT = FIRST_SHEET_SLOT + Composition.SHEET_SIZE;

    public final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == BOLT_SLOT
                    ? stack.getItem() instanceof CompositionBoltItem
                    : Composition.codeOf(stack).isPresent();
        }

        @Override
        public int getSlotLimit(int slot) {
            // One item to a square: a square is a beat, and a stack of three notes in one is not
            // three beats, it is an ambiguity.
            return 1;
        }
    };

    public CompositionTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMPOSITION_TABLE_BE.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.composition_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CompositionTableMenu(containerId, inventory, this);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    /**
     * The sheet as it stands, read left to right. Empty squares are skipped rather than counted as
     * rests - a rest is an item a player had to craft, and a gap in the middle of a sheet is far
     * more often somewhere they have not finished writing.
     */
    public List<Integer> sheet() {
        List<Integer> codes = new ArrayList<>(Composition.SHEET_SIZE);
        for (int square = 0; square < Composition.SHEET_SIZE; square++) {
            OptionalInt code = Composition.codeOf(itemHandler.getStackInSlot(FIRST_SHEET_SLOT + square));
            if (code.isPresent()) {
                codes.add(code.getAsInt());
            }
        }

        return codes;
    }

    /** Whether there is a bolt to write on and something to write on it. */
    public boolean canCompose() {
        return itemHandler.getStackInSlot(BOLT_SLOT).getItem() instanceof CompositionBoltItem
                && !sheet().isEmpty();
    }

    /** Whether the bolt on the table has anything on it to rub out. */
    public boolean canErase() {
        return Composition.isWritten(itemHandler.getStackInSlot(BOLT_SLOT));
    }

    /**
     * Writes the sheet onto the bolt and spends every note in it.
     * <p>
     * A bolt that was already written is written over rather than refused: the sheet in front of the
     * player is what the bolt says afterwards, which is the only rule that is not surprising.
     */
    public void compose() {
        if (!canCompose()) {
            return;
        }

        Composition.set(itemHandler.getStackInSlot(BOLT_SLOT), sheet());
        for (int square = 0; square < Composition.SHEET_SIZE; square++) {
            itemHandler.setStackInSlot(FIRST_SHEET_SLOT + square, ItemStack.EMPTY);
        }

        setChanged();
    }

    /**
     * Rubs the bolt clean. It gives nothing back - the notes went into the writing and there is
     * nothing left of them to return - so this is a way to reuse the bolt, not to undo a mistake.
     */
    public void erase() {
        if (!canErase()) {
            return;
        }

        Composition.set(itemHandler.getStackInSlot(BOLT_SLOT), List.of());
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));

        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
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
