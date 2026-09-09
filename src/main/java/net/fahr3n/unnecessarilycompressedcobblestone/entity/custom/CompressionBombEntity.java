package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.Optional;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DamageOverTime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.HitResult;

/**
 * A Compression Bomb in flight, and what it does when it stops.
 * <p>
 * The blast is not the point. What the bomb does is take every damage over time effect off
 * everything it catches and hand the whole of what those effects were still owed over as one hit of
 * magic damage - so a target three minutes into a Wither VIII pays for those three minutes now
 * rather than over them. See {@link DamageOverTime}, which owns both the sum and the removal.
 * <p>
 * Everything caught is hurt exactly once, and that is a rule rather than a tidiness: a
 * {@code LivingEntity} is invulnerable for twenty ticks after a hit unless the next one is larger,
 * so a bomb that dealt its own damage and then the collapsed total as two calls would have the
 * second swallowed or the second discounted by the first. The bomb's own {@value #DAMAGE} and the
 * collapse go into the same {@code hurt}.
 * <p>
 * That one call is {@code minecraft:indirect_magic} - vanilla's type for magic thrown by somebody -
 * which puts the kill on whoever threw it and, being in {@code #bypasses_armor}, lands the bill
 * whole. Four of the five effects it usually replaces bypassed armour too, so answering the
 * collapse with plate would be a discount on damage that was never going to be reduced.
 * <p>
 * A real explosion is fired for the shove, the particles and the noise, and damages nothing at all:
 * the calculator is {@code SimpleExplosionDamageCalculator(false, false, ...)}, which is a pure
 * push, since knockback is applied whether or not an explosion hurts anything.
 * <p>
 * It needs no renderer and no texture beyond the item's own icon: {@code ThrowableItemProjectile}
 * already implements {@code ItemSupplier}, so {@code ThrownItemRenderer} draws the bomb in flight
 * as the thing that was thrown.
 */
public class CompressionBombEntity extends ThrowableItemProjectile {
    /**
     * What the bomb is worth on its own, before anything it collapses. It is deliberately modest:
     * a grenade whose blast already killed would make the collapse decoration, and the collapse is
     * the item.
     */
    public static final float DAMAGE = 8.0F;

    /** How far the collapse reaches, in blocks. */
    public static final double RADIUS = 4.0;

    /** How hard the blast shoves. A stick of vanilla TNT is worth about one. */
    private static final float KNOCKBACK = 1.5F;

    /** The blast's reach, which is only ever the shove: nothing here damages or breaks anything. */
    private static final float BLAST_RADIUS = 3.0F;

    public CompressionBombEntity(EntityType<? extends CompressionBombEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** Thrown by hand: the projectile starts at the thrower's eye, the way an egg does. */
    public CompressionBombEntity(Level level, LivingEntity owner) {
        super(ModEntities.COMPRESSION_BOMB.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.COMPRESSION_BOMB.get();
    }

    /**
     * Every impact, whatever it hit. {@code ThrowableProjectile#onHit} is called for a block, an
     * entity and a miss alike, which is what a grenade wants - it goes off where it stops, not only
     * where it connects.
     */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (level() instanceof ServerLevel level) {
            detonate(level);

            // Vanilla's own "this projectile broke" event, the way the thrown egg finishes: the
            // client draws the item shattering off it.
            level.broadcastEntityEvent(this, (byte) 3);
        }

        discard();
    }

    private void detonate(ServerLevel level) {
        // A pure shove: no damage and no blocks. The bomb's own hit is dealt below, summed with the
        // collapse, so an explosion that also hurt would put everything inside its invulnerability
        // window before the bill arrived.
        level.explode(this, null,
                new SimpleExplosionDamageCalculator(false, false, Optional.of(KNOCKBACK), Optional.empty()),
                getX(), getY(), getZ(), BLAST_RADIUS, false, Level.ExplosionInteraction.NONE);

        Entity thrower = getOwner();
        for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(RADIUS),
                entity -> entity.isAlive() && !(entity instanceof ArmorStand))) {
            float collapsed = DamageOverTime.collapse(caught);
            caught.hurt(damageSources().indirectMagic(this, thrower), DAMAGE + collapsed);

            if (collapsed > 0.0F) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, caught.getX(),
                        caught.getY() + caught.getBbHeight() / 2.0, caught.getZ(), 24, 0.4, 0.5, 0.4, 0.3);
            }
        }

        level.sendParticles(ParticleTypes.INSTANT_EFFECT, getX(), getY(), getZ(), 120, 1.2, 1.0, 1.2, 0.4);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.2F, 1.6F);
    }
}
