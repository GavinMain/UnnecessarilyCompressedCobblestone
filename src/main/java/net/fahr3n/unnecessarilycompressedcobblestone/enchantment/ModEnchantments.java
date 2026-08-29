package net.fahr3n.unnecessarilycompressedcobblestone.enchantment;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.custom.CompressionEnchantmentEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> COMPRESSION = key("compression");
    public static final ResourceKey<Enchantment> SUPER_COMPRESSION = key("super_compression");
    public static final ResourceKey<Enchantment> HYPER_COMPRESSION = key("hyper_compression");
    public static final ResourceKey<Enchantment> GIGA_COMPRESSION = key("giga_compression");

    /**
     * The four Compression enchantments are variations on one another, so a weapon only ever
     * carries one of them. The tag is hand-written under {@code data/.../tags/enchantment/}, the way
     * vanilla writes {@code #minecraft:exclusive_set/damage}.
     */
    public static final TagKey<Enchantment> EXCLUSIVE_SET_COMPRESSION = TagKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "exclusive_set/compression"));

    /**
     * The highest level the enchanting table and the anvil hand out on their own. It is also
     * Compression's registered max level, which is what makes that cap absolute: the table never
     * offers a level above it and the anvil clamps every combination to it.
     */
    public static final int MAX_TABLE_LEVEL = 3;

    /**
     * The levels only a crafted Compression book can carry, above what the table and anvil reach on
     * their own. Vanilla has no way to produce or even carry over a level this high, so the anvil
     * side of it lives in {@code ModEvents}.
     */
    public static final int FIRST_CRAFTED_LEVEL = MAX_TABLE_LEVEL + 1;

    /**
     * The grid every Compression book of every rung is made in: a layer of compressed cobblestone
     * bound top and bottom in leather. Only the tier of the {@code C} changes, so no two books share
     * a tier - the book tier tables below are disjoint, which is what keeps one shape enough.
     */
    public static final List<String> BOOK_PATTERN = List.of("LLL",
                                                            "CCC",
                                                            "LLL");

    /**
     * One rung of the Compression family: which enchantment it is, how deep a block each level
     * squeezes out, how far the enchanting table gets on its own, and the compressed cobblestone
     * tier each crafted book is wrapped around.
     *
     * @param key               the enchantment itself
     * @param tierPerLevel      compression levels squeezed out per enchantment level
     * @param tableMaxLevel     the highest level the table and the vanilla anvil reach, 0 if the
     *                          enchantment is craft-only
     * @param firstCraftedLevel the lowest level that has a book recipe
     * @param bookTiers         the block tier each book from {@code firstCraftedLevel} upwards uses
     */
    public record Family(ResourceKey<Enchantment> key, int tierPerLevel, int tableMaxLevel,
                         int firstCraftedLevel, int[] bookTiers) {
        /** The deepest level this rung goes to, which is the last book there is a recipe for. */
        public int maxLevel() {
            return this.firstCraftedLevel + this.bookTiers.length - 1;
        }

        /**
         * What the enchantment is registered with. Compression stops at the table cap so that
         * neither the table nor the vanilla anvil can climb past it; the craft-only rungs have no
         * table to hold back, so they are registered at their true depth.
         */
        public int registeredMaxLevel() {
            return this.tableMaxLevel > 0 ? this.tableMaxLevel : this.maxLevel();
        }

        /** The compressed cobblestone tier the book for {@code level} is crafted from. */
        public int bookTier(int level) {
            return this.bookTiers[level - this.firstCraftedLevel];
        }

        /** Whether {@code level} is past what the table and the vanilla anvil can produce. */
        public boolean isCrafted(int level) {
            return level > this.tableMaxLevel;
        }
    }

    /**
     * Every rung, deepest last. Compression is the only one the enchanting table offers - it is the
     * only entry in {@code #minecraft:in_enchanting_table} - and the only one whose lower levels
     * behave like an ordinary enchantment.
     */
    public static final List<Family> FAMILIES = List.of(
            // Levels 1-3 from the table, 4-10 from a book: one block per level, at its own depth.
            new Family(COMPRESSION, 1, MAX_TABLE_LEVEL, FIRST_CRAFTED_LEVEL,
                    new int[] {6, 10, 14, 19, 24, 29, 33}),
            // Ten tiers a level: Super Compression IV drops four tier 40 blocks.
            new Family(SUPER_COMPRESSION, 10, 0, 1,
                    new int[] {38, 46, 53, 63, 70, 78, 86, 96, 106, 114, 122}),
            // Twenty tiers a level: Hyper Compression IV drops four tier 80 blocks.
            new Family(HYPER_COMPRESSION, 20, 0, 1,
                    new int[] {129, 137, 145, 153, 159, 170, 177, 189, 195, 204, 214, 221}),
            // The end of the line: one tier 250 block, wrapped out of tier 250 blocks.
            new Family(GIGA_COMPRESSION, 250, 0, 1,
                    new int[] {250}));

    /** The rung {@code holder} belongs to, or null if it is not a Compression enchantment. */
    public static Family family(Holder<Enchantment> holder) {
        for (Family family : FAMILIES) {
            if (holder.is(family.key())) {
                return family;
            }
        }

        return null;
    }

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);

        for (Family family : FAMILIES) {
            // #minecraft:enchantable/weapon is vanilla's melee weapon set - swords, axes and the
            // mace - so this goes on vanilla gear and on the mod's own sword (which is in
            // #minecraft:swords) alike, and on nothing that is not swung at something.
            register(context, family.key(), Enchantment.enchantment(Enchantment.definition(
                    items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                    items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                    5,
                    family.registeredMaxLevel(),
                    Enchantment.dynamicCost(10, 10),
                    Enchantment.dynamicCost(40, 10),
                    2,
                    EquipmentSlotGroup.MAINHAND))
                    .exclusiveWith(enchantments.getOrThrow(EXCLUSIVE_SET_COMPRESSION))
                    .withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER,
                            EnchantmentTarget.VICTIM, new CompressionEnchantmentEffect(family.tierPerLevel())));
        }
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }
}
