package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.MaterialCompressorBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.MaterialCompressorBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The same two-slot-and-an-arrow layout the Compression Inscriber uses. */
public class MaterialCompressorMenu extends AbstractContainerMenu {
    /** Width of the progress arrow on the screen, in pixels. */
    private static final int ARROW_WIDTH = 24;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int COMPRESSOR_SLOT_COUNT = 2;
    private static final int COMPRESSOR_FIRST_SLOT = VANILLA_SLOT_COUNT;

    public final MaterialCompressorBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MaterialCompressorMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public MaterialCompressorMenu(int containerId, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MATERIAL_COMPRESSOR_MENU.get(), containerId);
        this.blockEntity = (MaterialCompressorBlockEntity) entity;
        this.level = inventory.player.level();
        this.data = data;

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        // Coal on the left, the cobblestone it turns into on the right.
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, MaterialCompressorBlockEntity.INPUT_SLOT, 44, 35));

        // The output is take-only: the handler already refuses to hold anything but the machine's
        // own product, and this stops a player dropping one back in by hand.
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, MaterialCompressorBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addDataSlots(data);
    }

    public boolean isCompressing() {
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
            // Only the input slot, so shift-clicking never stuffs anything into the output.
            if (!moveItemStackTo(sourceStack, COMPRESSOR_FIRST_SLOT, COMPRESSOR_FIRST_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < COMPRESSOR_FIRST_SLOT + COMPRESSOR_SLOT_COUNT) {
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

    /**
     * Any tier of the machine keeps the screen open, since they share this menu. Vanilla's helper
     * only takes one block, so the check is spelled out against the class instead.
     */
    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(level, blockEntity.getBlockPos()).evaluate(
                (containerLevel, pos) -> containerLevel.getBlockState(pos).getBlock() instanceof MaterialCompressorBlock
                        && player.canInteractWithBlock(pos, 4.0),
                true);
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
