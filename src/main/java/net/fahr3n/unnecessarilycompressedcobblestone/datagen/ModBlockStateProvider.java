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

        // The Engraving Table is built the same way: a worked top and plain sides.
        String engravingTable = ModBlocks.ENGRAVING_TABLE.getId().getPath();
        var engravingTableModel = models().cubeBottomTop(engravingTable, modLoc("block/" + engravingTable + "_side"),
                modLoc("block/" + engravingTable + "_side"), modLoc("block/" + engravingTable + "_top"));
        simpleBlockWithItem(ModBlocks.ENGRAVING_TABLE.get(), engravingTableModel);

        // Every face of the compressor says what it does: coal goes in the top and the back, blocks
        // come out of the bottom and the front, and the two remaining sides are plain casing.
        // horizontalBlock turns the model so the north face is the one FACING points along, which
        // is the output - the same face the block entity hands its output handler to.
        // Every TNT is laid out the way vanilla's is: a banded side, a marked top, a plain bottom.
        ModBlocks.TNTS.forEach(this::tnt);

        blockWithItem(ModBlocks.LIGHTNING_CORE);

        leaves(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES);
        sapling(ModBlocks.COMPRESSED_COBBLESTONE_SAPLING);

        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_1);
        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_2);
        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_3);
    }

    /** Leaves are a solid cube drawn with a cutout so the gaps in the texture show through. */
    private void leaves(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        var model = models().cubeAll(name, modLoc("block/" + name)).renderType("cutout_mipped");
        simpleBlockWithItem(deferredBlock.get(), model);
    }

    /** A sapling is two crossed quads, and its item is the flat texture rather than that model. */
    private void sapling(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        simpleBlock(deferredBlock.get(), models().cross(name, modLoc("block/" + name)).renderType("cutout"));
        itemModels().withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc("block/" + name));
    }

    /** Five distinct faces: a marked top and bottom, an input face, an output face and two sides. */
    private void compressor(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        var model = models().cube(name,
                modLoc("block/" + name + "_bottom"),
                modLoc("block/" + name + "_top"),
                modLoc("block/" + name + "_output"),
                modLoc("block/" + name + "_input"),
                modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_side"))
                .texture("particle", modLoc("block/" + name + "_side"));

        horizontalBlock(deferredBlock.get(), model);
        simpleBlockItem(deferredBlock.get(), model);
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
