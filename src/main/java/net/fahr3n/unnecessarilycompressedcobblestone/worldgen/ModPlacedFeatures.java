package net.fahr3n.unnecessarilycompressedcobblestone.worldgen;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/** Where this mod's features are allowed to appear, and how often. */
public class ModPlacedFeatures {
    /** The tree as the world places it: the draw between shapes, one time in a hundred. */
    public static final ResourceKey<PlacedFeature> COMPRESSED_COBBLESTONE_TREE = key("compressed_cobblestone_tree");

    /**
     * One per shape, carrying no placement at all. They exist because {@code random_selector} draws
     * between placed features rather than configured ones; the spot has already been chosen by the
     * time one of these is picked, so there is nothing left for them to decide.
     */
    public static final List<ResourceKey<PlacedFeature>> SHAPES = ModConfiguredFeatures.SHAPES.stream()
            .map(shape -> key(shape.location().getPath()))
            .toList();

    /** One attempt in a hundred. Everything else about the placement is what a tree always gets. */
    public static final int RARITY = 100;

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        for (int i = 0; i < SHAPES.size(); i++) {
            context.register(SHAPES.get(i), new PlacedFeature(
                    features.getOrThrow(ModConfiguredFeatures.SHAPES.get(i)), List.of()));
        }

        // Vanilla's own tree placement, sapling check and all, with nothing changed but the filter in
        // front of it - so wherever an oak could have grown, one of these can, one time in a hundred
        // that the game asks.
        context.register(COMPRESSED_COBBLESTONE_TREE, new PlacedFeature(
                features.getOrThrow(ModConfiguredFeatures.COMPRESSED_COBBLESTONE_TREE),
                VegetationPlacements.treePlacement(RarityFilter.onAverageOnceEvery(RARITY),
                        ModBlocks.COMPRESSED_COBBLESTONE_SAPLING.get())));
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
    }
}
