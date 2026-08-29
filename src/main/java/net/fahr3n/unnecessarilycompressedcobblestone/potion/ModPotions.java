package net.fahr3n.unnecessarilycompressedcobblestone.potion;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    private static Potion compression(int duration) {
        return new Potion("compression_1",
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, AMPLIFIER),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, AMPLIFIER));
    }

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
