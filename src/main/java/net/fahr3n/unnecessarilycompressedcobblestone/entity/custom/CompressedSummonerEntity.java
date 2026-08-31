package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;
import java.util.function.BiConsumer;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.Level;

/**
 * The Summoner. A witch in everything it does moment to moment - it walks, it throws potions, it
 * drinks them when it is hurt - with one thing of its own on a twenty second clock: it blinks away
 * from whoever it is fighting, stands still for three seconds, and calls up an army.
 * <p>
 * The teleport is <em>away</em>, not towards. It puts a good twenty blocks and usually a wall
 * between itself and its target, which is what makes the channel survivable for it and the fight
 * about the things it summons rather than about the summoner. What arrives is one of three flocks,
 * drawn at random - four Compressed Golems, two Compressed Creepers or ten Compressed Phantoms -
 * and none of it is bound to the summoner, so killing it does not clean up what it already made -
 * though none of it will fight the summoner either, which each of the three answers for itself
 * through {@code isAlliedTo}.
 * <p>
 * It extends {@link Witch} for the potion AI, which drags in the raid brain with it; that is shut
 * off in {@link #canJoinRaid}. Its own targeting is the mod's boss rule - hostile to everything but
 * itself and the things that only stand there - laid over the witch's own preference for players.
 */
public class CompressedSummonerEntity extends Witch {
    public static final float MAX_HEALTH = 3000.0F;

    /** A full set of netherite: twenty armour and twelve toughness. */
    public static final double ARMOR = 20.0;
    public static final double ARMOR_TOUGHNESS = 12.0;

    /**
     * Twenty seconds from one blink to the next, the channel counted inside it: the cycle is the
     * wait, then the blink, then {@link #CHANNEL_TICKS} of standing still, then the flock.
     */
    private static final int SUMMON_INTERVAL = 400;

    /** Three seconds of standing still before the flock arrives. */
    private static final int CHANNEL_TICKS = 60;

    /** What is left of the cycle for the summoner to spend fighting, once the channel has its share. */
    private static final int SUMMON_COOLDOWN = SUMMON_INTERVAL - CHANNEL_TICKS;

    /** How far it tries to put between itself and its target when it blinks. */
    private static final double TELEPORT_MIN_DISTANCE = 20.0;
    private static final double TELEPORT_MAX_DISTANCE = 34.0;

    /** How many spots it will try before giving up on this cycle's blink and channelling anyway. */
    private static final int TELEPORT_ATTEMPTS = 24;

    /** How far it looks for something to fight. */
    private static final double SEARCH_RADIUS = 48.0;

    /** Ticks between searches when it has nothing to fight; a search walks every entity in reach. */
    private static final int SEARCH_INTERVAL = 20;

    private static final String TAG_SUMMON_COOLDOWN = "summon_cooldown";
    private static final String TAG_CHANNEL = "channel";

    /**
     * What it can call up. Each is a whole flock, drawn as one - it never mixes them, so a player
     * gets one problem at a time and can learn to answer each.
     */
    private static final List<Summon> SUMMONS = List.of(
            new Summon(4, (level, summoner) -> spawn(level, summoner, ModEntities.COMPRESSED_GOLEM.get())),
            new Summon(2, (level, summoner) -> spawn(level, summoner, ModEntities.COMPRESSED_CREEPER.get())),
            new Summon(10, (level, summoner) -> spawn(level, summoner, ModEntities.COMPRESSED_PHANTOM.get())));

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_summoner"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private int summonCooldown = SUMMON_COOLDOWN;
    private int channel;
    private int searchCooldown;

    public CompressedSummonerEntity(EntityType<? extends Witch> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 250;
        setPersistenceRequired();
    }

