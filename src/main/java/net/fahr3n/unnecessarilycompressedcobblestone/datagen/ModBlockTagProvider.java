package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
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
    }
}
