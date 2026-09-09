package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Ghasted, which does nothing at all here.
 * <p>
 * That is deliberate and is the same shape Corrosion uses. What the effect actually <em>is</em> is a
 * flock of {@code MiniGhastEntity}, one per level, each floating around the holder on a clock of its
 * own - and none of that can live in a {@code MobEffect}, because an effect ticks once for the
 * holder and the whole point of the flock is that its members are independent of each other. So
 * {@code Ghasted} owns the flock and this class owns nothing.
 * <p>
 * It is still registered, and it still has to be, because it is what everything else in the game
 * talks to: it is what a fire charge applies, what shows in the inventory and counts down there,
 * what milk and a bucket of Cleansing take off, what {@code MobEffectEvent.Applicable} lets an
 * immunity refuse, and what {@code Ghasted} reads every tick to decide how many ghasts the holder
 * should have. Take the effect off and the flock is gone within the tick.
 * <p>
 * A {@code MobEffect} constructor is protected, so even an effect that deliberately does nothing
 * needs a subclass to exist at all.
 */
public class GhastedMobEffect extends MobEffect {
    public GhastedMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
