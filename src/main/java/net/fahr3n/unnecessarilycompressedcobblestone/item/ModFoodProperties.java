package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.List;
import java.util.Optional;

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

    /**
     * Slowness II for exactly as long as it grants Resistance III and Regeneration III: the weight
     * of the stone is the price of what it protects you from.
     * <p>
     * Built through the record rather than {@code FoodProperties.Builder} because the builder only
     * offers the default eat time and {@code fast()}, and this one needs to be much slower.
     */
    public static final FoodProperties COMPRESSED_COBBLESTONE_APPLE = new FoodProperties(
            4,
            FoodConstants.saturationByModifier(4, FoodConstants.FOOD_SATURATION_SUPERNATURAL),
            false,
            APPLE_EAT_SECONDS,
            Optional.empty(),
            List.of(
                    new FoodProperties.PossibleEffect(
                            () -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, APPLE_EFFECT_DURATION, 1), 1.0F),
                    new FoodProperties.PossibleEffect(
                            () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, APPLE_EFFECT_DURATION, 2), 1.0F),
                    new FoodProperties.PossibleEffect(
                            () -> new MobEffectInstance(MobEffects.REGENERATION, APPLE_EFFECT_DURATION, 2), 1.0F)));
}
