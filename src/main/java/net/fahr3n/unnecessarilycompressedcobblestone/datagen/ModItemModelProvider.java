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

        basicItem(ModItems.COMPRESSION_LIGHTNING_HELMET.get());
        basicItem(ModItems.COMPRESSION_LIGHTNING_CHESTPLATE.get());
        basicItem(ModItems.COMPRESSION_LIGHTNING_LEGGINGS.get());
        basicItem(ModItems.COMPRESSION_LIGHTNING_BOOTS.get());

        basicItem(ModItems.COMPRESSION_ARROW_HELMET.get());
        basicItem(ModItems.COMPRESSION_ARROW_CHESTPLATE.get());
        basicItem(ModItems.COMPRESSION_ARROW_LEGGINGS.get());
        basicItem(ModItems.COMPRESSION_ARROW_BOOTS.get());

        ModItems.ENGRAVINGS.values().forEach(engraving -> basicItem(engraving.get()));

        basicItem(ModItems.COMPRESSED_COBBLESTONE_ARROW.get());
        basicItem(ModItems.SUPER_COMPRESSED_ARROW.get());
        basicItem(ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_7_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_1_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_2_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_3_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_4_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_5_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_6_COMPRESSED_HEART.get());
        basicItem(ModItems.COMPRESSED_VANILLA_BOLT.get());
        basicItem(ModItems.ARROW_VEIL.get());

        // Spawn eggs are the vanilla template tinted by the two colours on the item itself.
        // Every boss egg is drawn as a dragon egg rather than as a spawn egg, because that is what
        // it becomes when it is used. The vanilla item model is the block model, so this needs no
        // texture of its own and no tint layers.
        withExistingParent(ModItems.COMPRESSED_CREEPER_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_CONJURER_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_SKELETON_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_SKELETON_TIER_2_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_SKELETON_TIER_3_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_SUMMONER_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));

        // Tools and weapons are held in the hand, so they use the handheld parent instead.
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get());
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_SWORD.get());
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_MACE.get());

        // Both staves are absent for the same reason as the bow: its model carries a cast override to
        // the charged model, so both are written by hand in src/main/resources.
        // The bow is deliberately absent: its model needs pulling overrides and held-item display
        // transforms, so it is written by hand in src/main/resources alongside its three pulling
        // stages, the same way vanilla's bow model is.
    }
}
