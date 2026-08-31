package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.component.ModDataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Reading and writing the engravings on a piece of gear.
 * <p>
 * They are a data component, so they travel with the stack through every container, hopper and death
 * chest in the game with nothing here to keep them alive. A stack holds at most one of each kind:
 * an engraving is a thing a player crafts and can take back off, so letting four Lifesteals stack on
 * one sword would make the table a damage multiplier rather than a place to fit gear out. Stacking
 * is across the four armour slots instead, which is what {@link #wornCount} counts.
 */
public class Engravings {
    private Engravings() {
    }

    /** Every engraving on {@code stack}, in the order they were cut. Never null, often empty. */
    public static List<Engraving> get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENGRAVINGS.get(), List.of());
    }

    /** Whether {@code stack} carries {@code engraving}. */
    public static boolean has(ItemStack stack, Engraving engraving) {
        return get(stack).contains(engraving);
    }

    /**
     * Whether {@code engraving} would go onto {@code stack}: it has to fit the gear, and the gear
     * must not already carry one of that kind.
     */
    public static boolean canAdd(ItemStack stack, Engraving engraving) {
        return engraving.canApplyTo(stack) && !has(stack, engraving);
    }

    /** Cuts {@code engraving} into {@code stack}. The caller has already checked {@link #canAdd}. */
    public static void add(ItemStack stack, Engraving engraving) {
        List<Engraving> engravings = new ArrayList<>(get(stack));
        engravings.add(engraving);
        stack.set(ModDataComponents.ENGRAVINGS.get(), List.copyOf(engravings));
    }

    /**
     * Takes the last engraving back off {@code stack} and returns it, or null if there was none.
     * The component is removed outright once the last one is gone, so a piece of gear that has been
     * stripped is byte-identical to one that was never engraved.
     */
    public static Engraving removeLast(ItemStack stack) {
        List<Engraving> engravings = get(stack);
        if (engravings.isEmpty()) {
            return null;
        }

        Engraving removed = engravings.get(engravings.size() - 1);
        List<Engraving> left = List.copyOf(engravings.subList(0, engravings.size() - 1));
        if (left.isEmpty()) {
            stack.remove(ModDataComponents.ENGRAVINGS.get());
        } else {
            stack.set(ModDataComponents.ENGRAVINGS.get(), left);
        }

        return removed;
    }

    /**
     * How many of the four armour slots hold a piece carrying {@code engraving}. This is where the
     * armour engravings stack: one per piece, four at most.
     */
    public static int wornCount(LivingEntity entity, Engraving engraving) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (has(entity.getItemBySlot(slot), engraving)) {
                count++;
            }
        }

        return count;
    }
}
