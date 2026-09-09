package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * A storm that follows one person around. Every few seconds a bolt falls on whoever is carrying it.
 * <p>
 * It sets nothing on fire, and that costs a little work: a vanilla bolt ignites both the ground it
 * lands on and the target it hits, and neither is switchable. {@code setVisualOnly} removes both -
 * and the bolt's own damage with them - so the strike is thrown for the light and the noise, and
 * the damage is dealt separately as {@code minecraft:lightning_bolt}, so that anything resisting
 * lightning still resists this.
 */
public class LightningMobEffect extends MobEffect implements TickingDamage {
    /** How often a bolt falls at level I; each level up halves it. */
    private static final int BASE_INTERVAL = 60;

    private final float damage;

    public LightningMobEffect(int color, float damage) {
        super(MobEffectCategory.HARMFUL, color);
        this.damage = damage;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }

        VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
        if (bolt != null) {
            bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        entity.hurt(entity.damageSources().source(DamageTypes.LIGHTNING_BOLT),
                this.damage * (1 << amplifier));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = BASE_INTERVAL >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    /** Exactly what the bolt hurts for; the light and the noise cost nothing. */
    @Override
    public float damagePerApplication(int amplifier) {
        return this.damage * (1 << amplifier);
    }
}
