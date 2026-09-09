package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A bee that does not wait to be provoked.
 * <p>
 * Everything about it is a bee: the same attributes, the same flight, the same hive-finding,
 * flower-hunting, crop-growing goals, and the same sting that kills the bee that lands it. Two
 * things are different, and only two. It hunts on sight rather than only when angry, and what it
 * leaves in the wound is {@link ModMobEffects#CORROSION} rather than a few seconds of poison.
 * <p>
 * Being aggressive is not a goal so much as a rewiring of one. A bee's whole combat side is gated on
 * {@code isAngry()} - {@code BeeAttackGoal} will not run without it, and vanilla's own targeting
 * goal only looks at players the bee is <em>already</em> angry at - so a plain
 * {@code NearestAttackableTargetGoal} would find a target and then stand there. Setting the anger
 * timer from {@link #setTarget} is what closes that circle, and it does it wherever the target came
 * from: the new goal, the hurt-by goal, a command.
 * <p>
 * It stings once and dies of it, exactly as a bee does. That is deliberate rather than an oversight:
 * a swarm out of a Bee-nt is a single enormous burst of corrosion and then a field of dying bees,
 * and a swarm that could sting forever would be a different and much worse thing.
 */
public class CompressedBeeEntity extends Bee {
    /**
     * How long one sting corrodes for: eleven seconds, which is one clock's worth and a second's
     * grace. Ten would land its hit on the very tick it lapsed, which works but reads as a
     * coincidence rather than as a rule.
     */
    public static final int CORROSION_DURATION = 11 * 20;

    /** Corrosion I. The amplifier is one less than the numeral. */
    public static final int CORROSION_AMPLIFIER = 0;

    public CompressedBeeEntity(EntityType<? extends CompressedBeeEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** A bee's attributes exactly. What makes this one dangerous is the sting, not the creature. */
    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes();
    }

    /**
     * A bee's goals, and one more: hunt any player in sight, angry or not.
     * <p>
     * {@code super.registerGoals()} is called rather than skipped, because everything else about
     * being a bee is worth keeping and two of those goals are stored in fields that
     * {@code Bee.tick} and {@code Bee.customServerAiStep} dereference - a bee with no
     * {@code beePollinateGoal} throws the moment it ticks.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Anger follows the target, rather than the target following anger.
     * <p>
     * This is the whole of "aggressive". A bee's attack goal refuses to run unless
     * {@code isAngry()}, and nothing in vanilla makes a bee angry except being hit or having its
     * hive broken, so acquiring a target has to light the timer itself. It is guarded on
     * {@code isAngry()} so that an already-angry bee's countdown is not restarted every tick the
     * goal re-confirms its target, and on the target being non-null so that {@code stopBeingAngry},
     * which clears the target after a sting, is not immediately undone.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);

        if (target != null && !this.isAngry()) {
            this.setPersistentAngerTarget(target.getUUID());
            this.startPersistentAngerTimer();
        }
    }

    /**
     * The sting. Vanilla's does the damage, the stinger, the sound and the dying; this adds the
     * corrosion on top of it.
     * <p>
     * It is added rather than substituted for the poison, which vanilla only applies on Normal and
     * Hard - corrosion is applied on every difficulty, because it is the reason this creature
     * exists and a Peaceful-difficulty bee that stung for nothing would be a Bee-nt that did
     * nothing. Each sting starts its own clock, so a target caught by six bees has six of them
     * running side by side; see {@link net.fahr3n.unnecessarilycompressedcobblestone.util.Corrosion}.
     */
    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean stung = super.doHurtTarget(entity);
        if (stung && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, CORROSION_DURATION,
                    CORROSION_AMPLIFIER), this);
        }

        return stung;
    }
}
