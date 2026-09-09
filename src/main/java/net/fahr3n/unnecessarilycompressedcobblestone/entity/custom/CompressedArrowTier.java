package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * One strength of compressed arrow. Everything else about them is identical, so the entity, the item
 * and the renderer are all shared and a new arrow is a constant here plus a registration, a recipe
 * and two textures.
 * <p>
 * The suppliers are what keep this from being a circular reference: the enum is loaded long before
 * the registries are filled, so nothing here may hold an item or an entity type directly.
 */
public enum CompressedArrowTier {
    /** Twice what a vanilla arrow does. */
    COBBLESTONE("compressed_cobblestone_arrow", 4.0, 0.05),

    /** Three times the above, which is six times a vanilla arrow. */
    SUPER("super_compressed_arrow", 4.0 * 3.0, 0.05),

    /**
     * Five times the Super, and the only one of the three that is not simply better than the one
     * above it: it weighs so much that it is on the ground almost as soon as it leaves the string.
     * <p>
     * That is the whole design of it rather than a drawback bolted on. Fired from any ordinary bow
     * it is a stone dropped at your feet; fired from a bow carrying the Sniper engraving, which
     * throws an arrow far too fast for a twentieth of a second of fall to matter, it is sixty
     * points of damage delivered the instant the string is released. The arrow and that engraving
     * are one weapon in two halves.
     */
    HYPER("hyper_compressed_arrow", 4.0 * 3.0 * 5.0, 1.0);

    // The gravity above is written as a literal rather than a named constant because an enum
    // constant's arguments are evaluated before the enum's own static fields exist. Vanilla's is
    // 0.05 a tick, which the first two carry; the Hyper's 1.0 is twenty times that.

    private final String name;
    private final double baseDamage;
    private final double gravity;

    CompressedArrowTier(String name, double baseDamage, double gravity) {
        this.name = name;
        this.baseDamage = baseDamage;
        this.gravity = gravity;
    }

    /** What it does before Compression Energy, Power and the speed it was fired at. */
    public double baseDamage() {
        return this.baseDamage;
    }

    /** How hard it is pulled down each tick, which for one of the three is the point of it. */
    public double gravity() {
        return this.gravity;
    }

    public Supplier<? extends Item> item() {
        return switch (this) {
            case COBBLESTONE -> ModItems.COMPRESSED_COBBLESTONE_ARROW;
            case SUPER -> ModItems.SUPER_COMPRESSED_ARROW;
            case HYPER -> ModItems.HYPER_COMPRESSED_ARROW;
        };
    }

    public Supplier<EntityType<CompressedArrowEntity>> entityType() {
        return switch (this) {
            case COBBLESTONE -> ModEntities.COMPRESSED_COBBLESTONE_ARROW;
            case SUPER -> ModEntities.SUPER_COMPRESSED_ARROW;
            case HYPER -> ModEntities.HYPER_COMPRESSED_ARROW;
        };
    }

    /** Laid out like vanilla's: a 16x5 strip of the shaft with a 5x5 cap for the flights below it. */
    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID,
                "textures/entity/projectiles/" + this.name + ".png");
    }

    /** The tier a given entity type belongs to, so an arrow can work out what it is from its own type. */
    public static CompressedArrowTier of(EntityType<?> type) {
        for (CompressedArrowTier tier : values()) {
            if (tier.entityType().get() == type) {
                return tier;
            }
        }

        return COBBLESTONE;
    }
}
