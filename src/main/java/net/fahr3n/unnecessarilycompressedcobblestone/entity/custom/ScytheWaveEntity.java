package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * The crescent a Scythe Wave engraving turns a swing into: the melee attack itself, sent on ahead.
 * <p>
 * What it does when it arrives is not written here in any detail, and deliberately so. It calls
 * {@link net.minecraft.world.entity.player.Player#attack} on whatever it caught, which is the whole
 * of vanilla's melee - the wielder's attack damage attribute, their enchantments, the critical hit
 * and sweep events, the knockback, the exhaustion, and the weapon's own {@code hurtEnemy}, which is
 * what puts the scythe's bleed in the wound. "The same thing as the melee attack" is therefore not
 * an imitation of it that has to be kept in step; it is the same call.
 * <p>
 * Two things about the swing have to travel with the crescent rather than be read again on landing.
 * The first is the attack strength: a wave is fired at whatever charge the swing had and lands at
 * that charge however long it was in the air, which is why {@link #attackStrengthTicker} is carried
 * and put back on the wielder for the length of the call. The second is the reset - firing a wave
 * spends the swing, exactly as landing a hit does, so a scythe still swings once a second rather
 * than twenty times.
 * <p>
 * What is <em>not</em> carried is the weapon. {@code Player#attack} reads the wielder's main hand at
 * the moment the blow lands, so a crescent that arrives after its owner has put the scythe away hits
 * for whatever is in their hand instead - and, since the bleed comes from the scythe's own
 * {@code hurtEnemy}, leaves nothing behind. That is a real gap and it is left open deliberately:
 * closing it would mean either restating vanilla's melee against a saved stack, which is the whole
 * thing this class exists not to do, or lying to {@code attack} about what is being held. A flight
 * is about a second, so it costs a player who swaps weapons mid-cut.
 * <p>
 * Its range is the render distance in the honest sense: nothing here counts blocks. The crescent
 * simply keeps flying, and it stops being ticked when it leaves the chunks the server keeps loaded
 * around its players, which is the server view distance. It is an {@link AbstractArrow} for the
 * swept hit test in that class's tick and for nothing else - at three blocks a tick a box test
 * would pass straight through a target between two ticks - and it does none of an arrow's own
 * damage, never sticks and is never picked up.
 */
public class ScytheWaveEntity extends AbstractArrow {
    /** Blocks a tick as it leaves the blade. Vanilla's drag takes this down over the flight. */
    public static final float SPEED = 3.0F;

    /** How wide the crescent is drawn, in blocks. Nothing about the hit reads this. */
    public static final float SIZE = 1.4F;

    /**
     * How long a crescent lives at most, in ticks. It is a backstop rather than the range: a wave
     * normally stops against terrain or against a target, and one fired over open ground stops
     * being ticked when it leaves the loaded chunks. Without this a wave loosed at the sky in a
     * chunk somebody is standing in would hang there forever.
     */
    public static final int MAX_LIFETIME = 200;

    private static final String TAG_ATTACK_STRENGTH = "attack_strength";

    /**
     * Guards against the crescent's own hit being turned into another crescent.
     * <p>
     * {@code Player#attack} posts {@code AttackEntityEvent}, which is where the engraving catches a
     * swing in the first place - so resolving a wave with the engraved scythe still in hand would
     * fire a wave, which would fire a wave. It is a plain static because every wave resolves on the
     * server thread, inside one call, and is cleared in a {@code finally}.
     */
    private static boolean resolving;

    /**
     * The wielder's {@code attackStrengthTicker} at the moment the wave was loosed, so the blow
     * lands at the charge the swing had rather than at whatever has recharged since.
     */
    private int attackStrengthTicker;

    public ScytheWaveEntity(EntityType<? extends ScytheWaveEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ScytheWaveEntity(Level level, LivingEntity shooter, ItemStack scythe) {
        super(ModEntities.SCYTHE_WAVE.get(), shooter, level,
                new ItemStack(ModItems.COMPRESSED_SCYTHE.get()), scythe);
    }

    /**
     * Never picked up and never dropped. Called from {@link AbstractArrow}'s own constructor, before
     * any field of this class is assigned, so it can only ever answer with a constant - see
     * {@code CompressedArrowEntity} for the trap in full.
     */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.COMPRESSED_SCYTHE.get());
    }

    /** Whether a wave is being resolved right now, and so whether a swing should be left alone. */
    public static boolean isResolving() {
        return resolving;
    }

    public void setAttackStrengthTicker(int attackStrengthTicker) {
        this.attackStrengthTicker = attackStrengthTicker;
    }

    /** A crescent travels flat. It is a swing, not a thrown thing. */
    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    /** Water slows an arrow to a crawl; it does not slow a cut through the air above it. */
    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide() && this.tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    /**
     * The blow. The wielder's own {@code attack} is called with the charge the swing had, which is
     * every part of a melee hit at once - and is why a wave carrying the scythe bleeds, a wave off
     * an inscribed scythe hits for the inscription, and a wave crits when the swing would have.
     * <p>
     * One target. A swing hits one thing, and this is a swing.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();

        if (level() instanceof ServerLevel serverLevel && getOwner() instanceof ServerPlayer player
                && target != player) {
            int saved = player.attackStrengthTicker;
            player.attackStrengthTicker = this.attackStrengthTicker;
            resolving = true;
            try {
                player.attack(target);
            } finally {
                resolving = false;
                player.attackStrengthTicker = saved;
            }

            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(),
                    target.getY(0.5), target.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
        }

        discard();
    }

    /** Ground: the crescent breaks against it. Not calling super is what stops it sticking there. */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(), getZ(),
                    2, 0.1, 0.1, 0.1, 0.0);
        }

        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_ATTACK_STRENGTH, this.attackStrengthTicker);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.attackStrengthTicker = compound.getInt(TAG_ATTACK_STRENGTH);
    }
}
