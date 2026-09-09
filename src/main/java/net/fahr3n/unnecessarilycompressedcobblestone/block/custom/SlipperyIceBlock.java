package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Ice, but with almost nothing left to stand on.
 * <p>
 * Friction is the whole block, and it is a property rather than any code: vanilla's ice is 0.98 and
 * blue ice - the slipperiest thing in the game - is 0.989, which is the number this is measured
 * against. {@link #FRICTION} sits well past it. What friction actually means is how much of an
 * entity's horizontal momentum survives a tick, so the figure has to stay strictly below 1: at 1
 * nothing ever slows down again, and above it every step compounds into a speed nothing can stop.
 * <p>
 * It is {@link HalfTransparentBlock} rather than {@code IceBlock} on purpose. Ice melts in light,
 * turns to water when it is broken and is destroyed outright in the Nether, and none of those are
 * wanted from a block a TNT lays down by the thousand - so this is the transparent, slippery half
 * of ice with the melting left behind.
 */
public class SlipperyIceBlock extends HalfTransparentBlock {
    /**
     * How much horizontal momentum survives each tick. Blue ice keeps 0.989 of it; this keeps
     * 0.996, which is roughly three times as far to slide to a stop.
     */
    public static final float FRICTION = 0.996F;

    public SlipperyIceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
