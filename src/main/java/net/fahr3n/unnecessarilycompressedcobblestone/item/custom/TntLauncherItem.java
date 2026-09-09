package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.TntProjectileEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/**
 * A bow that throws TNT. Everything about drawing and loosing it is a bow's - the draw curve, the
 * sound, the ammo search, the enchantments - and the one thing that differs is what comes off the
 * string: a block of TNT, which flies as an arrow does and is lit wherever it lands.
 * <p>
 * Its ammunition is anything whose block is in {@code #ucc:tnt}, which is the tag the Random TNT
 * already draws from - so every one of this mod's twenty-odd variants is a shell, vanilla's TNT is
 * a shell, and another mod's TNT becomes one the moment it is added to that tag from a datapack.
 * Nothing about what a given charge does lives here; see {@link TntProjectileEntity}, which lights
 * the block rather than running an effect, so a launched Village TNT builds a village and a launched
 * Succ TNT opens a singularity with no case for either.
 * <p>
 * Compression Energy buys velocity, the way it does on the Compressed Cobblestone Bow and unlike the
 * Bolt Launcher next to it. That is the honest answer here: the blast belongs to the ammunition and
 * was decided when it was crafted, so what an inscribed launcher can offer is a harder throw - range
 * on a weapon whose whole difficulty is the arc.
 */
public class TntLauncherItem extends BowItem {
    /**
     * What this fires: any block item whose block is in {@code #ucc:tnt}. Read off the block rather
     * than off an item list, so the tag stays the one place the family is written down.
     */
    public static final Predicate<ItemStack> TNT = stack ->
            stack.getItem() instanceof BlockItem block
                    && block.getBlock().defaultBlockState().is(ModTags.Blocks.TNT);

    /**
     * Shell speed at a full draw, against the 3.0 a vanilla bow manages. A block of TNT is heavy and
     * leaves the string slowly; with {@link TntProjectileEntity}'s gravity that is a lob of roughly
     * twenty blocks, which is far enough to be useful and near enough to be dangerous.
     */
    public static final float MAX_VELOCITY = 1.5F;

    public TntLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return TNT;
    }

    /**
     * What a creative player fires with an empty inventory. Vanilla's answer is an arrow, which this
     * weapon cannot use at all, so a creative launcher would otherwise be a bow that never went off.
     */
    @Override
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
        return new ItemStack(Items.TNT);
    }

    /** Vanilla's {@code releaseUsing}, with a shell's velocity in place of an arrow's. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) {
            return;
        }

        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty()) {
            return;
        }

        int charge = EventHooks.onArrowLoose(stack, level, player, getUseDuration(stack, entityLiving) - timeLeft, true);
        if (charge < 0) {
            return;
        }

        float power = getPowerForTime(charge);
        if (power < 0.1F) {
            return;
        }

        List<ItemStack> projectiles = draw(stack, ammo, player);
        if (level instanceof ServerLevel serverLevel && !projectiles.isEmpty()) {
            // Compression Energy buys velocity, which on a lob is range rather than damage.
            float velocity = MAX_VELOCITY + CompressionEnergy.bonus(stack);
            this.shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles,
                    power * velocity, 1.0F, power == 1.0F, null);
        }

        // A bowstring under a block of TNT: the arrow's own release, pitched down as far as it goes.
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.5F + power * 0.2F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * The shell itself, in flight. Vanilla's version of this casts the ammunition to an
     * {@code ArrowItem} and asks it to make an arrow, which is exactly what must not happen here.
     */
    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack launcher,
                                          ItemStack ammo, boolean crit) {
        return new TntProjectileEntity(level, shooter, ammo, launcher);
    }
}
