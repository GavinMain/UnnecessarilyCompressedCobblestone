package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The dome an Arrow Veil throws up. It has no collision of its own: instead it watches the sphere
 * around it and takes apart any projectile inside that its owner did not fire.
 * <p>
 * Doing it that way rather than with a hitbox is what makes the rule "everything but mine" possible
 * at all. A block or a solid entity stops arrows by being in the way, and being in the way cannot
 * tell whose arrow it is - it would trap the owner inside their own shelter. Reading
 * {@link Projectile#getOwner()} instead means the owner shoots out freely, and everything shot at
 * them from outside comes apart on the way in, whether it was fired by a skeleton, a dispenser or
 * another player.
 */
public class ArrowVeilEntity extends Entity {
    /** Thirty seconds of cover. */
    public static final int LIFETIME = 600;

    /** How far the dome reaches, in blocks. */
    public static final float RADIUS = 4.0F;

    /** How long it takes to swell open and to fade away, in ticks. */
    public static final int FADE = 10;

    /**
     * How far past the dome the sweep looks for something that has already been and gone. A projectile
     * is caught by the path it travelled, so the search box has to be wide enough to still contain it
     * on the tick after it crossed - and the fastest thing in this mod moves a hundred and nine blocks
     * in that tick.
     */
    private static final double SWEEP_MARGIN = 128.0;

    private static final String TAG_OWNER = "veil_owner";
    private static final String TAG_AGE = "veil_age";

    @Nullable
    private UUID owner;

    private int age;

    public ArrowVeilEntity(EntityType<? extends ArrowVeilEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public ArrowVeilEntity(Level level, Vec3 pos, UUID owner) {
        this(ModEntities.ARROW_VEIL.get(), level);
        setPos(pos.x, pos.y, pos.z);
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        this.age++;
        if (this.age >= LIFETIME) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 1.0F, 1.4F);
            }

            discard();
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // The box is what the entity lookup can index; it is inflated by a tick of travel as well as
        // by the dome, because a fast enough arrow is never inside either when a tick happens to look.
        List<Projectile> caught = serverLevel.getEntitiesOfClass(Projectile.class,
                getBoundingBox().inflate(RADIUS + SWEEP_MARGIN), this::blocks);

        for (Projectile projectile : caught) {
            shatter(serverLevel, projectile);
        }
    }

    /** Takes a projectile apart where it stands, with a spark and a clang off the dome. */
    public void shatter(ServerLevel level, Projectile projectile) {
        level.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(),
                8, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 1.6F);
        projectile.discard();
    }

    /**
     * Whether this projectile crossed the dome since the last tick, and is not the owner's own shot.
     * <p>
     * It is the path that is tested rather than the position, and that is not a nicety: the
     * Compressed Skeleton's stream travels a hundred and nine blocks a tick, which is twenty-seven
     * times the width of the dome. Such an arrow is never once inside the sphere at the moment a tick
     * looks at it - it is short of the dome on one tick and long past it on the next - so a position
     * check would wave the entire attack straight through.
     */
    private boolean blocks(Projectile projectile) {
        if (!projectile.isAlive() || !shields(projectile)) {
            return false;
        }

        Vec3 from = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        return distanceToSegment(position(), from, projectile.position()) <= RADIUS;
    }

    /** Whether this projectile is something the dome stops at all: anything not fired by its owner. */
    public boolean shields(Projectile projectile) {
        Entity shooter = projectile.getOwner();
        return shooter == null || !shooter.getUUID().equals(this.owner);
    }

    /** Whether a point is inside the dome. */
    public boolean covers(Vec3 point) {
        return point.distanceToSqr(position()) <= RADIUS * RADIUS;
    }

    /** The closest the segment {@code a} to {@code b} comes to {@code point}. */
    private static double distanceToSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lengthSqr = ab.lengthSqr();
        if (lengthSqr < 1.0E-7) {
            return point.distanceTo(a);
        }

        double along = Mth.clamp(point.subtract(a).dot(ab) / lengthSqr, 0.0, 1.0);
        return point.distanceTo(a.add(ab.scale(along)));
    }

    /** How far open the dome is: it swells on the way in and shrinks on the way out. */
    public float openness(float partialTicks) {
        float age = this.age + partialTicks;
        if (age < FADE) {
            return age / FADE;
        }

        float left = LIFETIME - age;
        return left < FADE ? Math.max(0.0F, left / FADE) : 1.0F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.owner = compound.hasUUID(TAG_OWNER) ? compound.getUUID(TAG_OWNER) : null;
        this.age = compound.getInt(TAG_AGE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.owner != null) {
            compound.putUUID(TAG_OWNER, this.owner);
        }

        compound.putInt(TAG_AGE, this.age);
    }

    /** Nothing can hit it, stand on it or push it; it is a rule about projectiles, not an object. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
