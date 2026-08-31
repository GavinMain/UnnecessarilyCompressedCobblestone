package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * What every staff in this mod has in common: hold right click to cast, and the cast either completes
 * or it does not. Letting go early does nothing at all - there is no weak version of the shot, which
 * is what makes the wind-up a real commitment, since the vanilla use-item slowdown has the caster at a
 * fifth of their walking speed for the whole of it. Once the cast is full the staff changes to its
 * charged texture and stays there for as long as the button is held, so releasing on a target of your
 * choosing is part of the weapon rather than a race against a timer.
 * <p>
 * The three staff enchantments live here too, because all of them mean the same thing to every staff:
 * <ul>
 * <li>{@code Silent Cast} takes a tenth off the wind-up per level, to a floor of thirty percent.
 * <li>{@code Surge} multiplies whatever a staff's power actually is - lightning damage for one,
 *     arrow speed for the other - by a tenth per level, compounded.
 * <li>{@code Multicast} is how many times a cast goes off in a second. It is a multiplier on the
 *     ability's own rate, so five is five bolts a second from one staff and five times the rain from
 *     the other.
 * </ul>
 * A subclass supplies only the length of its wind-up and what happens when it is released.
 */
public abstract class CompressedStaffItem extends Item {
    /** Surge multiplies rather than adds: each level is another tenth of the running total. */
    public static final float SURGE_MULTIPLIER = 1.1F;

    /** A tenth off the cast per level of Silent Cast. */
    public static final float SILENT_CAST_PER_LEVEL = 0.1F;

    /**
     * However deep Silent Cast goes, a cast is never shorter than this fraction of its own. Seven
     * levels is the deepest book there is, and seven tenths off is exactly this floor, so nothing is
     * ever clamped away - the floor is only here so a deeper level than that could not reach zero,
     * which would let a staff fire on the tick it started.
     */
    public static final float MIN_CAST_FRACTION = 0.3F;

    /** Held indefinitely, the way a bow is; the cast is measured from how long it has been held. */
    private static final int USE_DURATION = 72000;

    protected CompressedStaffItem(Properties properties) {
        super(properties);
    }

    /** How long this staff takes to cast before Silent Cast is counted, in ticks. */
    public abstract int baseCastTicks();

    /** What the staff actually does. Only ever called on a full cast, and only on the server. */
    protected abstract void release(ServerLevel level, Player player, ItemStack stack);

    /**
     * How long this staff takes to cast, Silent Cast included. Both sides need it - the server to
     * decide whether a release counts, the client to know when to swap to the charged texture - so it
     * takes the level it is asked about rather than assuming one.
     */
    public int castTicks(ItemStack stack, @Nullable Level level) {
        int silentCast = enchantmentLevel(stack, level, ModEnchantments.SILENT_CAST);
        float fraction = Math.max(MIN_CAST_FRACTION, 1.0F - silentCast * SILENT_CAST_PER_LEVEL);

        return Math.max(1, Math.round(baseCastTicks() * fraction));
    }

    /** Whether {@code stack} has been held long enough to be ready to release. */
    public static boolean isCharged(ItemStack stack, @Nullable Level level, @Nullable LivingEntity holder) {
        if (holder == null || !holder.isUsingItem() || holder.getUseItem() != stack
                || !(stack.getItem() instanceof CompressedStaffItem staff)) {
            return false;
        }

        return stack.getUseDuration(holder) - holder.getUseItemRemainingTicks() >= staff.castTicks(stack, level);
    }

    /** What Surge is worth on this staff: a tenth more per level, compounded. */
    public static float surge(ItemStack stack, @Nullable Level level) {
        return (float) Math.pow(SURGE_MULTIPLIER, enchantmentLevel(stack, level, ModEnchantments.SURGE));
    }

    /**
     * How many times a second the cast goes off. One without the enchantment, and the level itself
     * with it - so Multicast V is five a second, which is what makes it worth its books.
     */
    public static int multicast(ItemStack stack, @Nullable Level level) {
        return Math.max(1, enchantmentLevel(stack, level, ModEnchantments.MULTICAST));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // BOW is what puts the caster at a fifth of their movement speed while the staff is held.
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /** Sparks around the caster while the cast runs, and a crack the moment it comes up full. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int held = getUseDuration(stack, entity) - remainingUseTicks;
        int cast = castTicks(stack, level);
        if (held > cast) {
            return;
        }

        int count = 1 + held * 4 / cast;
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getEyeY(), entity.getZ(),
                count, 0.5, 0.5, 0.5, 0.05);

        if (held == cast) {
            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TRIDENT_THUNDER,
                    SoundSource.PLAYERS, 1.0F, 1.6F);
        }
    }

    /** A full cast fires; anything short of one is simply dropped. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int held = getUseDuration(stack, entity) - timeLeft;
        if (held < castTicks(stack, level)) {
            // Nothing happens, and nothing is spent. The cast simply never finished.
            return;
        }

        release(serverLevel, player, stack);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * The level of one of the staff enchantments. They are datapack entries, so they can only be
     * reached through the level's registries - which is why nearly everything here takes a level.
     */
    protected static int enchantmentLevel(ItemStack stack, @Nullable Level level, ResourceKey<Enchantment> key) {
        if (level == null) {
            return 0;
        }

        Optional<Holder.Reference<Enchantment>> holder =
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key);

        return holder.map(reference -> EnchantmentHelper.getItemEnchantmentLevel(reference, stack)).orElse(0);
    }

    /**
     * Enchantable at a table. A plain {@link Item} has an enchantment value of zero, which is what
     * would otherwise keep a staff off the table entirely; nine is what the mod's armour and tools use.
     */
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 9;
    }

    /**
     * Never wears out, for the reason in {@link CompressedCobblestoneArmorItem}: the durability is
     * kept and simply never spent, because an item with the {@code unbreakable} component is not
     * damageable and so cannot be enchanted at a table at all.
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
