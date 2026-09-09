package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fire, held on for as long as the effect lasts.
 * <p>
 * It is the one of this mod's harmful effects that deals no damage of its own, and deliberately so:
 * vanilla already burns anything whose fire ticks are above zero, with the {@code minecraft:on_fire}
 * damage type, its own cadence and its own sound. So all this does is top the fire up - the same
 * shape {@link FreezeMobEffect} uses for frost - which means everything that already answers being
 * on fire answers this: Fire Resistance, Fire Protection, water, rain, a fire-immune mob and every
 * other mod's version of any of them. Setting a target alight and then hurting it again here would
 * be burning it twice, and only one of those two would be resistible.
 * <p>
 * The fire is topped up rather than set, so a target that was already burning harder than this keeps
 * the longer of the two, and so the last of it burns out on its own once the effect lapses.
 */
public class BurnMobEffect extends MobEffect {
    /**
     * How far ahead the fire is kept, in ticks. It has to outlast the gap between two applications
     * of the effect - which is every tick - and be short enough that the flames go out promptly once
     * the effect ends rather than adding a burn of their own on the end.
     */
    private static final int FIRE_TICKS = 40;

    public BurnMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), FIRE_TICKS));
        return true;
    }

    /** Every tick: the fire is a state to be held, not a hit to be landed. */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
