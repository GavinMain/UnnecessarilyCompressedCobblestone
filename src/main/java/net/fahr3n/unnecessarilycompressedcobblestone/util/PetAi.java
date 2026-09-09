package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.EnumSet;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Wolf AI for things that are not wolves.
 * <p>
 * Vanilla's {@code FollowOwnerGoal}, {@code OwnerHurtTargetGoal} and {@code OwnerHurtByTargetGoal}
 * are all written against {@code TamableAnimal}, so nothing in this mod can use one of them: a
 * summoned phantom, a summoned silverfish and a pet ghast are a {@code FlyingMob}, a
 * {@code Monster} and a {@code Ghast} respectively, and none of them is ever going to be tamable.
 * The three rules were written out by hand twice before this file existed, identically both times;
 * this is that code, once.
 * <p>
 * What a pet is, here, is three facts and nothing else - who owns it, which of its owner's fights it
 * has already joined, and how to write that second one down. That is {@link Pet}, and any mob that
 * can answer it gets the whole set: a target rule that never turns on its owner, a goal that joins
 * whatever fight its owner is in, and a follow that works for something with no interest in the
 * ground.
 */
public final class PetAi {
    /**
     * How long a fight is remembered. Vanilla's own combat tracker forgets one after five seconds of
     * nothing and keeps its flag private with no getter, so everything here reads the same idea off
     * {@code LivingEntity}'s two public timestamps instead.
     */
    public static final int COMBAT_MEMORY_TICKS = 100;

    private PetAi() {
    }

    /**
     * What a mob has to be able to say to be given the goals below.
     * <p>
     * The owner is a UUID rather than an entity so it survives the pet being unloaded while its
     * owner is away, and the fight timestamp lives on the mob rather than inside the goal because
     * goals are rebuilt every time an entity is loaded and "which fight have I already joined"
     * should not be forgotten by a chunk reload.
     */
    public interface Pet {
        /** Who this belongs to, or null for one that belongs to nobody and hunts as a monster. */
        @Nullable
        UUID petOwner();

        /** The timestamp of the last of its owner's fights this one has already joined. */
        int lastOwnerFight();

        void setLastOwnerFight(int timestamp);
    }

    /**
     * A pet that can be handed its owner after it has been created, which is what a pet spawn egg
     * needs and what a summoned one does not - a staff sets the owner on the mob it just built,
     * whereas an egg goes through vanilla's own spawn path and has to reach back afterwards.
     */
    public interface Adoptable extends Pet {
        void setPetOwner(@Nullable Player owner);
    }

    /** The player who owns {@code pet}, if they are in this world at all. */
    @Nullable
    public static Player ownerPlayer(Mob mob, Pet pet) {
        UUID owner = pet.petOwner();
        return owner == null ? null : mob.level().getPlayerByUUID(owner);
    }

    /**
     * Whether {@code entity} has hit something, or been hit, recently enough to still be fighting.
     * This is what lets a pet leave people alone in general and still defend against one.
     */
    public static boolean isInCombat(LivingEntity entity) {
        int now = entity.tickCount;

        return (entity.getLastHurtByMob() != null
                        && now - entity.getLastHurtByMobTimestamp() <= COMBAT_MEMORY_TICKS)
                || (entity.getLastHurtMob() != null
                        && now - entity.getLastHurtMobTimestamp() <= COMBAT_MEMORY_TICKS);
    }

    /**
     * Who a pet will go after, and the one predicate that covers both of its lives.
     * <p>
     * With no owner it is the ordinary hostile rule this project asks for: the player, and whoever
     * hit it, which a {@code HurtByTargetGoal} covers separately. With an owner it is the wolf's
     * rule instead - any hostile mob, and a person only while that person is already in a fight - so
     * a pet never turns on the person who called it up, and two pets of the same kind from different
     * owners ignore each other.
     * <p>
     * "Its own kind" is the mob's own class, which is exactly right: it is what stops a flock
     * tearing itself apart without any of the callers having to name themselves.
     */
    public static boolean isEnemyOf(Mob mob, Pet pet, LivingEntity candidate) {
        if (mob.getClass().isInstance(candidate) || candidate instanceof ArmorStand
                || !candidate.canBeSeenAsEnemy()) {
            return false;
        }

        UUID owner = pet.petOwner();
        if (owner == null) {
            return candidate instanceof Player;
        }

        if (candidate instanceof Player player) {
            return !player.getUUID().equals(owner) && isInCombat(player);
        }

        return candidate instanceof Enemy;
    }

