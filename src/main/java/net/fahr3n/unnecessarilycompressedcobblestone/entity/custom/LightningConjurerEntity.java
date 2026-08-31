package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The lesser conjurer: the one that wanders out of the dark on its own rather than being called up.
 * It hunts and channels exactly the way the boss does - the whole of that is
 * {@link AbstractConjurerEntity}'s - but it is a five second wind-up for five damage, and it dies to
 * about the same beating a zombie does.
 * <p>
 * Its bolt sets nothing on fire. That is deliberate and it is not something a real bolt can be asked
 * for: vanilla lightning ignites the ground it lands on and the target it hits, and neither is
 * switchable. So the bolt it throws is visual only - a flash, a crack of thunder and nothing else -
 * and the damage behind it is dealt here, as {@code minecraft:lightning_bolt} so that everything
 * built to resist lightning still does.
 */
public class LightningConjurerEntity extends AbstractConjurerEntity {
    /** A zombie's health. It is a nuisance at range, not something that survives being reached. */
    public static final float MAX_HEALTH = 20.0F;

    /** What one strike does, before the target's own armour and any lightning resistance. */
    public static final float STRIKE_DAMAGE = 5.0F;

    /** Five seconds of channelling - nearly twice the boss's, and the window to close on it. */
    private static final int CHANNEL_TICKS = 100;

    /** Far enough to be a threat across a clearing, nowhere near the boss's 128. */
    private static final double ATTACK_RADIUS = 24.0;

    /** The three blocks a real bolt reaches, kept so the flash and the damage agree. */
    private static final double STRIKE_SPLASH = 3.0;

    public LightningConjurerEntity(EntityType<? extends Drowned> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    /** A drowned's own numbers, bar the reach it needs to pick a target at cast range. */
    public static AttributeSupplier.Builder createAttributes() {
        return Drowned.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0)
                .add(Attributes.FOLLOW_RANGE, ATTACK_RADIUS);
    }

    /**
     * Unlike the boss, this one walks. The goals only ever move it - what it attacks and when is
     * still decided in {@code customServerAiStep}, which roots it for the whole of every cast, so a
     * conjurer that is casting stands still and one that is not closes the distance.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.0, (float) ATTACK_RADIUS));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public int channelTicks() {
        return CHANNEL_TICKS;
    }

    @Override
    public double attackRadius() {
        return ATTACK_RADIUS;
    }

    /**
     * A bolt with nothing behind it, and the damage dealt by hand. {@code setVisualOnly} is what
     * takes away the fire - both the fires it would start on the ground and the eight seconds it
     * would leave the target burning for - and it takes the bolt's own damage with it, which is why
     * the five is applied here instead.
     */
    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        // Credited to the conjurer, so a kill reads as its own rather than as an act of weather.
        DamageSource source = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, this);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(STRIKE_SPLASH), this::isValidTarget);

        for (LivingEntity victim : hit) {
            victim.hurt(source, STRIKE_DAMAGE);
        }
    }
}
