package net.fahr3n.unnecessarilycompressedcobblestone.screen.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.TeleportationGateBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The gate's screen has no slots at all - it is two lines of text - so this menu carries none
 * either. It is a menu rather than a bare screen for one reason: vanilla already knows how to open
 * one, keep the client's copy of the block entity in step and close it when the player walks away,
 * and {@link #stillValid} is the check the packet that carries the two names leans on.
 */
public class TeleportationGateMenu extends AbstractContainerMenu {
    public final TeleportationGateBlockEntity blockEntity;
    private final Level level;

    public TeleportationGateMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public TeleportationGateMenu(int containerId, Inventory inventory, BlockEntity entity) {
        super(ModMenuTypes.TELEPORTATION_GATE_MENU.get(), containerId);
        this.blockEntity = (TeleportationGateBlockEntity) entity;
        this.level = inventory.player.level();
    }

    /** There is nothing to shift-click, so nothing moves. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player,
                ModBlocks.TELEPORTATION_GATE.get());
    }
}
