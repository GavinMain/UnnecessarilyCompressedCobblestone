package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.EngravingItem;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.EngravingTableMenu;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The two boxes of the Engraving Table: a piece of gear on the left, an engraving on the right.
 * <p>
 * Neither operation is automatic. Putting a fitting engraving next to a piece of gear does nothing
 * until the player asks for it, because the two operations are exact opposites - an engraving that
 * applied itself the moment it appeared in the right slot could never be taken back off, since
 * taking one off is precisely how something appears in that slot. So the table has two buttons and
 * the menu drives them, and the block entity only knows how to do the work.
 */
public class EngravingTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GEAR_SLOT = 0;
    public static final int ENGRAVING_SLOT = 1;

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
                // Anything at all may be laid on the left: whether it can hold the engraving in the
                // right slot is the engraving's question, and it is asked when the button is pressed.
                case GEAR_SLOT -> true;
                case ENGRAVING_SLOT -> stack.getItem() instanceof EngravingItem;
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            // One piece of gear and one engraving; a stack of either would make the buttons ambiguous.
            return 1;
        }
    };

    public EngravingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ENGRAVING_TABLE_BE.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.engraving_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EngravingTableMenu(containerId, inventory, this);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    /** Whether the engraving on the right would go onto the gear on the left. */
    public boolean canEngrave() {
        Engraving engraving = EngravingItem.of(itemHandler.getStackInSlot(ENGRAVING_SLOT));

        return engraving != null && Engravings.canAdd(itemHandler.getStackInSlot(GEAR_SLOT), engraving);
    }

    /** Whether there is an engraving to take off, and somewhere to put it once it is off. */
    public boolean canRemove() {
        return !Engravings.get(itemHandler.getStackInSlot(GEAR_SLOT)).isEmpty()
                && itemHandler.getStackInSlot(ENGRAVING_SLOT).isEmpty();
    }

    /** Cuts the engraving into the gear and spends it. */
    public void engrave() {
        if (!canEngrave()) {
            return;
        }

        ItemStack gear = itemHandler.getStackInSlot(GEAR_SLOT);
        ItemStack engravingStack = itemHandler.getStackInSlot(ENGRAVING_SLOT);
        Engraving engraving = EngravingItem.of(engravingStack);
        Engravings.add(gear, engraving);

        // The Potion Engraving is the one that carries something of its own, and the gear's
        // engraving component is a bare list of constants with nowhere to put it - so the potion
        // rides along on the gear as ordinary potion contents, and comes back off with it below.
        PotionContents contents = engravingStack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            gear.set(DataComponents.POTION_CONTENTS, contents);
        }

        itemHandler.setStackInSlot(ENGRAVING_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    /**
     * Takes the last engraving back off the gear and lays it in the right slot as the item it was
     * crafted as. Nothing is lost either way round, which is the whole point of the system.
     */
    public void removeEngraving() {
        if (!canRemove()) {
            return;
        }

        ItemStack gear = itemHandler.getStackInSlot(GEAR_SLOT);
        Engraving removed = Engravings.removeLast(gear);
        if (removed == null) {
            return;
        }

        ItemStack engravingStack = new ItemStack(removed.item());

        // Whatever the engraving brought with it goes back onto it, so nothing is lost either way
        // round - which is the whole point of the system.
        PotionContents contents = gear.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            engravingStack.set(DataComponents.POTION_CONTENTS, contents);
            gear.remove(DataComponents.POTION_CONTENTS);
        }

        itemHandler.setStackInSlot(ENGRAVING_SLOT, engravingStack);
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
