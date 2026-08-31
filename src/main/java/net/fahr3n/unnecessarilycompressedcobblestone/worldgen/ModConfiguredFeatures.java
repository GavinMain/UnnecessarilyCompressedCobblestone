package net.fahr3n.unnecessarilycompressedcobblestone.worldgen;

import java.util.List;
import java.util.OptionalInt;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.AcaciaFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.DarkOakFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaPineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;

/**
 * The shapes this mod's worldgen features take, before anything decides where to put them.
 * <p>
 * A compressed cobblestone tree is not one shape but nine. Every one of them is a vanilla tree's own
 * trunk and foliage placers with this mod's stone and leaves substituted in, so the grove a Treent
 * leaves behind has the same variety a natural forest does - straight oaks, sprawling fancy ones,
 * conifers, an acacia's fork, a dark oak's canopy, a giant, a cherry's droop and the odd bush.
 * {@link #COMPRESSED_COBBLESTONE_TREE} is the one that picks between them, and it is what both
 * worldgen and the sapling actually place.
 */
public class ModConfiguredFeatures {
    /** The compression level the trunk is made of. Everything from 57 up is unbreakable stone. */
    public static final int TRUNK_LEVEL = 58;

    /** The draw: any one of the shapes below, all equally likely. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> COMPRESSED_COBBLESTONE_TREE = key("compressed_cobblestone_tree");

    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_SHAPE = key("compressed_cobblestone_tree_oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_SHAPE = key("compressed_cobblestone_tree_fancy");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPRUCE_SHAPE = key("compressed_cobblestone_tree_spruce");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PINE_SHAPE = key("compressed_cobblestone_tree_pine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ACACIA_SHAPE = key("compressed_cobblestone_tree_acacia");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DARK_OAK_SHAPE = key("compressed_cobblestone_tree_dark_oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_SHAPE = key("compressed_cobblestone_tree_giant");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CHERRY_SHAPE = key("compressed_cobblestone_tree_cherry");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BUSH_SHAPE = key("compressed_cobblestone_tree_bush");

    /** Every shape, in the order they are offered to the draw. */
    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> SHAPES = List.of(
            OAK_SHAPE, FANCY_SHAPE, SPRUCE_SHAPE, PINE_SHAPE, ACACIA_SHAPE,
            DARK_OAK_SHAPE, GIANT_SHAPE, CHERRY_SHAPE, BUSH_SHAPE);

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Vanilla's oak, birch and jungle are all the same straight trunk under a ball of leaves and
        // differ only in their numbers, so one of them stands in for the lot.
        tree(context, OAK_SHAPE, new StraightTrunkPlacer(4, 2, 0),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1));

        tree(context, FANCY_SHAPE, new FancyTrunkPlacer(3, 11, 0),
                new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4),
                new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4)));

        tree(context, SPRUCE_SHAPE, new StraightTrunkPlacer(5, 2, 1),
                new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 2), UniformInt.of(1, 2)),
                new TwoLayersFeatureSize(2, 0, 2));

        tree(context, PINE_SHAPE, new StraightTrunkPlacer(6, 4, 0),
                new PineFoliagePlacer(ConstantInt.of(1), ConstantInt.of(1), UniformInt.of(3, 4)),
                new TwoLayersFeatureSize(2, 0, 2));

        tree(context, ACACIA_SHAPE, new ForkingTrunkPlacer(5, 2, 2),
                new AcaciaFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0)),
                new TwoLayersFeatureSize(1, 0, 2));

        tree(context, DARK_OAK_SHAPE, new DarkOakTrunkPlacer(6, 2, 1),
                new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
                new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty()));

        // The two-by-two giant. It is the one shape that needs room, and the one worth finding.
        tree(context, GIANT_SHAPE, new GiantTrunkPlacer(13, 2, 14),
                new MegaPineFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), UniformInt.of(3, 7)),
                new TwoLayersFeatureSize(1, 1, 2));

        tree(context, CHERRY_SHAPE, new CherryTrunkPlacer(7, 1, 0,
                        new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder()
                                .add(ConstantInt.of(1), 1)
                                .add(ConstantInt.of(2), 1)
                                .add(ConstantInt.of(3), 1)
                                .build()),
                        UniformInt.of(2, 4), UniformInt.of(-4, -3), UniformInt.of(-1, 0)),
                new CherryFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), ConstantInt.of(5),
                        0.25F, 0.5F, 0.16666667F, 0.33333334F),
                new TwoLayersFeatureSize(1, 0, 2));

        // A single block of trunk under a low tangle, the way a jungle bush goes.
        tree(context, BUSH_SHAPE, new StraightTrunkPlacer(1, 0, 0),
                new BushFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 2),
                new TwoLayersFeatureSize(0, 0, 0));

        // What everything else actually places: one of the nine, drawn at random. It is built out of
        // placed features rather than configured ones because that is what random_selector takes;
        // each of those carries no placement of its own, since the choice is made after the spot is.
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        List<WeightedPlacedFeature> draw = ModPlacedFeatures.SHAPES.stream()
                .map(shape -> new WeightedPlacedFeature(placed.getOrThrow(shape), 1.0F))
                .toList();

        context.register(COMPRESSED_COBBLESTONE_TREE, new ConfiguredFeature<>(Feature.RANDOM_SELECTOR,
                new RandomFeatureConfiguration(draw.subList(0, draw.size() - 1),
                        placed.getOrThrow(ModPlacedFeatures.SHAPES.get(ModPlacedFeatures.SHAPES.size() - 1)))));
    }

    /** One shape: this mod's stone and leaves, poured into a vanilla tree's placers. */
    private static void tree(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                             TrunkPlacer trunk, FoliagePlacer foliage,
                             net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize size) {
        Block trunkBlock = ModBlocks.byLevel(TRUNK_LEVEL).get();

        context.register(key, new ConfiguredFeature<>(Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(trunkBlock),
                        trunk,
                        BlockStateProvider.simple(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES.get()),
                        foliage,
                        size)
                        .ignoreVines()
                        .build()));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
    }
}
