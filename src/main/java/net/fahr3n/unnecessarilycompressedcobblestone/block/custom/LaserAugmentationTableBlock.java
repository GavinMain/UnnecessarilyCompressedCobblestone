package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.LaserAugmentationTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The table itself; everything it does lives in {@link LaserAugmentationTableBlockEntity}. Like the
 * Engraving Table it has no ticker: fitting an augment is instant and happens on a button, so there
 * is nothing for it to work through between clicks.
 */
public class LaserAugmentationTableBlock extends BaseEntityBlock {
    public static final MapCodec<LaserAugmentationTableBlock> CODEC =
            simpleCodec(LaserAugmentationTableBlock::new);

    public LaserAugmentationTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new LaserAugmentationTableBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Whatever is still on the table falls off when it is broken. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof LaserAugmentationTableBlockEntity table) {
            table.drops();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof LaserAugmentationTableBlockEntity table) {
            ((ServerPlayer) player).openMenu(table, pos);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
