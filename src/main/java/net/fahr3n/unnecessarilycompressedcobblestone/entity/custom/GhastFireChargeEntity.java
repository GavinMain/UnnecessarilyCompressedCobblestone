package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Ghasted;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.Fireball;

/**
 * What a Compressed Ghast shoots. It is drawn as a fire charge and is a fireball in every way a
 * player can see; what is different is what it carries and, in one of its two forms, that it steers.
 * <p>
 * It is a {@link Fireball} rather than an {@code AbstractArrow} because that class already is a
 * ghast's ammunition: the fire charge item is synced and drawn for free through {@code ItemSupplier}
 * (so this needs no renderer and no texture), the smoke trail is there, the swept hit test in
 * {@code AbstractHurtingProjectile#tick} is the same one an arrow gets, and the self-acceleration
 * that makes a fireball speed up as it travels is a field rather than something to write.
 * <p>
 * Speed comes out of two numbers rather than one, and it is worth knowing which. The tick loop adds
 * {@code accelerationPower} along the current heading and then multiplies the whole delta by
 * {@link #getInertia()}, so the speed a charge settles at is the acceleration divided by one minus
 * the inertia - a vanilla ghast's 0.1 over 0.05 being two blocks a tick. Both volleys are quoted
 * against that: {@link #SCATTER_ACCELERATION} settles at four, and {@link #HOMING_ACCELERATION} at a
 * deliberately walkable one and a half.
 * <p>
 * It breaks nothing. A ghast's own fireball explodes and takes the wall with it, and a scatter shot
 * of {@code SCATTER_COUNT} of those would delete the ground the fight is standing on within two
 * volleys - so what lands here is the damage, the sound and the fire, and the arena survives.
 */
public class GhastFireChargeEntity extends Fireball {
    /** What one charge is worth, in health points, before armour has anything to say about it. */
    public static final float DAMAGE = 500.0F;

    /** How long the fire it leaves burns, in seconds. A ghast fireball's own five. */
    public static final float FIRE_SECONDS = 5.0F;

    /** Settles at four blocks a tick: twice a ghast's fireball, which is the "much faster" half. */
    public static final double SCATTER_ACCELERATION = 0.2;

    /** Settles at one and a half, so the homing volley can be outrun rather than only out-aimed. */
    public static final double HOMING_ACCELERATION = 0.075;

    /**
     * How hard a homing charge turns: the fraction of the way from its own heading to its target's
     * bearing that it takes each tick.
     * <p>
     * A twelfth is the whole of what makes the homing volley a different attack rather than a
     * cheat. It cannot turn faster than a player can sidestep at close range, so the counterplay is
     * to let it commit and then break off; at distance it has time to correct, which is what makes
     * standing still and shooting back the wrong answer.
     */
    public static final double HOMING_TURN = 1.0 / 12.0;

    /** What {@code ghastedAmplifier} means when the charge should press no Ghasted at all. */
    public static final int NO_GHASTED = -1;

    /** A backstop; a charge normally lands or leaves its loaded chunks long before this. */
    private static final int MAX_LIFETIME = 200;

    private static final String TAG_HOMING = "homing";
    private static final String TAG_AMPLIFIER = "ghasted_amplifier";
    private static final String TAG_TARGET = "homing_target";
    private static final String TAG_DAMAGE = "damage";

    private boolean homing;

    /**
     * Which level of Ghasted this charge presses, or {@link #NO_GHASTED} for one that presses none.
     * <p>
     * The boss's two volleys are the whole reason this is a number rather than a flag - see
     * {@code CompressedGhastEntity}. The pet ghast is the reason it can be switched off: a charge
     * that hangs a flock of screaming ghasts on whatever it hits is a boss's statement, and handing
     * that to something a player carries around would make the pet worth more than the fight it
     * came out of.
     */
    private int ghastedAmplifier;

    /**
     * What this one hits for. A field rather than {@link #DAMAGE} directly because the same charge
     * is fired by the boss and by the pet built out of its heart, and the difference between those
     * two is entirely this number.
     */
    private float damage = DAMAGE;

