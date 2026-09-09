package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.block.entity.TeleportationGateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * A doorway two blocks tall that anything can walk into and come out of somewhere else. Right
 * clicking either half opens the screen that names this gate and says which gate it points at; see
 * {@link TeleportationGateBlockEntity} for both, and {@code TeleporterNetwork} for how a name
 * becomes a place.
 * <p>
 * It is two halves of one block, laid down and taken up together the way a door or a tall flower is,
 * and only the lower half carries the block entity - the upper half is the same block with no data
 * of its own, and every interaction with it is passed down. That is why {@link #newBlockEntity}
 * answers only for the lower half: a second block entity on the upper one would be a second gate at
 * the same place, with its own name and its own destination.
 * <p>
 * The gate has no collision, so walking into it is what happens rather than walking against it, and
 * {@link #entityInside} is what catches that. It is also why the block cannot simply teleport
 * whatever it touches on sight: that method is called on every tick something is inside, so the
 * block entity holds a tick stamp per entity and treats an unbroken run of ticks as one visit.
 */
public class TeleportationGateBlock extends BaseEntityBlock {
    public static final MapCodec<TeleportationGateBlock> CODEC = simpleCodec(TeleportationGateBlock::new);

    /** Which half of the doorway this block is. Only {@code LOWER} has a block entity. */
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    /**
     * The way the gate faces, and so the side anything arriving here is put down on. It is set from
     * the placer the way a furnace's is, so a gate faces whoever built it.
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * The doorway itself, a hand's width thick. Nothing collides with it - this is the shape used
     * for picking it, for the outline and for {@code entityInside}, which tests the interaction
     * shape rather than the collision one.
     */
    private static final VoxelShape NORTH_SOUTH = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape EAST_WEST = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public TeleportationGateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    /**
     * Only the lower half has one. Returning null for the upper half is what makes a gate one gate
     * rather than two stacked on each other.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new TeleportationGateBlockEntity(pos, state)
                : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Nothing stands on it and nothing bumps into it; walking into a doorway is walking through it. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** The gate is not solid, so nothing may be built against it and nothing suffocates in it. */
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /**
     * A gate needs the block above it to be free, exactly as a door does. Returning null here is
     * what stops the item being spent on a place the second half cannot go.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** The two halves live and die together, the way a tall flower's do. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOtherHalf = direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP);

        if (towardsOtherHalf && (!neighbourState.is(this) || neighbourState.getValue(HALF) == half)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return true;
        }

        BlockState below = level.getBlockState(pos.below());
        // Called during placement, before this block is set, so anything that is not us yet is the
        // pre-check rather than a broken gate.
        return state.getBlock() != this || below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    /**
     * Takes the gate out of the network when it is actually broken. The block entity's own
     * {@code setRemoved} would be the obvious place, but that also runs when a chunk is merely
     * unloaded, and a gate that unnamed itself every time nobody was looking at it would be no gate
     * at all.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TeleportationGateBlockEntity gate) {
            gate.unbind();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Walking in is the whole of using it; the block entity decides whether this is a new visit.
     * <p>
     * Vanilla hands this method every block cell the entity's box touches rather than every shape it
     * actually meets, so the doorway itself is tested here - otherwise brushing past the outside of
     * a gate would be the same as walking through it.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!entity.getBoundingBox().intersects(getShape(state, level, pos, CollisionContext.of(entity))
                .bounds().move(pos))) {
            return;
        }

        if (level.getBlockEntity(base(state, pos)) instanceof TeleportationGateBlockEntity gate) {
            gate.accept(entity);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return open(state, level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return open(state, level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide())
                : InteractionResult.PASS;
    }

    /** Either half opens the same screen: the upper one is passed down to the lower one's data. */
    private boolean open(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return true;
        }

        if (level.getBlockEntity(base(state, pos)) instanceof TeleportationGateBlockEntity gate
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(gate, gate.getBlockPos());
            return true;
        }

        return false;
    }

    /** Where the block entity is: this block if it is the lower half, the one below it otherwise. */
    public static BlockPos base(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    /**
     * Where something arriving at the gate at {@code gatePos} is put down: the block in front of it,
     * on the side the gate faces, standing on the floor and looking away from the doorway.
     * <p>
     * The spot is checked for two blocks of room and falls back through the sides and then the gate
     * itself, because a gate built flush against a wall is not a broken gate - it is a gate you
     * arrive at slightly to one side of. The gate's own square is the last resort rather than the
     * first, since arriving inside the doorway is what the visit stamp exists to survive.
     */
    public static Vec3 landingSpot(Level level, BlockPos gatePos, Direction facing) {
        BlockPos[] candidates = {
                gatePos.relative(facing),
                gatePos.relative(facing).relative(facing.getClockWise()),
                gatePos.relative(facing).relative(facing.getCounterClockWise()),
                gatePos.relative(facing.getOpposite()),
                gatePos,
        };

        for (BlockPos candidate : candidates) {
            if (isClear(level, candidate)) {
                return candidate.getBottomCenter();
            }
        }

        return gatePos.getBottomCenter();
    }

    /** Two blocks of room to stand in, which is what anything walking through a doorway needs. */
    private static boolean isClear(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}
