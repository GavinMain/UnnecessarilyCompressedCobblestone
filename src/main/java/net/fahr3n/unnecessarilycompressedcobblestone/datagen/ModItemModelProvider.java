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
        basicItem(ModItems.COMPRESSION_RAIN_HELMET.get());
        basicItem(ModItems.COMPRESSION_RAIN_CHESTPLATE.get());
        basicItem(ModItems.COMPRESSION_RAIN_LEGGINGS.get());
        basicItem(ModItems.COMPRESSION_RAIN_BOOTS.get());
        basicItem(ModItems.COMPRESSION_MAGIC_HELMET.get());
        basicItem(ModItems.COMPRESSION_MAGIC_CHESTPLATE.get());
        basicItem(ModItems.COMPRESSION_MAGIC_LEGGINGS.get());
        basicItem(ModItems.COMPRESSION_MAGIC_BOOTS.get());

        basicItem(ModItems.ULTIMATE_COMPRESSED_HELMET.get());
        basicItem(ModItems.ULTIMATE_COMPRESSED_CHESTPLATE.get());
        basicItem(ModItems.ULTIMATE_COMPRESSED_LEGGINGS.get());
        basicItem(ModItems.ULTIMATE_COMPRESSED_BOOTS.get());

        ModItems.ENGRAVINGS.values().forEach(engraving -> basicItem(engraving.get()));
        ModItems.AUGMENTS.values().forEach(augment -> basicItem(augment.get()));
        basicItem(ModItems.RAY_OF_LASER.get());

        basicItem(ModItems.COMPRESSED_COBBLESTONE_ARROW.get());
        basicItem(ModItems.SUPER_COMPRESSED_ARROW.get());
        basicItem(ModItems.HYPER_COMPRESSED_ARROW.get());
        basicItem(ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_4_COMPRESSED_COBBLESTONE_APPLE.get());
        basicItem(ModItems.TIER_7_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_1_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_2_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_3_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_4_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_5_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_6_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_8_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_9_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_10_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_11_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_12_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_13_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_14_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_15_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_16_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_17_COMPRESSED_HEART.get());
        basicItem(ModItems.TIER_18_COMPRESSED_HEART.get());
        basicItem(ModItems.COMPRESSED_BONE.get());
        basicItem(ModItems.COMPRESSED_SADDLE.get());
        basicItem(ModItems.COMPRESSION_BOMB.get());
        basicItem(ModItems.COMPRESSED_TOTEM_OF_UNDYING.get());

        // Parented to vanilla's own shield model, which is `builtin/entity` plus the display
        // transforms that put a shield on the arm. The geometry comes from
        // CompressedShieldRenderer; what this inherits is where the renderer's output is placed.
        withExistingParent(ModItems.COMPRESSED_SHIELD.getId().getPath(), mcLoc("item/shield"));
        basicItem(ModItems.COMPRESSED_VANILLA_BOLT.get());
        ModItems.NOTE_BOLTS.forEach(bolt -> basicItem(bolt.get()));
        basicItem(ModItems.MOONLIGHT_BOLT.get());
        basicItem(ModItems.COMPOSITION_BOLT.get());
        basicItem(ModItems.REST_BOLT.get());
        basicItem(ModItems.LONG_REST_BOLT.get());
        basicItem(ModItems.ARROW_VEIL.get());
        basicItem(ModItems.COMPRESSED_FIREWORK.get());

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
        withExistingParent(ModItems.COMPRESSED_SPIRIT_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_WITCH_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_CHICKEN_BOSS_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_CREEPER_TIER_2_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_COMPOSER_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_GUARDIAN_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_HUSK_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_SNOW_GOLEM_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_GHAST_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_DRAGON_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        withExistingParent(ModItems.COMPRESSED_DRAGON_TIER_2_SPAWN_EGG.getId().getPath(), mcLoc("item/dragon_egg"));
        // The pet's egg is an ordinary spawn egg that puts the dragon down where it was clicked, so
        // it is the vanilla template tinted by the two colours on the item and needs no texture.
        withExistingParent(ModItems.COMPRESSED_DRAGON_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));

        // The two ghast eggs are the exception: they are ordinary spawn eggs that put the thing down
        // where they were clicked rather than starting a summon, so they are the vanilla template
        // tinted by the two colours on the item itself and need no texture of their own either.
        withExistingParent(ModItems.GHAST_PET_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.GHAST_MOUNT_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));

        // Tools and weapons are held in the hand, so they use the handheld parent instead.
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get());
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get());
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_SWORD.get());
        handheldItem(ModItems.COMPRESSED_KATANA.get());
        handheldItem(ModItems.COMPRESSED_SCYTHE.get());
        handheldItem(ModItems.COMPRESSED_SPEAR.get());
        handheldItem(ModItems.BROKEN_COMPRESSED_SWORD.get());
        handheldItem(ModItems.DEV_SWORD.get());
        handheldItem(ModItems.COMPRESSED_COBBLESTONE_MACE.get());

        // The fishing rod is absent for the same reason as the bow: its model carries a cast
        // override to a second model, so it and that second model are written by hand in
        // src/main/resources alongside vanilla's own pair.
        // Both staves are absent for the same reason as the bow: its model carries a cast override to
        // the charged model, so both are written by hand in src/main/resources.
        // The bow is deliberately absent: its model needs pulling overrides and held-item display
        // transforms, so it is written by hand in src/main/resources alongside its three pulling
        // stages, the same way vanilla's bow model is.
    }
}
