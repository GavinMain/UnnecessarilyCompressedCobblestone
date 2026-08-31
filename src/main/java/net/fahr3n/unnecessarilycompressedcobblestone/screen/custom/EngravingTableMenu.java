package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.EngravingTableBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EngravingTableMenu extends AbstractContainerMenu {
    /** The two buttons on the screen, as the ids {@link #clickMenuButton} is given. */
    public static final int ENGRAVE_BUTTON = 0;
    public static final int REMOVE_BUTTON = 1;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int TABLE_SLOT_COUNT = 2;
    private static final int TABLE_FIRST_SLOT = VANILLA_SLOT_COUNT;

    public final EngravingTableBlockEntity blockEntity;
    private final Level level;

    public EngravingTableMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public EngravingTableMenu(int containerId, Inventory inventory, BlockEntity entity) {
        super(ModMenuTypes.ENGRAVING_TABLE_MENU.get(), containerId);
        this.blockEntity = (EngravingTableBlockEntity) entity;
        this.level = inventory.player.level();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        // The gear on the left, the engraving on the right, in the inscriber's own two positions.
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, EngravingTableBlockEntity.GEAR_SLOT, 44, 35));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, EngravingTableBlockEntity.ENGRAVING_SLOT, 116, 35));
    }

    /**
     * The two buttons. Both are re-checked here rather than trusted from the screen: a client can
     * send either id at any time, whatever the buttons it was drawn look like.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) {
            return false;
        }

        switch (id) {
            case ENGRAVE_BUTTON -> blockEntity.engrave();
            case REMOVE_BUTTON -> blockEntity.removeEngraving();
            default -> {
                return false;
            }
        }

        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TABLE_FIRST_SLOT, TABLE_FIRST_SLOT + TABLE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TABLE_FIRST_SLOT + TABLE_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, 0, VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.ENGRAVING_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 142));
        }
    }
}
