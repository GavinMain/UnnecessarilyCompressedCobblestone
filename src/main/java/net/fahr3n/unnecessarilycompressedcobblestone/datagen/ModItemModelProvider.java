package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, UnnecessarilyCompressedCobblestone.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Each item listed here needs assets/<modid>/textures/item/<item_id>.png to exist
        // in src/main/resources, otherwise datagen fails with a missing texture error.
        basicItem(ModItems.COMPRESSED_COBBLESTONE_HELMET.get());
        basicItem(ModItems.COMPRESSED_COBBLESTONE_CHESTPLATE.get());
        basicItem(ModItems.COMPRESSED_COBBLESTONE_LEGGINGS.get());
        basicItem(ModItems.COMPRESSED_COBBLESTONE_BOOTS.get());
    }
}
