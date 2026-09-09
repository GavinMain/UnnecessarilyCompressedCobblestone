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
 * A staff that marks something and then buries it.
 * <p>
 * The cast throws a single Super Compressed Arrow, and that arrow does nothing special on its own -
 * what it does is find a target. Anything it hits has arrows fall on it for the next five seconds,
 * five a second, following it wherever it goes; a shot that hits nothing but ground is a shot wasted.
 * <p>
 * Of the three staff enchantments, Surge is the one that means speed here rather than damage - and on
 * an arrow speed is damage, since an arrow's hit is its base damage multiplied by how fast it was
 * travelling. Multicast multiplies the rain: five is five times the arrows a second.
 */
public class CompressedArrowStaffItem extends CompressedStaffItem {
    /** Five seconds, before Silent Cast. */
    public static final int CAST_TICKS = 100;

    /** What a dispenser fires an arrow at, which is what everything this staff throws travels at. */
    public static final float DISPENSER_VELOCITY = 1.1F;

    /** How long the rain falls for once something has been marked. */
    public static final int RAIN_TICKS = 100;

    /** How many arrows a second fall before Multicast multiplies it. */
    public static final int RAIN_PER_SECOND = 5;

    /** One second, which is the unit Multicast and the rain are both counted in. */
    public static final int TICKS_PER_SECOND = 20;

    public CompressedArrowStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public int baseCastTicks() {
        return CAST_TICKS;
    }

    /**
     * The marker shot. Everything the rain will need travels on the arrow itself, because the rain is
     * decided by what it hits and the arrow is the only thing that knows.
     */
    @Override
    protected void release(ServerLevel level, Player player, ItemStack stack) {
        float velocity = velocity(stack, level);
        int perSecond = RAIN_PER_SECOND * multicast(stack, level);

        CompressedArrowEntity arrow = new CompressedArrowEntity(CompressedArrowTier.SUPER, level, player,
                new ItemStack(CompressedArrowTier.SUPER.item().get()), stack);
        // The Arrow 2 engraving is the one thing that changes what falls: the same rain at the same
        // rate, with a random harmful effect on every arrow in it.
        arrow.markRain(perSecond * RAIN_TICKS / TICKS_PER_SECOND,
                Math.max(1, TICKS_PER_SECOND / perSecond), velocity,
                Engravings.has(stack, Engraving.ARROW_2));

        Vec3 look = player.getLookAngle();
        arrow.shoot(look.x, look.y, look.z, velocity, 0.0F);
        level.addFreshEntity(arrow);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /**
     * A dispenser's own speed, plus one per digit of Compression Energy inscribed into the staff - the
     * same trade the bow makes - all of it then multiplied by Surge.
     */
    public static float velocity(ItemStack stack, @Nullable Level level) {
        return (DISPENSER_VELOCITY + CompressionEnergy.bonus(stack)) * surge(stack, level);
    }
}
