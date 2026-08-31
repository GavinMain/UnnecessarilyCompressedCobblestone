package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.Optional;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;

/**
 * The dragon egg a boss is called up out of. It is what a spawn egg leaves behind instead of the
 * boss itself: it spins in place, climbs two blocks over five seconds while the glow builds, blows
 * everything nearby off its feet, and only then is the boss standing where the egg started.
 * <p>
 * The rise and the spin are both read off {@code tickCount}, so nothing about the animation has to
 * be synced - the client counts its own ticks and arrives at the same place. Only which boss is
 * coming has to travel, and that is saved rather than synced, because the client never needs it.
 */
public class BossSummonEggEntity extends Entity {
    /** Five seconds of climb. */
    public static final int RISE_TICKS = 100;

    /** How far it climbs in that time. */
    public static final double RISE_HEIGHT = 2.0;

    /** A turn every three seconds at the start, winding up to far faster by the end. */
    public static final float SPIN_START_DEGREES = 6.0F;
    public static final float SPIN_END_DEGREES = 40.0F;

    /**
     * A wind charge's own knockback multiplier is 1.22. This is four and a half times that, which is
     * what makes the burst throw rather than nudge - it still breaks nothing and hurts nothing,
     * because those are the first two flags, and knockback is the only thing being scaled.
     */
    private static final float BURST_KNOCKBACK = 1.22F * 4.5F;

    /** Roughly three times a wind charge's 1.2 reach, so a whole ritual circle is cleared. */
    private static final float BURST_RADIUS = 4.0F;

    private static final ExplosionDamageCalculator BURST =
            new SimpleExplosionDamageCalculator(false, false, Optional.of(BURST_KNOCKBACK), Optional.empty());

    private static final String TAG_BOSS = "boss";
    private static final String TAG_BASE_Y = "base_y";

    /** The height it started at; the climb is measured from here so a reload cannot double it. */
    private double baseY;

    private ResourceLocation boss = ResourceLocation.withDefaultNamespace("pig");

    public BossSummonEggEntity(EntityType<? extends BossSummonEggEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public BossSummonEggEntity(Level level, double x, double y, double z, EntityType<?> boss) {
        this(ModEntities.BOSS_SUMMON_EGG.get(), level);
        setPos(x, y, z);
        this.baseY = y;
        this.boss = BuiltInRegistries.ENTITY_TYPE.getKey(boss);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        double progress = Math.min(1.0, this.tickCount / (double) RISE_TICKS);
        setPos(getX(), this.baseY + RISE_HEIGHT * progress, getZ());

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount == 1) {
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, 2.0F, 1.4F);
        }

        glow(serverLevel, progress);

        if (this.tickCount >= RISE_TICKS) {
            burst(serverLevel);
            summon(serverLevel);
            discard();
        }
    }

    /**
     * The light show, thickening as the climb goes on: the dragon's own breath and end rods around
     * the egg, and a ring of them on the ground it is rising off.
     */
    private void glow(ServerLevel level, double progress) {
        int count = 1 + (int) (progress * 6.0);

        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), count, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 0.5, getZ(), count, 0.4, 0.4, 0.4, 0.01);

        // The ring is drawn from the egg's own spin, so the ground follows what the egg is doing.
        double angle = Math.toRadians(spinDegrees(this.tickCount, 0.0F));
        double radius = 1.5;
        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                getX() + Math.cos(angle) * radius, this.baseY + 0.1, getZ() + Math.sin(angle) * radius,
                2, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * A wind charge's burst, several times over. Nothing is broken and nothing is hurt - the
     * calculator's first two flags see to that - so all it does is clear the ground around whatever
     * is about to be standing there.
     */
    private void burst(ServerLevel level) {
        level.explode(this, null, BURST, getX(), this.baseY + 1.0, getZ(), BURST_RADIUS, false,
                Level.ExplosionInteraction.NONE,
                ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                SoundEvents.WIND_CHARGE_BURST);

        level.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 4, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    /** Puts the boss down on the ground the egg rose from, not up in the air where the egg ended. */
    private void summon(ServerLevel level) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(this.boss);
        BlockPos pos = BlockPos.containing(getX(), this.baseY, getZ());
        type.spawn(level, pos, MobSpawnType.SPAWN_EGG);
    }

    /**
     * How far round the egg has turned by {@code age}. It accelerates the whole way up rather than
     * spinning evenly, which is what makes the last second read as the thing about to go off.
     */
    public static float spinDegrees(float age, float partialTicks) {
        float t = Mth.clamp((age + partialTicks) / RISE_TICKS, 0.0F, 1.0F);
        // The integral of a speed rising linearly from start to end, so the turn is continuous even
        // though the speed is not constant.
        float speed = Mth.lerp(t, SPIN_START_DEGREES, SPIN_END_DEGREES);
        return (age + partialTicks) * (SPIN_START_DEGREES + speed) / 2.0F;
    }

    /** How far up the climb it is, for the renderer's glow. */
    public float riseProgress(float partialTicks) {
        return Mth.clamp((this.tickCount + partialTicks) / RISE_TICKS, 0.0F, 1.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains(TAG_BOSS)) {
            ResourceLocation saved = ResourceLocation.tryParse(compound.getString(TAG_BOSS));
            if (saved != null) {
                this.boss = saved;
            }
        }

        this.baseY = compound.contains(TAG_BASE_Y) ? compound.getDouble(TAG_BASE_Y) : getY();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString(TAG_BOSS, this.boss.toString());
        compound.putDouble(TAG_BASE_Y, this.baseY);
    }

    /** Nothing can push it, hit it or stand on it; it is a five second animation, not an obstacle. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
