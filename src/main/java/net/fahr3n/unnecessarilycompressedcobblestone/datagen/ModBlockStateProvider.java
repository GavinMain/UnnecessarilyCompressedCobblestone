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

        // Every face of the compressor says what it does: coal goes in the top and the back, blocks
        // come out of the bottom and the front, and the two remaining sides are plain casing.
        // horizontalBlock turns the model so the north face is the one FACING points along, which
        // is the output - the same face the block entity hands its output handler to.
        // Every TNT is laid out the way vanilla's is: a banded side, a marked top, a plain bottom.
        ModBlocks.TNTS.forEach(this::tnt);

        String compressor = ModBlocks.MATERIAL_COMPRESSOR_TIER_1.getId().getPath();
        var compressorModel = models().cube(compressor,
                modLoc("block/" + compressor + "_bottom"),
                modLoc("block/" + compressor + "_top"),
                modLoc("block/" + compressor + "_output"),
                modLoc("block/" + compressor + "_input"),
                modLoc("block/" + compressor + "_side"),
                modLoc("block/" + compressor + "_side"))
                .texture("particle", modLoc("block/" + compressor + "_side"));
        horizontalBlock(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get(), compressorModel);
        simpleBlockItem(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get(), compressorModel);
    }

    private void tnt(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        var model = models().cubeBottomTop(name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_bottom"), modLoc("block/" + name + "_top"));
        simpleBlockWithItem(deferredBlock.get(), model);
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        simpleBlockWithItem(deferredBlock.get(), models().cubeAll(name, modLoc("block/" + name)));
    }
}
