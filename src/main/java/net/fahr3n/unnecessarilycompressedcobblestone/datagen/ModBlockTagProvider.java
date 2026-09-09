package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, UnnecessarilyCompressedCobblestone.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var stoneTool = tag(BlockTags.NEEDS_STONE_TOOL);

        for (DeferredBlock<?> block : ModBlocks.COMPRESSED_COBBLESTONE_LEVELS) {
            pickaxe.add(block.get());
            stoneTool.add(block.get());
        }

        pickaxe.add(ModBlocks.DEV_COMPRESSED_COBBLESTONE.get());
        stoneTool.add(ModBlocks.DEV_COMPRESSED_COBBLESTONE.get());

        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get());
        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_2.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_2.get());
        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_3.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_3.get());
        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_4.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_4.get());
        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_5.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_5.get());
        pickaxe.add(ModBlocks.ENGRAVING_TABLE.get());
        stoneTool.add(ModBlocks.ENGRAVING_TABLE.get());
        pickaxe.add(ModBlocks.COMPOSITION_TABLE.get());
        stoneTool.add(ModBlocks.COMPOSITION_TABLE.get());
        pickaxe.add(ModBlocks.LASER_AUGMENTATION_TABLE.get());
        stoneTool.add(ModBlocks.LASER_AUGMENTATION_TABLE.get());
        pickaxe.add(ModBlocks.TELEPORTATION_GATE.get());
        stoneTool.add(ModBlocks.TELEPORTATION_GATE.get());

        // What the Compressed Golem Tier 2 heals standing on: dirt, sand, gravel, stone and the
        // several dozen things the game already groups with them, plus this mod's own stone, which
        // is what a player is most likely to be fighting it on. Built out of tags rather than a list
        // so another mod's ground counts without this file changing.
        tag(ModTags.Blocks.GOLEM_GROUND)
                .addTag(BlockTags.DIRT)
                .addTag(BlockTags.SAND)
                .addTag(BlockTags.BASE_STONE_OVERWORLD)
                .addTag(BlockTags.BASE_STONE_NETHER)
                .addTag(BlockTags.TERRACOTTA)
                .addTag(Tags.Blocks.GRAVELS)
                .addTag(Tags.Blocks.STONES)
                .addTag(Tags.Blocks.COBBLESTONES)
                .addTag(Tags.Blocks.SANDS)
                .addTag(Tags.Blocks.ORES)
                .add(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.SNOW_BLOCK,
                        Blocks.CLAY, Blocks.SOUL_SAND, Blocks.SOUL_SOIL);

        var golemGround = tag(ModTags.Blocks.GOLEM_GROUND);
        for (DeferredBlock<?> block : ModBlocks.COMPRESSED_COBBLESTONE_LEVELS) {
            golemGround.add(block.get());
        }
        pickaxe.add(ModBlocks.LIGHTNING_CORE.get());
        stoneTool.add(ModBlocks.LIGHTNING_CORE.get());

        // What Harvest Festival multiplies. Vanilla's own crop tag is the eight things that grow
        // on farmland; the other three are crops by every reading except that tag's, and each has
        // an age of its own, which is what the ripeness test reads.
        tag(ModTags.Blocks.HARVEST)
                .addTag(BlockTags.CROPS)
                .add(Blocks.NETHER_WART, Blocks.COCOA, Blocks.SWEET_BERRY_BUSH);

        // Leaves and saplings behave like vanilla's own: shears and hoes are the right tools, fire
        // spreads through the leaves, and the sapling is bone-mealable and pot-able.
        tag(BlockTags.LEAVES).add(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES.get());
        tag(BlockTags.SAPLINGS).add(ModBlocks.COMPRESSED_COBBLESTONE_SAPLING.get());

        // What the Random TNT draws from. Every TNT this mod has goes in, vanilla's own goes in,
        // and #c:tnt is included optionally: no such common tag exists today, but if one is ever
        // agreed on, every mod's TNT joins the draw without this file changing. The Random TNT
        // itself is in the tag as well - it is a TNT, and other things may want to read this list -
        // and the effect skips over itself when it draws rather than the tag leaving it out.
        var tnts = tag(ModTags.Blocks.TNT).add(Blocks.TNT);
        ModBlocks.TNTS.forEach(tnt -> tnts.add(tnt.get()));
        tnts.addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "tnt"));
    }
}
