package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * What a Compressed Snow Golem throws. A snowball in every respect that is drawn or thrown, and a
 * snowball that hits for {@link #DAMAGE} instead of for nothing.
 * <p>
 * A vanilla snowball deals literally zero - {@code Snowball#onHitEntity} hurts for 3 against a blaze
 * and 0 against everything else, so it is a shove and a particle and nothing more. There is nothing
 * to scale, which is why this is an entity of its own rather than an attribute on the golem: see the
 * fishing note in CLAUDE.md for the same trap in another form.
 * <p>
 * It needs no renderer and no texture. {@code Snowball} already implements {@code ItemSupplier} and
 * returns {@code Items.SNOWBALL}, so registering {@code ThrownItemRenderer::new} against it draws a
 * snowball in flight and nothing here has to know about it.
 */
public class CompressedSnowballEntity extends Snowball {
    /** What one hit is worth, before the target's armour and Protection have their say. */
    public static final float DAMAGE = 100.0F;

    public CompressedSnowballEntity(EntityType<? extends Snowball> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * The owner-carrying constructor {@code Snowball} has and does not expose.
     * <p>
     * Vanilla's is {@code Snowball(Level, LivingEntity)}, which hardcodes {@code EntityType.SNOWBALL}
     * and so cannot be called by a subclass with a type of its own; the constructor underneath it,
     * {@code ThrowableProjectile(EntityType, LivingEntity, Level)}, is not reachable through
     * {@code Snowball} either. Its whole body is the two lines repeated here.
     */
    public CompressedSnowballEntity(Level level, LivingEntity owner) {
        super(ModEntities.COMPRESSED_SNOWBALL.get(), level);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setOwner(owner);
    }

    /**
     * Hurts as a thrown projectile, which is what a snowball already is: the damage goes through
     * {@code thrown}, so armour, Protection, Resistance and Projectile Protection all answer it the
     * way they answer an arrow, and the kill is credited to the golem rather than to the snowball.
     * <p>
     * {@code super} is deliberately not called. All {@code Snowball.onHitEntity} does is hurt for
     * zero (three against a blaze), and landing that first would put the target inside its twenty
     * tick invulnerability window before the real hit arrived - which is the trap the "sum damage
     * into one hurt call" note in CLAUDE.md describes, arrived at from the other direction.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        hit.hurt(damageSources().thrown(this, getOwner()), DAMAGE);
    }
}
