package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BoltProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

/**
 * Something a Lightning Core can be loaded with. A core on its own does nothing at all; what it
 * strikes with is whatever bolt is sitting on it, and every kind of bolt is a subclass here.
 * <p>
 * This is the whole contract between the two: a new bolt is a subclass, an item registration, a
 * recipe and a texture, and the core needs no change to fire it.
 * <p>
 * It is also the contract with the Bolt Launcher, which fires a bolt as a projectile and calls the
 * same {@link #strike} down wherever it lands. That is the whole of "the lightning carries the
 * properties of the bolt": there is no second table of what each bolt means, because a bolt already
 * knows.
 * <p>
 * The core hands its redstone signal down with every strike. What a bolt makes of it is its own
 * business - the vanilla bolt turns it into how far the thunder carries - but it is always the same
 * shape of number, so {@link #strength(int)} is here rather than in any one bolt. The launcher
 * always hands down a full one: a shot bolt is as loud as it can be, whatever else is done to it.
 * <p>
 * Every bolt is also a {@link ProjectileItem}, which is what a dispenser asks of an arrow: it fires
 * the same {@link BoltProjectileEntity} the launcher does, so a dispensed bolt lands and strikes
 * exactly as a shot one. The behaviour is registered for every bolt in common setup.
 */
public abstract class BoltItem extends Item implements ProjectileItem {
    /** A full redstone signal, which is the strength every bolt is written against. */
    public static final int MAX_SIGNAL = 15;

    public BoltItem(Properties properties) {
        super(properties);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new BoltProjectileEntity(level, pos.x(), pos.y(), pos.z(), stack);
    }

    /**
     * Calls this bolt's lightning down.
     *
     * @param pos          where the lightning lands - the block above a core, or wherever a shot
     *                     bolt came to rest
     * @param stack        the stack that is striking. A bolt that carries something of its own -
     *                     the sheet on a Composition Bolt - reads it off here; the rest ignore it
     * @param signal       how hard it was called: what is powering the core, or a full
     *                     {@link #MAX_SIGNAL} out of the launcher
     * @param bonusDamage  added to whatever the lightning would have done, which is how the
     *                     Compression Inscriber and Power turn into a stronger strike. Bolts that
     *                     make no bolt of their own - a song is a hundred of them - ignore it.
     */
    public abstract void strike(ServerLevel level, BlockPos pos, ItemStack stack, int signal, float bonusDamage);

    /**
     * {@code signal} as a fraction of a full one: 0 for the weakest strike a core can call, 1 for
     * the strongest.
     * <p>
     * It is not clamped at the top, and that is deliberate. Redstone stops at 15 but the Bolt
     * Engraving hands down twice that, and a volume above 1 is a real thing in the sound engine -
     * the gain is already at its ceiling, so the rest becomes the distance the sound carries. A note
     * struck at 2 is heard from twice as far away.
     */
    protected static float strength(int signal) {
        return Math.max(0, signal) / (float) MAX_SIGNAL;
    }
}
