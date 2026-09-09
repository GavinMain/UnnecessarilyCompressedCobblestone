package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * "Every enchantment in the game", at whatever level the item asking for them names.
 * <p>
 * It is worked out at runtime rather than written into a recipe, and that is the whole point.
 * Enchantments are a datapack registry, so what "every enchantment" means is not known until a world
 * is loaded: a stack stamped at datagen time could only ever list the ones this mod and vanilla
 * ship, and would silently miss whatever any other mod adds. Reading the registry off the level
 * instead means an item picks up another mod's enchantments the first time it is held, and picks up
 * new ones again after a datapack reload adds them.
 * <p>
 * Curses are a tag rather than a list, so another mod's curse is left off by its own declaration
 * with nothing here naming it.
 */
public final class FullEnchantment {
    /**
     * The ceiling on any enchantment level: as high as anything in the game can be set, and no
     * item's business but the mixin's.
     * <p>
     * Vanilla's is 255 - the {@code minecraft:enchantments} component's codec is an
     * {@code intRange(0, 255)}, its constructor throws outside that, and its builder silently clamps
     * to it. All three are compiled checks rather than a value anything holds, so this mod moves
     * them with the only tool that can: see {@code mixin.ItemEnchantmentsMixin}, which is the whole
     * reason this mod has a mixin at all.
     * <p>
     * What an item is <em>stamped</em> at is the item's own business and is passed in, because the
     * two that use this are not the same claim. The tier 2 pickaxe takes the ceiling, since being
     * the most enchanted thing there is happens to be what it is for. The Broken Compressed Sword
     * takes vanilla's 255, which is what it was carrying long before the ceiling moved, and is the
     * more interesting number for it to hold: it is the sword that took everything the game had.
     * <p>
     * Any level shown needs an {@code enchantment.level.<n>} lang line of its own. Vanilla ships 1
     * through 10 and prints the raw key for anything else.
     */
    public static final int CEILING = 999;

    private FullEnchantment() {
    }

    /**
     * Sets every enchantment the registry knows about on {@code stack}, at {@code level}.
     * <p>
     * Levels are <em>set</em> rather than added to, so no amount of anvil work can push one past
     * the ceiling and nothing here can produce the out-of-range level {@link ItemEnchantments}
     * throws on.
     * <p>
     * The list is shown, every line of it. A hundred and thirty enchantment lines is not a tooltip,
     * it is a screen - and on these two items that is the joke rather than a problem to be tidied
     * away, so they are listed one by one like any other enchanted item's.
     *
     * @param curses whether the curses come along too
     * @param level  what to set each of them to; see {@link #CEILING} for why the caller chooses
     */
    public static void apply(ItemStack stack, HolderLookup.Provider registries, boolean curses, int level) {
        ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        wanted(registries, curses).forEach(enchantment -> all.set(enchantment, level));

        stack.set(DataComponents.ENCHANTMENTS, all.toImmutable());
    }

    /**
     * Whether {@code stack} needs writing again - a copy handed out by a command, one taken from the
     * creative tab before it was filled in, one held while a datapack reload added an enchantment
     * that was not there when it was made, or one made back when the list was hidden.
     * <p>
     * A size comparison rather than a rebuild, so the usual case costs one integer compare. Counting
     * the registry is the expensive half, which is why callers ask this every few seconds rather
     * than every tick.
     * <p>
     * The second test is how an item made before the list was shown catches up, since its
     * enchantments are all present and only the flag is wrong. {@code showInTooltip} is
     * package-private with no getter, but {@code equals} compares it, so a value that is unchanged
     * by {@code withTooltip(true)} is one that was already true.
     * <p>
     * The third is how an item stamped at a level the item no longer asks for catches up - one made
     * before the ceiling moved has every enchantment it should have and each of them at the old
     * figure, which neither of the other two tests can see. It reads <em>differs from</em> rather
     * than <em>below</em>, since an item can be asked to come down as well as up.
     */
    public static boolean needsTopUp(ItemStack stack, HolderLookup.Provider registries, boolean curses,
                                     int level) {
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        return current.size() < wanted(registries, curses).count()
                || !current.equals(current.withTooltip(true))
                || current.entrySet().stream().anyMatch(entry -> entry.getIntValue() != level);
    }

    private static Stream<Holder<Enchantment>> wanted(HolderLookup.Provider registries, boolean curses) {
        Stream<Holder<Enchantment>> all = registries.lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .map(reference -> (Holder<Enchantment>) reference);

        return curses ? all : all.filter(enchantment -> !enchantment.is(EnchantmentTags.CURSE));
    }
}
