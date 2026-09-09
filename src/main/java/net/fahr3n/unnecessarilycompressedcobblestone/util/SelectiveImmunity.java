package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * "This costs me nothing" for a mob, as distinct from "this never happened".
 * <p>
 * The obvious way to write a boss that only one thing can hurt is {@code isInvulnerableTo}, and it
 * is the wrong one. {@code LivingEntity#hurt} asks that question first and returns immediately on a
 * yes, so a refused blow is not a blow at all: no hurt animation, no knockback, no combat tracker
 * entry, no {@code AttackEntityEvent} follow-through and - the one that matters - no
 * {@code LivingDamageEvent}. Everything this mod hangs off landing a hit is hung off that event, so
 * a boss written that way is a boss that a sword passes straight through, and the Uppercut
 * engraving, which is the <em>intended answer</em> to the Compressed Snow Golem, could never fire on
 * it. The rule and its counterplay cancelled each other out.
 * <p>
 * So the rule is stated here instead. A mob implementing this is hit exactly like anything else -
 * it flashes, it is knocked back, every event fires, every engraving gets its turn - and the damage
 * is clipped to nothing on its way in, by the one handler in {@code ModEvents} that reads this.
 * <p>
 * Two kinds of damage are never refusable and neither is a matter of taste.
 * {@code #minecraft:bypasses_invulnerability} is {@code /kill} and the void, which have to work on
 * everything or a boss that got itself somewhere unreachable is unremovable. And
 * {@link ModTags.DamageTypes#TRUE_DAMAGE} is this mod's own answer to exactly this rule: a boss
 * refusing kinds of attack is making a statement about attacks, and true damage is the thing that is
 * not one - it is the number landing whatever the target thinks about it. Both are checked once, in
 * {@link #refusable}, so no implementation has to remember either.
 */
public interface SelectiveImmunity {
    /**
     * @return true when {@code source} may land on this and must then cost it nothing. Implementations
     *         state their own rule and begin it with {@link #refusable}.
     */
    boolean refusesDamageFrom(DamageSource source);

    /**
     * The same question asked of a blow whose size is now known, which is the form the handler
     * actually calls. It exists for rules that are about <em>how much</em> rather than about what
     * kind - the Compressed Dragon's second phase throws away any single hit worth a thousand or
     * more - and those override this and leave {@link #refusesDamageFrom} answering false.
     * <p>
     * {@code amount} is the figure as it stands after armour, Protection, Resistance and every other
     * handler have spoken, which is the only figure worth measuring a ceiling against: a rule about
     * the size of a blow that read the size before the reductions would be answering a different
     * question from the one it asks.
     */
    default boolean refusesDamage(DamageSource source, float amount) {
        return refusesDamageFrom(source);
    }

    /** Whether a rule of this kind is allowed to have an opinion about {@code source} at all. */
    static boolean refusable(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(ModTags.DamageTypes.TRUE_DAMAGE);
    }
}
