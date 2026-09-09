package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

/**
 * What one application of a ticking effect is worth, asked of the effect rather than tabled
 * somewhere else.
 * <p>
 * It exists for the Compression Bomb, which has to answer "how much damage is this effect still
 * going to do" without playing the effect out. Half of that question is already answerable through
 * vanilla: {@code MobEffect#shouldApplyEffectTickThisTick} is a pure function of the duration and
 * the amplifier, so how <em>many</em> more times an effect will fire can be counted by asking it,
 * for any mod's effect at all. The half vanilla cannot answer is how much each of those firings
 * costs, because a {@code MobEffect} has nowhere to say so - which is what this interface is.
 * <p>
 * It is deliberately not the only way in. {@code DamageOverTime} falls back on one point per
 * application for anything in {@code #ucc:damage_over_time} that does not implement this, because
 * one point is what vanilla's Poison and Wither are worth and is the overwhelmingly common figure;
 * an effect that hits harder than that and wants to be counted properly implements this instead of
 * being added to a table here.
 */
public interface TickingDamage {
    /**
     * What one firing of this effect costs its holder, before that holder's armour, Protection,
     * Resistance and everything else have had their say.
     * <p>
     * It is the figure the effect <em>intends</em>, not the figure that will land: the bomb's own
     * hit goes through a real damage type and is reduced in the ordinary way, so an estimate that
     * ignored mitigation twice would collapse to less than the effect it replaced.
     */
    float damagePerApplication(int amplifier);
}
