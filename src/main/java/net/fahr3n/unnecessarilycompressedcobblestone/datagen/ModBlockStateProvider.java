package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
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

        // The carved face is a texture of its own; the other five sides stay the block it was cut
        // out of. horizontalBlock rotates the model to whichever way it was placed.
        String carved = ModBlocks.CARVED_COBBLESTONE_TIER_1.getId().getPath();
        String body = ModBlocks.byLevel(CarvedCobblestoneBlock.CARVED_FROM_LEVEL).getId().getPath();
        var carvedModel = models().orientable(carved, modLoc("block/" + body), modLoc("block/" + carved), modLoc("block/" + body));
        horizontalBlock(ModBlocks.CARVED_COBBLESTONE_TIER_1.get(), carvedModel);
        simpleBlockItem(ModBlocks.CARVED_COBBLESTONE_TIER_1.get(), carvedModel);

        // The inscriber has a worked top and plain sides, like a crafting table.
        String inscriber = ModBlocks.COMPRESSION_INSCRIBER.getId().getPath();
        var inscriberModel = models().cubeBottomTop(inscriber, modLoc("block/" + inscriber + "_side"),
                modLoc("block/" + inscriber + "_side"), modLoc("block/" + inscriber + "_top"));
        simpleBlockWithItem(ModBlocks.COMPRESSION_INSCRIBER.get(), inscriberModel);
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        simpleBlockWithItem(deferredBlock.get(), models().cubeAll(name, modLoc("block/" + name)));
    }
}
