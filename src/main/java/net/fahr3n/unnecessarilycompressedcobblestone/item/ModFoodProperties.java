package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;

/** Holds the FoodProperties for this mod's edible items. */
public class ModFoodProperties {
    /** Eight seconds, against the 1.6 an ordinary food takes and the 0.8 of a fast one. */
    public static final float APPLE_EAT_SECONDS = 8.0F;

    /** How long each of the apple's effects lasts, in ticks. */
    public static final int APPLE_EFFECT_DURATION = 3 * 60 * 20;

    /** Tier 1: Slowness II, Resistance III, Regeneration III. */
    public static final FoodProperties TIER_1_COMPRESSED_COBBLESTONE_APPLE = apple(1);

    /** Tier 2: one rung less weight and one rung more of both boons - Slowness I, Resistance IV, Regeneration IV. */
    public static final FoodProperties TIER_2_COMPRESSED_COBBLESTONE_APPLE = apple(2);

    /** Tier 3: the weight is gone entirely, and both boons go one deeper again - Resistance V, Regeneration V. */
    public static final FoodProperties TIER_3_COMPRESSED_COBBLESTONE_APPLE = apple(3);

    /**
     * Two fifths of a second, which is half of vanilla's {@code fast()} and a twentieth of what the
     * three apples below this one take. It is the whole of what makes tier 4 a different item rather
     * than a fourth rung: the others are eaten before a fight, because eight seconds of standing
     * still is not something a fight allows, and this one is eaten during it.
     */
    public static final float TIER_4_APPLE_EAT_SECONDS = 0.4F;

    /**
     * Tier 4, where the ramp stops being a ramp.
     * <p>
     * Tiers 1 to 3 are one shape - Slowness paying for Resistance and Regeneration, the weight
     * lifting a level each time - and that shape ran out at tier 3, where the Slowness had to be
     * dropped from the list rather than clamped. So this one is not built by {@link #apple}: it is
     * four effects stated outright, and the trade it makes is a different one.
     * <p>
     * What it gives is Regeneration V, Resistance III, Strength V and Speed II, and what it costs is
     * that Resistance goes <em>backwards</em> - tier 3 hands out V and this hands out III. That is
     * the trade and it is deliberate: this is not the apple you eat to survive being hit, it is the
     * one you eat to hit back, and eaten in
     * {@value #TIER_4_APPLE_EAT_SECONDS} of a second it is the only one in the mod you can eat with
     * something already swinging at you.
     */
    public static final FoodProperties TIER_4_COMPRESSED_COBBLESTONE_APPLE = tier4Apple();

    /**
     * Resistance and Regeneration for exactly as long as the Slowness that pays for them: the weight
     * of the stone is the price of what it protects you from. Each tier lightens that weight by a
     * level and deepens both boons by one, so tier 1 is Slowness II with Resistance and Regeneration
     * III and tier 2 is Slowness I with both at IV.
     * <p>
     * Tier 3 is where the weight runs out: the amplifier would be negative, so the Slowness is left
     * off the apple altogether rather than clamped to none, and what is left is both boons at V for
     * three minutes with nothing to pay for them.
     * <p>
     * Built through the record rather than {@code FoodProperties.Builder} because the builder only
     * offers the default eat time and {@code fast()}, and these need to be much slower.
     *
     * @param tier 1 to 3; a tier past 3 would only repeat 3, since the weight cannot go below none
     */
    /** The four effects the tier 4 apple states outright, and the eat time that is its point. */
    private static FoodProperties tier4Apple() {
        return new FoodProperties(
                4,
                FoodConstants.saturationByModifier(4, FoodConstants.FOOD_SATURATION_SUPERNATURAL),
                false,
                TIER_4_APPLE_EAT_SECONDS,
                Optional.empty(),
                List.of(
                        effect(MobEffects.REGENERATION, 4),
                        effect(MobEffects.DAMAGE_RESISTANCE, 2),
                        effect(MobEffects.DAMAGE_BOOST, 4),
                        effect(MobEffects.MOVEMENT_SPEED, 1)));
    }

    /** One certain effect for the apples' three minutes. The numeral is one more than the field. */
    private static FoodProperties.PossibleEffect effect(Holder<MobEffect> effect, int amplifier) {
        return new FoodProperties.PossibleEffect(
                () -> new MobEffectInstance(effect, APPLE_EFFECT_DURATION, amplifier), 1.0F);
    }

    private static FoodProperties apple(int tier) {
        int slowness = 2 - tier;
        int boon = tier + 1;

        List<FoodProperties.PossibleEffect> effects = new ArrayList<>();
        if (slowness >= 0) {
            effects.add(new FoodProperties.PossibleEffect(
                    () -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, APPLE_EFFECT_DURATION,
                            slowness), 1.0F));
        }

        effects.add(new FoodProperties.PossibleEffect(
                () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, APPLE_EFFECT_DURATION, boon), 1.0F));
        effects.add(new FoodProperties.PossibleEffect(
                () -> new MobEffectInstance(MobEffects.REGENERATION, APPLE_EFFECT_DURATION, boon), 1.0F));

        return new FoodProperties(
                4,
                FoodConstants.saturationByModifier(4, FoodConstants.FOOD_SATURATION_SUPERNATURAL),
                false,
                APPLE_EAT_SECONDS,
                Optional.empty(),
                List.copyOf(effects));
    }
}
