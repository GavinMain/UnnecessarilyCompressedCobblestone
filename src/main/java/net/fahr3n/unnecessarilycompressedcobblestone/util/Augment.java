package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RayOfLaserItem;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The augments, and the two things that separate them: what family each belongs to and, for the ones
 * that are a level of something, how deep that level runs.
 * <p>
 * An augment is the engraving's opposite number for one weapon. It is an item a player crafts and a
 * Laser Augmentation Table then fits to a Ray of Laser, where it lives as a
 * {@code ModDataComponents.AUGMENTS} entry and shows in the tooltip the way an enchantment does. It
 * comes back off exactly as it went on, with no anvil and nothing lost - which is the same promise
 * the engravings make, and the reason both systems exist next to enchanting rather than inside it.
 * <p>
 * Nothing here says what an augment <em>does</em>. Damage and rate are read by
 * {@link RayOfLaserItem} when the beam is priced and when the charge is timed, and an effect augment
 * carries the effect it presses and nothing else, so a new one of those is a constant, a texture and
 * a lang line.
 * <p>
 * {@link Family} is the whole of the stacking rule: a laser holds at most one augment out of each
 * family, so Damage III and Damage IV can never be on the same weapon, while every effect augment is
 * a family of one and all six of them fit at once.
 */
public enum Augment implements StringRepresentable {
    /**
     * The four damage augments. Each level is another quarter of the beam, so Damage IV is twice
     * what the laser would otherwise do. What the multiplier is written against is the whole of the
     * beam, Compression Energy included, which is what keeps the augment worth as much on a deeply
     * inscribed laser as on a bare one.
     */
    DAMAGE_1("damage_1", 181, Family.DAMAGE, 1),
    DAMAGE_2("damage_2", 198, Family.DAMAGE, 2),
    DAMAGE_3("damage_3", 226, Family.DAMAGE, 3),
    DAMAGE_4("damage_4", 243, Family.DAMAGE, 4),

    /**
     * The three rate augments: {@value RayOfLaserItem#RATE_TICKS_PER_LEVEL} ticks - three tenths of
     * a second - off the charge per level, so a Rate III laser fires every 1.1 seconds against the
     * two seconds a guardian takes.
     */
    RATE_1("rate_1", 182, Family.RATE, 1),
    RATE_2("rate_2", 209, Family.RATE, 2),
    RATE_3("rate_3", 252, Family.RATE, 3),

    /** Poison, out of the beam rather than out of a bottle. */
    POISON("poison", 183, Family.POISON, () -> MobEffects.POISON),

    /** Wither, the same way. */
    WITHER("wither", 184, Family.WITHER, () -> MobEffects.WITHER),

    /**
     * A bolt every few seconds on whatever the beam hit. This is the mod's own Stormstruck rather
     * than anything of the laser's: that effect already calls real lightning down and already knows
     * to bring no fire with it.
     */
    LIGHTNING("lightning", 185, Family.LIGHTNING, () -> ModMobEffects.STORMSTRUCK),

    /** Powder snow damage and the frost that goes with it, which is the mod's Freezing effect. */
    FREEZE("freeze", 186, Family.FREEZE, () -> ModMobEffects.FREEZING),

    /** Fire, kept alight for as long as the effect lasts. See {@code BurnMobEffect}. */
    BURN("burn", 188, Family.BURN, () -> ModMobEffects.BURNING),

    /** A blast every few seconds, which is the mod's Detonation effect. */
    EXPLOSIVE("explosive", 190, Family.EXPLOSIVE, () -> ModMobEffects.DETONATION),

    /** Blindness, which on a mob is vanilla's own way of shortening how far it can see a target. */
    BLINDNESS("blindness", 191, Family.BLINDNESS, () -> MobEffects.BLINDNESS),

    /**
     * Slowness, at level I. See the note on {@code MOVEMENT_SPEED} in CLAUDE.md for why a deeper
     * level would not be slowness at all: the attribute floors at zero from amplifier 6, and a beam
     * that paralyses whatever it touches is not a fight.
     */
    SLOWNESS("slowness", 192, Family.SLOWNESS, () -> MobEffects.MOVEMENT_SLOWDOWN),

    /** Nausea - vanilla calls it {@code CONFUSION}, and it is only ever worth anything on a player. */
    NAUSEA("nausea", 193, Family.NAUSEA, () -> MobEffects.CONFUSION),

    /** Hunger, which likewise means something to a player and nothing at all to a mob. */
    HUNGER("hunger", 194, Family.HUNGER, () -> MobEffects.HUNGER),

