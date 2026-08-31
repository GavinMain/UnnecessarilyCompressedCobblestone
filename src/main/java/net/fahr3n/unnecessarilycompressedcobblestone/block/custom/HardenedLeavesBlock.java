package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.util.HardenedMining;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaves of compressed cobblestone: unbreakable by hand, by shears and by any tool but the one in
 * {@code #hardened_mining}, on the same rule as the deep compression levels below them.
 * <p>
 * Decay is untouched, and it is the difference between this and a wall. {@link LeavesBlock} drops a
 * leaf block's loot and removes itself on a random tick once its trunk is gone, and none of that
 * goes through breaking - so a canopy left standing over nothing still falls on its own, with its
 * saplings and apples, exactly as an oak's would.
 * <p>
 * Leaves are a quarter as tough as the stone: they are meant to be a chore to clear, not a wall.
 */
public class HardenedLeavesBlock extends LeavesBlock {
    /** A quarter of the stone's, so a canopy is a few seconds a block rather than eight. */
    private static final float EFFECTIVE_HARDNESS = HardenedMining.EFFECTIVE_HARDNESS / 4.0F;

    public HardenedLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return HardenedMining.destroyProgress(state, player, EFFECTIVE_HARDNESS);
    }
}
