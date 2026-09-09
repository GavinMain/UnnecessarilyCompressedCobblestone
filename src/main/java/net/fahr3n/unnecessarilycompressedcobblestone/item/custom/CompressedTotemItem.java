package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.util.Corrosion;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Compressed Totem of Undying: a totem that no death gets past.
 * <p>
 * Vanilla's totem is answered from inside {@code LivingEntity#hurt}, by
 * {@code checkTotemDeathProtection}, and the first line of that method is
 * {@code if (source.is(BYPASSES_INVULNERABILITY)) return false} - so the ordinary totem is refused
 * by exactly the deaths a player cannot otherwise do anything about: {@code /kill}, the void, and
 * anything else in that tag. There is no way to widen it from outside, because the check is private
 * and its refusal is unconditional.
 * <p>
 * So this one is not a totem at all as far as the game is concerned. It is caught a step later, at
 * {@code LivingDeathEvent} - the single gate every death in the game goes through, whatever killed
 * it and whatever tags that damage carried - and cancelling that event is what "saves from all
 * instances of death" actually means. See {@code ModEvents.onCompressedTotemDeath}.
 * <p>
 * Cancelling a death leaves the creature standing at zero health, which would die again on the next
 * hit of anything at all, so the save is a full heal rather than vanilla's one point. And it takes
 * <em>every</em> effect off, the good with the bad: that is the price, and it is a real one - a
 * fight survived by this is a fight resumed with no Strength, no Regeneration and no Resistance,
 * holding whatever was left of the stack.
 * <p>
 * The one death it cannot answer is one that is not a death: something removed from the world
 * outright never calls {@code die} and there is nothing to cancel.
 */
public class CompressedTotemItem extends Item {
    public CompressedTotemItem(Properties properties) {
        super(properties);
    }

    /**
     * Whichever hand is holding one of these, or null if neither is.
     * <p>
     * Hands only, exactly like vanilla's totem. A totem that worked from the inventory would be a
     * setting rather than a decision, and holding it is what a player gives up to carry it.
     */
    @Nullable
    public static InteractionHand heldIn(LivingEntity entity) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (entity.getItemInHand(hand).getItem() instanceof CompressedTotemItem) {
                return hand;
            }
        }

        return null;
    }

    /**
     * Spends one and puts the holder back on its feet: full health, no effects of any kind, no fire
     * and no fall to land.
     * <p>
     * The effects are collected before any are taken off, because {@code removeEffect} writes to the
     * map {@code getActiveEffects} is iterating - the same trap the Compression Rain set's sweep
     * has. Corrosion's clocks are cleared with them: they are not in {@code activeEffects} at all,
     * and a totem that left a hundred points a level ticking would not have saved anybody.
     */
    public static void save(LivingEntity entity, InteractionHand hand) {
        ItemStack totem = entity.getItemInHand(hand);

        // Read before the stack is spent: shrinking the last one empties it, and an empty stack's
        // item is air.
        Item spent = totem.getItem();
        totem.shrink(1);

        entity.setHealth(entity.getMaxHealth());

        List<Holder<MobEffect>> carried = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            carried.add(instance.getEffect());
        }

        for (Holder<MobEffect> effect : carried) {
            entity.removeEffect(effect);
        }

        Corrosion.clear(entity);
        entity.clearFire();
        entity.setTicksFrozen(0);
        entity.resetFallDistance();

        if (entity instanceof ServerPlayer player) {
            player.awardStat(Stats.ITEM_USED.get(spent));
        }

        // Vanilla's own totem animation: entity event 35 is what the client draws the spinning
        // totem, the golden flash and the full-screen item off, so this reads as a totem going off
        // rather than as a death that did not happen. It plays TOTEM_USE itself, client side, so
        // there is deliberately no sound sent from here to double it.
        entity.level().broadcastEntityEvent(entity, (byte) 35);
    }
}