    /**
     * Speed, onto whatever the beam hit. It is the one augment here that is not a debuff, which is
     * the whole of its use: a mob that runs at whoever shot it closes the distance the laser wants
     * closed, and a target hastened into the open is a target still in the beam.
     */
    SWIFTNESS("swiftness", 199, Family.SWIFTNESS, () -> MobEffects.MOVEMENT_SPEED);

    /**
     * What may not be fitted twice. A laser holds at most one augment out of each family, which is
     * how "only one of each name" is stated in code: Damage I through IV are one family and so are
     * mutually exclusive, the three rates are another, and each effect is a family of its own so all
     * six of them fit on one weapon at once.
     */
    public enum Family implements StringRepresentable {
        DAMAGE("damage"),
        RATE("rate"),
        POISON("poison"),
        WITHER("wither"),
        LIGHTNING("lightning"),
        FREEZE("freeze"),
        BURN("burn"),
        EXPLOSIVE("explosive"),
        BLINDNESS("blindness"),
        SLOWNESS("slowness"),
        NAUSEA("nausea"),
        HUNGER("hunger"),
        SWIFTNESS("swiftness");

        private final String name;

        Family(String name) {
            this.name = name;
        }

        /** What the family is called in a tooltip: the augment's name without its level. */
        public Component displayName() {
            return Component.translatable("augment.unnecessarilycompressedcobblestone." + this.name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /**
     * What sits in the middle of every augment's grid. It is a prismarine shard rather than the
     * engravings' diamond or the sigils' amethyst, and that is the whole reason the augments may
     * reuse tiers those two families have already spent: two recipes only collide when they are the
     * same shape, so a family with a core of its own only has to keep its tiers clear of its own
     * members. Within this enum they must stay disjoint, or two augments become one recipe and only
     * one of them is craftable.
     */
    public static final Item CORE = Items.PRISMARINE_SHARD;

    public static final Codec<Augment> CODEC = StringRepresentable.fromEnum(Augment::values);

    private final String name;
    private final int blockTier;
    private final Family family;
    private final int level;
    @Nullable
    private final Supplier<Holder<MobEffect>> effect;

    Augment(String name, int blockTier, Family family, int level) {
        this(name, blockTier, family, level, null);
    }

    Augment(String name, int blockTier, Family family, Supplier<Holder<MobEffect>> effect) {
        this(name, blockTier, family, 1, effect);
    }

    Augment(String name, int blockTier, Family family, int level, @Nullable Supplier<Holder<MobEffect>> effect) {
        this.name = name;
        this.blockTier = blockTier;
        this.family = family;
        this.level = level;
        this.effect = effect;
    }

    /**
     * The compressed cobblestone tier this augment's item is cut from. Every augment is crafted in
     * the one grid, so the tier is the only thing that tells two of them apart - see {@link #CORE}.
     */
    public int blockTier() {
        return this.blockTier;
    }

    /** Which family this belongs to, and so what it may not sit beside. */
    public Family family() {
        return this.family;
    }

    /** How deep a level of its family this is. One for anything that has only the one level. */
    public int level() {
        return this.level;
    }

    /**
     * The effect this augment presses onto whatever the beam hits, or null if it is not one of the
     * effect augments. It is a supplier because {@code MobEffects} and this mod's own effects are
     * registry holders, and an enum constant's arguments are worked out long before either registry
     * is anywhere near ready.
     */
    @Nullable
    public Holder<MobEffect> effect() {
        return this.effect == null ? null : this.effect.get();
    }

    /** Whether this augment has anywhere to go on {@code stack}, which is to say: is it a laser. */
    public boolean canApplyTo(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof RayOfLaserItem;
    }

    /** The item this augment is crafted as, and the item the table gives back. */
    public Item item() {
        return ModItems.AUGMENTS.get(this).get();
    }

    /** The item id of the augment item this is cut from, which is also its lang key. */
    public String itemName() {
        return this.name + "_augment";
    }

    /**
     * What the tooltip on an augmented laser says. A family that runs to more than one level is
     * written the way an enchantment is - the family, then the level in Roman numerals - so a player
     * reads "Damage III" rather than a name they would have to learn.
     */
    public Component displayName() {
        return this.family == Family.DAMAGE || this.family == Family.RATE
                ? Component.translatable("augment.unnecessarilycompressedcobblestone.leveled",
                        this.family.displayName(), Component.translatable("enchantment.level." + this.level))
                : this.family.displayName();
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * The augment saved under {@code name}. This is what the component's network codec decodes
     * through, so an unknown name is a client and server that disagree about what augments exist -
     * worth failing loudly rather than putting a null in a list nothing else expects one in.
     */
    public static Augment byName(String name) {
        for (Augment augment : values()) {
            if (augment.name.equals(name)) {
                return augment;
            }
        }

        throw new IllegalArgumentException("Unknown augment: " + name);
    }
}
