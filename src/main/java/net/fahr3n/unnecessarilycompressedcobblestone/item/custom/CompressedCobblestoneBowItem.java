package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/**
 * A bow that takes four times as long to draw and shoots three times as fast. Everything else -
 * the ammo it accepts, its enchantments, the way it is drawn and released - is vanilla bow
 * behaviour, so anything that works on a bow works on this.
 * <p>
 * Draw time and velocity both live on the weapon, not on the arrow: {@code releaseUsing} turns how
 * long the bow was held into a 0 to 1 charge and hands that to the projectile as its speed. Since
 * an arrow's damage is its base damage multiplied by how fast it is travelling when it lands, the
 * velocity here is a damage multiplier as much as it is a range one.
 */
public class CompressedCobblestoneBowItem extends BowItem {
    /** Ticks to a full draw, against {@link BowItem#MAX_DRAW_DURATION} for a vanilla bow. */
    public static final int DRAW_DURATION = BowItem.MAX_DRAW_DURATION * 4;

    /** Arrow speed at a full draw, against the 3.0 a vanilla bow manages. */
    public static final float MAX_VELOCITY = 3.0F * 3.0F;

    public CompressedCobblestoneBowItem(Properties properties) {
        super(properties);
    }

    /** Vanilla's {@code releaseUsing}, with the draw stretched out and the velocity scaled up. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) {
            return;
        }

        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty()) {
            return;
        }

        int charge = EventHooks.onArrowLoose(stack, level, player, this.getUseDuration(stack, entityLiving) - timeLeft, true);
        if (charge < 0) {
            return;
        }

        float power = getPowerForTime(charge);
        if (power < 0.1F) {
            return;
        }

        List<ItemStack> projectiles = draw(stack, ammo, player);
        if (level instanceof ServerLevel serverLevel && !projectiles.isEmpty()) {
            // Compression Energy buys velocity, which is what an arrow's damage is multiplied by.
            float velocity = MAX_VELOCITY + CompressionEnergy.bonus(stack);
            this.shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles,
                    power * velocity, 1.0F, power == 1.0F, null);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * The same curve {@link BowItem#getPowerForTime} uses, spread over {@link #DRAW_DURATION}
     * instead of 20 ticks. A full draw still returns exactly 1.0, which is what marks the shot
     * as a critical one.
     */
    public static float getPowerForTime(int charge) {
        float f = (float) charge / (float) DRAW_DURATION;
        f = (f * f + f * 2.0F) / 3.0F;

        return Math.min(f, 1.0F);
    }
}
