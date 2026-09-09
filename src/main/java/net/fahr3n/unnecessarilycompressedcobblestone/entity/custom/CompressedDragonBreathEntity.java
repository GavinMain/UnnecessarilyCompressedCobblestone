package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Dragon's breath: vanilla's dragon fireball, leaving a cloud of every damaging
 * effect in the game rather than one of Instant Damage.
 * <p>
 * What "every damaging effect" means is {@code #ucc:damage_over_time}, which the Compression Bomb
 * already reads and which exists precisely because nothing on a {@code MobEffect} can be asked
 * whether it hurts its holder - a category is only as fine as "harmful", which is also blindness and
 * nausea. So the tag is the answer, and it is the right shape for one: another mod's poison is in
 * this breath the day a datapack adds it, and a pack that empties the tag gets a cloud that is
 * nothing but scenery, which is what it asked for.
 * <p>
 * It is a {@link DragonFireball} subclass for its flight, its trail and its renderer, and overrides
 * exactly one thing: {@code onHit}, which deliberately does <em>not</em> call {@code super}. That
 * method is where vanilla builds its own Instant Damage cloud, so calling it would leave two clouds
 * where one was wanted. Everything it did that matters - the guard against the dragon shooting
 * itself, the cloud's shape, the level event and the discard - is repeated below.
 */
public class CompressedDragonBreathEntity extends DragonFireball {
    /** How long each effect the cloud applies runs for, and at what level. */
    private static final int EFFECT_TICKS = 200;
    private static final int EFFECT_AMPLIFIER = 1;

    /** The cloud's shape: vanilla's dragon breath, over a third of the time. */
    private static final float RADIUS = 3.0F;
    private static final float MAX_RADIUS = 7.0F;
    private static final int DURATION = 200;

    /** How far from the impact something is dragged the cloud onto instead. Vanilla's four blocks. */
    private static final double SPLASH = 4.0;

    public CompressedDragonBreathEntity(EntityType<? extends CompressedDragonBreathEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Aimed by hand rather than by the inherited constructor, which pins itself to
     * {@code EntityType.DRAGON_FIREBALL} - the same trap {@code WitherSkull} sets. What that
     * constructor adds is a position, an owner and a starting velocity, and all three are done here.
     */
    public CompressedDragonBreathEntity(Level level, LivingEntity owner, Vec3 heading) {
        super(ModEntities.COMPRESSED_DRAGON_BREATH.get(), level);
        moveTo(owner.getX(), owner.getEyeY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        reapplyPosition();
        setOwner(owner);

        if (heading.lengthSqr() > 1.0E-4) {
            setDeltaMovement(heading.normalize().scale(this.accelerationPower));
            this.hasImpulse = true;
        }
    }

    /**
     * The impact. See the class note for why {@code super.onHit} is not called.
     */
    @Override
    protected void onHit(HitResult result) {
        if (level().isClientSide()) {
            return;
        }

        // A dragon that flew into its own breath has not breathed on anything.
        if (result.getType() == HitResult.Type.ENTITY && ownedBy(((EntityHitResult) result).getEntity())) {
            return;
        }

        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        if (getOwner() instanceof LivingEntity owner) {
            cloud.setOwner(owner);
        }

        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(RADIUS);
        cloud.setDuration(DURATION);
        cloud.setRadiusPerTick((MAX_RADIUS - cloud.getRadius()) / (float) cloud.getDuration());
        fill(cloud);

        // Vanilla's own last touch: if something living is standing near where it landed, the cloud
        // is put on them rather than on the block face, so a breath aimed at somebody catches them.
        for (LivingEntity caught : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(SPLASH, SPLASH / 2.0, SPLASH))) {
            if (distanceToSqr(caught) < SPLASH * SPLASH) {
                cloud.setPos(caught.getX(), caught.getY(), caught.getZ());
                break;
            }
        }

        // 2006 is the dragon breath puff, and -1 in the data is vanilla's "do it silently".
        level().levelEvent(2006, blockPosition(), isSilent() ? -1 : 1);
        level().addFreshEntity(cloud);
        discard();
    }

    /**
     * Everything in {@code #ucc:damage_over_time}, at level two, for ten seconds a dose. A cloud
     * re-applies to whatever is standing in it, so the ten seconds are the floor of what standing in
     * it costs rather than the whole of it.
     */
    private static void fill(AreaEffectCloud cloud) {
        BuiltInRegistries.MOB_EFFECT.getTag(ModTags.MobEffects.DAMAGE_OVER_TIME).ifPresent(effects -> {
            for (Holder<MobEffect> effect : effects) {
                cloud.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, EFFECT_AMPLIFIER,
                        false, true, true));
            }
        });
    }

    /** Nothing shoots a breath out of the air; it is a puff of gas with a fuse. */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    /** Its owner is the dragon, and a dragon is never in the way of its own breath. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return !(target instanceof CompressedDragonEntity) && super.canHitEntity(target);
    }
}
