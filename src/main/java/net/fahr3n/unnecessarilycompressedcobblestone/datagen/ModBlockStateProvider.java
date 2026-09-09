package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.TeleportationGateBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
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
        for (DeferredBlock<CarvedCobblestoneBlock> head : ModBlocks.CARVED_HEADS) {
            String carved = head.getId().getPath();
            String body = ModBlocks.byLevel(head.get().carvedFromLevel()).getId().getPath();
            var carvedModel = models().orientable(carved, modLoc("block/" + body), modLoc("block/" + carved),
                    modLoc("block/" + body));
            horizontalBlock(head.get(), carvedModel);
            simpleBlockItem(head.get(), carvedModel);
        }

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

        // And the Composition Table again, which is the third of the family.
        String compositionTable = ModBlocks.COMPOSITION_TABLE.getId().getPath();
        var compositionTableModel = models().cubeBottomTop(compositionTable,
                modLoc("block/" + compositionTable + "_side"), modLoc("block/" + compositionTable + "_side"),
                modLoc("block/" + compositionTable + "_top"));
        simpleBlockWithItem(ModBlocks.COMPOSITION_TABLE.get(), compositionTableModel);

        // And the Laser Augmentation Table, which is the fourth: a worked top and plain sides.
        String augmentationTable = ModBlocks.LASER_AUGMENTATION_TABLE.getId().getPath();
        var augmentationTableModel = models().cubeBottomTop(augmentationTable,
                modLoc("block/" + augmentationTable + "_side"), modLoc("block/" + augmentationTable + "_side"),
                modLoc("block/" + augmentationTable + "_top"));
        simpleBlockWithItem(ModBlocks.LASER_AUGMENTATION_TABLE.get(), augmentationTableModel);

        // Every face of the compressor says what it does: coal goes in the top and the back, blocks
        // come out of the bottom and the front, and the two remaining sides are plain casing.
        // horizontalBlock turns the model so the north face is the one FACING points along, which
        // is the output - the same face the block entity hands its output handler to.
        // Every TNT is laid out the way vanilla's is: a banded side, a marked top, a plain bottom.
        ModBlocks.TNTS.forEach(this::tnt);

        blockWithItem(ModBlocks.LIGHTNING_CORE);

        // The dev block borrows the first compression level's texture rather than shipping one of
        // its own: it is a compressed cobblestone block, it is creative-only, and a placeholder
        // nobody but a developer sees is one more file to keep in step. TEXTURE.md says so.
        simpleBlockWithItem(ModBlocks.DEV_COMPRESSED_COBBLESTONE.get(),
                models().cubeAll(ModBlocks.DEV_COMPRESSED_COBBLESTONE.getId().getPath(),
                        modLoc("block/" + ModBlocks.byLevel(1).getId().getPath())));

        // Slippery Ice is drawn as ice is: one texture on every face, with a cutout so the
        // translucency reads. cubeAll plus a render type is all that takes.
        String slipperyIce = ModBlocks.SLIPPERY_ICE.getId().getPath();
        simpleBlockWithItem(ModBlocks.SLIPPERY_ICE.get(),
                models().cubeAll(slipperyIce, modLoc("block/" + slipperyIce)).renderType("translucent"));

        leaves(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES);
        sapling(ModBlocks.COMPRESSED_COBBLESTONE_SAPLING);

        // A cobweb is drawn exactly as a sapling is - two crossed quads with a cutout - and its item
        // is the flat texture rather than that model, so the same helper covers both.
        sapling(ModBlocks.REGEN_WEB);
        sapling(ModBlocks.DAMAGE_WEB);

        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_1);
        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_2);
        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_3);
        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_4);
        compressor(ModBlocks.MATERIAL_COMPRESSOR_TIER_5);

        teleportationGate();
    }

    /**
     * The gate is a pane four pixels thick standing in the middle of its block, in two halves with a
     * texture each, turned to face whichever way it was placed.
     * <p>
     * The model is built here rather than by one of the {@code models()} helpers because none of
     * them makes a slab standing on edge, and it is drawn with a translucent render type so the
     * doorway reads as something to walk through rather than as a wall. The item is the lower half's
     * model, which is why the gate needs no item texture of its own.
     */
    private void teleportationGate() {
        String name = ModBlocks.TELEPORTATION_GATE.getId().getPath();
        var lower = gatePane(name + "_lower");
        var upper = gatePane(name + "_upper");

        getVariantBuilder(ModBlocks.TELEPORTATION_GATE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(TeleportationGateBlock.HALF) == DoubleBlockHalf.LOWER ? lower : upper)
                // The pane is modelled across the north face, so this is the same arithmetic
                // horizontalBlock does: turn the model until its north face is the way FACING points.
                .rotationY((int) state.getValue(TeleportationGateBlock.FACING).toYRot())
                .build());

        simpleBlockItem(ModBlocks.TELEPORTATION_GATE.get(), lower);
    }

    private BlockModelBuilder gatePane(String name) {
        return models().getBuilder("block/" + name)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/" + name))
                .texture("gate", modLoc("block/" + name))
                .renderType("translucent")
                .element()
                .from(0.0F, 0.0F, 6.0F)
                .to(16.0F, 16.0F, 10.0F)
                .allFaces((direction, face) -> face.texture("#gate"))
                .end();
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
