package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPhantomEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSilverfishEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
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

    /**
     * What one silverfish out of an engraved staff starts at, before the staff's energy and Surge.
     * A vanilla silverfish's eight health, and the one point of unblockable damage that is the whole
     * identity of the mob - the staff makes that harder to survive, never easier to block.
     */
    private static final float SILVERFISH_HEALTH = 8.0F;
    private static final float SILVERFISH_DAMAGE = 1.0F;

    /** How many arrive per cast when the staff is engraved: a swarm, not a single creature. */
    private static final int SILVERFISH_PER_CAST = 5;

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
        boolean swarm = Engravings.has(stack, Engraving.SILVERFISH);

        // The engraving swaps what is called up and nothing else: the same energy, the same Surge
        // and the same Multicast, spent on a different creature. Which base numbers those scale is
        // the creature's own, since a silverfish's bite is one point of unblockable damage and a
        // phantom's is ten of ordinary damage.
        float baseHealth = swarm ? SILVERFISH_HEALTH : CompressedPhantomEntity.MAX_HEALTH;
        float baseDamage = swarm ? SILVERFISH_DAMAGE : CompressedPhantomEntity.ATTACK_DAMAGE;
        float health = (baseHealth + digits * HEALTH_PER_DIGIT) * surge;
        float damage = (baseDamage + digits * DAMAGE_PER_DIGIT) * surge;

        for (int index = 0; index < multicast(stack, level); index++) {
            if (swarm) {
                summonSilverfish(level, player, health, damage);
            } else {
                summon(level, player, health, damage);
            }
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

    /**
     * A handful of Compressed Silverfish instead, and already the caster's.
     * <p>
     * There is no {@code finalizeSpawn} dance here - that is a phantom problem, its anchor point
     * being package private and starting at the world origin - so this is the plain three lines:
     * place it, hand it its owner, hand it its numbers. Every one is marked a brood member, because
     * an unbreakable staff that summoned things dropping the deepest stone in the mod would be a
     * printer; see {@code CompressedSilverfishEntity#setBroodling}.
     */
    private static void summonSilverfish(ServerLevel level, Player player, float health, float damage) {
        for (int i = 0; i < SILVERFISH_PER_CAST; i++) {
            CompressedSilverfishEntity silverfish = ModEntities.COMPRESSED_SILVERFISH.get().create(level);
            if (silverfish == null) {
                return;
            }

            double x = player.getX() + (level.random.nextDouble() - 0.5) * 4.0;
            double z = player.getZ() + (level.random.nextDouble() - 0.5) * 4.0;
            double y = player.getY() + SPAWN_HEIGHT * level.random.nextDouble();

            silverfish.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
            silverfish.setOwner(player);
            silverfish.setStats(health, damage);
            silverfish.setBroodling();
            silverfish.setPersistenceRequired();

            level.addFreshEntity(silverfish);
            level.sendParticles(ParticleTypes.INFESTED, x, y, z, 12, 0.4, 0.4, 0.4, 0.05);
        }
    }
}
