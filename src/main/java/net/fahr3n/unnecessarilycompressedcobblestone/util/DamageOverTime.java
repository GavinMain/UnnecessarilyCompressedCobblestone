package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.TickingDamage;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * How much damage a creature is still owed by the effects it is carrying, and how to take those
 * effects off it - which together are the whole of the Compression Bomb.
 * <p>
 * The bomb's trade is that it stops being a countdown and becomes a bill. Poison, Wither, corrosion
 * and this mod's five potion damages are all slow on purpose: what they cost is measured in the
 * seconds during which the target can still run, drink milk, or simply outlast them. This collapses
 * every one of them into a single hit landing now, which is worth far more against something about
 * to escape and worth nothing at all against something that was never going to survive the clock.
 * <p>
 * Two rules, and they are the two halves of the same question. <em>Which</em> effects count is the
 * {@code #ucc:damage_over_time} tag, because a {@code MobEffect} has no flag saying it hurts and a
 * datapack is the right place to answer a question the registry cannot. <em>What</em> each is worth
 * is asked of the effect: how many times it will still fire comes from vanilla's own
 * {@code shouldApplyEffectTickThisTick}, which is a pure function of the duration and the
 * amplifier, and what one firing costs comes from {@link TickingDamage} where the effect implements
 * it and from {@link #DEFAULT_DAMAGE} where it does not.
 * <p>
 * Corrosion is counted separately by {@link Corrosion#pending}, because its clocks are not in
 * {@code activeEffects} at all - see that class for why.
 */
public final class DamageOverTime {
    /**
     * What one firing of a tagged effect is worth when the effect does not say.
     * <p>
     * One point is Poison's figure and Wither's figure, which between them are what almost every
     * damage over time effect in the game is modelled on, so it is the right default rather than a
     * placeholder. An effect worth more than this implements {@link TickingDamage}.
     */
    public static final float DEFAULT_DAMAGE = 1.0F;

    /**
     * How far ahead the bill is read, in ticks - twenty minutes.
     * <p>
     * The count has to be a walk rather than arithmetic, because the only honest source for "will
     * this effect fire on that tick" is the effect's own answer, and an effect is free to answer
     * anything at all. Twenty minutes of ticks is a walk short enough to do for every effect on
     * every creature caught in one blast and long enough that nothing a player meets is cut short
     * by it: the longest brew in this mod is eight minutes, and a beacon's is far weaker than
     * anything in the tag. An effect longer than this is counted for its first twenty minutes and
     * loses the rest, which is a bomb underpaying rather than a bomb misfiring.
     */
    public static final int HORIZON = 20 * 60 * 20;

    private DamageOverTime() {
    }

    /**
     * Takes every damage over time effect off {@code entity} and returns what they would have been
     * worth if they had all played out.
     * <p>
     * The removal and the sum are one operation on purpose: the bomb has to pay for exactly what it
     * takes away, and a caller that could do one without the other would eventually do one without
     * the other. Corrosion's clocks go with them.
     * <p>
     * The holders are collected before anything is removed, because {@code removeEffect} writes to
     * the map {@code getActiveEffects} is reading.
     */
    public static float collapse(LivingEntity entity) {
        // Corrosion first, because it has to be read before anything is taken off: the clocks are
        // cleared by the effect being removed, and reading them afterwards would find nothing.
        // Gated on the same tag as everything else, so a pack that says corrosion is not a damage
        // over time effect gets a bomb that neither charges for it nor clears it.
        boolean corroding = ModMobEffects.CORROSION.is(ModTags.MobEffects.DAMAGE_OVER_TIME);
        float total = corroding ? Corrosion.pending(entity) : 0.0F;

        List<Holder<MobEffect>> collapsing = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (!instance.getEffect().is(ModTags.MobEffects.DAMAGE_OVER_TIME)) {
                continue;
            }

            collapsing.add(instance.getEffect());
            total += pending(instance);
        }

        for (Holder<MobEffect> effect : collapsing) {
            entity.removeEffect(effect);
        }

        if (corroding) {
            // Belt and braces: removing the effect above already clears the clocks through
            // MobEffectEvent.Remove, but only if the entity was carrying the instance at all - and
            // a clock can outlive it, since the instance holds the longest of them and expires
            // when that one does.
            Corrosion.clear(entity);
        }

        return total;
    }

    /**
     * What one effect instance is still going to cost: how many more times it fires, multiplied by
     * what one firing is worth.
     * <p>
     * The firing count is the effect's own answer, asked once per remaining tick the way
     * {@code MobEffectInstance#tick} asks it, so an effect whose cadence is anything at all - every
     * tick, every fortieth, a pattern of its own - is counted correctly and no interval has to be
     * guessed at from outside.
     */
    public static float pending(MobEffectInstance instance) {
        MobEffect effect = instance.getEffect().value();
        int amplifier = instance.getAmplifier();

        // An infinite effect is not a bill anybody can pay, so it is read as the horizon's worth.
        int remaining = instance.isInfiniteDuration() ? HORIZON : Math.min(instance.getDuration(), HORIZON);
        int firings = 0;

        // Counted down the way vanilla counts it: the duration on the instance is what this tick
        // was asked about, and it is decremented afterwards, so the ticks still to come are
        // `remaining` down to 1.
        for (int duration = remaining; duration > 0; duration--) {
            if (effect.shouldApplyEffectTickThisTick(duration, amplifier)) {
                firings++;
            }
        }

        float each = effect instanceof TickingDamage ticking
                ? ticking.damagePerApplication(amplifier)
                : DEFAULT_DAMAGE;

        return firings * each;
    }
}
