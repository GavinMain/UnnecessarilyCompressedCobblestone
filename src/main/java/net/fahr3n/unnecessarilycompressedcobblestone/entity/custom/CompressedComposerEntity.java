package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;
import net.fahr3n.unnecessarilycompressedcobblestone.util.SelectiveImmunity;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.LightningSong;
import net.fahr3n.unnecessarilycompressedcobblestone.util.PianoNote;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.level.Level;

/**
 * The Compressed Composer. A boss that is fought in music and in nothing else.
 * <p>
 * It stands still - it has no movement goals at all, and its only movement is the blink it takes
 * before every performance. Every ten seconds it puts thirty-odd blocks between itself and whoever
 * it is fighting, raises its arms, and plays one of {@link #SONGS} down on them in real lightning:
 * one bolt per note, on the ground where they were standing when it started. The teleport is the
 * only reason it survives its own attack, and it is why the blink is <em>away</em> - the ring it
 * plays into is four blocks wide and the composer is never within thirty of it.
 * <p>
 * Nothing else in the game can hurt it. Not a sword, not an arrow, not a blast, not even ordinary
 * lightning: {@link #thunderHit} is the one door in, and only a bolt carrying a piano key comes
 * through it. That makes the Bolt Launcher loaded with note bolts, a rank of Lightning Cores, or a
 * Composition Bolt the whole of a player's answer.
 * <p>
 * And the answer is a musical one, because a note remembers being played. Every key struck - by the
 * player, and by the composer's own performances - raises what that key is worth against it next
 * time by {@link #CHARGE_PER_USE}, for good. So the songs it plays are not only the attack, they are
 * the instruction: a player who listens to which keys the Ode to Joy leans on and answers in the
 * same key does many times the damage of one firing whatever came to hand. The ramp is linear and
 * uncapped, which is safe for the reason a flat number always is - it buys nothing that approaches a
 * ratio, and every point of it was paid for by a note actually struck.
 * <p>
 * It extends {@link Evoker} for the model, the renderer and the arms-raised spellcasting pose, which
 * is exactly a conductor's; none of the evoker's goals are kept, and it is no raider - see
 * {@link #registerGoals}.
 */
public class CompressedComposerEntity extends Evoker implements SelectiveImmunity {
    public static final float MAX_HEALTH = 5000.0F;

    /** Deliberately light: what defends this boss is what it is immune to, not what it wears. */
    public static final double ARMOR = 10.0;
    public static final double ARMOR_TOUGHNESS = 5.0;

    /** Ten seconds from the start of one performance to the start of the next. */
    private static final int PERFORMANCE_INTERVAL = 200;

    /**
     * What every key it has already heard is worth the next time it is struck: a quarter more, and
     * again a quarter more the time after that. Uncapped - see the note on this class.
     */
    private static final float CHARGE_PER_USE = 0.25F;

    /** How wide the ring of bolts a performance falls in is. Tight, so the song is one place. */
    private static final double SONG_RADIUS = 4.0;

    /** How far it puts itself from its target before it plays. */
    private static final double TELEPORT_MIN_DISTANCE = 30.0;
    private static final double TELEPORT_MAX_DISTANCE = 44.0;

    /** How many spots it will try before giving up on this blink and playing from where it is. */
    private static final int TELEPORT_ATTEMPTS = 24;

    /** How far it looks for something to fight, which is well past the distance it blinks to. */
    private static final double SEARCH_RADIUS = 64.0;

    /** Ticks between searches when it has nothing to fight; a search walks every entity in reach. */
    private static final int SEARCH_INTERVAL = 20;

    private static final String TAG_PERFORMANCE_COOLDOWN = "performance_cooldown";
    private static final String TAG_CHARGES = "note_charges";

    /**
     * The five it knows. Each is a datapack file under {@code data/<namespace>/songs/}, written by
     * {@code tools/build_composer_songs.py} off the opening phrase of something well known, and each
     * is under nine seconds so a performance always finishes before the next one is due. A pack that
     * replaces one of these files changes what the boss plays and nothing else.
     */
    private static final List<ResourceLocation> SONGS = List.of(
            song("ode_to_joy"), song("fate"), song("fur_elise"), song("eine_kleine"), song("canon_in_d"));

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_composer"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    /**
     * How many times each of the eighty-eight keys has been played at this composer, which is the
     * whole of the fight's arithmetic. Saved, so a boss left overnight remembers the fight.
     */
    private final int[] charges = new int[PianoNote.KEYS];

    private int performanceCooldown = PERFORMANCE_INTERVAL;
    private int searchCooldown;

    public CompressedComposerEntity(EntityType<? extends Evoker> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 750;
        setPersistenceRequired();
        setCanJoinRaid(false);
    }

    /**
     * Monster numbers with the health, armour and toughness the fight is written around. Movement
     * speed is zero because it never walks: everything it does about position it does by blinking,
     * and a composer that could be kited into its own ring would be a different fight.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS);
    }

    /**
     * None at all, and {@code super} is deliberately not called: the evoker's goals are its three
     * spells, a stroll and an avoid-players, none of which a stationary boss with one attack of its
     * own wants. Its targeting is the mod's boss rule instead, chosen in {@link #customServerAiStep}.
     */
    @Override
    protected void registerGoals() {
    }

    /** A boss has no business wandering off to join somebody's village raid. */
    @Override
    public boolean canJoinRaid() {
        return false;
    }

