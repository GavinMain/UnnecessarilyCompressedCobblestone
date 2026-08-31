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
    COBBLESTONE("compressed_cobblestone_arrow", 4.0),

    /** Three times the above, which is six times a vanilla arrow. */
    SUPER("super_compressed_arrow", 4.0 * 3.0);

    private final String name;
    private final double baseDamage;

    CompressedArrowTier(String name, double baseDamage) {
        this.name = name;
        this.baseDamage = baseDamage;
    }

    /** What it does before Compression Energy, Power and the speed it was fired at. */
    public double baseDamage() {
        return this.baseDamage;
    }

    public Supplier<? extends Item> item() {
        return this == COBBLESTONE ? ModItems.COMPRESSED_COBBLESTONE_ARROW : ModItems.SUPER_COMPRESSED_ARROW;
    }

    public Supplier<EntityType<CompressedArrowEntity>> entityType() {
        return this == COBBLESTONE ? ModEntities.COMPRESSED_COBBLESTONE_ARROW : ModEntities.SUPER_COMPRESSED_ARROW;
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
