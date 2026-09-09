package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * A potion that goes off. Every few seconds it lets off a real explosion where the drinker is
 * standing - blocks and all - so it is the only one of these five that changes the world rather
 * than only what is standing in it.
 * <p>
 * The blast is the <em>square</em> of the potion's level: 1 at level I, 4 at level II, which is
 * about what a stick of TNT makes. That is a steep curve on purpose - a glowstone brew is not twice
 * this potion, it is four times it - and it is why the base brew is small enough to drink indoors.
 * <p>
 * The damage is the explosion's own, so it is {@code minecraft:explosion} and everything that
 * reduces a blast reduces this. Nothing is credited with it: whoever drank it did this to
 * themselves.
 */
public class ExplosionMobEffect extends MobEffect implements TickingDamage {
    /** How often it goes off at level I; each level up halves it. */
    private static final int BASE_INTERVAL = 60;

    public ExplosionMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return true;
        }

        // TNT rather than MOB, so it breaks ground whatever the mob griefing rule says: a potion
        // somebody chose to drink is not a mob wrecking their build.
        entity.level().explode(null, entity.getX(), entity.getY(), entity.getZ(), power(amplifier),
                Level.ExplosionInteraction.TNT);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = BASE_INTERVAL >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    /**
     * What one blast costs whoever is standing at the middle of it, which is where the holder
     * always is: vanilla's own explosion damage with the distance term at zero, so
     * {@code ((1 + 1) / 2) * 7 * (power * 2) + 1}.
     * <p>
     * It is an estimate rather than a figure this class controls, and it is an upper bound: an
     * explosion is the one effect here whose damage depends on where things are rather than only on
     * the level, so a holder behind cover takes less than this. The bomb rounds in the holder's
     * favour on purpose - it is taking the blast away, and the hit it hands over instead ought not
     * to be the cheaper of the two.
     */
    @Override
    public float damagePerApplication(int amplifier) {
        float reach = power(amplifier) * 2.0F;
        return 7.0F * reach + 1.0F;
    }

    /** The blast, which is the level squared: 1, 4, 9, 16. */
    public static float power(int amplifier) {
        int level = amplifier + 1;
        return (float) (level * level);
    }
}
