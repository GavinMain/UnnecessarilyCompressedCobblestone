package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.CompressionInscriberBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CompressionInscriberMenu extends AbstractContainerMenu {
    /** Width of the progress arrow on the screen, in pixels. */
    private static final int ARROW_WIDTH = 24;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int INSCRIBER_SLOT_COUNT = 2;
    private static final int INSCRIBER_FIRST_SLOT = VANILLA_SLOT_COUNT;

    public final CompressionInscriberBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public CompressionInscriberMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public CompressionInscriberMenu(int containerId, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.COMPRESSION_INSCRIBER_MENU.get(), containerId);
        this.blockEntity = (CompressionInscriberBlockEntity) entity;
        this.level = inventory.player.level();
        this.data = data;

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        // Cobblestone on the left, the gear it feeds on the right.
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, CompressionInscriberBlockEntity.INPUT_SLOT, 44, 35));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, CompressionInscriberBlockEntity.GEAR_SLOT, 116, 35));

        addDataSlots(data);
    }

    public boolean isInscribing() {
        return data.get(0) > 0;
    }

    /** How much of the arrow to draw, in pixels. */
    public int getScaledArrowProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);

        return maxProgress != 0 && progress != 0 ? progress * ARROW_WIDTH / maxProgress : 0;
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
            if (!moveItemStackTo(sourceStack, INSCRIBER_FIRST_SLOT, INSCRIBER_FIRST_SLOT + INSCRIBER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < INSCRIBER_FIRST_SLOT + INSCRIBER_SLOT_COUNT) {
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.COMPRESSION_INSCRIBER.get());
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
