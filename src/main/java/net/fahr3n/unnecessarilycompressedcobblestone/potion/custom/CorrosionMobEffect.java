package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.util.Corrosion;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Corrosion, which is the one effect in this mod that deliberately does nothing.
 * <p>
 * Everything corrosion actually <em>does</em> is in {@link Corrosion}, because the thing that makes
 * it worth having is a thing a {@code MobEffect} cannot do: a second application starts a second
 * ten second clock rather than refreshing the first, and there is only ever one instance of an
 * effect on an entity. So this class is the half of it vanilla can carry - the name in the
 * inventory, the icon, the colour, the category that milk and the Potion of Cleansing read, the
 * gate that {@code MobEffectEvent.Applicable} lets an immunity close - and the clocks are kept
 * beside it.
 * <p>
 * It is a class of its own rather than a bare {@code MobEffect} only because that constructor is
 * protected. There is nothing to override: {@code shouldApplyEffectTickThisTick} already answers
 * false for every tick, which is exactly right.
 */
public class CorrosionMobEffect extends MobEffect {
    public CorrosionMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
