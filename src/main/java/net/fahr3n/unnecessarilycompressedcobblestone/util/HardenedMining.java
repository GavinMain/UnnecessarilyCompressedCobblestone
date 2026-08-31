package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The one rule shared by everything in this mod that is unbreakable: a hardness of -1 keeps every
 * tool out, and a tool in {@link ModTags.Items#HARDENED_MINING} is the single exception.
 * <p>
 * It lives here rather than in a block because the two blocks that use it - the deep compression
 * levels and the leaves of the tree they grow - have different vanilla parents and cannot share one.
 */
public final class HardenedMining {
    /**
     * The hardness these blocks behave as once something can mine them at all. It is not the real
     * hardness - that stays at -1, which is what keeps every other tool out - but the number the
     * progress is worked out against.
     */
    public static final float EFFECTIVE_HARDNESS = 50.0F;

    /** Vanilla's divisor for a tool that can harvest what it is breaking. */
    private static final float CORRECT_TOOL_DIVISOR = 30.0F;

    private HardenedMining() {
    }

    /**
     * How much of {@code state} a player breaks in one tick, against {@code hardness} rather than
     * the block's real one. Zero for anyone not holding the right tool, which is what makes the
     * block unbreakable to everything else.
     * <p>
     * {@code getDestroySpeed} carries the tool's own speed, whatever Compression Energy has been
     * inscribed into it, and everything the game does to mining speed afterwards - Efficiency,
     * Haste, Mining Fatigue, standing in water and being off the ground.
     */
    public static float destroyProgress(BlockState state, Player player, float hardness) {
        if (!player.getMainHandItem().is(ModTags.Items.HARDENED_MINING)) {
            return 0.0F;
        }

        return player.getDestroySpeed(state) / hardness / CORRECT_TOOL_DIVISOR;
    }
}
