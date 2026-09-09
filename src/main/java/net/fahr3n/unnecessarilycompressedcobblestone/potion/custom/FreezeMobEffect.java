package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Powder snow in a bottle. It deals real {@code minecraft:freeze} damage - which is in
 * {@code #bypasses_armor}, so no amount of plate helps - and frosts the screen over while it lasts,
 * because a freeze potion that did not look cold would be a poison potion with a different number.
 */
public class FreezeMobEffect extends DamageMobEffect {
    /** How long the frost overlay is topped up by, in ticks. Vanilla's own full-frost mark is 140. */
    private static final int FROST_TICKS = 160;

    public FreezeMobEffect(int color, float damage) {
        super(color, DamageTypes.FREEZE, damage);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Topped up rather than set, so it thaws on its own once the potion runs out.
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), FROST_TICKS));
        return super.applyEffectTick(entity, amplifier);
    }
}
