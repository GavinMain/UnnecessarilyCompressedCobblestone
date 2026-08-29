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

        basicItem(ModItems.COMPRESSION_JUMP_HELMET.get());
        basicItem(ModItems.COMPRESSION_JUMP_CHESTPLATE.get());
        basicItem(ModItems.COMPRESSION_JUMP_LEGGINGS.get());
        basicItem(ModItems.COMPRESSION_JUMP_BOOTS.get());

        basicItem(ModItems.COMPRESSED_COBBLESTONE_ARROW.get());
        basicItem(ModItems.COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_1_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_2_COMPRESSED_HEART.get());

        // Spawn eggs are the vanilla template tinted by the two colours on the item itself.
        withExistingParent(ModItems.COMPRESSED_CREEPER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));

        // Tools and weapons are held in the hand, so they use the handheld parent instead.
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_SWORD.get());
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_MACE.get());

        // The bow is deliberately absent: its model needs pulling overrides and held-item display
        // transforms, so it is written by hand in src/main/resources alongside its three pulling
        // stages, the same way vanilla's bow model is.
    }
}
