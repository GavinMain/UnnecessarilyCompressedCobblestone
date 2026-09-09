package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One of the little ghasts the Ghasted effect hangs around its holder: a real ghast, a quarter size,
 * that nothing can touch and that screams on a clock of its own.
 * <p>
 * It is a {@link Ghast} subclass for one reason and it is a good one - the model, the renderer and
 * the ambient moan all come free, and the moan is half the effect. Everything a ghast <em>does</em>
 * is taken away instead: no goals at all (so {@code super.registerGoals()} is deliberately not
 * called, which is what drops vanilla's fireball goal and its player-only targeting), no gravity, no
 * physics, no collision, nothing that can be hit or pushed or looked at as an enemy. It is a
 * decoration with a timer, and the timer is the whole content.
 * <p>
 * <b>The clock is on the entity rather than on the effect, and that is the design.</b> A
 * {@code MobEffect} ticks once for its holder, so an effect that owned the screams could only ever
 * make them happen together; putting the countdown on each ghast is what makes a flock of four into
 * four independent sources of dread rather than one loud one. Each rolls its own
 * {@code rand(SCREAM_MIN, SCREAM_MAX)} the moment it arrives and rolls again after every scream.
 * <p>
 * Size is {@link Attributes#SCALE} rather than a scaled pose stack, which is the mod's rule
 * everywhere: the attribute is read by {@code getDimensions}, by the renderer and by the shadow, so
 * the three agree. That the hitbox shrinks with it happens to matter not at all here, since nothing
 * may touch one anyway.
 * <p>
 * It is never saved. On a reload {@code Ghasted} simply finds the holder short of ghasts and puts
 * new ones back - which is cheaper than making a flock survive a restart and, more to the point,
 * means a ghast can never be orphaned by its holder disappearing while the chunk was unloaded.
 */
public class MiniGhastEntity extends Ghast {
    /** A quarter of a ghast, which is about a player's height and reads as a hanging lantern. */
    public static final double SCALE = 0.25;

    /** What one scream costs, in health points. */
    public static final float SCREAM_DAMAGE = 100.0F;

    /** How long between screams, in ticks: two to five seconds, rolled fresh every time. */
    public static final int SCREAM_MIN_TICKS = 40;
    public static final int SCREAM_MAX_TICKS = 100;

    /** How far out from its holder one floats, and how fast the ring turns, in radians a tick. */
    private static final double ORBIT_RADIUS = 1.9;
    private static final double ORBIT_SPEED = 0.04;

    /** How high above the holder's feet the ring sits, as a fraction of their height. */
    private static final double ORBIT_HEIGHT = 1.4;

    private static final String TAG_OWNER = "ghast_owner";

    @Nullable
    private UUID ownerId;

    /** Where on the ring this one sits, so a flock spreads out instead of stacking up. */
    private float phase;

    private int screamIn;

    public MiniGhastEntity(EntityType<? extends MiniGhastEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setInvulnerable(true);
        setNoGravity(true);
        setSilent(false);
        this.noPhysics = true;
        setPersistenceRequired();
        this.screamIn = rollScream();
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
    }

    /** A ghast's own attributes, shrunk. The health is never spent - nothing can hurt one. */
    public static AttributeSupplier.Builder createAttributes() {
        return Ghast.createAttributes()
                .add(Attributes.SCALE, SCALE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Not one goal. Deliberately does not call super, which is what drops a ghast's whole AI. */
    @Override
    protected void registerGoals() {
    }

    public void setGhastOwner(LivingEntity owner) {
        this.ownerId = owner.getUUID();
    }

    /** Spreads a flock evenly around the ring instead of letting the random phases clump. */
    public void setPhase(float phase) {
        this.phase = phase;
    }

    /** Whether this one belongs to {@code candidate}, which is how {@code Ghasted} counts a flock. */
    public boolean isOwnedBy(LivingEntity candidate) {
        return candidate.getUUID().equals(this.ownerId);
    }

    @Nullable
    private LivingEntity owner(ServerLevel level) {
        return this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity living
                ? living : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity owner = owner(serverLevel);

        // The flock exists only for as long as the effect does. Checking here rather than only in
        // Ghasted is what makes an effect that is cured, drunk off or simply run out take its ghasts
        // with it on the same tick, from either end.
        if (owner == null || !owner.isAlive() || !owner.hasEffect(ModMobEffects.GHASTED)) {
            discard();
            return;
        }

        orbit(owner);

        if (--this.screamIn > 0) {
            return;
        }

        this.screamIn = rollScream();
        scream(serverLevel, owner);
    }

    /** Placed rather than steered: a ghast with no goals has nothing to fly it. */
    private void orbit(LivingEntity owner) {
        double angle = this.phase + this.tickCount * ORBIT_SPEED;
        setDeltaMovement(Vec3.ZERO);
        setPos(owner.getX() + Math.cos(angle) * ORBIT_RADIUS,
                owner.getY() + owner.getBbHeight() * ORBIT_HEIGHT,
                owner.getZ() + Math.sin(angle) * ORBIT_RADIUS);

        // Facing outwards, so a flock reads as a ring watching whatever is around its holder.
        setYRot((float) (-Math.toDegrees(angle) - 90.0));
        this.yBodyRot = getYRot();
    }

    /**
     * The scream, and the damage it carries.
     * <p>
     * The sound is played at full volume with no attenuation cut, and it is not decoration: it is
     * the only warning a player gets that a hit is landing, and the only way to count how many
     * ghasts are on them without turning round. It goes out on the hostile channel, at a ghast's own
     * scream sample, so it is the sound the fight has already taught them to be afraid of.
     */
    private void scream(ServerLevel level, LivingEntity owner) {
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GHAST_SCREAM,
                SoundSource.HOSTILE, 4.0F, 1.6F);

        // Credited to this ghast directly and, through it, to whatever put the effect on: the mini
        // ghast is the direct entity so the message reads as a scream, and the boss is the cause so
        // a kill during the fight belongs to the fight.
        DamageSource source = level.damageSources().source(ModDamageTypes.GHAST_SCREAM, this, this);
        owner.hurt(source, SCREAM_DAMAGE);
    }

    private int rollScream() {
        return SCREAM_MIN_TICKS + this.random.nextInt(SCREAM_MAX_TICKS - SCREAM_MIN_TICKS + 1);
    }

    /* NOTHING MAY TOUCH ONE */

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** Conjured rather than paid for, so it leaves nothing behind - the mod's rule for every summon. */
    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    /** Never written to disk; {@code Ghasted} puts the flock back on the next tick after a reload. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.ownerId != null) {
            compound.putUUID(TAG_OWNER, this.ownerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.ownerId = compound.hasUUID(TAG_OWNER) ? compound.getUUID(TAG_OWNER) : null;
    }
}
