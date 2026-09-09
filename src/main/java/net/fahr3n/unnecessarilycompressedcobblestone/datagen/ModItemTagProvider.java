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

        // The broken sword is a sword in every list the ordinary one is in. It arrives already
        // carrying every enchantment there is, so being enchantable buys it nothing - but it is
        // still what other mods read to know what kind of weapon it is, and what the Reach
        // engraving's #c:tools test reaches it through.
        tag(ItemTags.SWORDS).add(ModItems.BROKEN_COMPRESSED_SWORD.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.BROKEN_COMPRESSED_SWORD.get());

        // The dev sword is a sword in every list too, for the same reason the broken one is: what
        // it is has to be legible to everything that reads a weapon, even though enchanting a blade
        // that already deals the largest number a float holds changes nothing. It is deliberately
        // *not* in #ucc:inscribable - the inscriber's whole trade is a point of attack damage per
        // digit, and there is no point to add to a figure that is already the ceiling.
        tag(ItemTags.SWORDS).add(ModItems.DEV_SWORD.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.DEV_SWORD.get());

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
                ModItems.SUPER_COMPRESSED_ARROW.get(),
                ModItems.HYPER_COMPRESSED_ARROW.get());

        // The mace is listed item by item in each of these the same way vanilla lists its own:
        // #minecraft:enchantable/weapon is what Compression itself hangs off, /mace is Density,
        // Breach and Wind Burst, /fire_aspect is Fire Aspect and /durability is Unbreaking and
        // Mending. NeoForge's #c:tools/melee_weapon is the same list for other mods.
        tag(ItemTags.WEAPON_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(ItemTags.MACE_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(ItemTags.FIRE_ASPECT_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.COMPRESSED_COBBLESTONE_MACE.get());

        // The shield, in the two lists vanilla puts minecraft:shield in item by item -
        // #minecraft:enchantable/durability is Unbreaking and Mending, and through it
        // #minecraft:enchantable/vanishing - plus NeoForge's #c:tools/shield for other mods.
        // Being unbreakable does not make it wrong to be mendable: an enchantment that does
        // nothing is better than a shield other mods cannot recognise.
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.COMPRESSED_SHIELD.get());
        tag(Tags.Items.TOOLS_SHIELD).add(ModItems.COMPRESSED_SHIELD.get());

        // The katana is a sword in every list a sword is in: #minecraft:swords routes it into the
        // same six enchantable tags the compressed sword gets its enchantments from, and
        // #c:tools/melee_weapon is the same statement for other mods. Its reach and its speed are
        // attributes on the item, so nothing about being a sword has to be given up to have them.
        tag(ItemTags.SWORDS).add(ModItems.COMPRESSED_KATANA.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.COMPRESSED_KATANA.get());

        // The scythe is an axe in every list an axe is in: #minecraft:axes routes it into the same
        // enchantable tags a vanilla axe draws Sharpness, Efficiency and the rest from, and
        // #c:tools/melee_weapon is the same statement for other mods. The bleed it applies is the
        // item's own behaviour and costs it none of that.
        // The spear is a sword in every list a sword is in - #minecraft:swords routes it into the
        // six enchantable tags the compressed sword draws from, and #c:tools/melee_weapon is the
        // same statement for other mods. Its reach, its minimum range and its charge cost it none
        // of that: the first is an attribute on the item and the other two are the item's own
        // behaviour.
        tag(ItemTags.SWORDS).add(ModItems.COMPRESSED_SPEAR.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.COMPRESSED_SPEAR.get());

        tag(ItemTags.AXES).add(ModItems.COMPRESSED_SCYTHE.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.COMPRESSED_SCYTHE.get());

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

        tag(ItemTags.HEAD_ARMOR).add(ModItems.COMPRESSION_MAGIC_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COMPRESSION_MAGIC_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COMPRESSION_MAGIC_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COMPRESSION_MAGIC_BOOTS.get());

        // And the Compression Rain set, in the same four.
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COMPRESSION_RAIN_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COMPRESSION_RAIN_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COMPRESSION_RAIN_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COMPRESSION_RAIN_BOOTS.get());

        // And the Ultimate Compressed set, in the same four. Being in these is also what puts each
        // piece within reach of the armour engravings, which are written against them rather than
        // against any class here.
        tag(ItemTags.HEAD_ARMOR).add(ModItems.ULTIMATE_COMPRESSED_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.ULTIMATE_COMPRESSED_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.ULTIMATE_COMPRESSED_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.ULTIMATE_COMPRESSED_BOOTS.get());

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
                ModItems.COMPRESSION_ARROW_BOOTS.get(),
                ModItems.COMPRESSION_MAGIC_HELMET.get(),
                ModItems.COMPRESSION_MAGIC_CHESTPLATE.get(),
                ModItems.COMPRESSION_MAGIC_LEGGINGS.get(),
                ModItems.COMPRESSION_MAGIC_BOOTS.get(),
                ModItems.COMPRESSION_RAIN_HELMET.get(),
                ModItems.COMPRESSION_RAIN_CHESTPLATE.get(),
                ModItems.COMPRESSION_RAIN_LEGGINGS.get(),
                ModItems.COMPRESSION_RAIN_BOOTS.get(),
                ModItems.ULTIMATE_COMPRESSED_HELMET.get(),
                ModItems.ULTIMATE_COMPRESSED_CHESTPLATE.get(),
                ModItems.ULTIMATE_COMPRESSED_LEGGINGS.get(),
                ModItems.ULTIMATE_COMPRESSED_BOOTS.get());

        // What the inscriber turns into attack damage.
        tag(ModTags.Items.COMPRESSION_MELEE_WEAPON).add(
                ModItems.COMPRESSED_COBBLESTONE_SWORD.get(),
                ModItems.BROKEN_COMPRESSED_SWORD.get(),
                ModItems.COMPRESSED_COBBLESTONE_MACE.get(),
                ModItems.COMPRESSED_KATANA.get(),
                ModItems.COMPRESSED_SCYTHE.get(),
                // The spear earns its point per digit on both attacks. The jab gets it through this
                // tag like every other weapon here; the charge never reads the attack damage
                // attribute at all, so it adds the same bonus itself - see CompressedSpearItem.
                ModItems.COMPRESSED_SPEAR.get());

        // Every apple is food, and the closest thing to an apple the common tags have is fruit.
        // Being in these is what lets other mods' recipes and effects see them at all.
        tag(Tags.Items.FOODS).add(
                ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_4_COMPRESSED_COBBLESTONE_APPLE.get());
        tag(Tags.Items.FOODS_FRUIT).add(
                ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get(),
                ModItems.TIER_4_COMPRESSED_COBBLESTONE_APPLE.get());

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
        tag(ItemTags.PICKAXES).add(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get(),
                ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get());
        tag(Tags.Items.MINING_TOOL_TOOLS).add(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get(),
                ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get());

        // Everything a Lightning Core will take, and so everything the Bolt Launcher will fire.
        // The eighty-eight keys are added by walking the list rather than by naming them.
        var bolts = tag(ModTags.Items.BOLTS).add(ModItems.COMPRESSED_VANILLA_BOLT.get(),
                ModItems.MOONLIGHT_BOLT.get(), ModItems.COMPOSITION_BOLT.get(),
                ModItems.REST_BOLT.get(), ModItems.LONG_REST_BOLT.get());
        ModItems.NOTE_BOLTS.forEach(bolt -> bolts.add(bolt.get()));

        // The launcher is a bow in every list a bow is in, so Power, Punch, Flame, Infinity,
        // Unbreaking and Mending all reach it off a table or an anvil with nothing further to do.
        tag(ItemTags.BOW_ENCHANTABLE).add(ModItems.BOLT_LAUNCHER.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.BOLT_LAUNCHER.get());
        tag(Tags.Items.TOOLS_BOW).add(ModItems.BOLT_LAUNCHER.get());
        tag(Tags.Items.RANGED_WEAPON_TOOLS).add(ModItems.BOLT_LAUNCHER.get());

        // The TNT Launcher is a bow for enchanting and a ranged weapon for the Extra Shot
        // engravings, and deliberately not in #c:tools/bow: the engravings written against that tag
        // are about arrows - the Sniper's flat shot, the Potion engraving's tipped one - and neither
        // has anything to say about a block of TNT. A weapon should not be offered an upgrade that
        // goes on and does nothing.
        tag(ItemTags.BOW_ENCHANTABLE).add(ModItems.TNT_LAUNCHER.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.TNT_LAUNCHER.get());
        tag(Tags.Items.RANGED_WEAPON_TOOLS).add(ModItems.TNT_LAUNCHER.get());

        // Vanilla lists minecraft:fishing_rod item by item in each of these rather than going
        // through a tag, so the rod has to be added to each: #minecraft:enchantable/fishing is Lure,
        // Luck of the Sea and this mod's own Fishing and Hook, and #minecraft:enchantable/durability
        // is Unbreaking and Mending (and, through it, #minecraft:enchantable/vanishing).
        tag(ItemTags.FISHING_ENCHANTABLE).add(ModItems.COMPRESSED_FISHING_ROD.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.COMPRESSED_FISHING_ROD.get());
        tag(Tags.Items.TOOLS_FISHING_ROD).add(ModItems.COMPRESSED_FISHING_ROD.get());

        // The one tag that opens the hardened levels. Nothing else in the game can touch them. The
        // tier 2 pickaxe is in it as well as in the tag below: a tool that reached the deeper stone
        // and not the shallower one would be a strange thing to hold.
        tag(ModTags.Items.HARDENED_MINING).add(ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get(),
                ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get());

        // And the one tag that opens the second floor, from ModBlocks.HARDENED_LEVEL_TIER_2 up.
        // The pickaxe above it cannot get in there, which is the whole of what the tag says.
        tag(ModTags.Items.HARDENED_MINING_TIER_2).add(ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get());

        // What Harvest Festival goes on: every hoe in the game by vanilla's own list, and the
        // scythe, which is the one thing here that clears a field without being one.
        tag(ModEnchantments.HARVEST_ENCHANTABLE)
                .addTag(ItemTags.HOES)
                .add(ModItems.COMPRESSED_SCYTHE.get());

        // What the staff's three enchantments go on, and the two vanilla lists that make the staff
        // mendable and unbreakable-enchantable like any other tool.
        tag(ModEnchantments.STAFF_ENCHANTABLE).add(ModItems.COMPRESSED_LIGHTNING_STAFF.get(),
                ModItems.COMPRESSED_ARROW_STAFF.get(), ModItems.COMPRESSED_ARROW_TNT_STAFF.get(),
                ModItems.COMPRESSED_SUMMONING_STAFF.get(),
                // The healing staff takes all three even though Silent Cast can do nothing with a
                // staff that has no cast: the tag is what a staff *is*, and leaving it out would
                // also cost it Surge and Multicast, both of which mean something here.
                ModItems.COMPRESSED_HEALING_STAFF.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.COMPRESSED_LIGHTNING_STAFF.get(),
                ModItems.COMPRESSED_ARROW_STAFF.get(), ModItems.COMPRESSED_ARROW_TNT_STAFF.get(),
                ModItems.COMPRESSED_SUMMONING_STAFF.get(),
                ModItems.COMPRESSED_HEALING_STAFF.get());

        // The laser is not a staff and takes none of the staff enchantments - its charge and its
        // damage are the augments' business, which is the whole point of that system - but it is
        // still a piece of gear that wears out in principle, so Unbreaking and Mending reach it the
        // way they reach everything else here.
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.RAY_OF_LASER.get());

        // What the Compression Inscriber will pour energy into: the two tags above, plus the ranged
        // gear, which earns arrow velocity rather than a stat and so has no tag of its own.
        tag(ModTags.Items.INSCRIBABLE)
                .addTag(ModTags.Items.COMPRESSION_ARMOR)
                .addTag(ModTags.Items.COMPRESSION_MELEE_WEAPON)
                .add(ModItems.COMPRESSED_COBBLESTONE_BOW.get(),
                        ModItems.COMPRESSED_COBBLESTONE_ARROW.get(),
                        ModItems.SUPER_COMPRESSED_ARROW.get(),
                        ModItems.HYPER_COMPRESSED_ARROW.get(),
                        // The staff earns lightning damage rather than a stat, so like the ranged
                        // gear it is listed here directly and reads its own energy when it fires.
                        ModItems.COMPRESSED_LIGHTNING_STAFF.get(),
                        // The arrow staff earns velocity, the way the bow does.
                        ModItems.COMPRESSED_ARROW_STAFF.get(),
                        // And the TNT one earns the size of every blast in its rain.
                        ModItems.COMPRESSED_ARROW_TNT_STAFF.get(),
                        // The healing staff earns healing, a point per digit. Like the lightning
                        // staff it is listed here directly rather than joining a stat tag: what it
                        // buys is neither armour, damage nor velocity.
                        ModItems.COMPRESSED_HEALING_STAFF.get(),
                        // The summoning staff earns the health and the damage of the phantom it
                        // calls up: a point of health and a quarter of a point of damage per digit.
                        ModItems.COMPRESSED_SUMMONING_STAFF.get(),
                        // The launcher earns lightning damage, the way the lightning staff does -
                        // it is the one piece of ranged gear here whose energy is not velocity,
                        // because the strike is the hit and a faster bolt does not hit harder.
                        ModItems.BOLT_LAUNCHER.get(),
                        // The TNT Launcher earns velocity, the way the bow does: the blast belongs
                        // to whatever was loaded into it, so range is the only thing left to buy.
                        ModItems.TNT_LAUNCHER.get(),
                        // The pickaxe earns mining speed the same way, and so does the tier 2 one.
                        ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get(),
                        ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get(),
                        // The laser earns lightning-straight damage on its beam, a point per digit -
                        // it is listed here rather than joining a stat tag for the reason the Bolt
                        // Launcher is: a beam has no velocity to buy, and the strike is the hit.
                        ModItems.RAY_OF_LASER.get(),
                        // And the rod earns damage on its hook, which is the only stat a bobber
                        // has - it is not a melee weapon, so it is listed here rather than joining
                        // #compression_melee_weapon and picking up an attack damage modifier.
                        ModItems.COMPRESSED_FISHING_ROD.get());
    }
}
