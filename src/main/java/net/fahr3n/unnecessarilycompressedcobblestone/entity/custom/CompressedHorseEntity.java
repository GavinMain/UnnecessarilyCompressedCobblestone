package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;

/**
 * The Compressed Horse. A horse in every line of its brain - the same wandering, the same rearing,
 * the same chest and saddle, the same breeding - and a horse that outruns anything that has ever
 * been ridden.
 * <p>
 * Nothing here touches the AI, for the same reason the {@link CompressedWolfEntity} does not: the
 * animal a player gets back for a Compressed Saddle behaves exactly like the horse they saddled, so
 * there is nothing to learn about it. All that changes is five attributes and the one vanilla method
 * that would quietly undo three of them.
 */
public class CompressedHorseEntity extends Horse {
    /**
     * Just under the 1024 ceiling {@code MAX_HEALTH} is a {@code RangedAttribute} with, and five
     * hundred hearts either way.
     */
    public static final double MAX_HEALTH = 1000.0;

    /**
     * Armour is capped at 30 by the attribute itself and toughness at 20, and both of these sit
     * exactly on their ceilings - a higher figure would be silently clamped rather than applied.
     * See the armour note in {@code CLAUDE.md} for why 30 is also as far as it is *worth* going:
     * vanilla's own {@code CombatRules} stops armour's contribution at 80% however much is worn.
     */
    public static final double ARMOR = 30.0;
    public static final double ARMOR_TOUGHNESS = 20.0;

    /**
     * Twice the fastest horse there is. Vanilla rolls a horse's speed as
     * {@code (0.45 + r*0.3 + r*0.3 + r*0.3) * 0.25}, so the best roll in the game is 0.3375 and this
     * is exactly two of them. It is read off that arithmetic rather than written as a number, so it
     * stays "twice the fastest horse" if vanilla ever retunes the roll.
     */
    public static final double MOVEMENT_SPEED = 2.0 * (0.45 + 0.3 + 0.3 + 0.3) * 0.25;

    /**
     * A horse's own step height is 1.0, and this is four blocks over that - the point being that a
     * horse this fast is otherwise stopped by the first fence post it meets. The attribute's own
     * ceiling is 10, so there is room left in it.
     */
    public static final double STEP_HEIGHT = 5.0;

    /** As high as a horse jumps at the best roll vanilla offers, so the legs match the speed. */
    public static final double JUMP_STRENGTH = 1.0;

    public CompressedHorseEntity(EntityType<? extends Horse> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    /**
     * A horse's own attributes with the five that matter raised.
     * <p>
     * These are base values rather than modifiers so that {@link #randomizeAttributes} - which is
     * the thing that would otherwise overwrite three of them - is the only place they can be
     * disturbed, and that is overridden below.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Horse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.JUMP_STRENGTH, JUMP_STRENGTH)
                .add(Attributes.STEP_HEIGHT, STEP_HEIGHT)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS);
    }

    /**
     * Nothing. This is the same trap {@code Wolf.applyTamingSideEffects} is: vanilla's
     * {@code Horse.randomizeAttributes} writes rolled figures straight into the base values of
     * {@code MAX_HEALTH}, {@code MOVEMENT_SPEED} and {@code JUMP_STRENGTH}, and it is called from
     * {@code AbstractHorse.finalizeSpawn} - so a Compressed Horse that ever went through a spawn
     * path, a spawn egg or a breeding would arrive with an ordinary horse's numbers and look as
     * though the attributes had simply not been applied.
     * <p>
     * A Compressed Horse has nothing to randomise: every one of them is the same horse.
     */
    @Override
    protected void randomizeAttributes(RandomSource random) {
    }
}
