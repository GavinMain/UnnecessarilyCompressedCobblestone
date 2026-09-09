package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;

/**
 * A ghast that is not a monster.
 * <p>
 * It exists for one reason, and it is a reason no amount of overriding could have answered:
 * {@link Ghast} implements {@link Enemy}, a marker interface, and an iron golem's targeting goal
 * looks for {@code Mob}s that are {@code instanceof Enemy}. A subclass cannot un-implement an
 * interface, so a pet or a mount built on {@code Ghast} is something every golem in every village
 * attacks on sight and every {@code Enemy}-shaped rule in every other mod treats as a threat. The
 * only fix is to not be one, so this extends {@link FlyingMob} - which is what {@code Ghast} extends
 * and is where all of its flight comes from - and stops there.
 * <p>
 * What is lost by not being a {@code Ghast} is only what {@code Ghast} adds on top: the fireball
 * goal, the drift, the player-only targeting, the reflected-fireball rule and the explosion power.
 * All five are things the pet and the mount throw away anyway. What is worth keeping is copied here
 * and is three small things - the moan, the charging flag the renderer draws as an open mouth, and
 * a ghast's sound volume - so the two subclasses read as ghasts in every way a player can see.
 * <p>
 * The model and the texture are not lost either: {@code GhastModel} is generic over {@code Entity}
 * rather than over {@code Ghast}, so {@code FriendlyGhastRenderer} draws these with vanilla's own
 * model and vanilla's own two textures.
 */
public abstract class AbstractFriendlyGhastEntity extends FlyingMob {
    /**
     * Vanilla's own charging flag, re-declared because {@code Ghast}'s is private to that class.
     * It is what the renderer picks the open-mouthed texture off, so a ghast that is winding
     * something up still looks as though it is.
     */
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING =
            SynchedEntityData.defineId(AbstractFriendlyGhastEntity.class, EntityDataSerializers.BOOLEAN);

    protected AbstractFriendlyGhastEntity(EntityType<? extends AbstractFriendlyGhastEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * A ghast's own attributes. {@code Ghast.createAttributes()} cannot be called for them - it is
     * declared on a class this no longer extends - so its two lines are here instead, unchanged.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FOLLOW_RANGE, 100.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_CHARGING, false);
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_IS_CHARGING);
    }

    public void setCharging(boolean charging) {
        this.entityData.set(DATA_IS_CHARGING, charging);
    }

    /* THE MOAN, WHICH IS HALF OF WHAT MAKES A GHAST ONE */

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    /** A ghast is heard from a long way off, which is its own figure and not the default one. */
    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    // getSoundSource is deliberately not overridden. Ghast puts its own sounds on the hostile
    // channel; these are not hostile, so they belong on the neutral one, which is Mob's default.
}
