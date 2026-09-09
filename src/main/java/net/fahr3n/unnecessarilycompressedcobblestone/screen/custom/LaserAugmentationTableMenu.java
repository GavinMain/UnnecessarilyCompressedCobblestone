package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.LaserAugmentationTableBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
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

/**
 * The table's two boxes, and the buttons the screen works them with.
 * <p>
 * There is one Apply button and one Remove button per augment there could ever be, which is what
 * lets the player pick which one comes off. A button id is all {@code clickMenuButton} carries, so
 * removal is encoded as {@link #REMOVE_BUTTON_BASE} plus the augment's own ordinal rather than as an
 * index into the list the client happens to be drawing - the two could disagree if the laser were
 * pulled off the table between the click and the packet, and an ordinal cannot: it names the augment
 * outright, and the block entity checks that the laser really carries it before taking it off.
 */
public class LaserAugmentationTableMenu extends AbstractContainerMenu {
    /** Fit the augment in the box to the laser. */
    public static final int APPLY_BUTTON = 0;

    /** Take one named augment off: this plus the augment's ordinal. */
    public static final int REMOVE_BUTTON_BASE = 1;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int TABLE_SLOT_COUNT = 2;
    private static final int TABLE_FIRST_SLOT = VANILLA_SLOT_COUNT;

    /** Where the two boxes sit on the panel, which the screen's own layout is written around. */
    public static final int LASER_SLOT_X = 16;
    public static final int LASER_SLOT_Y = 22;
    public static final int AUGMENT_SLOT_X = 16;
    public static final int AUGMENT_SLOT_Y = 54;

    /** Where the player's own inventory sits, which is lower than usual: the panel is taller. */
    private static final int INVENTORY_Y = 104;
    private static final int HOTBAR_Y = 162;

    public final LaserAugmentationTableBlockEntity blockEntity;
    private final Level level;

    public LaserAugmentationTableMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public LaserAugmentationTableMenu(int containerId, Inventory inventory, BlockEntity entity) {
        super(ModMenuTypes.LASER_AUGMENTATION_TABLE_MENU.get(), containerId);
        this.blockEntity = (LaserAugmentationTableBlockEntity) entity;
        this.level = inventory.player.level();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.addSlot(new SlotItemHandler(blockEntity.itemHandler,
                LaserAugmentationTableBlockEntity.LASER_SLOT, LASER_SLOT_X, LASER_SLOT_Y));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler,
                LaserAugmentationTableBlockEntity.AUGMENT_SLOT, AUGMENT_SLOT_X, AUGMENT_SLOT_Y));
    }

    /**
     * Both operations, re-checked here rather than trusted from the screen: a client can send any id
     * at any time, whatever the buttons it was drawn look like, so the block entity's own
     * {@code canApply} and {@code canRemove} are the only things that decide.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) {
            return false;
        }

        if (id == APPLY_BUTTON) {
            blockEntity.apply();
        } else {
            int ordinal = id - REMOVE_BUTTON_BASE;
            Augment[] augments = Augment.values();
            if (ordinal < 0 || ordinal >= augments.length) {
                return false;
            }

            blockEntity.removeAugment(augments[ordinal]);
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player,
                ModBlocks.LASER_AUGMENTATION_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(playerInventory, slot, 8 + slot * 18, HOTBAR_Y));
        }
    }
}
