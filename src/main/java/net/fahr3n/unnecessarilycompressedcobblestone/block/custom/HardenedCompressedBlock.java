package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.util.HardenedMining;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compressed cobblestone that has been squeezed past the point where an ordinary tool can get back
 * into it. From {@code ModBlocks.HARDENED_LEVEL} up, the stone is bedrock as far as the rest of the
 * game is concerned - hardness of -1 and an explosion resistance nothing reaches - and the only way
 * through it is a tool in {@link ModTags.Items#HARDENED_MINING}.
 * <p>
 * That exception is this class's whole purpose. Vanilla's own
 * {@code BlockBehaviour#getDestroyProgress} gives up the moment hardness reads -1 and returns zero
 * progress forever; overriding it is what lets one tool through a block that is otherwise
 * unbreakable, and it is why these levels are a block of their own rather than the plain
 * {@code Block} every level below them is.
 */
public class HardenedCompressedBlock extends Block {
    public HardenedCompressedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // A bare Compressed Cobblestone Pickaxe takes some eight seconds a block here, and an
        // inscribed one is faster in proportion to the energy on it.
        return HardenedMining.destroyProgress(state, player, HardenedMining.EFFECTIVE_HARDNESS);
    }
}