    /**
     * Witch numbers with the health at 3000 and a full set of netherite's protection. The armour is
     * well clear of the 100 the armour note warns about, so the ordinary vanilla curve applies and
     * nothing here can produce a reduction over 100%.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Witch.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS);
    }

    /** A boss has no business wandering off to join somebody's village raid. */
    @Override
    public boolean canJoinRaid() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !BossTargeting.isValid(this, target, SEARCH_RADIUS, this::isOwnKind)) {
            setTarget(null);
            if (this.searchCooldown-- <= 0) {
                this.searchCooldown = SEARCH_INTERVAL;
                setTarget(BossTargeting.choose(serverLevel, this, SEARCH_RADIUS, this::isOwnKind));
            }

            return;
        }

        // Mid-channel: rooted, sparking, and counting down to the flock. The navigation is stopped
        // every tick because the witch's own stroll goal would otherwise walk it off the spot.
        if (this.channel > 0) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0, getZ(),
                    6, 0.4, 0.8, 0.4, 0.02);

            if (--this.channel == 0) {
                summon(serverLevel);
            }

            return;
        }

        if (this.summonCooldown-- > 0) {
            return;
        }

        this.summonCooldown = SUMMON_COOLDOWN;
        this.channel = CHANNEL_TICKS;
        teleportAwayFrom(target);
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.6F);
    }

    /**
     * Blinks to somewhere well away from {@code target}.
     * <p>
     * {@code randomTeleport} is what decides whether a spot is safe - it refuses anything inside a
     * block, over a drop or in fluid - so the search here is only about distance: a ring at least
     * {@link #TELEPORT_MIN_DISTANCE} out, tried until one of them takes. If none does, the summoner
     * simply stays where it is and channels anyway; a cycle that silently did nothing would read as
     * the boss being broken.
     */
    private void teleportAwayFrom(LivingEntity target) {
        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = TELEPORT_MIN_DISTANCE
                    + this.random.nextDouble() * (TELEPORT_MAX_DISTANCE - TELEPORT_MIN_DISTANCE);
            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            double y = target.getY() + this.random.nextInt(9) - 4;

            if (randomTeleport(x, y, z, true)) {
                level().playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT,
                        getSoundSource(), 1.0F, 0.7F);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.7F);
                return;
            }
        }
    }

    /** One flock, drawn at random, standing where the summoner is standing. */
    private void summon(ServerLevel level) {
        Summon summon = SUMMONS.get(this.random.nextInt(SUMMONS.size()));
        for (int index = 0; index < summon.count(); index++) {
            summon.spawn().accept(level, this);
        }

        level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 1.0F, 0.6F);
    }

    /**
     * Puts one of {@code type} down beside the summoner.
     * <p>
     * {@code finalizeSpawn} is not optional for any of the three: a phantom reads its anchor point
     * there and starts at the world origin without it, and the golem and the creeper both do their
     * own setup in it. The spawn is offset a little so ten of them do not all arrive inside one
     * another.
     */
    private static void spawn(ServerLevel level, CompressedSummonerEntity summoner, EntityType<? extends Mob> type) {
        Mob mob = type.create(level);
        if (mob == null) {
            return;
        }

        double x = summoner.getX() + (summoner.getRandom().nextDouble() - 0.5) * 6.0;
        double z = summoner.getZ() + (summoner.getRandom().nextDouble() - 0.5) * 6.0;
        double y = summoner.getY() + (mob instanceof CompressedPhantomEntity ? 6.0 : 0.0);

        mob.moveTo(x, y, z, summoner.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                MobSpawnType.MOB_SUMMONED, null);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 24, 0.4, 0.8, 0.4, 0.4);
    }

    /** Its own kind, which is the one thing the boss rule says it will not attack. */
    private boolean isOwnKind(Entity entity) {
        return entity instanceof CompressedSummonerEntity;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_SUMMON_COOLDOWN, this.summonCooldown);
        compound.putInt(TAG_CHANNEL, this.channel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.summonCooldown = compound.getInt(TAG_SUMMON_COOLDOWN);
        this.channel = compound.getInt(TAG_CHANNEL);
    }

    /** One kind of flock: how many arrive, and how one of them is put down. */
    private record Summon(int count, BiConsumer<ServerLevel, CompressedSummonerEntity> spawn) {
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
