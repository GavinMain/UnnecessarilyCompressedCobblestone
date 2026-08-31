package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
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
        tag(ItemTags.ARROWS).add(ModItems.COMPRESSED_COBBLESTONE_ARROW.get(),
                ModItems.SUPER_COMPRESSED_ARROW.get());

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

        // The Compression Lightning set, in the same four vanilla tags as every other armour here.
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COMPRESSION_LIGHTNING_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COMPRESSION_LIGHTNING_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COMPRESSION_LIGHTNING_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COMPRESSION_LIGHTNING_BOOTS.get());

        // And the Compression Arrow set, in the same four.
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COMPRESSION_ARROW_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COMPRESSION_ARROW_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COMPRESSION_ARROW_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COMPRESSION_ARROW_BOOTS.get());

        // What the inscriber turns into max health.
        tag(ModTags.Items.COMPRESSION_ARMOR).add(
                ModItems.COMPRESSED_COBBLESTONE_HELMET.get(),
                ModItems.COMPRESSED_COBBLESTONE_CHESTPLATE.get(),
                ModItems.COMPRESSED_COBBLESTONE_LEGGINGS.get(),
                ModItems.COMPRESSED_COBBLESTONE_BOOTS.get(),
                ModItems.COMPRESSION_JUMP_HELMET.get(),
                ModItems.COMPRESSION_JUMP_CHESTPLATE.get(),
                ModItems.COMPRESSION_JUMP_LEGGINGS.get(),
                ModItems.COMPRESSION_JUMP_BOOTS.get(),
                ModItems.COMPRESSION_LIGHTNING_HELMET.get(),
                ModItems.COMPRESSION_LIGHTNING_CHESTPLATE.get(),
                ModItems.COMPRESSION_LIGHTNING_LEGGINGS.get(),
                ModItems.COMPRESSION_LIGHTNING_BOOTS.get(),
                ModItems.COMPRESSION_ARROW_HELMET.get(),
                ModItems.COMPRESSION_ARROW_CHESTPLATE.get(),
                ModItems.COMPRESSION_ARROW_LEGGINGS.get(),
                ModItems.COMPRESSION_ARROW_BOOTS.get());

        // What the inscriber turns into attack damage.
        tag(ModTags.Items.COMPRESSION_MELEE_WEAPON).add(
                ModItems.COMPRESSED_COBBLESTONE_SWORD.get(),
                ModItems.COMPRESSED_COBBLESTONE_MACE.get());

        // Every apple is food, and the closest thing to an apple the common tags have is fruit.
        // Being in these is what lets other mods' recipes and effects see them at all.
        tag(Tags.Items.FOODS).add(
                ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get());
        tag(Tags.Items.FOODS_FRUIT).add(
                ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get());

        // What the Apple TNT throws. It is #c:foods/fruit plus vanilla's three apples by name, so
        // any mod that tags its own fruit is in the throw without knowing this mod exists; a pack
        // that wants apples alone can narrow this one tag and nothing in code has to change.
        tag(ModTags.Items.APPLES)
                .addTag(Tags.Items.FOODS_FRUIT)
                .add(Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);

        // #minecraft:pickaxes is what makes this a pickaxe to the rest of the game: vanilla routes
        // it into #minecraft:enchantable/mining, /durability, /vanishing and /mining_loot, so
        // Efficiency, Fortune, Silk Touch, Unbreaking and Mending all come off a table or an anvil
        // with nothing further to do. NeoForge's #c:tools/mining_tool is the same list for mods.
        tag(ItemTags.PICKAXES).add(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get());
        tag(Tags.Items.MINING_TOOL_TOOLS).add(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get());

        // Everything a Lightning Core will take.
        tag(ModTags.Items.BOLTS).add(ModItems.COMPRESSED_VANILLA_BOLT.get());

        // The one tag that opens the hardened levels. Nothing else in the game can touch them.
        tag(ModTags.Items.HARDENED_MINING).add(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get());

        // What the staff's three enchantments go on, and the two vanilla lists that make the staff
        // mendable and unbreakable-enchantable like any other tool.
        tag(ModEnchantments.STAFF_ENCHANTABLE).add(ModItems.COMPRESSED_LIGHTNING_STAFF.get(),
                ModItems.COMPRESSED_ARROW_STAFF.get(), ModItems.COMPRESSED_ARROW_TNT_STAFF.get(),
                ModItems.COMPRESSED_SUMMONING_STAFF.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.COMPRESSED_LIGHTNING_STAFF.get(),
                ModItems.COMPRESSED_ARROW_STAFF.get(), ModItems.COMPRESSED_ARROW_TNT_STAFF.get(),
                ModItems.COMPRESSED_SUMMONING_STAFF.get());

        // What the Compression Inscriber will pour energy into: the two tags above, plus the ranged
        // gear, which earns arrow velocity rather than a stat and so has no tag of its own.
        tag(ModTags.Items.INSCRIBABLE)
                .addTag(ModTags.Items.COMPRESSION_ARMOR)
                .addTag(ModTags.Items.COMPRESSION_MELEE_WEAPON)
                .add(ModItems.COMPRESSED_COBBLESTONE_BOW.get(),
                        ModItems.COMPRESSED_COBBLESTONE_ARROW.get(),
                        ModItems.SUPER_COMPRESSED_ARROW.get(),
                        // The staff earns lightning damage rather than a stat, so like the ranged
                        // gear it is listed here directly and reads its own energy when it fires.
                        ModItems.COMPRESSED_LIGHTNING_STAFF.get(),
                        // The arrow staff earns velocity, the way the bow does.
                        ModItems.COMPRESSED_ARROW_STAFF.get(),
                        // And the TNT one earns the size of every blast in its rain.
                        ModItems.COMPRESSED_ARROW_TNT_STAFF.get(),
                        // The summoning staff earns the health and the damage of the phantom it
                        // calls up: a point of health and a quarter of a point of damage per digit.
                        ModItems.COMPRESSED_SUMMONING_STAFF.get(),
                        // The pickaxe earns mining speed the same way.
                        ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get());
    }
}
