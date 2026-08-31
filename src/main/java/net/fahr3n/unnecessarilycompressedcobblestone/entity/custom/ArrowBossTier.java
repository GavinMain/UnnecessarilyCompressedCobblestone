package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;

/**
 * One rung of the arrow boss. All three are the same skeleton with a different set of attacks, so
 * they share an entity class and differ only in what is written here: how much of it there is, what
 * it fires, which of the four attacks it knows, and whether it can be hurt by blasts and arrows.
 * <p>
 * The weights are what the mix is made of. A weight of zero means the boss simply does not have that
 * attack, so the first rung has no volley at all and the second has no stream.
 */
public enum ArrowBossTier {
    /** The first: a bow and the stream, and nothing that cannot be walked away from. */
    TIER_1(750.0F, CompressedArrowTier.COBBLESTONE, 75, 25, 0, 0, false),

    /**
     * The second: heavier arrows, no stream, and the sky full of falling TNT instead. Arrows and
     * blasts are the two things it throws, and it is immune to both, so a fight with it cannot be won
     * by turning either back on it.
     */
    TIER_2(1000.0F, CompressedArrowTier.SUPER, 65, 0, 35, 0, true),

    /** The last: everything the first two do, and the volley of spent arrows on top of it. */
    TIER_3(1500.0F, CompressedArrowTier.SUPER, 55, 20, 20, 5, true);

    private final float maxHealth;
    private final CompressedArrowTier arrowTier;
    private final int shotWeight;
    private final int streamWeight;
    private final int tntRainWeight;
    private final int volleyWeight;
    private final boolean immuneToItsOwnWeapons;

    ArrowBossTier(float maxHealth, CompressedArrowTier arrowTier, int shotWeight, int streamWeight,
                  int tntRainWeight, int volleyWeight, boolean immuneToItsOwnWeapons) {
        this.maxHealth = maxHealth;
        this.arrowTier = arrowTier;
        this.shotWeight = shotWeight;
        this.streamWeight = streamWeight;
        this.tntRainWeight = tntRainWeight;
        this.volleyWeight = volleyWeight;
        this.immuneToItsOwnWeapons = immuneToItsOwnWeapons;
    }

    public float maxHealth() {
        return this.maxHealth;
    }

    /** What its ordinary shot and its stream are made of. */
    public CompressedArrowTier arrowTier() {
        return this.arrowTier;
    }

    public int shotWeight() {
        return this.shotWeight;
    }

    public int streamWeight() {
        return this.streamWeight;
    }

    public int tntRainWeight() {
        return this.tntRainWeight;
    }

    public int volleyWeight() {
        return this.volleyWeight;
    }

    /** Whether explosions and arrows can hurt it at all. */
    public boolean immuneToItsOwnWeapons() {
        return this.immuneToItsOwnWeapons;
    }

    public Supplier<EntityType<CompressedSkeletonEntity>> entityType() {
        return switch (this) {
            case TIER_1 -> ModEntities.COMPRESSED_SKELETON;
            case TIER_2 -> ModEntities.COMPRESSED_SKELETON_TIER_2;
            case TIER_3 -> ModEntities.COMPRESSED_SKELETON_TIER_3;
        };
    }

    /**
     * A skeleton's own numbers, with a boss's health and a full diamond set's protection. The
     * toughness matters as much as the armour: unlike the Compressed Creeper, which pairs armour 30
     * with no toughness so that big hits punch through, these are meant to be worn down.
     */
    public AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, this.maxHealth)
                .add(Attributes.ARMOR, CompressedSkeletonEntity.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, CompressedSkeletonEntity.ARMOR_TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, CompressedSkeletonEntity.ATTACK_RADIUS);
    }

    /** The rung a given entity type belongs to, so a boss can work out what it is from its own type. */
    public static ArrowBossTier of(EntityType<?> type) {
        for (ArrowBossTier tier : values()) {
            if (tier.entityType().get() == type) {
                return tier;
            }
        }

        return TIER_1;
    }
}
