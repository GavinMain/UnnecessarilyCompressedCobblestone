package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.EnumSet;

import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Ghast. A ghast that has stopped throwing one fireball at a time.
 * <p>
 * Vanilla's single attack is replaced by two, alternating, and the pair is the whole fight:
 * <ul>
 * <li><b>The scatter shot</b> - {@value #SCATTER_COUNT} charges at once in a cone, travelling at
 *     twice a ghast fireball's speed. It cannot be dodged by sidestepping, because it is not aimed
 *     at a point; it is answered by not being in the cone, which means cover or distance.
 * <li><b>The homing shot</b> - {@value #HOMING_COUNT} charges that steer, travelling at well under
 *     half the scatter's speed. It cannot be answered by cover for long, because it will come round;
 *     it is answered by moving, because {@code GhastFireChargeEntity.HOMING_TURN} is slower than a
 *     player at a sprint.
 * </ul>
 * They are opposites on purpose. Each is the counterplay to the other's counterplay, so a player who
 * finds one answer and stops there is caught by the next volley, and the boss telegraphs which is
 * coming through {@link #isCharging()} - vanilla's own charging flag, which the ghast renderer
 * already draws as the open mouth.
 * <p>
 * Neither volley is what kills anybody, which is the real design. Every charge that lands presses
 * Ghasted, and Ghasted is a flock of little ghasts screaming on clocks of their own for as long as
 * it lasts - see {@code MiniGhastEntity}. The fight is therefore about how much of it you carry
 * away, not about how much you take while you are in it, and the counter is a Fire Resistance potion
 * rather than armour: every charge and every scream is in {@code #minecraft:is_fire}.
 * <p>
 * Vanilla's goals are all package private, so {@code super.registerGoals()} is not called and the
 * two it is worth keeping - the drift and the head-turn - are written out again below. The move
 * control is not: {@code Ghast}'s constructor installs its own and this class is happy with it.
 */
public class CompressedGhastEntity extends Ghast {
    public static final float MAX_HEALTH = 5000.0F;

    /** Twenty-eight of the thirty {@code Attributes.ARMOR} allows. */
    public static final double ARMOR = 28.0;

    /**
     * Twenty, which is not the thirty this boss was specified with - {@code ARMOR_TOUGHNESS} is a
     * {@code RangedAttribute} that stops at 20, and anything above it is clamped on the way in with
     * nothing anywhere saying so. Stating the ceiling is better than stating a figure that silently
     * becomes it.
     * <p>
     * It costs the fight nothing. Toughness only widens the band over which armour keeps its full
     * contribution, and armour's contribution is capped by {@code CombatRules} at four fifths
     * however deep either number goes; 28 behind 20 is already at that ceiling against everything a
     * player can swing.
     */
    public static final double ARMOR_TOUGHNESS = 20.0;

    /** How far it looks for something to fight, and how far either volley carries. */
    public static final double SEARCH_RADIUS = 64.0;

    /** How many charges each volley throws. */
    private static final int SCATTER_COUNT = 9;
    private static final int HOMING_COUNT = 3;

    /**
     * How wide the scatter cone is, as the radius of the offset added to each charge's heading at
     * one block out. A third is about twenty degrees, which at thirty blocks is a wall ten blocks
     * across - wide enough that standing still is fatal and narrow enough that it can be left.
     */
    private static final double SCATTER_SPREAD = 0.33;

    /** How far apart the homing charges start, so three of them do not fly as one. */
    private static final double HOMING_SPREAD = 0.12;

    /** How long each volley winds up for, in ticks, and how long the boss waits after one. */
    private static final int SCATTER_WIND_UP = 40;
    private static final int HOMING_WIND_UP = 60;
    private static final int VOLLEY_COOLDOWN = 40;

    /** How many ticks into a wind-up the mouth opens, which is also when the charge sound plays. */
    private static final int MOUTH_OPENS = 10;

    /** How far in front of the ghast a charge is born, so a volley does not start inside its face. */
    private static final double MUZZLE = 4.0;

    /** Which level of Ghasted each volley presses. The slower, rarer shot is worth the deeper one. */
    private static final int SCATTER_GHASTED_AMPLIFIER = 0;
    private static final int HOMING_GHASTED_AMPLIFIER = 1;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_ghast"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    public CompressedGhastEntity(EntityType<? extends CompressedGhastEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 900;
        setPersistenceRequired();
    }

    /**
     * A ghast's attributes with the fight's numbers on them. {@code FOLLOW_RANGE} is stated because
     * a targeting goal reads it and a boss that never acquires a target never moves, climbs past six
     * hundred ticks of {@code noActionTime} and is despawned by vanilla.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Ghast.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS);
    }

    /**
     * The drift, the head-turn and the two volleys. {@code super} is deliberately not called: it
     * would add vanilla's fireball goal, which is the thing being replaced, and a targeting goal
     * that only ever looks at players within four blocks of its own height.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new DriftGoal(this));
        this.goalSelector.addGoal(7, new FaceTargetGoal(this));
        this.goalSelector.addGoal(7, new VolleyGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> BossTargeting.isValid(this, target, SEARCH_RADIUS,
                        entity -> entity instanceof Ghast)));
    }

    /**
     * Peaceful is a separate branch of {@code Mob.checkDespawn} that runs before the persistence
     * flag is looked at, so a boss has to answer this as well as being persistent. A ghast's own
     * answer is true.
     */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
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

    /**
     * The two volleys, alternating, in one goal.
     * <p>
     * One goal rather than two because they share the thing that has to be true only once: a boss
     * with two attack goals would have both of them wanting the LOOK flag, and whichever vanilla
     * happened to start would hold it while the other never ran. Alternating inside a single goal
     * also makes the pattern legible, which is the point of having two attacks at all.
     */
    static class VolleyGoal extends Goal {
        private final CompressedGhastEntity ghast;
        private int charge;
        /** Which volley is due. It flips after every one, so the two always alternate. */
        private boolean homing;

        VolleyGoal(CompressedGhastEntity ghast) {
            this.ghast = ghast;
            // No flags, exactly as vanilla's shooting goal has none: the head-turn goal holds LOOK
            // and the two are meant to run together, one aiming while the other fires.
        }

        @Override
        public boolean canUse() {
            return this.ghast.getTarget() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.charge = 0;
        }

        @Override
        public void stop() {
            this.ghast.setCharging(false);
        }

        @Override
        public void tick() {
            LivingEntity target = this.ghast.getTarget();
            if (target == null) {
                return;
            }

            int windUp = this.homing ? HOMING_WIND_UP : SCATTER_WIND_UP;

            // Line of sight and range are checked every tick rather than only at the start, so a
            // target that gets behind a wall mid-wind-up costs the ghast the wind-up rather than
            // being shot through the wall. The charge unwinds rather than resetting, which is what
            // makes ducking in and out of cover cost the player something too.
            if (this.ghast.distanceToSqr(target) > SEARCH_RADIUS * SEARCH_RADIUS
                    || !this.ghast.hasLineOfSight(target)) {
                if (this.charge > 0) {
                    this.charge--;
                }

                this.ghast.setCharging(this.charge > MOUTH_OPENS);
                return;
            }

            this.charge++;

            if (this.charge == MOUTH_OPENS && !this.ghast.isSilent()) {
                // 1015 is vanilla's ghast warning, and 1016 below is the shot. Both are level events
                // rather than sounds so they carry exactly as a real ghast's do.
                this.ghast.level().levelEvent(null, 1015, this.ghast.blockPosition(), 0);
            }

            if (this.charge >= windUp) {
                fire(target);
                this.charge = -VOLLEY_COOLDOWN;
                this.homing = !this.homing;
            }

            this.ghast.setCharging(this.charge > MOUTH_OPENS);
        }

        /** One volley of whichever kind is due. */
        private void fire(LivingEntity target) {
            Level level = this.ghast.level();
            if (!this.ghast.isSilent()) {
                level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
            }

            Vec3 view = this.ghast.getViewVector(1.0F);
            Vec3 muzzle = this.ghast.position()
                    .add(0.0, this.ghast.getBbHeight() * 0.5, 0.0)
                    .add(view.scale(MUZZLE));
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                    .subtract(muzzle).normalize();

            RandomSource random = this.ghast.getRandom();
            int count = this.homing ? HOMING_COUNT : SCATTER_COUNT;
            double spread = this.homing ? HOMING_SPREAD : SCATTER_SPREAD;

            for (int i = 0; i < count; i++) {
                Vec3 heading = aim.add(
                        (random.nextDouble() - 0.5) * spread,
                        (random.nextDouble() - 0.5) * spread,
                        (random.nextDouble() - 0.5) * spread).normalize();

                GhastFireChargeEntity charge = new GhastFireChargeEntity(level, this.ghast, heading);
                charge.setPos(muzzle);

                if (this.homing) {
                    charge.setGhastedAmplifier(HOMING_GHASTED_AMPLIFIER);
                    charge.makeHoming(target);
                } else {
                    charge.setGhastedAmplifier(SCATTER_GHASTED_AMPLIFIER);
                    charge.accelerationPower = GhastFireChargeEntity.SCATTER_ACCELERATION;
                    charge.setDeltaMovement(
                            heading.scale(GhastFireChargeEntity.SCATTER_ACCELERATION));
                }

                level.addFreshEntity(charge);
            }
        }
    }

    /**
     * Vanilla's {@code RandomFloatAroundGoal}, written out because that class is package private.
     * It is unchanged: pick somewhere within sixteen blocks and drift at it, and pick again once the
     * move control has either arrived or lost the thread.
     */
    static class DriftGoal extends Goal {
        private final CompressedGhastEntity ghast;

        DriftGoal(CompressedGhastEntity ghast) {
            this.ghast = ghast;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            MoveControl control = this.ghast.getMoveControl();
            if (!control.hasWanted()) {
                return true;
            }

            double x = control.getWantedX() - this.ghast.getX();
            double y = control.getWantedY() - this.ghast.getY();
            double z = control.getWantedZ() - this.ghast.getZ();
            double distance = x * x + y * y + z * z;

            return distance < 1.0 || distance > 3600.0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            RandomSource random = this.ghast.getRandom();
            this.ghast.getMoveControl().setWantedPosition(
                    this.ghast.getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F,
                    this.ghast.getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F,
                    this.ghast.getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F, 1.0);
        }
    }

    /** Vanilla's {@code GhastLookGoal}, written out for the same reason: it is package private. */
    static class FaceTargetGoal extends Goal {
        private final CompressedGhastEntity ghast;

        FaceTargetGoal(CompressedGhastEntity ghast) {
            this.ghast = ghast;
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.ghast.getTarget();
            if (target == null) {
                Vec3 movement = this.ghast.getDeltaMovement();
                this.ghast.setYRot(-((float) Mth.atan2(movement.x, movement.z)) * (180.0F / (float) Math.PI));
            } else {
                this.ghast.setYRot(-((float) Mth.atan2(target.getX() - this.ghast.getX(),
                        target.getZ() - this.ghast.getZ())) * (180.0F / (float) Math.PI));
            }

            this.ghast.yBodyRot = this.ghast.getYRot();
        }
    }
}
