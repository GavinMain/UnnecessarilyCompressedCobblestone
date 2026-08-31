package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        /**
         * Every block the Random TNT may copy its detonation from. Nothing in the game ships a tag
         * of TNT blocks - neither vanilla nor NeoForge has one - so this is the mod's own, and it is
         * what the Random TNT reads rather than any list in code: another mod's TNT joins the draw
         * by being added to this tag from a datapack, and this mod's own TNTs are put in it by
         * {@code ModBlockTagProvider} as they are added.
         */
        public static final TagKey<Block> TNT = createTag("tnt");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
        }
    }

    public static class Items {
        /**
         * Gear the Compression Inscriber will pour energy into. It is built out of the tags below
         * rather than listing items, so a new piece of gear only has to join the tag that says what
         * kind of thing it is and the inscriber picks it up with no further change.
         */
        public static final TagKey<Item> INSCRIBABLE = createTag("inscribable");

        /** Armour that turns its Compression Energy into max health, one heart point per digit. */
        public static final TagKey<Item> COMPRESSION_ARMOR = createTag("compression_armor");

        /** Weapons that turn their Compression Energy into attack damage, one point per digit. */
        public static final TagKey<Item> COMPRESSION_MELEE_WEAPON = createTag("compression_melee_weapon");

        /**
         * What a Lightning Core will accept. The core reads the item class rather than this tag -
         * a bolt has to be a {@code BoltItem} to know what lightning it makes - but the tag is what
         * a recipe, a mod or a datapack can see the family through.
         */
        public static final TagKey<Item> BOLTS = createTag("bolts");

        /**
         * Tools that can mine compressed cobblestone at {@code ModBlocks.HARDENED_LEVEL} and above.
         * Those blocks are unbreakable to everything else in the game, and this tag is the only
         * exception to that, so another tool - this mod's or anyone's - earns the right by joining
         * it and needs no code of its own.
         */
        public static final TagKey<Item> HARDENED_MINING = createTag("hardened_mining");

        /**
         * What the Apple TNT throws. This is built on top of NeoForge's {@code #c:foods/fruit},
         * which is the finest cross-mod tag that exists for this - there is no common tag for
         * apples alone - so another mod's apples come along as soon as that mod tags its own food,
         * at the price of its melons and chorus fruit coming with them.
         */
        public static final TagKey<Item> APPLES = createTag("apples");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
        }
    }
}
