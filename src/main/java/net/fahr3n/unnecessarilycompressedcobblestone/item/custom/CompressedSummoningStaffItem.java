package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPhantomEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A staff that calls up a Compressed Phantom of your own.
 * <p>
 * Ten seconds is the longest cast of any staff here, and what it buys is the only thing in the mod
 * that fights for you: the phantom it makes carries an owner, which turns the mob's targeting round -
 * it hunts hostile mobs, never its owner, and other people only while they are already in a fight.
 * It is not on a timer and is not bound to the staff, so it stays until something kills it.
 * <p>
 * The three staff enchantments mean here what they mean everywhere: Silent Cast shortens the ten
 * seconds, Surge is this staff's power and so multiplies what the phantom is worth, and Multicast is
 * how many arrive at once.
 */
public class CompressedSummoningStaffItem extends CompressedStaffItem {
    /** Ten seconds, before Silent Cast. The longest wind-up of any staff in the mod. */
    public static final int CAST_TICKS = 200;

    /** A point of health per digit of Compression Energy inscribed into the staff. */
    public static final float HEALTH_PER_DIGIT = 1.0F;

    /** And a quarter point of damage per digit. */
    public static final float DAMAGE_PER_DIGIT = 0.25F;

    /** How far above the caster the phantom appears, so it starts with room to fly. */
    private static final double SPAWN_HEIGHT = 4.0;

    public CompressedSummoningStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public int baseCastTicks() {
        return CAST_TICKS;
    }

    @Override
    protected void release(ServerLevel level, Player player, ItemStack stack) {
        float digits = CompressionEnergy.bonus(stack);
        float surge = surge(stack, level);
        float health = (CompressedPhantomEntity.MAX_HEALTH + digits * HEALTH_PER_DIGIT) * surge;
        float damage = (CompressedPhantomEntity.ATTACK_DAMAGE + digits * DAMAGE_PER_DIGIT) * surge;

        for (int index = 0; index < multicast(stack, level); index++) {
            summon(level, player, health, damage);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /**
     * One phantom, over the caster's head and already theirs.
     * <p>
     * {@code finalizeSpawn} has to run before the stats are set, in that order and no other: vanilla
     * reads the phantom's anchor point there - without it the phantom flies off towards the world
     * origin - and it also rewrites the attack damage from the phantom size on its way through, so
     * anything set beforehand would be thrown away.
     */
    private static void summon(ServerLevel level, Player player, float health, float damage) {
        CompressedPhantomEntity phantom = ModEntities.COMPRESSED_PHANTOM.get().create(level);
        if (phantom == null) {
            return;
        }

        double x = player.getX() + (level.random.nextDouble() - 0.5) * 4.0;
        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 4.0;
        double y = player.getY() + SPAWN_HEIGHT;

        phantom.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                MobSpawnType.MOB_SUMMONED, null);
        phantom.setOwner(player);
        phantom.setStats(health, damage);
        phantom.setPersistenceRequired();

        level.addFreshEntity(phantom);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 30, 0.5, 0.5, 0.5, 0.05);
    }
}