    /** Nothing shoves a conductor off their podium. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * Peaceful is a separate branch of {@code Mob.checkDespawn} that runs before the persistence
     * flag is even looked at, so a boss has to answer this as well as being persistent.
     */
    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // It never walks, and nothing else here stops it drifting: buoyancy and currents move a mob
        // that has no goals, which is the same reason the conjurers zero their own delta.
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));

        // Vanilla drops the pose in a spell goal's stop(), and there are no goals here - so the
        // arms have to be put down by hand once the piece the counter was set to has run out, or
        // the composer conducts for the rest of its life.
        if (this.spellCastingTickCount <= 0 && getCurrentSpell() != SpellcasterIllager.IllagerSpell.NONE) {
            setIsCastingSpell(SpellcasterIllager.IllagerSpell.NONE);
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

        getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.performanceCooldown-- > 0) {
            return;
        }

        this.performanceCooldown = PERFORMANCE_INTERVAL;
        perform(serverLevel, target);
    }

    /**
     * One performance: blink well away, then play a song down on wherever the target is standing.
     * <p>
     * The order matters and is the whole of the composer's safety. The song is anchored to the
     * ground the target was on when it started rather than following them, so the blink has to have
     * happened first - and once it has, the ring is thirty blocks away from the only thing that can
     * be hurt by it standing in it.
     */
    private void perform(ServerLevel level, LivingEntity target) {
        teleportAwayFrom(target);

        LightningSong song = LightningSong.get(level.getServer(), SONGS.get(this.random.nextInt(SONGS.size())));
        if (song.notes().isEmpty()) {
            return;
        }

        // Every key in the piece is now a key this composer has heard, which is what makes the
        // performance an instruction as well as an attack.
        for (LightningSong.Note note : song.notes()) {
            charge(note.key());
        }

        DeferredStrikes.queueSong(level, target.blockPosition(), song, SONG_RADIUS);

        // The arms stay up for as long as the piece runs. Both halves of the pose have to be set:
        // the client reads the synched spell id and the server reads the tick counter.
        setIsCastingSpell(SpellcasterIllager.IllagerSpell.SUMMON_VEX);
        this.spellCastingTickCount = song.lengthTicks();

        level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, getSoundSource(),
                2.0F, 0.8F);
    }

    /**
     * Blinks to somewhere well away from {@code target}. {@code randomTeleport} is what decides
     * whether a spot is safe - it refuses anything inside a block, over a drop or in fluid - so the
     * search here is only about distance. If none of the tries lands, it plays from where it is,
     * because a cycle that silently did nothing would read as the boss being broken.
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

    /**
     * The only way in.
     * <p>
     * Vanilla's lightning damage source carries no entity - {@code DamageSources.lightningBolt()} is
     * a bare source shared by every bolt in the world - so which key struck cannot be read in
     * {@code hurt}. It can be read here, where the bolt itself is the argument, which is why the
     * whole rule lives in this override rather than in the damage path.
     * <p>
     * A bolt with no note does nothing at all, and neither does the fire vanilla would set: super is
     * not called. A bolt with one is worth what it always was, multiplied by every previous time
     * that key was played, and then the key is worth a little more still.
     */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        int key = lightning instanceof VanillaLightningBoltEntity bolt
                ? bolt.getNote() : VanillaLightningBoltEntity.NO_NOTE;
        if (!PianoNote.isKey(key)) {
            return;
        }

        float amount = lightning.getDamage() * (1.0F + CHARGE_PER_USE * this.charges[key]);
        charge(key);

        hurt(damageSources().lightningBolt(), amount);
        level.sendParticles(ParticleTypes.NOTE, getX(), getEyeY() + 0.5, getZ(), 8, 0.4, 0.4, 0.4, 0.0);
    }

    /** Records that {@code key} has been played here, which is what makes it worth more next time. */
    private void charge(int key) {
        if (PianoNote.isKey(key)) {
            this.charges[key]++;
        }
    }

    /**
     * Everything but a note costs it nothing. Lightning is let through because {@link #thunderHit}
     * is the thing that actually decides, and it is stricter than this: a plain bolt reaches that
     * method and is turned away there.
     * <p>
     * {@link SelectiveImmunity} rather than {@code isInvulnerableTo}, so a blow that is not a note
     * still lands as a blow and only its number is taken - which is what lets a player see they hit
     * it and learn what the fight wants. {@code /kill}, the void and this mod's true damage are past
     * the rule entirely; see {@link SelectiveImmunity#refusable}.
     */
    @Override
    public boolean refusesDamageFrom(DamageSource source) {
        return SelectiveImmunity.refusable(source) && !source.is(DamageTypes.LIGHTNING_BOLT);
    }

    /** Its own kind, which is the one thing the boss rule says it will not attack. */
    private boolean isOwnKind(Entity entity) {
        return entity instanceof CompressedComposerEntity;
    }

    private static ResourceLocation song(String name) {
        return ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_PERFORMANCE_COOLDOWN, this.performanceCooldown);
        compound.putIntArray(TAG_CHARGES, this.charges);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.performanceCooldown = compound.getInt(TAG_PERFORMANCE_COOLDOWN);

        int[] saved = compound.getIntArray(TAG_CHARGES);
        for (int key = 0; key < this.charges.length; key++) {
            this.charges[key] = key < saved.length ? saved[key] : 0;
        }
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
