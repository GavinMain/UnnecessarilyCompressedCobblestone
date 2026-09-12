package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BoltProjectileEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/**
 * A bow that shoots lightning. Everything about drawing and loosing it is a bow's - the draw curve,
 * the sound, the ammo search, the enchantments - and the one thing that differs is what comes off
 * the string: a bolt, which flies as an arrow does and then calls its own lightning down wherever
 * it lands.
 * <p>
 * Because the projectile is only a delivery, none of the damage is on it. A bolt does what its own
 * {@link BoltItem#strike} does, and what the launcher adds is how hard: a point of lightning damage
 * per digit of Compression Energy poured into it, and {@value #DAMAGE_PER_POWER} more per level of
 * Power. That is the one place this differs from the mod's other ranged gear, which turns its energy
 * into velocity - a faster bolt does not hit harder, since the lightning is the hit.
 * <p>
 * Its ammunition is {@code #ucc:bolts} rather than a class check, so a datapack can add another
 * mod's item to the family; making the lightning is still a {@link BoltItem}'s job, so anything in
 * that tag which is not one is loaded and flies but lands as nothing.
 * <p>
 * The Bolt Engraving doubles both halves of what it hands the bolt: the damage, and the signal -
 * which is the volume. Twice a full signal is not a thing redstone can produce and is not meant to
 * be; above 1 the sound engine has no gain left to give and spends the rest on distance, so a
 * doubled note is heard twice as far off rather than twice as loudly.
 */
public class BoltLauncherItem extends BowItem {
    /** Anything the Lightning Core would take, this fires. */
    public static final Predicate<ItemStack> BOLTS = stack -> stack.is(ModTags.Items.BOLTS);

    /** How much lightning damage a level of Power is worth, against vanilla's 25% per level on an arrow. */
    public static final float DAMAGE_PER_POWER = 1.0F;

    /** A bolt is heavier than an arrow and leaves the string more slowly. */
    public static final float MAX_VELOCITY = 2.0F;

    /** What the Bolt Engraving multiplies the strike's damage and its volume by. */
    public static final float ENGRAVED_MULTIPLIER = 2.0F;

    public BoltLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return BOLTS;
    }

    /**
     * What a creative player with no bolts fires. Left alone it is vanilla's arrow, which is not a
     * {@link BoltItem} and so flies and lands as nothing.
     */
    @Override
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack launcher) {
        return new ItemStack(ModItems.COMPRESSED_VANILLA_BOLT.get());
    }

    /**
     * Vanilla's {@code releaseUsing}, with the bolt's velocity in place of an arrow's and the
     * lightning damage worked out once and handed to every projectile the shot makes - Multishot and
     * anything else that turns one shot into several included.
     */
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
            this.shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles,
                    power * MAX_VELOCITY, 1.0F, power == 1.0F, null);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * The bolt itself, in flight. Vanilla's version of this casts the ammunition to an
     * {@code ArrowItem} and asks it to make an arrow, which is exactly what must not happen here.
     */
    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack launcher,
                                          ItemStack ammo, boolean crit) {
        BoltProjectileEntity projectile = new BoltProjectileEntity(level, shooter, ammo, launcher);
        projectile.setBonusDamage(bonusDamage(level, launcher));
        projectile.setSignal(signal(launcher));
        if (crit) {
            projectile.setCritArrow(true);
        }

        return projectile;
    }

    /**
     * What this launcher adds to whatever the bolt's own lightning does.
     * <p>
     * The engraving doubles the <em>whole</em> strike rather than only the launcher's share of it,
     * so the base damage every bolt carries is doubled here and then taken back off: what a bolt is
     * handed is always a bonus on top of {@link CompressedVanillaBoltItem#VANILLA_DAMAGE}, and that
     * is the one number both ends have to agree on.
     */
    public static float bonusDamage(Level level, ItemStack launcher) {
        float damage = CompressedVanillaBoltItem.VANILLA_DAMAGE
                + CompressionEnergy.bonus(launcher) + DAMAGE_PER_POWER * powerLevel(level, launcher);

        if (Engravings.has(launcher, Engraving.BOLT)) {
            damage *= ENGRAVED_MULTIPLIER;
        }

        return damage - CompressedVanillaBoltItem.VANILLA_DAMAGE;
    }

    /** How hard the shot calls its lightning down, which is what every bolt turns into volume. */
    public static int signal(ItemStack launcher) {
        return Engravings.has(launcher, Engraving.BOLT)
                ? (int) (BoltItem.MAX_SIGNAL * ENGRAVED_MULTIPLIER)
                : BoltItem.MAX_SIGNAL;
    }

    /**
     * Power, read off the launcher. Enchantments are a datapack registry, so they are only reachable
     * through a level - which is why this takes one and answers 0 without it rather than throwing.
     */
    private static int powerLevel(@Nullable Level level, ItemStack launcher) {
        if (level == null) {
            return 0;
        }

        Optional<Holder.Reference<Enchantment>> power =
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.POWER);

        return power.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, launcher)).orElse(0);
    }
}
