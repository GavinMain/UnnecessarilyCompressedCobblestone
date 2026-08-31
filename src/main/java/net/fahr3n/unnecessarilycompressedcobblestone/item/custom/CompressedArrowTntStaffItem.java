package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowTier;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Arrow Staff's louder cousin. It marks something the same way - one Super Compressed Arrow, and
 * whatever it hits is the target - but what falls afterwards is Arrow TNT rather than arrows.
 * <p>
 * The TNT lands within four blocks of the mark and always from above, and none of it touches the
 * ground: an Arrow TNT breaks nothing, so the whole of it lands on whoever was marked. That is what
 * Compression Energy buys here - not speed, as on the plain Arrow Staff, but the size of every blast
 * in the rain.
 */
public class CompressedArrowTntStaffItem extends CompressedStaffItem {
    /** Five seconds, before Silent Cast: the same wind-up as the staff it is built from. */
    public static final int CAST_TICKS = 100;

    /** What a dispenser fires an arrow at, which is what the marker travels at. */
    public static final float MARKER_VELOCITY = 1.1F;

    /** How long the rain falls for once something has been marked. */
    public static final int RAIN_TICKS = 100;

    /** How many fall a second before Multicast multiplies it. */
    public static final int RAIN_PER_SECOND = 2;

    /** How far from the mark a given one may land. Always above it, never further out than this. */
    public static final double RAIN_RADIUS = 4.0;

    /** One second, which is the unit Multicast and the rain are both counted in. */
    public static final int TICKS_PER_SECOND = 20;

    public CompressedArrowTntStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public int baseCastTicks() {
        return CAST_TICKS;
    }

    @Override
    protected void release(ServerLevel level, Player player, ItemStack stack) {
        int perSecond = RAIN_PER_SECOND * multicast(stack, level);

        CompressedArrowEntity arrow = new CompressedArrowEntity(CompressedArrowTier.SUPER, level, player,
                new ItemStack(CompressedArrowTier.SUPER.item().get()), stack);
        // The Spiral engraving is the one thing that changes what falls: Arrow Spiral TNT rather
        // than Arrow TNT, at the same rate and the same blast.
        arrow.markTntRain(perSecond * RAIN_TICKS / TICKS_PER_SECOND,
                Math.max(1, TICKS_PER_SECOND / perSecond), blastScale(stack, level),
                Engravings.has(stack, Engraving.SPIRAL));

        Vec3 look = player.getLookAngle();
        arrow.shoot(look.x, look.y, look.z, MARKER_VELOCITY, 0.0F);
        level.addFreshEntity(arrow);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /**
     * What every blast in the rain is worth: one to begin with, one more per digit of Compression
     * Energy inscribed into the staff, all of it multiplied by Surge. It is the same one-per-digit
     * every other piece of inscribed gear gets, spent on explosion damage rather than on a stat.
     */
    public static float blastScale(ItemStack stack, @Nullable Level level) {
        return (1.0F + CompressionEnergy.bonus(stack)) * surge(stack, level);
    }
}
