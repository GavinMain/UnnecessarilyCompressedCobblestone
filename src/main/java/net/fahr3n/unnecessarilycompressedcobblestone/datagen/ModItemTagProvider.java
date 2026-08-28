package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, UnnecessarilyCompressedCobblestone.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // These four tags are what the rest of the game hangs off: #minecraft:enchantable/armor
        // (and so every protection enchantment), #minecraft:enchantable/durability,
        // #minecraft:enchantable/equippable, #minecraft:enchantable/vanishing,
        // #minecraft:trimmable_armor and NeoForge's #c:armors all include them, so the set is
        // enchantable, mendable, trimmable and visible to other mods without any further work.
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COMPRESSED_COBBLESTONE_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COMPRESSED_COBBLESTONE_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COMPRESSED_COBBLESTONE_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COMPRESSED_COBBLESTONE_BOOTS.get());
    }
}
