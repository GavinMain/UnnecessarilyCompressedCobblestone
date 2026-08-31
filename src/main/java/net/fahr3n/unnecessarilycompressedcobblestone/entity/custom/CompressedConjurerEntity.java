package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The Compressed Conjurer. It stands exactly where it was put and never takes a step; what it does
 * instead is pick something within a hundred and twenty-eight blocks, spend three seconds calling the
 * sky down on it, and repeat. The strike does not care about walls, water or a roof of stone: the
 * bolt is placed on the target itself, so being underground is no defence at all.
 * <p>
 * Everything about how it finds and marks a target is {@link AbstractConjurerEntity}'s. What is here
 * is the boss of it: the health bar, the standing still, the storm it arrives in, and a bolt that
 * lands for thousands rather than for five.
 */
public class CompressedConjurerEntity extends AbstractConjurerEntity {
    /**
     * Twenty damage through a full Compression Lightning set wearing Protection V, rounded up to the
     * next hundred.
     * <p>
     * The chain, in the order the game applies it: the set's own bonus leaves 5% of the raw number
     * ({@code ModEvents.onLightningDamage}); leather-grade armour, which the set is, takes another
     * 5.6% off, because at this size {@code armour - damage / 2} is far below the {@code armour / 5}
     * floor and the reduction pins to {@code 7 * 0.2 / 25}; and Protection V on four pieces is 20
     * points, exactly the cap {@code CombatRules} clamps to, which is a further 80%. That is
     * {@code 0.05 * 0.944 * 0.2} = 0.00944 of what is set here, so twenty damage needs 2118.6 and the
     * next hundred up is 2200 - which lands 20.77 on that armour.
     * <p>
     * Anyone not wearing the set is hit by all 2200 of it, less their own armour and Protection. This
     * is not survivable by any vanilla means, which is the point of a boss that ignores walls.
     */
    public static final float LIGHTNING_DAMAGE = 2200.0F;

    /**
     * It cannot dodge, cannot chase and cannot be pulled out of position, so the whole fight is a
     * damage race against its three second cycle. That is worth more health than the Compressed
     * Creeper's 250, since none of that health is ever spent on repositioning.
     */
    public static final float MAX_HEALTH = 400.0F;

    /** How far it will reach for a target, in blocks. */
    public static final double ATTACK_RADIUS = 128.0;

    /** Three seconds of channelling, then the strike. */
    private static final int CHANNEL_TICKS = 60;

    /** Ten minutes of storm, called up along with it. */
    private static final int THUNDER_TICKS = 12000;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_conjurer"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    public CompressedConjurerEntity(EntityType<? extends Drowned> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 150;
        setPersistenceRequired();
    }

    /**
     * Armour 20 with no toughness, on the same reasoning as the Compressed Creeper's 30: a big hit
     * eats through far more of it than a small one, so burst damage is worth several times chip
     * damage. Movement speed is zero, and there are no goals that would use it anyway; knockback
     * resistance is total, because a boss that could be shoved off its spot would not be a stationary
     * one.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Drowned.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0)
                .add(Attributes.FOLLOW_RANGE, ATTACK_RADIUS);
    }

    @Override
    public int channelTicks() {
        return CHANNEL_TICKS;
    }

    @Override
    public double attackRadius() {
        return ATTACK_RADIUS;
    }

    @Override
    protected boolean isRooted() {
        return true;
    }

    /**
     * It arrives in its own weather. The storm is set however it was called up - out of the summoning
     * egg, out of a spawn egg, out of a command - because it belongs to the boss rather than to any
     * one way of getting one.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        level.getLevel().setWeatherParameters(0, THUNDER_TICKS, true, true);
        return data;
    }

    /**
     * A real bolt, at a damage vanilla's own lightning never reaches. It is placed on the target
     * rather than on the sky above it, which is what lets it hit something standing under a mountain:
     * nothing in {@link LightningBolt} asks for a view of the sky, and its damage reaches three blocks
     * around wherever it is put.
     */
    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }

        bolt.moveTo(target.getX(), target.getY(), target.getZ());
        bolt.setDamage(LIGHTNING_DAMAGE);
        level.addFreshEntity(bolt);
    }

    /** Nothing shoves it out of position - not a mob walking into it, not a crowd. */
    @Override
    public boolean isPushable() {
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
}
