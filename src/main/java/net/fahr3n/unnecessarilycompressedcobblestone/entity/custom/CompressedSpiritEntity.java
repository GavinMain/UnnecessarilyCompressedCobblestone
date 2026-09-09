package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.fahr3n.unnecessarilycompressedcobblestone.util.SelectiveImmunity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Spirit. A vex in everything it looks like and most of what it does - it flies, it
 * charges, it goes through walls - wound up until none of those things behave the way they do on a
 * vex.
 * <p>
 * Three numbers are the whole fight. It moves <b>five times</b> as fast as a vex, which is fast
 * enough to cross a chunk in six ticks and fast enough that it is rarely where it looks like it is.
 * It hits for {@link #ATTACK_DAMAGE}, which is not a typo: worked back through vanilla's armour
 * curve, 100 raw is exactly ten hearts through a full set of netherite with Protection IV on every
 * piece, which is what it was asked to be worth. And it has only {@link #MAX_HEALTH} - it is a thin
 * thing, not a wall - but nothing in the game can take a point of that off it except
 * <b>lightning</b>. See {@link #refusesDamageFrom}.
 * <p>
 * That last rule is what makes it a boss rather than a nuisance: a player who has never built a
 * Lightning Core or carried a Lightning Staff has no answer to it at all, and one who has kills it
 * in forty bolts.
 */
public class CompressedSpiritEntity extends Vex implements SelectiveImmunity {
    /** Thin. What makes it dangerous is that almost nothing can touch it, not that it soaks damage. */
    public static final float MAX_HEALTH = 200.0F;

    /**
     * Ten hearts through a full set of netherite with Protection IV on all four pieces.
     * <p>
     * Vanilla's two reductions are both worked into it. Netherite's 20 armour and 12 toughness take
     * a hit of {@code D} down by {@code min(20, max(20/5, 20 - D/5)) / 25}, and four Protection IVs
     * are 20 EPF, which is the cap, so the rest is multiplied by 0.2. Solving
     * {@code D * (1 - (20 - D/5)/25) * 0.2 = 20} gives exactly 100, which is a pleasing enough
     * accident to be worth writing down: a spirit that hits an unarmoured player hits for fifty
     * hearts, and that is the same number seen from the other end.
     */
    public static final double ATTACK_DAMAGE = 100.0;

    /**
     * How much faster than a vex. It multiplies the acceleration rather than the velocity, which is
     * the only way to say "five times" and mean it: the move control adds a fixed step to the
     * delta every tick and {@code travel} takes 9% of the delta back off, so terminal speed is the
     * step over the drag and scales with it exactly. Scaling the velocity after the fact instead
     * would feed back through the control and run away - the same trap the Compressed Phantom hit.
     */
    public static final double SPEED_MULTIPLIER = 5.0;

    /** How far it looks for something to fight. It closes that in well under a second. */
    private static final double SEARCH_RADIUS = 48.0;

    /** Ticks between searches when it has nothing to fight; a search walks every entity in reach. */
    private static final int SEARCH_INTERVAL = 20;

    /**
     * How near it has to pass to land a hit, and how long before it can land another.
     * <p>
     * The reach is generous and the test is a swept one on purpose. At full speed it covers nearly
     * three blocks a tick, and {@code move} puts all of that down in one step - so the vex's own
     * "do our bounding boxes overlap this tick" check would have it pass clean through a player
     * between two ticks without ever touching them. Anything this fast has to be tested against the
     * line it travelled, not against where it stopped.
     */
    private static final double HIT_RADIUS = 1.6;
    private static final int HIT_COOLDOWN = 20;

    private static final String TAG_HIT_COOLDOWN = "hit_cooldown";

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_spirit"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private int searchCooldown;
    private int hitCooldown;

    public CompressedSpiritEntity(EntityType<? extends Vex> entityType, Level level) {
        super(entityType, level);

        // The vex's own control, five times as hard. It is replaced rather than wrapped because
        // Vex.VexMoveControl is package private and cannot be reached from here at all.
        this.moveControl = new SpiritMoveControl(this);
        this.xpReward = 100;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS)
                // No armour and no toughness, deliberately: it is already immune to everything but
                // lightning, and armour on top of that would only make the one answer to it slower.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * Nothing costs it anything but lightning.
     * <p>
     * The tag rather than a damage type, so a Lightning Core, a Lightning Staff, a note bolt, a
     * charged trident, a natural storm and anything another mod counts as lightning all work.
     * <p>
     * {@link SelectiveImmunity} rather than {@code isInvulnerableTo}, so a sword still <em>hits</em>
     * it - it flashes, it is knocked back, and everything this mod hangs off landing a blow gets its
     * turn - and simply takes nothing off it. {@code /kill}, the void and this mod's true damage are
     * past the rule entirely; see {@link SelectiveImmunity#refusable}.
     */
    @Override
    public boolean refusesDamageFrom(DamageSource source) {
        return SelectiveImmunity.refusable(source) && !source.is(DamageTypeTags.IS_LIGHTNING);
    }

    /**
     * The vex's goals, minus its targeting. What it hunts is the mod's boss rule instead - everything
     * alive except its own kind and the things that only stand there - which is chosen in
     * {@link #customServerAiStep}, so the target goals would only fight it.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(goal -> true);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.hitCooldown > 0) {
            this.hitCooldown--;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !BossTargeting.isValid(this, target, SEARCH_RADIUS, this::isOwnKind)) {
            setTarget(null);
            if (this.searchCooldown-- <= 0) {
                this.searchCooldown = SEARCH_INTERVAL;
                setTarget(BossTargeting.choose(serverLevel, this, SEARCH_RADIUS, this::isOwnKind));
            }

            return;
        }

        sweep(target);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.3, getZ(),
                2, 0.1, 0.1, 0.1, 0.01);
    }

    /**
     * Hits {@code target} if this tick's flight passed near enough to it.
     * <p>
     * The test is the distance from the target to the segment between where the spirit was last tick
     * and where it is now, which is the only test that survives its speed - see {@link #HIT_RADIUS}.
     * The cooldown is a full second because a shorter one would be spent on nothing: a living entity
     * is invulnerable for twenty ticks after a hit unless the next one is bigger, and every hit here
     * is the same size.
     */
    private void sweep(LivingEntity target) {
        if (this.hitCooldown > 0) {
            return;
        }

        Vec3 from = new Vec3(this.xo, this.yo, this.zo);
        Vec3 to = position();
        Vec3 point = target.getBoundingBox().getCenter();

        Vec3 travelled = to.subtract(from);
        double length = travelled.lengthSqr();
        double along = length < 1.0E-6 ? 0.0
                : Mth.clamp(point.subtract(from).dot(travelled) / length, 0.0, 1.0);

        if (from.add(travelled.scale(along)).distanceToSqr(point) <= HIT_RADIUS * HIT_RADIUS) {
            this.hitCooldown = HIT_COOLDOWN;
            doHurtTarget(target);
            setIsCharging(false);
        }
    }

    /** Its own kind, which is the one thing the boss rule says it will not attack. */
    private boolean isOwnKind(Entity entity) {
        return entity instanceof CompressedSpiritEntity;
    }

    /** Summoned, not spawned: it never despawns, however far it gets from whoever called it up. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * The peaceful branch of {@code Mob.checkDespawn} runs before the persistence check, so it needs
     * answering separately - the same three-part rule the Compressed Phantom needs.
     */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_HIT_COOLDOWN, this.hitCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.hitCooldown = compound.getInt(TAG_HIT_COOLDOWN);
    }

    /**
     * The vex's move control with its acceleration multiplied. Everything else about it - the stop
     * when it arrives, the way it faces its target rather than its heading - is vanilla's, copied
     * because the class it lives in cannot be subclassed from outside {@code Vex}.
     */
    private static class SpiritMoveControl extends MoveControl {
        private final CompressedSpiritEntity spirit;

        private SpiritMoveControl(CompressedSpiritEntity spirit) {
            super(spirit);
            this.spirit = spirit;
        }

        @Override
        public void tick() {
            if (this.operation != Operation.MOVE_TO) {
                return;
            }

            Vec3 toWanted = new Vec3(this.wantedX - this.spirit.getX(), this.wantedY - this.spirit.getY(),
                    this.wantedZ - this.spirit.getZ());
            double distance = toWanted.length();
            if (distance < this.spirit.getBoundingBox().getSize()) {
                this.operation = Operation.WAIT;
                this.spirit.setDeltaMovement(this.spirit.getDeltaMovement().scale(0.5));
                return;
            }

            this.spirit.setDeltaMovement(this.spirit.getDeltaMovement()
                    .add(toWanted.scale(this.speedModifier * 0.05 * SPEED_MULTIPLIER / distance)));

            LivingEntity target = this.spirit.getTarget();
            Vec3 facing = target != null
                    ? new Vec3(target.getX() - this.spirit.getX(), 0.0, target.getZ() - this.spirit.getZ())
                    : this.spirit.getDeltaMovement();
            this.spirit.setYRot(-((float) Mth.atan2(facing.x, facing.z)) * (180.0F / (float) Math.PI));
            this.spirit.yBodyRot = this.spirit.getYRot();
        }
    }

    /* BOSS BAR */

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
        this.bossEvent.addPlayer(serverPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer serverPlayer) {
        super.stopSeenByPlayer(serverPlayer);
        this.bossEvent.removePlayer(serverPlayer);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }
}
