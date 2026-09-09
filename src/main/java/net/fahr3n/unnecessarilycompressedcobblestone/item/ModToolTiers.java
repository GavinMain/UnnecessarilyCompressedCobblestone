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
            () -> Ingredient.of(ModBlocks.byLevel(1).get()));

    /**
     * Netherite-equivalent, and repaired with the stone it is made of. It is the tier of the one
     * pickaxe that gets into the hardened levels, so it has to out-harvest everything vanilla has;
     * the mining speed is only its starting point, since Compression Energy adds to it.
     */
    public static final Tier HARDENED_COMPRESSED_COBBLESTONE = new SimpleTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 4.0F, 15,
            () -> Ingredient.of(ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL).get()));

    /**
     * The tier of the one pickaxe that gets into the second floor, and repaired with the stone that
     * floor starts at. Four times the mining speed of the tier above it, which is what makes the
     * deeper stone cost about the same wait per block as the shallower hardened stone does to a bare
     * tier 1 pickaxe - see {@code HardenedMining.EFFECTIVE_HARDNESS_TIER_2}. Compression Energy adds
     * to that the same way it does for every other tool here.
     */
    public static final Tier HARDENED_COMPRESSED_COBBLESTONE_TIER_2 = new SimpleTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 36.0F, 6.0F, 15,
            () -> Ingredient.of(ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL_TIER_2).get()));
}
