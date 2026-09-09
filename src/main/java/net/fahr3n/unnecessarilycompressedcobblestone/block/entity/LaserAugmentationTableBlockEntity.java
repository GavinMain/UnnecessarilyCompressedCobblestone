package net.fahr3n.unnecessarilycompressedcobblestone.block.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.AugmentItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RayOfLaserItem;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.custom.LaserAugmentationTableMenu;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augments;
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
 * The two boxes of the Laser Augmentation Table: the Ray of Laser on the left, and a box for one
 * augment beneath it.
 * <p>
 * It is the Engraving Table's shape with one difference, and the difference is the screen rather
 * than this class: an engraving comes off last first, because that is the only ordering a player
 * ever sees, whereas the augments fitted to a laser are all listed at once and any one of them may
 * be picked out. So {@link #removeAugment} takes the augment to remove rather than popping the end
 * of the list, and the menu is what says which one the player chose.
 * <p>
 * Neither operation is automatic, for the reason the Engraving Table gives: applying and removing
 * are exact opposites through the same box, so an augment that fitted itself on sight could never be
 * taken off - taking one off is precisely how something appears in that box.
 */
public class LaserAugmentationTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LASER_SLOT = 0;
    public static final int AUGMENT_SLOT = 1;

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
                // Only a laser on the left, unlike the Engraving Table, which takes anything and
                // asks the engraving about it: every augment fits exactly one item, so a box that
                // took a helmet would only ever be a box with a helmet stuck in it.
                case LASER_SLOT -> stack.getItem() instanceof RayOfLaserItem;
                case AUGMENT_SLOT -> stack.getItem() instanceof AugmentItem;
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            // One laser and one augment; a stack of either would make the buttons ambiguous.
            return 1;
        }
    };

    public LaserAugmentationTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LASER_AUGMENTATION_TABLE_BE.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.unnecessarilycompressedcobblestone.laser_augmentation_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LaserAugmentationTableMenu(containerId, inventory, this);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    /** The laser on the table, which is what the screen lists the augments of. Possibly empty. */
    public ItemStack laser() {
        return itemHandler.getStackInSlot(LASER_SLOT);
    }

    /** Everything fitted to the laser on the table, in the order it went on. Never null. */
    public List<Augment> fitted() {
        return Augments.get(laser());
    }

    /** Whether the augment in the box would go onto the laser on the table. */
    public boolean canApply() {
        Augment augment = AugmentItem.of(itemHandler.getStackInSlot(AUGMENT_SLOT));

        return augment != null && Augments.canAdd(laser(), augment);
    }

    /** Whether {@code augment} is on the laser, and whether there is somewhere to put it once off. */
    public boolean canRemove(@Nullable Augment augment) {
        return augment != null && Augments.has(laser(), augment)
                && itemHandler.getStackInSlot(AUGMENT_SLOT).isEmpty();
    }

    /** Fits the augment to the laser and spends it. */
    public void apply() {
        if (!canApply()) {
            return;
        }

        Augment augment = AugmentItem.of(itemHandler.getStackInSlot(AUGMENT_SLOT));
        Augments.add(laser(), augment);

        itemHandler.setStackInSlot(AUGMENT_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    /**
     * Takes {@code augment} back off the laser and lays it in the box as the item it was crafted as.
     * Nothing is lost either way round, which is the whole point of the system.
     */
    public void removeAugment(@Nullable Augment augment) {
        if (!canRemove(augment) || !Augments.remove(laser(), augment)) {
            return;
        }

        itemHandler.setStackInSlot(AUGMENT_SLOT, new ItemStack(augment.item()));
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

    /**
     * The whole table, sent to whoever is looking at it. The screen lists the augments fitted to the
     * laser, so the client needs the laser stack itself rather than a summary of it - which it gets
     * here for free, the stack carrying its own augment component wherever it goes.
     */
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
