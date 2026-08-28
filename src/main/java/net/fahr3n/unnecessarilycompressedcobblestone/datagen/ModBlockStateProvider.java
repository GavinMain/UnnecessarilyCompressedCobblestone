package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, UnnecessarilyCompressedCobblestone.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Every level has its own texture file at block/<block id>, so each level can be
        // redrawn on its own later even though they all currently share the same image.
        for (DeferredBlock<?> block : ModBlocks.COMPRESSED_COBBLESTONE_LEVELS) {
            blockWithItem(block);
        }
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        simpleBlockWithItem(deferredBlock.get(), models().cubeAll(name, modLoc("block/" + name)));
    }
}
