package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Corrosion's counters, which are the whole of what the effect actually is.
 * <p>
 * Every other effect in this mod is a {@code MobEffect} and nothing else, because vanilla's rule -
 * one instance of an effect per entity, a second application merging into the first - is what
 * everybody expects. Corrosion is the one that wants the opposite: <em>every</em> application runs
 * its own ten second clock, so being stung by six bees is six clocks and not one refreshed one.
 * There is nowhere in {@code activeEffects} to put six of anything, so the clocks live here instead,
 * in a list on the entity itself, and are ticked by {@code ModEvents}.
 * <p>
 * The registered {@code MobEffect} is still there and still does the other half of the job: it is
 * what a potion, a tipped arrow, a lingering cloud and a bee all apply, what
 * {@code MobEffectEvent.Applicable} lets an immunity refuse, what milk and the Potion of Cleansing
 * take off, and what the inventory draws. It simply does nothing on a tick. Because vanilla merges
 * by taking the longer of two durations, the instance it holds always has exactly the longest of
 * these clocks left on it, so the icon counts down correctly with no syncing at either end.
 * <p>
 * A clock never fires on the tick it was started - the first hit is a full
 * {@value #INTERVAL} ticks in - so an application shorter than that does nothing at all. That is not
 * a rounding artefact but the point of the effect: corrosion is a countdown to something very large
 * rather than a trickle, and outlasting it is the counterplay.
 */
public final class Corrosion {
    /** Ten seconds. How long one clock runs before it hits, and between hits after that. */
    public static final int INTERVAL = 200;

    /**
     * What one hit is worth per level. It is an enormous number on purpose - five times a player's
     * whole health at level I - and it is left as a real damage type rather than true damage, so
     * armour, Protection, Resistance and the Compression Magic set all apply to it in the ordinary
     * way. What none of them can do is stop it landing: see {@code ModDamageTypeTagProvider}, where
     * corrosion is in {@code #minecraft:bypasses_cooldown}, so a clock that comes due a tick after
     * something else hit is not swallowed by the invulnerability window - and neither is a second
     * clock coming due on the same tick as the first.
     */
    public static final float DAMAGE_PER_LEVEL = 100.0F;

    /**
     * How many clocks may run at once. Nothing a player meets ought to come near it - forty bees
     * are forty - and it is here for the one case that can run away on its own: a lingering cloud
     * re-applies its potion every twenty ticks to everything standing in it, so a cloud of corrosion
     * is a new clock three times a second for as long as somebody stays in it.
     * <p>
     * Sixty-four clocks at level I are six thousand four hundred points, which is six times the
     * ceiling of {@code Attributes.MAX_HEALTH}, so the cap costs nothing that was ever survivable
     * and stops the list being unbounded. Applications past it are dropped rather than replacing
     * older ones: the clocks already running are the ones closest to coming due, and they are worth
     * more than the ones that would have replaced them.
     */
    public static final int MAX_CLOCKS = 64;

    private static final String TAG_CLOCKS = "ucc:corrosion";
    private static final String TAG_LEVEL = "level";
    private static final String TAG_ELAPSED = "elapsed";
    private static final String TAG_REMAINING = "remaining";

    private Corrosion() {
    }

    /**
     * Starts one more clock. The level is the numeral rather than the amplifier, so a
     * {@code MobEffectInstance} at amplifier 0 is corrosion I and hits for
     * {@value #DAMAGE_PER_LEVEL}.
     */
    public static void apply(LivingEntity entity, int amplifier, int duration) {
        if (duration <= 0) {
            return;
        }

        ListTag clocks = clocks(entity);
        if (clocks.size() >= MAX_CLOCKS) {
            return;
        }

        CompoundTag clock = new CompoundTag();
        clock.putInt(TAG_LEVEL, amplifier + 1);
        clock.putInt(TAG_ELAPSED, 0);
        clock.putInt(TAG_REMAINING, duration);
        clocks.add(clock);
    }

    /**
     * Stops every clock. This is what milk and the Potion of Cleansing do, and it is hung off
     * {@code MobEffectEvent.Remove} rather than {@code Expired} deliberately: an expiring instance
     * is the clocks running out on their own, and clearing them there would throw away the last hit
     * on the very tick it was due.
     */
    public static void clear(LivingEntity entity) {
        entity.getPersistentData().remove(TAG_CLOCKS);
    }

    /**
     * What every clock still running is going to be worth, added up - the answer to "how much has
     * this creature already lost" before it has lost it.
     * <p>
     * It exists for the Compression Bomb, which collapses damage that has not happened yet into one
     * hit. Corrosion has to be counted here rather than through the effect it registers, for the
     * same reason it keeps its clocks here in the first place: the {@code MobEffectInstance} on the
     * entity carries the longest of the clocks and nothing about the other five, so reading a
     * duration off it would price six stings as one.
     * <p>
     * A clock hits on every {@value #INTERVAL} ticks of its <em>elapsed</em> count and is dropped
     * the tick its remaining count runs out, so what is left is the number of multiples of the
     * interval strictly after where it is now and up to where it ends. Counted rather than looped,
     * since a clock can be minutes long and there may be {@value #MAX_CLOCKS} of them.
     */
    public static float pending(LivingEntity entity) {
        if (!isCorroding(entity)) {
            return 0.0F;
        }

        ListTag clocks = entity.getPersistentData().getList(TAG_CLOCKS, Tag.TAG_COMPOUND);
        float total = 0.0F;

        for (int i = 0; i < clocks.size(); i++) {
            CompoundTag clock = clocks.getCompound(i);
            int elapsed = clock.getInt(TAG_ELAPSED);
            int end = elapsed + Math.max(0, clock.getInt(TAG_REMAINING));
            int hits = end / INTERVAL - elapsed / INTERVAL;
            total += hits * DAMAGE_PER_LEVEL * clock.getInt(TAG_LEVEL);
        }

        return total;
    }

    /** Whether anything is corroding, which is the cheap test the tick handler opens with. */
    public static boolean isCorroding(LivingEntity entity) {
        return entity.getPersistentData().contains(TAG_CLOCKS, Tag.TAG_LIST);
    }

    /**
     * One tick of every clock. Each is stepped, hits if it has come due, and is dropped when it runs
     * out - in that order, so a clock whose duration is exactly {@value #INTERVAL} lands its hit on
     * the tick it ends and one a single tick shorter lands nothing.
     * <p>
     * Every hit is its own {@code hurt} call rather than a sum, which is the opposite of the rule
     * the Compressed Skeleton's volley follows. It is right here for the same reason that one is:
     * corrosion bypasses the invulnerability window, so nothing is thrown away by landing twice on
     * one tick, and keeping them separate is what makes the clocks independent all the way down.
     */
    public static void tick(LivingEntity entity) {
        ListTag clocks = clocks(entity);
        for (int i = clocks.size() - 1; i >= 0; i--) {
            CompoundTag clock = clocks.getCompound(i);
            int elapsed = clock.getInt(TAG_ELAPSED) + 1;
            int remaining = clock.getInt(TAG_REMAINING) - 1;
            clock.putInt(TAG_ELAPSED, elapsed);
            clock.putInt(TAG_REMAINING, remaining);

            if (elapsed % INTERVAL == 0) {
                entity.hurt(entity.damageSources().source(ModDamageTypes.CORROSION),
                        DAMAGE_PER_LEVEL * clock.getInt(TAG_LEVEL));
            }

            if (remaining <= 0) {
                clocks.remove(i);
            }
        }

        if (clocks.isEmpty()) {
            entity.getPersistentData().remove(TAG_CLOCKS);
        }
    }

    /**
     * The clock list, created on the entity if it is not there yet. It is kept in the entity's
     * persistent data rather than in a map here so that it saves, loads and is discarded with the
     * creature carrying it, and so nothing has to be swept when a player logs out - and so that
     * corrosion does not survive a death, which a map keyed by UUID would have had to remember not
     * to do.
     */
    private static ListTag clocks(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG_CLOCKS, Tag.TAG_LIST)) {
            data.put(TAG_CLOCKS, new ListTag());
        }

        return data.getList(TAG_CLOCKS, Tag.TAG_COMPOUND);
    }
}
