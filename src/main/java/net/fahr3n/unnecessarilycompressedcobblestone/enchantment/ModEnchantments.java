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

    /** How many times a second a staff's cast goes off: the level itself, not one plus it. */
    public static final ResourceKey<Enchantment> MULTICAST = key("multicast");

    /**
     * Ten percent more of whatever a staff's power is, per level, compounded - lightning damage on
     * one staff, arrow speed on the other.
     */
    public static final ResourceKey<Enchantment> SURGE = key("surge");

    /** A tenth off the cast per level, to a floor of thirty percent of it. */
    public static final ResourceKey<Enchantment> SILENT_CAST = key("silent_cast");

    /**
     * How many times a fishing rod rolls its catch: {@code level + 1} times what the rod rolls on
     * its own, every roll independent of the others, so it is more catches rather than a better one.
     */
    public static final ResourceKey<Enchantment> FISHING = key("fishing");

    /** Twenty percent more damage on a hook that strikes something, per level, compounded. */
    public static final ResourceKey<Enchantment> HOOK = key("hook");

    /**
     * Sixty-four times the yield out of a ripe crop broken with a hoe or a scythe. What counts as a
     * crop, and what counts as ripe, both live in {@code ModEvents}.
     */
    public static final ResourceKey<Enchantment> HARVEST_FESTIVAL = key("harvest_festival");

    /**
     * The two fishing enchantments. Unlike the Compression family and the staff's three, both stop
     * where the enchanting table stops - there is no crafted rung above them and so no book recipe,
     * which is why they are a plain list of numbers rather than another record.
     *
     * @param key      the enchantment itself
     * @param maxLevel the deepest level, which the table and the anvil both reach
     * @param weight   how often the table offers it against everything else it could
     */
    public record FishingEnchantment(ResourceKey<Enchantment> key, int maxLevel, int weight) {
    }

    /**
     * Both of them go on {@code #minecraft:enchantable/fishing}, which is vanilla's own list and
     * holds every fishing rod in the game - so these reach another mod's rod exactly the way Lure
     * and Luck of the Sea do, with nothing here naming an item.
     */
    public static final List<FishingEnchantment> FISHING_ENCHANTMENTS = List.of(
            // Levels 1-3: two, three and four times the catch. Rarer than Hook, since a rod pulling
            // four independent catches is worth far more than one that hits harder.
            new FishingEnchantment(FISHING, 3, 2),
            // Levels 1-5: 1.2^5, so a fifth over twice the damage at the top.
            new FishingEnchantment(HOOK, 5, 5));

    /**
     * The staff's three enchantments and the highest level the enchanting table hands out for each.
     * That number is also each one's registered max level, which is what makes the cap absolute:
     * neither the table nor the anvil will climb past a registered maximum.
     */
    public static final List<StaffEnchantment> STAFF_ENCHANTMENTS = List.of(
            // Levels 1-2 from the table, 3-5 from a book: one more activation a second each time.
            new StaffEnchantment(MULTICAST, 2, 4, new int[] {55, 89, 117}),
            // Levels 1-5 from the table, 6-10 from a book: ten percent more power each, compounded.
            new StaffEnchantment(SURGE, 5, 6, new int[] {52, 62, 82, 94, 101}),
            // Levels 1-4 from the table, 5-7 from a book. Seven is seventy percent off, which is the
            // floor a staff clamps at, so it is the last rung there is any point in.
            new StaffEnchantment(SILENT_CAST, 4, 4, new int[] {54, 77, 95}));

    /**
     * One of the staff's enchantments: how far the enchanting table gets on its own, and the
     * compressed cobblestone tier each book above that is wrapped around. The enchantment is
     * registered at the table cap, so neither the table nor the anvil ever climbs to a crafted rung -
     * the only way up is the book, exactly as it is for the Compression family. Book tiers must stay
     * disjoint from every other book's, since the one {@link #BOOK_PATTERN} grid means the tier is
     * all that tells two books apart.
     *
     * @param key           the enchantment itself
     * @param tableMaxLevel the deepest level the table and the vanilla anvil reach
     * @param weight        how often the table offers it against everything else it could
     * @param bookTiers     the block tier each book from {@link #firstCraftedLevel()} upwards uses
     */
    public record StaffEnchantment(ResourceKey<Enchantment> key, int tableMaxLevel, int weight, int[] bookTiers) {
        /** The lowest level that has a book recipe: the first one past the table. */
        public int firstCraftedLevel() {
            return this.tableMaxLevel + 1;
        }

        /** The deepest level this enchantment goes to, which is the last book there is a recipe for. */
        public int maxLevel() {
            return this.tableMaxLevel + this.bookTiers.length;
        }

        /** The compressed cobblestone tier the book for {@code level} is crafted from. */
        public int bookTier(int level) {
            return this.bookTiers[level - firstCraftedLevel()];
        }
    }

    /** The staff rung {@code holder} belongs to, or null if it is not one of the staff's own. */
    public static StaffEnchantment staff(Holder<Enchantment> holder) {
        for (StaffEnchantment staff : STAFF_ENCHANTMENTS) {
            if (holder.is(staff.key())) {
                return staff;
            }
        }

        return null;
    }

    /** Whether this enchantment has levels that only a crafted book can produce. */
    public static boolean hasCraftedLevels(Holder<Enchantment> holder) {
        return family(holder) != null || staff(holder) != null;
    }

    /**
     * Whether {@code level} of {@code holder} is one of those crafted levels - the test the anvil
     * needs, since a crafted level must neither be clamped away nor combined up to.
     */
    public static boolean isCraftedLevel(Holder<Enchantment> holder, int level) {
        Family family = family(holder);
        if (family != null) {
            return family.isCrafted(level);
        }

        StaffEnchantment staff = staff(holder);
        return staff != null && level > staff.tableMaxLevel();
    }

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
                    new int[] {38, 46, 53, 63, 74, 78, 86, 96, 106, 114, 122}),
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

    /**
     * What the staff's enchantments go on. It is a tag rather than the item itself so that a second
     * staff, or another mod's, can join them by joining the tag.
     */
    public static final TagKey<Item> STAFF_ENCHANTABLE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "enchantable/staff"));

    /**
     * What Harvest Festival goes on: every hoe in the game, by vanilla's own
     * {@code #minecraft:hoes}, plus the Compressed Scythe, which is the one thing in this mod that
     * clears a field without being a hoe. A tag rather than the two of them named in code, so
     * another mod's harvesting tool joins by joining it.
     */
    public static final TagKey<Item> HARVEST_ENCHANTABLE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "enchantable/harvest"));

    /**
     * An enchantment with no table levels at all: one rung, on one book, wrapped around one block
     * tier. It is the smallest shape a crafted enchantment comes in - no ladder, so no tier table
     * and nothing for the anvil to climb - and it is registered at its true maximum, which is what
     * lets vanilla's own anvil handle the book and keeps {@code ModEvents} out of it entirely.
     * <p>
     * Book tiers must still stay disjoint from every other book's, since the one
     * {@link #BOOK_PATTERN} grid means the tier is all that tells two books apart.
     *
     * @param key       the enchantment itself
     * @param supported what it may go on
     * @param maxLevel  the deepest level, which is also how many books there are
     * @param bookTier  the compressed cobblestone tier its book is crafted from
     */
    public record CraftedEnchantment(ResourceKey<Enchantment> key, TagKey<Item> supported, int maxLevel,
                                     int bookTier) {
    }

    /**
     * Every craft-only enchantment that is not part of a family. Harvest Festival is the first, and
     * it has one level because there is nothing above sixty-four times a crop worth having: a second
     * rung would be stacks of stacks off one wheat plant.
     */
    public static final List<CraftedEnchantment> CRAFTED_ENCHANTMENTS = List.of(
            new CraftedEnchantment(HARVEST_FESTIVAL, HARVEST_ENCHANTABLE, 1, 225));

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

        // The two fishing ones, on vanilla's own fishing tag. Like the staff's three they carry no
        // effect components: "roll the loot table again" and "multiply the damage a bobber does"
        // are not things vanilla's effect components can describe, so ModEvents reads the level off
        // the rod at the moment it matters.
        for (FishingEnchantment fishing : FISHING_ENCHANTMENTS) {
            register(context, fishing.key(), Enchantment.enchantment(Enchantment.definition(
                    items.getOrThrow(ItemTags.FISHING_ENCHANTABLE),
                    items.getOrThrow(ItemTags.FISHING_ENCHANTABLE),
                    fishing.weight(),
                    fishing.maxLevel(),
                    Enchantment.dynamicCost(15, 9),
                    Enchantment.dynamicCost(65, 9),
                    4,
                    EquipmentSlotGroup.MAINHAND)));
        }

        // The craft-only singles. Harvest Festival carries no effect component either: "sixty-four
        // times whatever the loot table produced" is not a thing vanilla's effect components can
        // describe, so ModEvents reads the level off the tool at the moment the block breaks.
        for (CraftedEnchantment crafted : CRAFTED_ENCHANTMENTS) {
            register(context, crafted.key(), Enchantment.enchantment(Enchantment.definition(
                    items.getOrThrow(crafted.supported()),
                    items.getOrThrow(crafted.supported()),
                    1,
                    crafted.maxLevel(),
                    Enchantment.dynamicCost(15, 9),
                    Enchantment.dynamicCost(65, 9),
                    4,
                    EquipmentSlotGroup.MAINHAND)));
        }

        // The staff's own three. They carry no effect components at all: what each of them does is
        // read off the level by the staff itself at the moment of the cast, because none of them -
        // an extra bolt, a multiplied bolt, a shorter cast - is a thing vanilla's effect components
        // can describe.
        for (StaffEnchantment staff : STAFF_ENCHANTMENTS) {
            register(context, staff.key(), Enchantment.enchantment(Enchantment.definition(
                    items.getOrThrow(STAFF_ENCHANTABLE),
                    items.getOrThrow(STAFF_ENCHANTABLE),
                    staff.weight(),
                    staff.tableMaxLevel(),
                    Enchantment.dynamicCost(8, 10),
                    Enchantment.dynamicCost(40, 10),
                    2,
                    EquipmentSlotGroup.MAINHAND)));
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
