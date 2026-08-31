package net.fahr3n.unnecessarilycompressedcobblestone.worldgen;

import java.util.Optional;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.world.level.block.grower.TreeGrower;

/**
 * What a sapling grows into. A {@link TreeGrower} is not a registry - it registers itself in a map
 * keyed by the name given here, which is what the sapling's block state is saved and loaded by, so
 * that name has to be unique and has to stay put.
 */
public class ModTreeGrowers {
    public static final TreeGrower COMPRESSED_COBBLESTONE = new TreeGrower(
            UnnecessarilyCompressedCobblestone.MOD_ID + ":compressed_cobblestone",
            Optional.empty(),
            Optional.of(ModConfiguredFeatures.COMPRESSED_COBBLESTONE_TREE),
            Optional.empty());
}
