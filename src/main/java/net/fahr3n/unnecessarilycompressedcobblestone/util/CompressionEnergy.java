package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.component.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/**
 * Compression Energy: one point for every cobblestone that went into a block, so a compression
 * level is worth nine of the level below it and level n is worth 9^n.
 * <p>
 * Level 255 is 9^255, a 244 digit number, so the energy is never held as a number anywhere. Every
 * value in this class is a base 10 logarithm: a block's worth is {@code level * log10(9)}, adding
 * energy is one {@link Math#log10} call, and the digit count that drives the gear's stats is the
 * exponent plus one. Precision in the leading digits is lost, which is why the tooltip shows the
 * energy in scientific notation and nothing anywhere compares two totals for equality.
 */
public class CompressionEnergy {
    private static final double LOG10_NINE = Math.log10(9.0);

    /** Item to compression level, built on first use because the blocks register before this runs. */
    private static volatile Map<Item, Integer> compressionLevels;

    /**
     * The energy a single item of this kind carries, or empty if it is not cobblestone at all.
     * Modded cobblestone counts through {@code #c:cobblestones} and is worth one, the same as the
     * vanilla block.
     */
    public static OptionalDouble sourceValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalDouble.empty();
        }

        int level = compressionLevel(stack.getItem());
        if (level > 0) {
            return OptionalDouble.of(level * LOG10_NINE);
        }

        return stack.is(Tags.Items.COBBLESTONES) ? OptionalDouble.of(0.0) : OptionalDouble.empty();
    }

    /** Whether this is a piece of gear the inscriber can pour energy into. */
    public static boolean canHoldEnergy(ItemStack stack) {
        return stack.is(ModTags.Items.INSCRIBABLE);
    }

    /** The energy already on a stack, as a logarithm, or empty if it has never been inscribed. */
    public static OptionalDouble get(ItemStack stack) {
        Double log10 = stack.get(ModDataComponents.COMPRESSION_ENERGY.get());
        return log10 == null ? OptionalDouble.empty() : OptionalDouble.of(log10);
    }

    /**
     * Pours {@code log10} worth of energy into the stack.
     * <p>
     * In logarithms a sum is {@code log(a + b) = log(a) + log(1 + 10^(log(b) - log(a)))}, taken
     * from the larger of the two so the power is never bigger than one and never overflows.
     */
    public static void add(ItemStack stack, double log10) {
        OptionalDouble current = get(stack);
        if (current.isEmpty()) {
            stack.set(ModDataComponents.COMPRESSION_ENERGY.get(), log10);
            return;
        }

        double higher = Math.max(current.getAsDouble(), log10);
        double lower = Math.min(current.getAsDouble(), log10);
        stack.set(ModDataComponents.COMPRESSION_ENERGY.get(), higher + Math.log10(1.0 + Math.pow(10.0, lower - higher)));
    }

    /** How many digits the energy is written with; one for 1 through 9, two for 10 through 99. */
    public static int digits(double log10) {
        return (int) Math.floor(log10) + 1;
    }

    /** The stat bonus a piece of gear has earned: one per digit of the energy on it. */
    public static int bonus(ItemStack stack) {
        OptionalDouble log10 = get(stack);
        return log10.isEmpty() ? 0 : Math.max(0, digits(log10.getAsDouble()));
    }

    /** The energy in scientific notation, which past the first few blocks is the only way to write it. */
    public static String format(double log10) {
        int exponent = (int) Math.floor(log10);
        double mantissa = Math.pow(10.0, log10 - exponent);

        return String.format(Locale.ROOT, "%.2f × 10^%d", mantissa, exponent);
    }

    private static int compressionLevel(Item item) {
        Map<Item, Integer> levels = compressionLevels;
        if (levels == null) {
            Map<Item, Integer> built = new IdentityHashMap<>(ModBlocks.MAX_COMPRESSION_LEVEL);
            for (int level = 1; level <= ModBlocks.MAX_COMPRESSION_LEVEL; level++) {
                built.put(ModBlocks.byLevel(level).get().asItem(), level);
            }

            // Racing threads would each build the same map, so publishing whichever finishes last
            // is safe; what matters is that the map is complete before anything can read it.
            levels = Map.copyOf(built);
            compressionLevels = levels;
        }

        return levels.getOrDefault(item, 0);
    }
}