    @Nullable
    private UUID targetId;

    public GhastFireChargeEntity(EntityType<? extends GhastFireChargeEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GhastFireChargeEntity(Level level, LivingEntity shooter, Vec3 heading) {
        super(ModEntities.GHAST_FIRE_CHARGE.get(), shooter, heading, level);
    }

    /**
     * Turns this into the homing half of the pair: it steers at {@code target} and travels slower
     * for it. The target is held by id rather than by reference so it survives a save and never
     * keeps a dead entity alive.
     */
    public void makeHoming(LivingEntity target) {
        this.homing = true;
        this.targetId = target.getUUID();
        this.accelerationPower = HOMING_ACCELERATION;
        setDeltaMovement(getDeltaMovement().normalize().scale(HOMING_ACCELERATION));
    }

    public void setGhastedAmplifier(int ghastedAmplifier) {
        this.ghastedAmplifier = ghastedAmplifier;
    }

    /** What this charge is worth, for a shooter that is not the boss. */
    public void setDamage(float damage) {
        this.damage = damage;
    }

    /** Ghast fire rather than the default smoke: this is a fire charge and should read as one. */
    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMALL_FLAME;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            if (this.homing) {
                steer(serverLevel);
            }

            if (this.tickCount > MAX_LIFETIME) {
                discard();
                return;
            }
        }

        super.tick();
    }

    /**
     * One tick of the turn, done <em>before</em> {@code super.tick()} so the acceleration that class
     * adds is applied along the heading this leaves behind rather than the one before it.
     * <p>
     * The turn is written on the direction and the speed is put back afterwards, so steering costs a
     * charge nothing - a homing charge that slowed down every time it corrected would stall into a
     * hover the moment its target started circling.
     */
    private void steer(ServerLevel level) {
        Entity target = this.targetId == null ? null : level.getEntity(this.targetId);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        Vec3 heading = getDeltaMovement();
        double speed = heading.length();
        if (speed < 1.0E-4) {
            return;
        }

        Vec3 wanted = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0)
                .subtract(position()).normalize();

        setDeltaMovement(heading.normalize().lerp(wanted, HOMING_TURN).normalize().scale(speed));
    }

    /**
     * The hit: one blow of {@link #DAMAGE}, a few seconds of burning, and the Ghasted effect - which
     * is the half that outlives the fight.
     * <p>
     * The damage source is vanilla's own {@code fireball}, so it is already in
     * {@code #minecraft:is_fire} and Fire Resistance answers the charge exactly as it answers the
     * screams that follow it. The kill is credited to the ghast that shot it.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!(level() instanceof ServerLevel) || !(result.getEntity() instanceof LivingEntity hit)) {
            return;
        }

        Entity owner = getOwner();
        hit.hurt(damageSources().fireball(this, owner), this.damage);
        hit.igniteForSeconds(FIRE_SECONDS);

        if (this.ghastedAmplifier != NO_GHASTED) {
            Ghasted.apply(hit, this.ghastedAmplifier, owner instanceof LivingEntity living ? living : null);
        }
    }

    /**
     * Nothing is broken and nothing explodes - see the note on this class. What is left is the
     * flash and the bang, which is what a player actually reads a fireball landing by.
     */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(),
                    12, 0.3, 0.3, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(),
                    16, 0.3, 0.3, 0.3, 0.05);
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 1.0F, 1.6F);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(TAG_HOMING, this.homing);
        compound.putInt(TAG_AMPLIFIER, this.ghastedAmplifier);
        compound.putFloat(TAG_DAMAGE, this.damage);
        if (this.targetId != null) {
            compound.putUUID(TAG_TARGET, this.targetId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.homing = compound.getBoolean(TAG_HOMING);
        this.ghastedAmplifier = compound.getInt(TAG_AMPLIFIER);
        if (compound.contains(TAG_DAMAGE)) {
            this.damage = compound.getFloat(TAG_DAMAGE);
        }
        this.targetId = compound.hasUUID(TAG_TARGET) ? compound.getUUID(TAG_TARGET) : null;
    }
}
