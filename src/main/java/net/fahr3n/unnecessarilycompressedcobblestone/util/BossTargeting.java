package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

/**
 * How this mod's bosses pick what to shoot at. They are all hostile to everything alive and in reach
 * except their own kind and the things that only stand there, and they all prefer the same order:
 * players first, then anything that fights at range, then whatever is nearest - and nearest within
 * each of those groups.
 * <p>
 * Ranged attackers are singled out because they are the only things that can trade with something
 * that has to stand still to attack; everything else has to come to it.
 */
public final class BossTargeting {
    private BossTargeting() {
    }

    /** The best thing in reach to attack, or null if there is nothing. */
    @Nullable
    public static LivingEntity choose(ServerLevel level, Mob boss, double radius, Predicate<LivingEntity> ownKind) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                boss.getBoundingBox().inflate(radius), entity -> isValid(boss, entity, radius, ownKind));

        return candidates.stream()
                .min(Comparator.<LivingEntity>comparingInt(BossTargeting::priority)
                        .thenComparingDouble(boss::distanceToSqr))
                .orElse(null);
    }

    /**
     * Armour stands are named out because they are the common case; anything else that cannot be an
     * enemy - a spectator, a creative player, an invulnerable marker - is caught by
     * {@code canBeSeenAsEnemy}, which is the same check vanilla's own targeting uses.
     */
    public static boolean isValid(Mob boss, LivingEntity entity, double radius, Predicate<LivingEntity> ownKind) {
        return entity != boss
                && entity.isAlive()
                && !ownKind.test(entity)
                && !(entity instanceof ArmorStand)
                && entity.canBeSeenAsEnemy()
                && boss.distanceToSqr(entity) <= radius * radius;
    }

    /** Lower sorts first: players, then ranged attackers, then everything else. */
    private static int priority(LivingEntity entity) {
        if (entity instanceof Player) {
            return 0;
        }

        return entity instanceof RangedAttackMob ? 1 : 2;
    }
}
