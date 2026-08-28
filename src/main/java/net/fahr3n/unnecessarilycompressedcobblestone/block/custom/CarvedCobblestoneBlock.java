package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedGolemEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * A carved pumpkin in compressed stone: it faces the way it was placed, can be worn on the head,
 * and is the head of a golem. Placing one on top of the right stack of blocks builds a
 * {@link CompressedGolemEntity}, exactly as a carved pumpkin builds an iron golem.
 */
public class CarvedCobblestoneBlock extends HorizontalDirectionalBlock implements Equipable {
    public static final MapCodec<CarvedCobblestoneBlock> CODEC = simpleCodec(CarvedCobblestoneBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** The compression level this block is carved out of. */
    public static final int CARVED_FROM_LEVEL = 11;

    /** The compression level the golem's body is built out of. */
    public static final int GOLEM_BODY_LEVEL = 12;

    @Nullable
    private BlockPattern golemBase;
    @Nullable
    private BlockPattern golemFull;

    public CarvedCobblestoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<? extends CarvedCobblestoneBlock> codec() {
        return CODEC;
    }

    /** The head completes the golem, so the check runs when this is the block being placed. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            this.trySpawnGolem(level, pos);
        }
    }

    /** Whether a headless body is waiting here, mirroring {@link CarvedPumpkinBlock#canSpawnGolem}. */
    public boolean canSpawnGolem(LevelReader level, BlockPos pos) {
        return this.getOrCreateGolemBase().find(level, pos) != null;
    }

    private void trySpawnGolem(Level level, BlockPos pos) {
        BlockPattern.BlockPatternMatch match = this.getOrCreateGolemFull().find(level, pos);
        if (match == null) {
            return;
        }

        CompressedGolemEntity golem = ModEntities.COMPRESSED_GOLEM.get().create(level);
        if (golem == null) {
            return;
        }

        // Deliberately not marked player created: that flag stops an iron golem from ever
        // attacking a player, and this one is meant to turn on whoever built it. Persistence keeps
        // the boss around instead of letting it despawn once everybody walks away.
        golem.setPersistenceRequired();
        CarvedPumpkinBlock.clearPatternBlocks(level, match);

        BlockPos spawnPos = match.getBlock(1, 2, 0).getPos();
        golem.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(golem);

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
        }

        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Wearable on the head, the way a carved pumpkin is. */
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    /** The body without its head: the same T shape an iron golem is built in. */
    private BlockPattern getOrCreateGolemBase() {
        if (this.golemBase == null) {
            this.golemBase = BlockPatternBuilder.start()
                    .aisle("~ ~", "###", "~#~")
                    .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(body())))
                    .where('~', block -> block.getState().isAir())
                    .build();
        }

        return this.golemBase;
    }

    private BlockPattern getOrCreateGolemFull() {
        if (this.golemFull == null) {
            this.golemFull = BlockPatternBuilder.start()
                    .aisle("~^~", "###", "~#~")
                    .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(this)))
                    .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(body())))
                    .where('~', block -> block.getState().isAir())
                    .build();
        }

        return this.golemFull;
    }

    private static Block body() {
        return ModBlocks.byLevel(GOLEM_BODY_LEVEL).get();
    }
}
