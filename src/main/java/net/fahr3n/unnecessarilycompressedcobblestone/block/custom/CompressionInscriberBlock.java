package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.CompressionInscriberBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The table itself; everything it does lives in {@link CompressionInscriberBlockEntity}. */
public class CompressionInscriberBlock extends BaseEntityBlock {
    public static final MapCodec<CompressionInscriberBlock> CODEC = simpleCodec(CompressionInscriberBlock::new);

    public CompressionInscriberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CompressionInscriberBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Whatever is still inside falls out when the table is broken. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof CompressionInscriberBlockEntity inscriber) {
            inscriber.drops();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CompressionInscriberBlockEntity inscriber) {
            ((ServerPlayer) player).openMenu(inscriber, pos);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntities.COMPRESSION_INSCRIBER_BE.get(),
                (tickLevel, pos, tickState, blockEntity) -> blockEntity.tick(tickLevel, pos, tickState));
    }
}
