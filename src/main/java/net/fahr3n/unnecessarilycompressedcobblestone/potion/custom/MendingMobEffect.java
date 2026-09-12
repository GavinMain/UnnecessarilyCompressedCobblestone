package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import javax.annotation.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedHealingStaffItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.component.CustomData;

/**
 * Instant Health with a linear dial on it: this heals {@code amplifier + 1} points rather than
 * vanilla's {@code 4 << amplifier}.
 * <p>
 * That one difference is the whole reason the effect exists. The Compressed Healing Staff earns a
 * point of healing per digit of Compression Energy inscribed into it, and there is nowhere in
 * vanilla's Instant Health to put a figure like that: its only dial is the amplifier and the
 * amplifier <em>doubles</em>, so a staff at eleven digits would have to choose between three levels
 * (eight points) and four (sixteen). A linear effect can say eleven.
 * <p>
 * Everything else is copied from {@code HealOrHarmMobEffect} deliberately, above all the inversion:
 * the undead are hurt by this rather than healed, exactly as they are by a Potion of Healing. That
 * is what keeps the staff honest as a healing potion rather than as a heal - a splash landed on a
 * skeleton is a weapon, and the deeper the staff is inscribed the more of one it is.
 * <p>
 * Both halves are implemented because both are reachable. {@code applyInstantenousEffect} is the one
 * that runs when a splash potion lands - and is where the distance falloff arrives, as
 * {@code health} - while {@code applyEffectTick} is what runs if the effect is ever applied
 * directly; an instantaneous effect that only implemented the first would do nothing at all when
 * added to an entity.
 */
public class MendingMobEffect extends InstantenousMobEffect {
    public MendingMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    /** What one level is worth, in health points. Linear - that is the point of the class. */
    private static int amount(int amplifier) {
        return amplifier + 1;
    }

    /**
     * What a splash is worth. The amplifier cannot say more than 256, since vanilla clamps it, so a
     * Healing Staff bottle carries its exact figure on the item and that wins wherever it is present.
     */
    private static float amount(@Nullable Entity source, int amplifier) {
        if (source instanceof ThrownPotion potion) {
            CompoundTag tag = potion.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(CompressedHealingStaffItem.HEALING_TAG)) {
                return tag.getFloat(CompressedHealingStaffItem.HEALING_TAG);
            }
        }

        return amount(amplifier);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity.isInvertedHealAndHarm()) {
            livingEntity.hurt(livingEntity.damageSources().magic(), amount(amplifier));
        } else {
            livingEntity.heal(amount(amplifier));
        }

        return true;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource,
                                        LivingEntity livingEntity, int amplifier, double health) {
        // `health` is the splash falloff - one at the middle of the cloud, down to nothing at its
        // edge - so a potion landed at somebody's feet is worth its whole figure and one landed
        // four blocks off is worth very little of it.
        float points = (float) (health * amount(source, amplifier));
        if (points < 0.5F) {
            return;
        }

        if (!livingEntity.isInvertedHealAndHarm()) {
            livingEntity.heal(points);
        } else if (source == null) {
            livingEntity.hurt(livingEntity.damageSources().magic(), points);
        } else {
            livingEntity.hurt(livingEntity.damageSources().indirectMagic(source, indirectSource), points);
        }
    }
}
