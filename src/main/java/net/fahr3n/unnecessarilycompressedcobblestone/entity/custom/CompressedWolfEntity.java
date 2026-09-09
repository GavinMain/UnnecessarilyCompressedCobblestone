package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

/**
 * The Compressed Wolf. A wolf in every line of its brain - the same goals, the same sitting, the
 * same following, the same jealousy about who hit its owner - and a wolf with two orders of
 * magnitude more of everything that decides a fight.
 * <p>
 * Nothing here touches the AI, which is the whole point of it: the mob a player gets back for a
 * Compressed Bone behaves exactly like the wolf they fed, so there is nothing to learn about it and
 * nothing surprising about what it does. The only overridden behaviour is
 * {@link #applyTamingSideEffects}, and that is defensive rather than a change - see below.
 */
public class CompressedWolfEntity extends Wolf {
    public static final double MAX_HEALTH = 1000.0;
    public static final double ARMOR = 25.0;
    public static final double ARMOR_TOUGHNESS = 20.0;
    public static final double ATTACK_DAMAGE = 100.0;

    public CompressedWolfEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    /**
     * A wolf's own attributes with the four that matter raised. Movement speed is left exactly where
     * a wolf's is: a pet that outruns its owner is a pet that is never where it was left.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS);
    }

    /**
     * Vanilla's taming side effects, with the health left alone.
     * <p>
     * {@code Wolf.applyTamingSideEffects} sets {@code MAX_HEALTH} to a flat 40 on taming and 8 on
     * untaming, written as literals rather than read off anything - so a Compressed Wolf that went
     * through it would arrive with forty health instead of a thousand and would look like the
     * attribute had simply not been applied. It is called from {@code setTame}, which every path to
     * a tame wolf goes through including reading one back off disk, so overriding it is the only
     * place this can be answered.
     * <p>
     * The heal is what taming is worth here, since the health is no longer the thing being changed.
     */
    @Override
    protected void applyTamingSideEffects() {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(MAX_HEALTH);
        }

        if (isTame()) {
            setHealth(getMaxHealth());
        }
    }
}
