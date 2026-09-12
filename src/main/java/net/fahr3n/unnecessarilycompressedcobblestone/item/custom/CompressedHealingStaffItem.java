package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * A staff that throws a Splash Potion of Healing, and the one staff in the mod with no cast at all.
 * <p>
 * Every other staff here is a wind-up: hold the button, wait, let go, and the whole weapon is the
 * commitment that wait represents. This one is the opposite by design - a heal that has to be
 * charged for two seconds while the caster walks at a fifth speed is a heal that arrives after the
 * fight has decided itself. So it fires on the click and is priced by a lockout instead:
 * {@value #COOLDOWN_TICKS} ticks, half a minute, which is the longest cooldown on any item in the
 * mod and is what stops it being a bottomless supply of health.
 * <p>
 * That trade is what each of the three staff enchantments has to be answered against, and the
 * answers are not the same:
 * <ul>
 * <li><b>Silent Cast</b> does nothing, and cannot. It is a fraction off the wind-up, and there is no
 *     wind-up to take a fraction of - {@link #baseCastTicks()} is zero and nothing reads it. That is
 *     stated here rather than worked around, because a staff that took the enchantment and quietly
 *     ignored it would be worse than one that plainly has no use for it.
 * <li><b>Surge</b> is this staff's power, which is the healing, so it multiplies the inscribed half
 *     of it. It is worth nothing on an uninscribed staff for the honest reason that an uninscribed
 *     staff has no power of its own - what it throws is a vanilla Potion of Healing, and Surge has
 *     no business changing what a vanilla potion is worth.
 * <li><b>Multicast</b> is how many bottles leave the hand at once. On the other staves it is casts
 *     per second and here there is no second to count, so it becomes a spread rather than a rate:
 *     five bottles across a fight rather than one at whoever is nearest.
 * </ul>
 * What is thrown is a <em>real</em> {@link ThrownPotion}, not an area effect written here, and that
 * buys the whole of the block's behaviour for free: the arc, the glass breaking, the four block
 * splash with its falloff, the colour of the cloud, the fact that it can be thrown over a wall - and
 * above all that it hurts the undead, because Instant Health inverts on them. A healing staff that
 * doubles as a weapon against skeletons is a more interesting item than one that only heals, and
 * none of it is written here.
 * <p>
 * The inscribed bonus rides on the same bottle as a second custom effect. It cannot be folded into
 * the vanilla potion, because Instant Health's only dial is an amplifier that doubles and the bonus
 * is a point per digit - see {@link ModMobEffects#MENDING}, which is that effect with a linear dial.
 */
public class CompressedHealingStaffItem extends CompressedStaffItem {
    /** Half a minute between casts. The whole price of the staff, since the cast itself is free. */
    public static final int COOLDOWN_TICKS = 600;

    /**
     * What one bottle heals before anything is inscribed into it, in health points. Eight is four
     * hearts, twice a vanilla Potion of Healing - which is the least a tier 234 item can be worth
     * and still be worth carrying.
     */
    public static final float BASE_HEALING = 8.0F;

    /**
     * How much healing one digit of Compression Energy adds, in health points. Ten, which is five
     * hearts - so a staff inscribed to twenty digits throws a bottle worth over a hundred hearts, and
     * a fully inscribed one heals thousands. Anything less falls hopelessly behind the health the
     * inscribed armour hands out, which is what this is meant to refill.
     */
    public static final float HEALING_PER_DIGIT = 10.0F;

    /** Where the exact figure rides on the thrown bottle. See {@link #contents}. */
    public static final String HEALING_TAG = "unnecessarilycompressedcobblestone_mending";

    /**
     * The colour a Potion of Healing's cloud and glass are. It is stated here rather than taken from
     * the potion, because the bottle deliberately carries no potion at all - see {@link #contents}.
     */
    private static final int HEALING_COLOR = 0xF82423;

    /** Vertical aim, throw speed and inaccuracy: a splash potion's own three numbers. */
    private static final float THROW_PITCH_OFFSET = -20.0F;
    private static final float THROW_SPEED = 0.5F;
    private static final float THROW_INACCURACY = 1.0F;

    /** How far apart the bottles of a Multicast volley are aimed, in degrees of yaw. */
    private static final float MULTICAST_SPREAD_DEGREES = 12.0F;

    public CompressedHealingStaffItem(Properties properties) {
        super(properties);
    }

    /**
     * Zero, and nothing reads it. The abstract staff demands a figure and this staff has none: it
     * never calls {@code startUsingItem}, so there is no held tick count for a cast length to be
     * compared against. It is the reason Silent Cast is worth nothing here.
     */
    @Override
    public int baseCastTicks() {
        return 0;
    }

    /**
     * What one bottle heals, in health points: the base, plus a point per digit inscribed, all of it
     * multiplied by Surge.
     * <p>
     * Surge covers the base as well as the inscribed points, which is the same rule the Ray of
     * Laser's Damage augments follow: an upgrade written against a staff's power should be worth as
     * much on a bare staff as on a deep one.
     * <p>
     * Not clamped. It used to travel as the effect amplifier, which vanilla pins to 0-255, and that
     * capped every staff at 256 points however deep it was inscribed; the figure now rides on the
     * bottle itself instead.
     */
    public static float healing(ItemStack stack, Level level) {
        return Math.max(1.0F, (BASE_HEALING + CompressionEnergy.bonus(stack) * HEALING_PER_DIGIT)
                * surge(stack, level));
    }

    /**
     * Fired on the click rather than on a release. {@code use} is overridden outright instead of
     * calling {@code super}, which would start a hold this staff has nothing to do with.
     * <p>
     * The cooldown is checked as well as set, even though vanilla will not deliver a use for an item
     * on cooldown: that check lives in the client's own item-use path, and the server is where this
     * has to be true.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        if (level instanceof ServerLevel serverLevel) {
            release(serverLevel, player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** One volley: {@link CompressedStaffItem#multicast} bottles, fanned out around the aim. */
    @Override
    protected void release(ServerLevel level, Player player, ItemStack stack) {
        int bottles = multicast(stack, level);
        float healing = healing(stack, level);
        PotionContents contents = contents(healing);

        for (int i = 0; i < bottles; i++) {
            // Centred on where the player is looking: one bottle goes exactly there, and any
            // others fan evenly to either side of it.
            float yaw = player.getYRot()
                    + (i - (bottles - 1) / 2.0F) * MULTICAST_SPREAD_DEGREES;

            ThrownPotion bottle = new ThrownPotion(level, player);
            bottle.setItem(bottle(contents, healing));
            bottle.shootFromRotation(player, player.getXRot(), yaw,
                    THROW_PITCH_OFFSET, THROW_SPEED, THROW_INACCURACY);
            level.addFreshEntity(bottle);
        }
    }

    /**
     * What is in the bottle: no potion at all, the healing colour, and one Mending effect carrying
     * the whole figure.
     * <p>
     * It is deliberately <em>not</em> a Potion of Healing, and the reason is that the amount is the
     * point of the item. Instant Health's only dial is an amplifier that doubles, so a staff worth
     * eleven points of healing could not be written as one; carrying the vanilla potion beside a
     * custom effect would mean the vanilla four points were a floor the staff's own figure had to be
     * measured on top of, which is a second number for a player to hold in their head.
     * <p>
     * Nothing about that is visible. The splash particles, the cloud and the glass all read their
     * colour off {@code PotionContents#getColor}, which honours a stated colour exactly as it
     * honours a potion's own - so this is a Splash Potion of Healing in every respect a player can
     * see, and differs only in that the number behind it is the staff's rather than vanilla's.
     * {@link ModMobEffects#MENDING} keeps the one behaviour that mattered: the undead take it as
     * damage.
     * <p>
     * The amplifier is only the figure as far as 256 goes, since vanilla clamps it there; the exact
     * figure is written onto the bottle under {@link #HEALING_TAG}, and Mending reads it off the
     * bottle that splashed in preference to the amplifier.
     */
    private static PotionContents contents(float healing) {
        List<MobEffectInstance> effects = new ArrayList<>(1);
        // Duration 1 because the effect is instantaneous.
        effects.add(new MobEffectInstance(ModMobEffects.MENDING, 1,
                Math.min(255, Math.round(healing) - 1), false, true, true));

        return new PotionContents(Optional.empty(), Optional.of(HEALING_COLOR), effects);
    }

    /** The splash potion item the thrown entity carries and is drawn as, with the exact figure on it. */
    private static ItemStack bottle(PotionContents contents, float healing) {
        ItemStack bottle = new ItemStack(Items.SPLASH_POTION);
        bottle.set(DataComponents.POTION_CONTENTS, contents);
        CustomData.update(DataComponents.CUSTOM_DATA, bottle, tag -> tag.putFloat(HEALING_TAG, healing));
        return bottle;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                        "tooltip.unnecessarilycompressedcobblestone.compressed_healing_staff",
                        COOLDOWN_TICKS / 20)
                .withStyle(ChatFormatting.GRAY));
    }
}
