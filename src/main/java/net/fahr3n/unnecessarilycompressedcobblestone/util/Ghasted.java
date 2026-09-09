package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.MiniGhastEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Ghasted effect, which is a flock rather than a tick.
 * <p>
 * The registered {@code MobEffect} does nothing at all - see {@code GhastedMobEffect} for why it
 * still has to exist. Everything the effect means is here, and it is two rules:
 * <ul>
 * <li><b>The flock is topped up to the level.</b> A holder of Ghasted at amplifier n should have
 *     {@code n + 1} {@link MiniGhastEntity} around them; every {@link #TOP_UP_INTERVAL} ticks the
 *     shortfall is made up. Making it a top-up rather than a spawn-on-apply is what makes the whole
 *     thing self-healing: a ghast lost to a reload, a dimension change or a chunk unload comes back
 *     on its own, and the effect running out takes the flock with it because each ghast checks its
 *     holder rather than waiting to be told.
 * <li><b>Durations sum.</b> Vanilla merges two applications of the same effect by keeping the longer
 *     one, which is wrong for this: a player caught by a second volley should be ghasted for the
 *     rest of the first <em>and</em> the whole of the second. That cannot be done inside
 *     {@code MobEffectEvent.Added} - {@code MobEffectInstance} has no public duration setter and
 *     re-adding from inside the event would recurse - so the summed instance is queued and applied
 *     on the holder's next tick, at most one tick late.
 * </ul>
 * The level is deliberately <em>not</em> summed with it. Levels are how hard a source hits - the
 * boss's scatter volley presses one and its homing volley two - and a player caught by twenty
 * charges should be ghasted for a very long time by four ghasts, not permanently by twenty. Time is
 * the thing that stacks; the flock's size is the attack's own statement.
 */
public final class Ghasted {
    /** How often the flock is counted and made up. Half a second - a ghast is not urgent work. */
    private static final int TOP_UP_INTERVAL = 10;

    /** How long one fire charge is worth, in ticks. Ten seconds, before anything is summed onto it. */
    public static final int DURATION = 200;

    /**
     * The most ghasts one holder may ever have.
     * <p>
     * The level is what sets the count and nothing in the mod presses Ghasted above II today, so
     * this is not reached - it is here because a flock is a number of *entities* rather than a
     * reward, and this file's own rule is that a figure like that is bounded whatever the thing
     * driving it happens to be worth.
     */
    private static final int MAX_FLOCK = 8;

    private Ghasted() {
    }

    /**
     * Presses Ghasted onto {@code target} at {@code amplifier}, summing with whatever is already
     * there. This is what a fire charge calls; nothing else needs to know how the summing works.
     */
    public static void apply(LivingEntity target, int amplifier, @Nullable LivingEntity source) {
        target.addEffect(new MobEffectInstance(ModMobEffects.GHASTED, DURATION, amplifier,
                false, true, true), source);
    }

    /**
     * The summing half, called from {@code MobEffectEvent.Added} - which fires <em>before</em>
     * vanilla merges the two instances, and fires whether or not the merge would change anything.
     * <p>
     * Nothing is written here. The wanted duration is worked out and stamped on the holder as
     * persistent data, and {@link #tick} applies it on the holder's next tick; going through the
     * data rather than a static map is what makes it survive the holder being unloaded mid-fight.
     */
    public static void onAdded(LivingEntity target, @Nullable MobEffectInstance old,
                               MobEffectInstance added) {
        if (old == null) {
            return;
        }

        // Vanilla is about to keep max(old, added); this is the difference it needs to be told.
        target.getPersistentData().putInt(TAG_PENDING, old.getDuration() + added.getDuration());
        target.getPersistentData().putInt(TAG_PENDING_AMPLIFIER,
                Math.max(old.getAmplifier(), added.getAmplifier()));
    }

    /** Ghasted is gone, so the queued sum is meaningless and must not be applied to nothing. */
    public static void onRemoved(LivingEntity target) {
        target.getPersistentData().remove(TAG_PENDING);
        target.getPersistentData().remove(TAG_PENDING_AMPLIFIER);
    }

    /**
     * Whether {@code holder} has anything for {@link #tick} to do. The cheap test the tick handler
     * puts in front of everything, so the cost on a world full of things that are not ghasted is one
     * map read and one tag lookup.
     */
    public static boolean isGhasted(LivingEntity holder) {
        return holder.hasEffect(ModMobEffects.GHASTED)
                || holder.getPersistentData().contains(TAG_PENDING);
    }

    /** One tick of the effect for one holder: the queued sum, then the flock. */
    public static void tick(ServerLevel level, LivingEntity holder) {
        applyPending(holder);

        MobEffectInstance ghasted = holder.getEffect(ModMobEffects.GHASTED);
        if (ghasted == null || holder.tickCount % TOP_UP_INTERVAL != 0) {
            return;
        }

        int wanted = Math.min(MAX_FLOCK, ghasted.getAmplifier() + 1);
        List<MiniGhastEntity> flock = level.getEntitiesOfClass(MiniGhastEntity.class,
                holder.getBoundingBox().inflate(FLOCK_SEARCH_RADIUS),
                ghast -> ghast.isAlive() && ghast.isOwnedBy(holder));

        for (int i = flock.size(); i < wanted; i++) {
            MiniGhastEntity ghast = ModEntities.MINI_GHAST.get().create(level);
            if (ghast == null) {
                return;
            }

            ghast.setGhastOwner(holder);
            // Spread evenly around the ring rather than left on the random phase each one is born
            // with, so a flock of four is four points of a compass and not a huddle.
            ghast.setPhase(Mth.TWO_PI * i / wanted);
            ghast.moveTo(holder.getX(), holder.getY() + holder.getBbHeight(), holder.getZ(),
                    holder.getYRot(), 0.0F);
            level.addFreshEntity(ghast);
        }
    }

    /** Replaces the merged instance with the summed one, if {@link #onAdded} left one waiting. */
    private static void applyPending(LivingEntity holder) {
        if (!holder.getPersistentData().contains(TAG_PENDING)) {
            return;
        }

        int duration = holder.getPersistentData().getInt(TAG_PENDING);
        int amplifier = holder.getPersistentData().getInt(TAG_PENDING_AMPLIFIER);
        onRemoved(holder);

        // Only if the effect is still on: a Ghasted cured within the tick it was re-applied should
        // stay cured, and forcing the sum back on would be the one way to make milk not work.
        if (holder.hasEffect(ModMobEffects.GHASTED)) {
            // forceAddEffect rather than addEffect, because addEffect would merge again and keep
            // the longer of the two - which is exactly the rule being overruled here.
            holder.forceAddEffect(new MobEffectInstance(ModMobEffects.GHASTED, duration, amplifier,
                    false, true, true), null);
        }
    }

    /** How far from its holder a ghast may have drifted and still be counted as one of the flock. */
    private static final double FLOCK_SEARCH_RADIUS = 8.0;

    private static final String TAG_PENDING = "ucc_ghasted_pending";
    private static final String TAG_PENDING_AMPLIFIER = "ucc_ghasted_pending_amplifier";
}
