package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * One effect drawn at random, for the arrows that carry one.
 * <p>
 * What may be drawn is the effect registry itself rather than a list, so another mod's effects are
 * in the bag the day it adds them - the same rule the Effect TNT and the Breeding TNT follow. All
 * that is ever filtered on is {@link MobEffectCategory}, which is the mod's own declaration of what
 * its effect is for, so "only the nasty ones" stays correct across every mod without naming one.
 */
public final class ArrowEffects {
    /**
     * How long a drawn effect lasts. Ten seconds is a quarter of what a tipped arrow gives, which is
     * the trade for its being free and unaimed: an arrow that happened to land carries it.
     */
    public static final int DURATION = 200;

    /** Worked out once each way and kept: walking the registry per arrow would cost a burst of sixty. */
    private static List<Holder<MobEffect>> all;
    private static List<Holder<MobEffect>> harmful;

    private ArrowEffects() {
    }

    /**
     * An effect drawn uniformly from the registry, or from the harmful half of it.
     *
     * @param harmfulOnly whether to draw only from {@link MobEffectCategory#HARMFUL}
     */
    public static MobEffectInstance random(RandomSource random, boolean harmfulOnly) {
        List<Holder<MobEffect>> pool = pool(harmfulOnly);
        Holder<MobEffect> effect = pool.get(random.nextInt(pool.size()));

        // An instantaneous effect - harming, healing - is applied the tick it lands rather than held,
        // so its duration is never read. Vanilla still refuses a zero, so it is given one tick.
        int duration = effect.value().isInstantenous() ? 1 : DURATION;

        return new MobEffectInstance(effect, duration, 0, false, true, true);
    }

    private static List<Holder<MobEffect>> pool(boolean harmfulOnly) {
        List<Holder<MobEffect>> cached = harmfulOnly ? harmful : all;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<Holder<MobEffect>> built = new ArrayList<>();
        for (Holder<MobEffect> effect : BuiltInRegistries.MOB_EFFECT.holders().toList()) {
            if (!harmfulOnly || effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                built.add(effect);
            }
        }

        if (harmfulOnly) {
            harmful = built;
        } else {
            all = built;
        }

        return built;
    }
}
