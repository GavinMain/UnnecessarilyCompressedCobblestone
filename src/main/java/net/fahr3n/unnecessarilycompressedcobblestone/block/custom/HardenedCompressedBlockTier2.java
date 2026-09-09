package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.util.HardenedMining;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The second floor. From {@code ModBlocks.HARDENED_LEVEL_TIER_2} up, the stone is closed even to
 * the pickaxe that opens every hardened level below it, and the only way through is a tool in
 * {@link ModTags.Items#HARDENED_MINING_TIER_2}.
 * <p>
 * That is the whole of the class, and it is deliberately the same shape as
 * {@link HardenedCompressedBlock}: the block's real hardness is still -1, which is what keeps every
 * other tool in the game out, and the one exception is a tag rather than an item - so another mod's
 * tool earns the right by joining it and nothing here has to change. A third floor, if one is ever
 * wanted, is a tag, a constant and a subclass of this shape and nothing else.
 */
public class HardenedCompressedBlockTier2 extends HardenedCompressedBlock {
    public HardenedCompressedBlockTier2(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return HardenedMining.destroyProgress(state, player, HardenedMining.EFFECTIVE_HARDNESS_TIER_2,
                ModTags.Items.HARDENED_MINING_TIER_2);
    }
}
