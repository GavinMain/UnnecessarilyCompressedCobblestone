package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
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

        /**
         * What the Compressed Golem Tier 2 counts as ground under its feet, and so what it heals
         * standing on. It is a tag rather than a list of blocks so a datapack can say what counts as
         * ground in whatever the golem is being fought in; what it must never contain is anything
         * that can be flooded away, since taking its footing is the whole answer to the fight.
         */
        public static final TagKey<Block> GOLEM_GROUND = createTag("golem_ground");

        /**
         * What Harvest Festival counts as a crop. Vanilla's own {@code #minecraft:crops} is the
         * bulk of it and is included wholesale, but it is only the things that grow on farmland -
         * nether wart, cocoa and the berry bush are crops by every reading except that tag's, and
         * another mod's crop is a crop the day a datapack adds it here. Whether a member is
         * <em>ready</em> is not a tag question and is read off the block's own age; see
         * {@code ModEvents.isRipe}.
         */
        public static final TagKey<Block> HARVEST = createTag("harvest");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
        }
    }

    public static class DamageTypes {
        /**
         * What the Compression Magic set counts as a potion doing it.
         * <p>
         * It is "most" rather than "all" on purpose. Every type in here is one a potion is the usual
         * way to meet - harming and poison's magic, wither, the dragon's breath and the freezing
         * this mod brews - while the three left out are the ones a potion only borrows: an explosion
         * is far more often TNT, lightning is far more often weather, and a falling stalactite is
         * far more often a cave. Each of those already has its own answer elsewhere, and covering
         * them here would quietly make this the set that resists everything.
         */
        public static final TagKey<DamageType> POTION_DAMAGE = createTag("potion_damage");

        /**
         * True damage: the types this mod puts in all five {@code bypasses_*} tags that mean "a form
         * of protection does not apply", so armour, shields, Resistance, Protection and every set
         * bonus here miss them.
         * <p>
         * A tag rather than a list of keys because it is asked one question that no
         * {@code bypasses_*} tag can answer: <b>which damage gets past a refusal.</b> A boss that
         * says "only a fall hurts me" or "only lightning hurts me" is stating a rule about kinds of
         * attack, and true damage is the thing that is not a kind of attack - it is the mod saying
         * the number lands whatever the target thinks about it. Every such rule is written against
         * this tag, so a datapack that adds a type to it has armed it against all of them at once,
         * and none of those bosses has to name a type.
         */
        public static final TagKey<DamageType> TRUE_DAMAGE = createTag("true_damage");

        private static TagKey<DamageType> createTag(String name) {
            return TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
        }
    }

    public static class MobEffects {
        /**
         * Every effect that hurts its holder on a clock, and so everything a Compression Bomb
         * collapses into one hit.
         * <p>
         * It is a tag rather than a list in code because there is nothing on a {@code MobEffect} to
         * ask. Vanilla ships no "this one deals damage over time" flag and no category finer than
         * harmful, so "is this a damage over time effect" cannot be read off the registry the way
         * "does this go on a bow" can - which makes it exactly the kind of question a datapack
         * ought to be able to answer. Another mod's poison joins the collapse by being added here
         * and needs no code at either end.
         * <p>
         * What it must not contain is anything whose {@code applyEffectTick} does something other
         * than hurt: the bomb takes the effect off, so a beneficial or stateful effect in this tag
         * would be quietly stripped for nothing. See {@code DamageOverTime}, which is the other
         * half - the tag says which effects count, and that class says what each one is worth.
         */
        public static final TagKey<MobEffect> DAMAGE_OVER_TIME = createTag("damage_over_time");

        private static TagKey<MobEffect> createTag(String name) {
            return TagKey.create(Registries.MOB_EFFECT,
                    ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
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
         * Tools that can mine compressed cobblestone at {@code ModBlocks.HARDENED_LEVEL_TIER_2} and
         * above. Those levels are a second floor under the first: they are unbreakable to
         * everything in {@link #HARDENED_MINING} as well, and only a tool in this tag gets through
         * them. Anything in here should be in {@code #hardened_mining} too, since a tool that opens
         * the deeper stone and not the shallower one would be a strange thing to hold.
         */
        public static final TagKey<Item> HARDENED_MINING_TIER_2 = createTag("hardened_mining_tier_2");

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
