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

        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get());
        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_2.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_2.get());
        pickaxe.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_3.get());
        stoneTool.add(ModBlocks.MATERIAL_COMPRESSOR_TIER_3.get());
        pickaxe.add(ModBlocks.ENGRAVING_TABLE.get());
        stoneTool.add(ModBlocks.ENGRAVING_TABLE.get());
        pickaxe.add(ModBlocks.LIGHTNING_CORE.get());
        stoneTool.add(ModBlocks.LIGHTNING_CORE.get());

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
