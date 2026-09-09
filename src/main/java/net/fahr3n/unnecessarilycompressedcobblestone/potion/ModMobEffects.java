package net.fahr3n.unnecessarilycompressedcobblestone.potion;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import java.util.EnumMap;
import java.util.Map;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.BurnMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.CompressedMiningMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossSpoil;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.CompressedInfestationMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.CorrosionMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.DamageMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.ExplosionMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.FreezeMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.GhastedMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.LightningMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.MendingMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.StalactiteMobEffect;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.custom.StepHeightMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The five ways this mod has of hurting somebody over time, one per damage type worth a potion.
 * <p>
 * Every one of them ticks rather than landing all at once, which is what a vanilla damage potion
 * does <em>not</em> do: Instant Damage is instantaneous, so redstone has no duration to stretch and
 * there is no such thing as a Potion of Harming (Extended). Ticking gives all four vanilla
 * modifiers something to mean - redstone is longer, glowstone is harder and more often, gunpowder
 * throws it and dragon's breath lingers - so these brew exactly like anything else in a stand.
 * <p>
 * None of them invents a number for damage: each hurts with a real vanilla {@link DamageTypes}
 * entry, so armour, Protection, Resistance and every tag another mod hangs off them apply without
 * this file knowing anything about it.
 */
public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, UnnecessarilyCompressedCobblestone.MOD_ID);

    /** Powder snow damage, and the frost on the screen that goes with it. Bypasses armour. */
    public static final Holder<MobEffect> FREEZING = MOB_EFFECTS.register("freezing",
            () -> new FreezeMobEffect(0x9AD7F0, 3.0F));

    /**
     * Fire, held on for as long as it lasts. It hurts with nothing of its own - vanilla's burning is
     * the whole of the damage - which is what the Burn augment presses onto whatever its laser hits.
     */
    public static final Holder<MobEffect> BURNING = MOB_EFFECTS.register("burning",
            () -> new BurnMobEffect(0xE07B1C));

    /** A blast every few seconds, the level squared across. Breaks blocks. */
    public static final Holder<MobEffect> DETONATION = MOB_EFFECTS.register("detonation",
            () -> new ExplosionMobEffect(0xE0552B));

    /** Dripstone forming overhead and falling. The one of the five that armour answers. */
    public static final Holder<MobEffect> COLLAPSE = MOB_EFFECTS.register("collapse",
            () -> new StalactiteMobEffect(0x8C7A6B));

    /** A bolt every few seconds, and no fire with it. */
    public static final Holder<MobEffect> STORMSTRUCK = MOB_EFFECTS.register("stormstruck",
            () -> new LightningMobEffect(0x7FC8FF, 5.0F));

    /** What the dragon breathes, without the dragon. Bypasses armour. */
    public static final Holder<MobEffect> DRAGON_BREATH = MOB_EFFECTS.register("dragon_breath",
            () -> new DamageMobEffect(0xB050D0, DamageTypes.DRAGON_BREATH, 3.0F));

    /**
     * A step height of one extra block per level. The only beneficial effect here, and the only one
     * that is an attribute rather than damage: it is what the Zoom TNT hands out alongside Speed X,
     * and it exists as an effect rather than as a bare modifier so that it takes itself back off
     * again when it lapses - see {@link StepHeightMobEffect}.
     */
    public static final Holder<MobEffect> ZOOM = MOB_EFFECTS.register("zoom",
            () -> new StepHeightMobEffect(0x5BCA51));

    /**
     * Vanilla's Infested around this mod's silverfish: being hurt shakes some of them loose. The
     * chance is a fifth against vanilla's tenth and the brood is bigger, because what hatches here
     * flies, is fast, and bites through armour - and because there is only one way to catch it,
     * which is to have already been bitten.
     */
    public static final Holder<MobEffect> COMPRESSED_INFESTATION = MOB_EFFECTS.register("compressed_infestation",
            () -> new CompressedInfestationMobEffect(0x6C6C6C, 0.2F, 1, 3));

    /**
     * A hundred points a level, once every ten seconds, and nothing at all before the first ten are
     * up. It is the one effect here that keeps its own clocks rather than living in
     * {@code activeEffects} - every application runs a countdown of its own, so six stings are six
     * countdowns - and this registration is only the half of it vanilla can hold. See
     * {@link net.fahr3n.unnecessarilycompressedcobblestone.util.Corrosion}, which is the rest.
     */
    public static final Holder<MobEffect> CORROSION = MOB_EFFECTS.register("corrosion",
            () -> new CorrosionMobEffect(0x8FA31E));

    /**
     * What a Compressed Scythe leaves in a wound. Two points every two seconds, and - the half that
     * matters - no healing of any kind while it lasts.
     * <p>
     * The damage is an ordinary {@link DamageMobEffect} over the mod's own bleeding type, which
     * bypasses armour and nothing else. The healing lock is not here at all, because there is no
     * hook on a {@code MobEffect} for it: it lives in {@code ModEvents}, which cancels
     * {@code LivingHealEvent} outright for anything carrying this. That is the one gate every heal
     * in the game goes through - regeneration, a golden apple, a potion, a full hunger bar, another
     * mod's anything - so one handler covers all of them.
     */
    public static final Holder<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding",
            () -> new DamageMobEffect(0xA8161B, ModDamageTypes.BLEEDING, 2.0F));

    /**
     * The spoils, one effect per {@link BossSpoil} constant and registered in one loop rather than
     * as a field each - the same shape the engravings and the augments take, so a new boss's spoil
     * is a constant in that enum, a lang line and an icon and nothing here.
     * <p>
     * They are the only effects in this file that do nothing on their own: each is a flag saying
     * which compression level the holder's next stone comes up as, read off the breaker in
     * {@code ModEvents} when a block's drops are decided. See {@link CompressedMiningMobEffect} for
     * why there is one per boss rather than one effect at fifteen amplifiers.
     */
    /**
     * Instant Health with a linear dial instead of a doubling one: {@code amplifier + 1} points.
     * <p>
     * It exists for the Compressed Healing Staff, whose inscribed bonus is a point per digit and so
     * cannot be written as an amplifier of vanilla's Instant Health - see {@link MendingMobEffect},
     * which also explains why it still hurts the undead. Nothing brews it; it rides on the splash
     * potion the staff throws as a custom effect beside the vanilla healing.
     */
    public static final Holder<MobEffect> MENDING = MOB_EFFECTS.register("mending",
            () -> new MendingMobEffect(0xF82423));

    /**
     * Ghasted: a flock of little ghasts around the holder, one per level, each screaming on a clock
     * of its own. The effect itself does nothing - see {@link GhastedMobEffect} for why it has to
     * exist anyway, and {@code Ghasted} for what it actually means.
     */
    public static final Holder<MobEffect> GHASTED = MOB_EFFECTS.register("ghasted",
            () -> new GhastedMobEffect(0xE8E8E8));

    public static final Map<BossSpoil, Holder<MobEffect>> SPOILS = registerSpoils();

    private static Map<BossSpoil, Holder<MobEffect>> registerSpoils() {
        Map<BossSpoil, Holder<MobEffect>> spoils = new EnumMap<>(BossSpoil.class);
        for (BossSpoil spoil : BossSpoil.values()) {
            spoils.put(spoil, MOB_EFFECTS.register(spoil.effectName(),
                    () -> new CompressedMiningMobEffect(spoil)));
        }

        return spoils;
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
