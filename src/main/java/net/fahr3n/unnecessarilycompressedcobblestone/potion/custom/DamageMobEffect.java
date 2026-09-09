package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * An effect that simply hurts whoever has it, with a damage type of its own.
 * <p>
 * It is Poison and Wither's shape rather than Instant Damage's, and that is the whole reason these
 * potions have a redstone brew at all: an instantaneous effect has no duration for redstone to
 * stretch, so a damage potion built that way can only ever take glowstone. Ticking instead, both
 * modifiers mean something - redstone is how long it goes on and glowstone is how hard and how
 * often - and every one of these potions has the four forms a vanilla potion has.
 * <p>
 * What the damage <em>is</em> is a real {@link DamageType} in every case, never a raw number, so
 * armour, Protection, Resistance and anything another mod hangs off a damage type tag all apply
 * exactly as they would to the thing the potion is named after.
 */
public class DamageMobEffect extends MobEffect implements TickingDamage {
    /** How often a hit lands at amplifier 0; each level up halves it. */
    private static final int BASE_INTERVAL = 40;

    private final ResourceKey<DamageType> damageType;
    private final float damage;

    /**
     * @param damage what one hit is worth at level I. Each level doubles it, the way Instant Damage
     *               does, so a glowstone brew is twice the damage twice as often.
     */
    public DamageMobEffect(int color, ResourceKey<DamageType> damageType, float damage) {
        super(MobEffectCategory.HARMFUL, color);
        this.damageType = damageType;
        this.damage = damage;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().source(this.damageType), damageAt(amplifier));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = BASE_INTERVAL >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    /** What one hit is worth at this level. */
    protected float damageAt(int amplifier) {
        return this.damage * (1 << amplifier);
    }

    /**
     * The same number, offered to anything that wants to know what this effect is going to cost
     * without waiting for it - which today is the Compression Bomb. Every effect on this line is
     * exact here rather than an estimate: {@link #applyEffectTick} hurts for precisely this.
     */
    @Override
    public float damagePerApplication(int amplifier) {
        return damageAt(amplifier);
    }
}
