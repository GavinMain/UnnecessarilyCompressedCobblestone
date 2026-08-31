package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.LightningCoreBlockEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.ModBlockEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

/**
 * A pedestal for lightning. On its own it does nothing at all; loaded with a bolt and given a
 * redstone signal, it calls that bolt's lightning down on itself once a second for as long as the
 * signal lasts - so a button is a single strike and a lever is a storm.
 * <p>
 * What kind of lightning that is remains entirely the bolt's business: see {@link BoltItem}. The
 * core knows only that it is holding one, and how hard it is being powered - the strength of the
 * signal is handed to the bolt with every strike and is what decides how strong that strike is.
 */
public class LightningCoreBlock extends BaseEntityBlock {
    public static final MapCodec<LightningCoreBlock> CODEC = simpleCodec(LightningCoreBlock::new);

    public LightningCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightningCoreBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Loading it: a bolt in hand goes onto an empty core, one at a time. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Anything that is not a bolt, and any core that is already loaded, falls through to the
        // empty-handed path below, which is what takes a bolt back off.
        if (!(stack.getItem() instanceof BoltItem)
                || !(level.getBlockEntity(pos) instanceof LightningCoreBlockEntity core)
                || !core.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            core.setBolt(stack.copyWithCount(1));
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Unloading it: whatever is on the core goes back to whoever asked for it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof LightningCoreBlockEntity core) || core.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            ItemStack taken = core.removeBolt();

            // Into the inventory if there is room, and at their feet if there is not.
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }

            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The bolt falls off when the core is broken. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof LightningCoreBlockEntity core) {
            core.drops();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntities.LIGHTNING_CORE_BE.get(),
                (tickLevel, pos, tickState, blockEntity) -> blockEntity.tick(tickLevel, pos, tickState));
    }
}
