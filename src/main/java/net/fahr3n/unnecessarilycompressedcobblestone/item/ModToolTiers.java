package net.fahr3n.unnecessarilycompressedcobblestone.item;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Tiers are not a registry, so unlike {@link ModArmorMaterials} these are plain constants.
 */
public class ModToolTiers {
    /**
     * Iron-equivalent numbers: 250 uses, 6.0 mining speed, +2 attack damage, enchantability 14,
     * repaired with the block it is made of. The uses are never actually spent (see
     * {@code CompressedCobblestoneSwordItem}) but a real durability is what keeps the item
     * damageable, and therefore enchantable and repairable, like any vanilla tool.
     */
    public static final Tier COMPRESSED_COBBLESTONE = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14,
            () -> Ingredient.of(ModBlocks.COMPRESSED_COBBLESTONE.get()));
}
