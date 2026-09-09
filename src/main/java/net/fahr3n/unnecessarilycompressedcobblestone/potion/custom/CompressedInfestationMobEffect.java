package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSilverfishEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * Vanilla's Infested, rewritten around this mod's silverfish.
 * <p>
 * The shape is deliberately vanilla's, down to the numbers it hides: nothing happens on a tick, and
 * everything happens on {@link #onMobHurt} - being hit is what shakes them loose. That is what makes
 * it an ugly effect to carry rather than a slow one, since the thing that triggers it is the thing
 * a player is already doing something about.
 * <p>
 * Every one that hatches is marked a brood member, which is what stops the loop printing blocks: an
 * infested player who is hit spawns silverfish, those silverfish bite and re-apply the effect, and
 * without the mark each turn of that would also hand out a block of the deepest stone in the mod -
 * see {@link CompressedSilverfishEntity#setBroodling}.
 */
public class CompressedInfestationMobEffect extends MobEffect {
    /** How often a hit shakes any loose at all, and how many come out when it does. */
    private final float chanceToSpawn;
    private final int minSpawned;
    private final int maxSpawned;

    public CompressedInfestationMobEffect(int color, float chanceToSpawn, int minSpawned, int maxSpawned) {
        super(MobEffectCategory.HARMFUL, color, ParticleTypes.INFESTED);
        this.chanceToSpawn = chanceToSpawn;
        this.minSpawned = minSpawned;
        this.maxSpawned = maxSpawned;
    }

    @Override
    public void onMobHurt(LivingEntity entity, int amplifier, DamageSource damageSource, float amount) {
        Level level = entity.level();
        if (level.isClientSide() || entity.getRandom().nextFloat() > this.chanceToSpawn) {
            return;
        }

        // A deeper level shakes more of them loose, which is the only thing an amplifier means here.
        int count = Mth.nextInt(entity.getRandom(), this.minSpawned, this.maxSpawned) * (amplifier + 1);
        for (int i = 0; i < count; i++) {
            hatch(level, entity);
        }
    }

    /**
     * One silverfish, thrown out along the host's own look direction and fanned sideways, which is
     * vanilla's own arithmetic: it is what stops a stack of them appearing inside the host.
     */
    private void hatch(Level level, LivingEntity host) {
        CompressedSilverfishEntity silverfish = ModEntities.COMPRESSED_SILVERFISH.get().create(level);
        if (silverfish == null) {
            return;
        }

        RandomSource random = host.getRandom();
        float fan = Mth.randomBetween(random, (float) (-Math.PI / 2), (float) (Math.PI / 2));
        Vector3f thrown = host.getLookAngle().toVector3f().mul(0.3F).mul(1.0F, 1.5F, 1.0F).rotateY(fan);

        silverfish.moveTo(host.getX(), host.getY() + host.getBbHeight() / 2.0, host.getZ(),
                random.nextFloat() * 360.0F, 0.0F);
        silverfish.setDeltaMovement(new Vec3(thrown));
        silverfish.setBroodling();
        level.addFreshEntity(silverfish);
        silverfish.playSound(SoundEvents.SILVERFISH_HURT);
    }
}
