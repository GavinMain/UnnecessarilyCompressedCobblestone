package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Reading and writing the augments fitted to a Ray of Laser.
 * <p>
 * They are a data component, so they travel with the stack through every container, hopper and death
 * chest in the game with nothing here to keep them alive. What may sit beside what is
 * {@link Augment.Family}: one augment out of each family and no more, which is how "only one of each
 * name" is enforced without anything having to know that Damage runs to four levels and Rate to
 * three.
 * <p>
 * Unlike {@link Engravings}, which gives its engravings back last first, an augment is taken off by
 * name - the table shows the fitted list and the player picks one out of it - so removal here is
 * {@link #remove} rather than a pop.
 */
public class Augments {
    private Augments() {
    }

    /** Every augment on {@code stack}, in the order they were fitted. Never null, often empty. */
    public static List<Augment> get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AUGMENTS.get(), List.of());
    }

    /** Whether {@code stack} carries {@code augment}. */
    public static boolean has(ItemStack stack, Augment augment) {
        return get(stack).contains(augment);
    }

    /** The augment of {@code family} fitted to {@code stack}, or null if the family is not on it. */
    @Nullable
    public static Augment of(ItemStack stack, Augment.Family family) {
        for (Augment augment : get(stack)) {
            if (augment.family() == family) {
                return augment;
            }
        }

        return null;
    }

    /**
     * The level of {@code family} on {@code stack}, or zero if none of it is fitted. This is what
     * the laser reads for its damage and its charge, so a family with no augment on it costs a
     * caller no special case.
     */
    public static int level(ItemStack stack, Augment.Family family) {
        Augment augment = of(stack, family);
        return augment == null ? 0 : augment.level();
    }

    /**
     * Whether {@code augment} would go onto {@code stack}: it has to fit the weapon, and the weapon
     * must not already carry something of that family - which is the rule that stops Damage III and
     * Damage IV being fitted at once.
     */
    public static boolean canAdd(ItemStack stack, Augment augment) {
        return augment.canApplyTo(stack) && of(stack, augment.family()) == null;
    }

    /** Fits {@code augment} to {@code stack}. The caller has already checked {@link #canAdd}. */
    public static void add(ItemStack stack, Augment augment) {
        List<Augment> augments = new ArrayList<>(get(stack));
        augments.add(augment);
        stack.set(ModDataComponents.AUGMENTS.get(), List.copyOf(augments));
    }

    /**
     * Takes {@code augment} back off {@code stack} and says whether it was there to take. The
     * component is removed outright once the last one is gone, so a laser that has been stripped is
     * byte-identical to one that was never augmented.
     */
    public static boolean remove(ItemStack stack, Augment augment) {
        List<Augment> augments = get(stack);
        if (!augments.contains(augment)) {
            return false;
        }

        List<Augment> left = new ArrayList<>(augments);
        left.remove(augment);

        if (left.isEmpty()) {
            stack.remove(ModDataComponents.AUGMENTS.get());
        } else {
            stack.set(ModDataComponents.AUGMENTS.get(), List.copyOf(left));
        }

        return true;
    }
}
