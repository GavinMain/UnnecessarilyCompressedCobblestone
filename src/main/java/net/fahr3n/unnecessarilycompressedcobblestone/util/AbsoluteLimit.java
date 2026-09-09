package net.fahr3n.unnecessarilycompressedcobblestone.util;

/**
 * "No single blow may be worth more than this" for a mob, and the one thing true damage does not
 * get past.
 * <p>
 * It is a different rule from {@link SelectiveImmunity} and the two are deliberately not the same
 * interface. That one is about <em>what kind</em> of attack a mob will answer, and a statement about
 * kinds is exactly what {@link ModTags.DamageTypes#TRUE_DAMAGE} is written to ignore: true damage is
 * the number landing whatever the target thinks about the attack that carried it. This is about
 * <em>how much</em>, and it is the ceiling on that number itself - so it applies to true damage,
 * to the dev sword's {@code Float.MAX_VALUE}, and to {@code /kill}'s, all of which are only ever
 * refused by a rule of this shape.
 * <p>
 * The ceiling is stated twice, and both halves are needed. A mob overrides {@code hurt} and refuses
 * a blow this large outright, which is the version that keeps the hit from happening at all; and
 * {@code ModEvents.onAbsoluteLimit} clips the final figure at {@code LivingDamageEvent.Pre} and
 * {@code EventPriority.LOWEST}, because a blow can be <em>raised</em> after {@code hurt} has judged
 * it and the dev sword's whole trick is doing exactly that. The second can only take a hit to
 * nothing, so it lands, flashes and knocks the mob about while costing it not one point.
 * <p>
 * A mob wanting this needs only the constant and the two lines that read it:
 * <pre>{@code
 * public float absoluteLimit() { return 1000.0F; }
 *
 * @Override
 * public boolean hurt(DamageSource source, float amount) {
 *     if (exceedsAbsoluteLimit(amount)) {
 *         refuseAbsoluteLimit();
 *         return false;
 *     }
 *     return super.hurt(source, amount);
 * }
 * }</pre>
 */
public interface AbsoluteLimit {
    /** The size of blow this refuses. A hit worth this much or more lands for nothing at all. */
    float absoluteLimit();

    /**
     * Whether a blow of this size is refused.
     * <p>
     * Only the figure as it stands is measured, and a reduction the mob applies on the way in needs
     * no second test: what lands is never larger than what arrived, so a hit that passes this before
     * the reduction cannot fail it afterwards.
     */
    default boolean exceedsAbsoluteLimit(float amount) {
        return amount >= absoluteLimit();
    }

    /**
     * What a refused blow looks and sounds like. Nothing by default; a mob that wants the hit to
     * read as turned away rather than as missed says so here.
     */
    default void refuseAbsoluteLimit() {
    }
}
