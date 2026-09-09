package net.fahr3n.unnecessarilycompressedcobblestone.potion;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Compression potions. Being crushed under a tier of compressed cobblestone leaves you barely
 * able to move and very hard to hurt, so the pair of effects follows vanilla's Turtle Master - the
 * one potion that already trades speed for hide - just further in both directions.
 */
public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, UnnecessarilyCompressedCobblestone.MOD_ID);

    /** Four minutes, the ordinary brew. */
    public static final int COMPRESSION_1_DURATION = 4 * 60 * 20;

    /** Six minutes, what redstone stretches it to. */
    public static final int LONG_COMPRESSION_1_DURATION = 6 * 60 * 20;

    /** Slowness IV and Resistance IV are both amplifier 3: the numeral is one more than the field. */
    private static final int AMPLIFIER = 3;

    public static final Holder<Potion> COMPRESSION_1 = POTIONS.register("compression_1",
            () -> compression(COMPRESSION_1_DURATION));

    /**
     * The redstone-extended brew. It carries the same {@code "compression_1"} display name as the
     * base potion, the way {@code long_night_vision} is still called Potion of Night Vision, so the
     * two are told apart by the duration in the tooltip rather than by the item name.
     */
    public static final Holder<Potion> LONG_COMPRESSION_1 = POTIONS.register("long_compression_1",
            () -> compression(LONG_COMPRESSION_1_DURATION));

    /**
     * The five damage brews, each in the three forms a stand can make: the plain one, the redstone
     * one that lasts half again as long, and the glowstone one that is a level deeper and, being a
     * level deeper, half as long. Splash, lingering and tipped arrows are container recipes in
     * vanilla and come free with all fifteen.
     * <p>
     * The block tier each is brewed from is the whole of what tells them apart in a stand - the
     * grid, the base potion and the modifiers are identical - so the tiers must stay disjoint, the
     * same rule the enchanted books and the engravings follow.
     */
    public static final int DAMAGE_DURATION = 10 * 20;
    public static final int LONG_DAMAGE_DURATION = 15 * 20;
    public static final int STRONG_DAMAGE_DURATION = 5 * 20;

    /** Freezing, brewed from level 116 stone. */
    public static final Holder<Potion> FREEZING = POTIONS.register("freezing",
            () -> damage("freezing", ModMobEffects.FREEZING, DAMAGE_DURATION, 0));
    public static final Holder<Potion> LONG_FREEZING = POTIONS.register("long_freezing",
            () -> damage("freezing", ModMobEffects.FREEZING, LONG_DAMAGE_DURATION, 0));
    public static final Holder<Potion> STRONG_FREEZING = POTIONS.register("strong_freezing",
            () -> damage("freezing", ModMobEffects.FREEZING, STRONG_DAMAGE_DURATION, 1));

    /** Detonation, brewed from level 118 stone. */
    public static final Holder<Potion> DETONATION = POTIONS.register("detonation",
            () -> damage("detonation", ModMobEffects.DETONATION, DAMAGE_DURATION, 0));
    public static final Holder<Potion> LONG_DETONATION = POTIONS.register("long_detonation",
            () -> damage("detonation", ModMobEffects.DETONATION, LONG_DAMAGE_DURATION, 0));
    public static final Holder<Potion> STRONG_DETONATION = POTIONS.register("strong_detonation",
            () -> damage("detonation", ModMobEffects.DETONATION, STRONG_DAMAGE_DURATION, 1));

    /** Collapse, brewed from level 119 stone. */
    public static final Holder<Potion> COLLAPSE = POTIONS.register("collapse",
            () -> damage("collapse", ModMobEffects.COLLAPSE, DAMAGE_DURATION, 0));
    public static final Holder<Potion> LONG_COLLAPSE = POTIONS.register("long_collapse",
            () -> damage("collapse", ModMobEffects.COLLAPSE, LONG_DAMAGE_DURATION, 0));
    public static final Holder<Potion> STRONG_COLLAPSE = POTIONS.register("strong_collapse",
            () -> damage("collapse", ModMobEffects.COLLAPSE, STRONG_DAMAGE_DURATION, 1));

    /** Stormstruck, brewed from level 120 stone. */
    public static final Holder<Potion> STORMSTRUCK = POTIONS.register("stormstruck",
            () -> damage("stormstruck", ModMobEffects.STORMSTRUCK, DAMAGE_DURATION, 0));
    public static final Holder<Potion> LONG_STORMSTRUCK = POTIONS.register("long_stormstruck",
            () -> damage("stormstruck", ModMobEffects.STORMSTRUCK, LONG_DAMAGE_DURATION, 0));
    public static final Holder<Potion> STRONG_STORMSTRUCK = POTIONS.register("strong_stormstruck",
            () -> damage("stormstruck", ModMobEffects.STORMSTRUCK, STRONG_DAMAGE_DURATION, 1));

    /** Dragon's breath, brewed from level 121 stone. */
    public static final Holder<Potion> DRAGON_BREATH = POTIONS.register("dragon_breath",
            () -> damage("dragon_breath", ModMobEffects.DRAGON_BREATH, DAMAGE_DURATION, 0));
    public static final Holder<Potion> LONG_DRAGON_BREATH = POTIONS.register("long_dragon_breath",
            () -> damage("dragon_breath", ModMobEffects.DRAGON_BREATH, LONG_DAMAGE_DURATION, 0));
    public static final Holder<Potion> STRONG_DRAGON_BREATH = POTIONS.register("strong_dragon_breath",
            () -> damage("dragon_breath", ModMobEffects.DRAGON_BREATH, STRONG_DAMAGE_DURATION, 1));

    /**
     * Infection, brewed from level 152 stone. It is in the damage family's machinery because it is
     * brewed and shaped exactly like one - one block, three forms, the tier the only thing telling
     * it from its neighbours in a stand - but its durations are its own: nothing happens on a tick,
     * so a ten second brew would usually lapse before anything hit the drinker at all.
     */
    public static final int INFECTION_DURATION = 30 * 20;
    public static final int LONG_INFECTION_DURATION = 45 * 20;
    public static final int STRONG_INFECTION_DURATION = 15 * 20;

    public static final Holder<Potion> INFECTION = POTIONS.register("infection",
            () -> damage("infection", ModMobEffects.COMPRESSED_INFESTATION, INFECTION_DURATION, 0));
    public static final Holder<Potion> LONG_INFECTION = POTIONS.register("long_infection",
            () -> damage("infection", ModMobEffects.COMPRESSED_INFESTATION, LONG_INFECTION_DURATION, 0));
    public static final Holder<Potion> STRONG_INFECTION = POTIONS.register("strong_infection",
            () -> damage("infection", ModMobEffects.COMPRESSED_INFESTATION, STRONG_INFECTION_DURATION, 1));

    /**
     * Corrosion, brewed from level 218 stone. Like Infection it is in the damage family's machinery
     * and brewed exactly like one, and like Infection its durations are its own - for the sharper
     * version of the same reason. Nothing at all happens for the first ten seconds of a corrosion
     * clock, so the family's ten second brew would be a potion that did nothing whatever, and its
     * five second glowstone brew would be one that could not.
     * <p>
     * The three are priced in hits rather than in seconds, which is the only honest way to read
     * them: thirty seconds is three hundred, forty-five is four hundred, and the glowstone brew is
     * also four hundred but delivers it in two hits of two hundred rather than four of a hundred.
     * That is the trade the whole family makes, and it is worth more here than anywhere else in the
     * mod - two hundred points in one blow is past what anything short of a boss survives, whatever
     * its armour, while a hundred is not.
     */
    public static final int CORROSION_DURATION = 30 * 20;
    public static final int LONG_CORROSION_DURATION = 45 * 20;
    public static final int STRONG_CORROSION_DURATION = 20 * 20;

    public static final Holder<Potion> CORROSION = POTIONS.register("corrosion",
            () -> damage("corrosion", ModMobEffects.CORROSION, CORROSION_DURATION, 0));
    public static final Holder<Potion> LONG_CORROSION = POTIONS.register("long_corrosion",
            () -> damage("corrosion", ModMobEffects.CORROSION, LONG_CORROSION_DURATION, 0));
    public static final Holder<Potion> STRONG_CORROSION = POTIONS.register("strong_corrosion",
            () -> damage("corrosion", ModMobEffects.CORROSION, STRONG_CORROSION_DURATION, 1));

    /**
     * Ghasted, brewed from level 237 stone. It is in the damage family's machinery because it is
     * brewed and shaped exactly like one, and like Infection and Corrosion its durations are its
     * own - for a reason of its own again. Ghasted is not a tick of damage but a flock, and each
     * ghast in it rolls two to five seconds before its first scream, so a ten second brew would
     * often be a potion that screamed once and a five second one a potion that never screamed at
     * all. Thirty seconds is about six screams from one ghast, which is the shortest a bottle of it
     * can be and still read as the effect the fight teaches.
     * <p>
     * The glowstone brew is the interesting one, and the only place in the mod where a deeper level
     * is a <em>second</em> source rather than a bigger number: Ghasted's amplifier is the size of
     * the flock, so level II is two ghasts on two independent clocks and therefore twice the rate,
     * paid for the usual way in half the time. Redstone is the plain trade of rate for length.
     * <p>
     * The counterplay is unchanged from the boss's own: every scream is {@code #minecraft:is_fire},
     * so Fire Resistance refuses the whole bottle, and milk or a Potion of Cleansing takes it off
     * with the flock. Thrown at something, that makes it the one brew here whose damage arrives
     * after the fight it was thrown in has finished.
     */
    public static final int GHASTED_DURATION = 30 * 20;
    public static final int LONG_GHASTED_DURATION = 45 * 20;
    public static final int STRONG_GHASTED_DURATION = 15 * 20;

    public static final Holder<Potion> GHASTED = POTIONS.register("ghasted",
            () -> damage("ghasted", ModMobEffects.GHASTED, GHASTED_DURATION, 0));
    public static final Holder<Potion> LONG_GHASTED = POTIONS.register("long_ghasted",
            () -> damage("ghasted", ModMobEffects.GHASTED, LONG_GHASTED_DURATION, 0));
    public static final Holder<Potion> STRONG_GHASTED = POTIONS.register("strong_ghasted",
            () -> damage("ghasted", ModMobEffects.GHASTED, STRONG_GHASTED_DURATION, 1));

    /** Every damage brew, in the order they are brewed: base, redstone, glowstone, and the tier. */
    public static final List<Brew> BREWS = List.of(
            new Brew(FREEZING, LONG_FREEZING, STRONG_FREEZING, 116),
            new Brew(DETONATION, LONG_DETONATION, STRONG_DETONATION, 118),
            new Brew(COLLAPSE, LONG_COLLAPSE, STRONG_COLLAPSE, 119),
            new Brew(STORMSTRUCK, LONG_STORMSTRUCK, STRONG_STORMSTRUCK, 120),
            new Brew(DRAGON_BREATH, LONG_DRAGON_BREATH, STRONG_DRAGON_BREATH, 121),
            new Brew(INFECTION, LONG_INFECTION, STRONG_INFECTION, 152),
            new Brew(CORROSION, LONG_CORROSION, STRONG_CORROSION, 218),
            new Brew(GHASTED, LONG_GHASTED, STRONG_GHASTED, 237));

    /**
     * One family of brew: the three potions and the compression level the base one is brewed from.
     * Everything downstream - the brewing recipes and the creative tab - loops over these rather
     * than naming fifteen potions again.
     */
    public record Brew(Holder<Potion> base, Holder<Potion> extended, Holder<Potion> strong, int blockTier) {
    }

    /**
     * Milk in a bottle: it clears what is harmful and leaves the rest.
     * <p>
     * That is the one thing a bucket of milk cannot do - vanilla's milk strips every effect a player
     * has, good and bad together, which makes it useless the moment any of them was worth having.
     * The potion carries no effects of its own; what it does is in {@link #cleanse}, and both the
     * player drinking one and the Compressed Witch drinking one go through that.
     */
    public static final Holder<Potion> CLEANSING = POTIONS.register("cleansing",
            () -> new Potion("cleansing"));

    /**
     * Takes every harmful effect off {@code entity} and touches nothing else.
     * <p>
     * The category is the game's own judgement of which is which, so another mod's effect is cleared
     * or kept by whatever that mod declared it as, and nothing here has to keep a list.
     */
    public static void cleanse(LivingEntity entity) {
        List<Holder<MobEffect>> harmful = entity.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .map(MobEffectInstance::getEffect)
                .toList();

        harmful.forEach(entity::removeEffect);
    }

    /**
     * One brew. The name is shared by all three forms of it, the way {@code long_night_vision} is
     * still called Potion of Night Vision: the tooltip's duration and level are what tell them
     * apart, and that is also why the lang keys live under {@code item.minecraft.potion.effect.*}
     * rather than in this mod's namespace.
     */
    private static Potion damage(String name, Holder<MobEffect> effect, int duration, int amplifier) {
        return new Potion(name, new MobEffectInstance(effect, duration, amplifier));
    }

    private static Potion compression(int duration) {
        return new Potion("compression_1",
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, AMPLIFIER),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, AMPLIFIER));
    }

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
