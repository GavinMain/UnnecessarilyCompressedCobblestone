package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
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

        // #minecraft:swords is the weapon equivalent: vanilla routes it into
        // #minecraft:enchantable/sword, /sharp_weapon, /weapon, /fire_aspect, /durability and
        // /vanishing, so the sword takes Sharpness, Looting, Fire Aspect, Unbreaking, Mending
        // and the rest off an enchanting table or anvil with nothing further to do.
        tag(ItemTags.SWORDS).add(ModItems.COMPRESSED_COBBLESTONE_SWORD.get());

        // NeoForge's #c:tools/melee_weapon lists items one by one rather than including
        // #minecraft:swords, so mods that look at it need the sword added explicitly.
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.COMPRESSED_COBBLESTONE_SWORD.get());

        // Vanilla lists minecraft:bow item by item in both of these rather than going through a
        // tag, so the bow has to be added to each: #minecraft:enchantable/bow is Power, Punch,
        // Flame and Infinity, and #minecraft:enchantable/durability is Unbreaking and Mending
        // (and, through it, #minecraft:enchantable/vanishing).
        tag(ItemTags.BOW_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_BOW.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_BOW.get());

        // The same two lists on NeoForge's side, for mods that treat every bow alike.
        tag(Tags.Items.TOOLS_BOW).add(ModItems.COMPRESSED_COBBLESTONE_BOW.get());
        tag(Tags.Items.RANGED_WEAPON_TOOLS).add(ModItems.COMPRESSED_COBBLESTONE_BOW.get());

        // Being in #minecraft:arrows is what makes this ammo: both this bow and every vanilla bow,
        // crossbow and dispenser will fire it.
        tag(ItemTags.ARROWS).add(ModItems.COMPRESSED_COBBLESTONE_ARROW.get());

        // The mace is listed item by item in each of these the same way vanilla lists its own:
        // #minecraft:enchantable/weapon is what Compression itself hangs off, /mace is Density,
        // Breach and Wind Burst, /fire_aspect is Fire Aspect and /durability is Unbreaking and
        // Mending. NeoForge's #c:tools/melee_weapon is the same list for other mods.
        tag(ItemTags.WEAPON_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(ItemTags.MACE_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(ItemTags.FIRE_ASPECT_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());

        // The Compression Jump set is armour like any other, so it goes in the same four vanilla
        // tags the plain set does and picks up the same enchantments and trims.
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COMPRESSION_JUMP_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COMPRESSION_JUMP_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COMPRESSION_JUMP_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COMPRESSION_JUMP_BOOTS.get());

        // What the inscriber turns into max health.
        tag(ModTags.Items.COMPRESSION_ARMOR).add(
                ModItems.COMPRESSED_COBBLESTONE_HELMET.get(),
                ModItems.COMPRESSED_COBBLESTONE_CHESTPLATE.get(),
                ModItems.COMPRESSED_COBBLESTONE_LEGGINGS.get(),
                ModItems.COMPRESSED_COBBLESTONE_BOOTS.get(),
                ModItems.COMPRESSION_JUMP_HELMET.get(),
                ModItems.COMPRESSION_JUMP_CHESTPLATE.get(),
                ModItems.COMPRESSION_JUMP_LEGGINGS.get(),
                ModItems.COMPRESSION_JUMP_BOOTS.get());

        // What the inscriber turns into attack damage.
        tag(ModTags.Items.COMPRESSION_MELEE_WEAPON).add(
                ModItems.COMPRESSED_COBBLESTONE_SWORD.get(),
                ModItems.COMPRESSED_COBBLESTONE_MACE.get());

        // What the Compression Inscriber will pour energy into: the two tags above, plus the ranged
        // gear, which earns arrow velocity rather than a stat and so has no tag of its own.
        tag(ModTags.Items.INSCRIBABLE)
                .addTag(ModTags.Items.COMPRESSION_ARMOR)
                .addTag(ModTags.Items.COMPRESSION_MELEE_WEAPON)
                .add(ModItems.COMPRESSED_COBBLESTONE_BOW.get(),
                        ModItems.COMPRESSED_COBBLESTONE_ARROW.get());
    }
}
