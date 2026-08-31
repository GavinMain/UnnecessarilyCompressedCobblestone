package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.function.Predicate;

import com.mojang.serialization.Codec;

import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowTntStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/**
 * The engravings, and the one thing that separates them: what each may be cut into.
 * <p>
 * An engraving is an item a player crafts and an Engraving Table then moves onto a piece of gear,
 * where it lives as a {@code ModDataComponents.ENGRAVINGS} entry rather than as an enchantment. That
 * is the whole reason the system exists next to enchanting: an engraving comes back off the gear the
 * way it went on, with no anvil, no experience and no losing the thing.
 * <p>
 * What an engraving actually does is never in this enum - it is in whatever reads the component
 * ({@code ModEvents} for most of them, the staff itself for {@link #SPIRAL}). All that lives here is
 * the name it is saved under and the test for what it fits, and the test is written against item
 * tags wherever it can be, so another mod's helmet or bow is engravable without this file changing.
 */
public enum Engraving implements StringRepresentable {
    /**
     * The one engraving that is not a stat: it changes what an Arrow TNT Staff rains. Only that
     * staff has anywhere to put it, which is why this is the one applicability test written against
     * a class rather than a tag - the behaviour it swaps out is that item's own.
     */
    SPIRAL("spiral", 85, stack -> stack.getItem() instanceof CompressedArrowTntStaffItem),

    /** Night vision while it is worn, and the wearer's arrows light up what they hit. */
    VISION("vision", 87, stack -> stack.is(ItemTags.HEAD_ARMOR)),

    /** A slice off every projectile that lands, counted once per engraved piece worn. */
    PROJECTILE_PROTECTION("projectile_protection", 88, Engraving::isArmor),

    /** The same, for lightning. */
    LIGHTNING_PROTECTION("lightning_protection", 97, Engraving::isArmor),

    /** Part of the damage a melee weapon deals comes back as health. */
    LIFESTEAL("lifesteal", 91, stack -> stack.is(Tags.Items.MELEE_WEAPON_TOOLS)),

    /**
     * The Compressed Cobblestone Bow comes to full draw in three quarters of the time. This one is
     * deliberately not written against the bow tag: the draw it shortens is four times a vanilla
     * bow's, so a quarter off it is worth fifteen ticks here and five on anything else, and the
     * engraving is priced for the former.
     */
    QUICK_DRAW("quick_draw", 92, stack -> stack.getItem() instanceof CompressedCobblestoneBowItem);

    public static final Codec<Engraving> CODEC = StringRepresentable.fromEnum(Engraving::values);

    private final String name;
    private final int blockTier;
    private final Predicate<ItemStack> appliesTo;

    Engraving(String name, int blockTier, Predicate<ItemStack> appliesTo) {
        this.name = name;
        this.blockTier = blockTier;
        this.appliesTo = appliesTo;
    }

    /**
     * The compressed cobblestone tier this engraving's item is cut from. Every engraving is crafted
     * in the one grid in {@code ModRecipeProvider}, so the tier is the only thing that tells two of
     * them apart: they must stay disjoint from each other and from every book and TNT, or two
     * recipes become the same recipe and only one of them is craftable.
     */
    public int blockTier() {
        return this.blockTier;
    }

    /** Whether this engraving has anywhere to go on {@code stack}. */
    public boolean canApplyTo(ItemStack stack) {
        return !stack.isEmpty() && this.appliesTo.test(stack);
    }

    /** The item this engraving is crafted as, and the item an Engraving Table gives back. */
    public Item item() {
        return ModItems.ENGRAVINGS.get(this).get();
    }

    /** The item id of the engraving item this is cut from, which is also its lang key. */
    public String itemName() {
        return this.name + "_engraving";
    }

    /** What the tooltip on an engraved piece of gear says. */
    public Component displayName() {
        return Component.translatable("engraving.unnecessarilycompressedcobblestone." + this.name);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * The engraving saved under {@code name}. This is what the component's network codec decodes
     * through, so an unknown name is a client and server that disagree about what engravings exist -
     * worth failing loudly rather than putting a null in a list nothing else expects one in.
     */
    public static Engraving byName(String name) {
        for (Engraving engraving : values()) {
            if (engraving.name.equals(name)) {
                return engraving;
            }
        }

        throw new IllegalArgumentException("Unknown engraving: " + name);
    }

    /** Any of the four armour slots, by the vanilla tags rather than by {@code ArmorItem}. */
    private static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR);
    }
}
