package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

/**
 * Something a Lightning Core can be loaded with. A core on its own does nothing at all; what it
 * strikes with is whatever bolt is sitting on it, and every kind of bolt is a subclass here.
 * <p>
 * This is the whole contract between the two: a new bolt is a subclass, an item registration, a
 * recipe and a texture, and the core needs no change to fire it.
 * <p>
 * The core hands its redstone signal down with every strike. What a bolt makes of it is its own
 * business - the vanilla bolt turns it into how far the thunder carries - but it is always the same
 * shape of number, so {@link #strength(int)} is here rather than in any one bolt.
 */
public abstract class BoltItem extends Item {
    /** A full redstone signal, which is the strength every bolt is written against. */
    public static final int MAX_SIGNAL = 15;

    public BoltItem(Properties properties) {
        super(properties);
    }

    /**
     * Calls this bolt's lightning down on the core.
     *
     * @param core   the core's own position; the strike lands on the block above it
     * @param signal what is powering the core, 1 to {@link #MAX_SIGNAL}
     */
    public abstract void strike(ServerLevel level, BlockPos core, int signal);

    /** {@code signal} as a fraction of a full one: 0 for the weakest strike, 1 for the strongest. */
    protected static float strength(int signal) {
        return Mth.clamp(signal, 0, MAX_SIGNAL) / (float) MAX_SIGNAL;
    }
}
