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
        // public static final TagKey<Block> SOME_BLOCK_TAG = createTag("some_block_tag");

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

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
        }
    }
}