    /**
     * A wolf's two owner goals in one: the pet goes after whatever its owner last hit, and after
     * whoever last hit its owner.
     * <p>
     * Each fight is acted on once - that is what the timestamp is for - so a target that was given
     * up on, or that fled, is not picked straight back up on the next tick.
     */
    public static final class OwnerCombatTargetGoal<T extends Mob & Pet> extends TargetGoal {
        private final T pet;
        @Nullable
        private LivingEntity found;
        private int timestamp;

        public OwnerCombatTargetGoal(T pet) {
            super(pet, false);
            this.pet = pet;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = ownerPlayer(this.pet, this.pet);
            if (owner == null) {
                return false;
            }

            LivingEntity struck = owner.getLastHurtMob();
            LivingEntity attacker = owner.getLastHurtByMob();
            int struckAt = owner.getLastHurtMobTimestamp();
            int attackedAt = owner.getLastHurtByMobTimestamp();

            // Whichever of the two happened later is the fight it joins.
            if (attacker != null && (struck == null || attackedAt >= struckAt)) {
                this.found = attacker;
                this.timestamp = attackedAt;
            } else if (struck != null) {
                this.found = struck;
                this.timestamp = struckAt;
            } else {
                return false;
            }

            return this.timestamp != this.pet.lastOwnerFight()
                    && this.found != this.pet
                    && !this.pet.getClass().isInstance(this.found)
                    && this.found != owner
                    && canAttack(this.found, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.found);
            this.pet.setLastOwnerFight(this.timestamp);
            super.start();
        }
    }

    /**
     * A wolf's follow, for something that flies.
     * <p>
     * Vanilla's teleport hunts for solid ground to land a pet on, which is exactly wrong for
     * everything here: the whole point of these is that they do not need any. One left far enough
     * behind is put back into the air over its owner's shoulder instead, where there is always room.
     */
    public static final class FollowOwnerGoal<T extends Mob & Pet> extends Goal {
        private final T pet;
        private final double start;
        private final double stop;
        private final double teleport;
        @Nullable
        private Player owner;
        private int pathTimer;

        /**
         * @param start    how far away the owner has to be before the pet sets off after them
         * @param stop     how close it has to get before it stops
         * @param teleport how far behind is too far to bother flying, and is put back by hand
         */
        public FollowOwnerGoal(T pet, double start, double stop, double teleport) {
            this.pet = pet;
            this.start = start;
            this.stop = stop;
            this.teleport = teleport;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player found = ownerPlayer(this.pet, this.pet);
            if (found == null || found.isSpectator()
                    || this.pet.distanceToSqr(found) < this.start * this.start) {
                return false;
            }

            this.owner = found;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null && !this.pet.getNavigation().isDone()
                    && this.pet.distanceToSqr(this.owner) > this.stop * this.stop;
        }

        @Override
        public void stop() {
            this.owner = null;
            this.pet.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }

            this.pet.getLookControl().setLookAt(this.owner, 10.0F, this.pet.getMaxHeadXRot());
            if (--this.pathTimer > 0) {
                return;
            }

            this.pathTimer = adjustedTickDelay(10);
            if (this.pet.distanceToSqr(this.owner) >= this.teleport * this.teleport) {
                this.pet.moveTo(this.owner.getX() + this.pet.getRandom().nextDouble() * 2.0 - 1.0,
                        this.owner.getY() + 2.0,
                        this.owner.getZ() + this.pet.getRandom().nextDouble() * 2.0 - 1.0,
                        this.pet.getYRot(), this.pet.getXRot());
                this.pet.getNavigation().stop();
                return;
            }

            this.pet.getNavigation().moveTo(this.owner, 1.0);
        }
    }
}
