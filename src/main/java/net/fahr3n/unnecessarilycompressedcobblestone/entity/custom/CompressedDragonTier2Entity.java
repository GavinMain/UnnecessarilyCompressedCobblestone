package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.ArrayList;
import java.util.List;
import net.fahr3n.unnecessarilycompressedcobblestone.util.AbsoluteLimit;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Dragon, second phase: the same beast half again as large, with everything the
 * first phase could do and eight things it could not.
 * <p>
 * It is the first phase's class with its dials turned and two hooks filled in - the flight, the
 * ground walk, the breath, the thrown TNT, the dive, the bite, the rush and the tail swipe are all
 * inherited unchanged, because they are the same dragon. What is new is here and nowhere else.
 * <p>
 * Three things about how it takes damage, and they are meant to be read together:
 * <ul>
 * <li><b>A hundredth of everything lands</b>, except true damage, which lands whole. Five thousand
 *     health behind that hundredth is half a million points of damage, which is a fight measured in
 *     what a player can bring rather than in seconds; {@code #ucc:true_damage} is the one thing that
 *     is not reduced, because a reduction is a kind of protection and that tag is the mod's name for
 *     the damage no protection answers.</li>
 * <li><b>Any single hit worth {@value #ABSOLUTE_LIMIT} or more lands for nothing at all</b>, true
 *     damage included. That is the answer to the obvious response to the line above - one enormous
 *     blow instead of ten thousand small ones - and it is the one rule here that true damage does
 *     not get past, since it is a ceiling on the number rather than a statement about the attack
 *     carrying it. See {@link AbsoluteLimit}, which any mob can state. It is checked <em>twice</em>:
 *     once as {@code hurt} is handed the blow, and again on the final figure, because a blow can be
 *     raised after the first check and the dev sword's whole trick is doing exactly that.</li>
 * <li><b>{@code /kill} does not work on it, and neither does its own Singularity.</b> The first is
 *     the one place in this mod where {@code #bypasses_invulnerability} is refused, and it is
 *     deliberate: this is the last thing in the game, and it is not meant to be removed by typing.
 *     The second is why that TNT has a damage type of its own at all.</li>
 * </ul>
 * Lightning of its own - out of its storm or out of its own calling - costs it nothing, since those
 * bolts fall around the dragon itself. A player's lightning hurts it exactly as it hurts anything,
 * which is the whole reason those bolts are marked rather than being recognised by their class -
 * see {@link #thunderHit}.
 * <p>
 * And it will not tolerate a spectator. A player in creative is killed on sight, which is the one
 * mechanic here that is about the person rather than about the character.
 */
public class CompressedDragonTier2Entity extends CompressedDragonEntity implements AbsoluteLimit {
    public static final float MAX_HEALTH = 5000.0F;

    /** A hundredth of every hit lands. See the class note. */
    private static final float DAMAGE_TAKEN = 0.01F;

    /** A hit worth this much or more lands for nothing. See {@link AbsoluteLimit}. */
    private static final float ABSOLUTE_LIMIT = 1000.0F;

    /** How much bigger than the first phase it is. Hitbox, drawing and shadow together. */
    public static final double BOSS_SCALE = 1.6;

    /** How far it looks for a creative player to remove, and how often it looks. */
    private static final double CREATIVE_RADIUS = 64.0;
    private static final int CREATIVE_INTERVAL = 20;

    /* WHAT THE AIR ADDS */

    /**
     * The air table, in parts of a hundred: the three the first phase already had, re-weighted, and
     * the two summonings. The two rare ones below are not added to this - they are taken out of the
     * breath as they unlock, so the table stays a hundred parts however much of the fight is left.
     */
    private static final int BREATH_WEIGHT = 30;
    private static final int BROOD_WEIGHT = 20;
    private static final int FLOCK_WEIGHT = 20;
    private static final int TNT_WEIGHT = 15;
    private static final int SLAM_WEIGHT = 15;

    /** How many chickens a brood is, and how far out they arrive. */
    private static final int BROOD_SIZE = 8;
    private static final double BROOD_SPREAD = 8.0;

    /**
     * What a summoned chicken lives on, and the two effects that make it a hatchery rather than a
     * chicken: the infestation, which answers being hurt rather than the clock, and something that
     * hurts it once a second so that it is answered. The chicken boss's own arithmetic - health
     * divided by damage less regeneration is how long it lasts - and the same reason for it.
     */
    private static final float BROOD_HEALTH = 60.0F;
    private static final int BROOD_EFFECT_TICKS = 20 * 60;
    private static final int BROOD_DAMAGE_AMPLIFIER = 1;
    private static final int BROOD_REGENERATION_AMPLIFIER = 1;

    /** How many phantoms a flock is, and how high over the fight they arrive. */
    private static final int FLOCK_SIZE = 6;
    private static final double FLOCK_HEIGHT = 10.0;
    private static final double FLOCK_SPREAD = 8.0;

    /**
     * The rare one: a Singularity TNT thrown at whoever it is fighting, never before the fight is
     * four fifths over.
     * <p>
     * It is a row of the table rather than a clock, but the clock is still there behind it: the tick
     * its health drops under {@link #SINGULARITY_THRESHOLD} the row joins the table at a twentieth,
     * paid for out of the breath, and once one is thrown the row leaves the table again for ten
     * minutes - the weight going back to the breath while it is away, so a table with a spent
     * Singularity in it is exactly the table that had not unlocked it yet. Being drawn one time in
     * twenty is what makes it the fight's rare attack; the cooldown is what stops two of them being
     * a minute apart.
     */
    private static final int SINGULARITY_WEIGHT = 5;
    private static final int SINGULARITY_INTERVAL = 20 * 60 * 10;
    private static final float SINGULARITY_THRESHOLD = 0.2F;
    private static final float SINGULARITY_VELOCITY = 1.4F;

    /**
     * The last one, and the only attack in this mod that is not damage: below
     * {@link #KILL_THRESHOLD} of its health it runs {@code /kill} on whatever it is fighting.
     * <p>
     * It is telegraphed rather than instant, and the wind-up is not a softening - it is the whole
     * counterplay. {@link #KILL_WINDUP} ticks of a roar and a column of particles is time enough to
     * break line of sight or to leave {@link #KILL_RANGE}, and either of those spends the attack.
     */
    private static final int KILL_WEIGHT = 1;
    private static final int KILL_INTERVAL = 400;
    private static final float KILL_THRESHOLD = 0.05F;
    private static final int KILL_WINDUP = 40;
    private static final double KILL_RANGE = 40.0;

    /* WHAT THE GROUND ADDS */

    /**
     * The ground table, in parts of a hundred. The bite and the rush are half what they were worth
     * in the first phase, and what they gave up is the four things below.
     */
    private static final int BITE_WEIGHT = 20;
    private static final int RUSH_WEIGHT = 20;
    private static final int TAIL_WEIGHT = 10;
    private static final int LIGHTNING_WEIGHT = 10;
    private static final int BANISH_WEIGHT = 10;
    private static final int ARENA_WEIGHT = 10;
    private static final int CHARGE_WEIGHT = 20;

    /** How many bolts a calling is, how far out they fall, and what one is worth. */
    private static final int LIGHTNING_BOLTS = 8;
    private static final double LIGHTNING_SPREAD = 10.0;
    private static final float LIGHTNING_DAMAGE = 150.0F;

    /** How blind and how slow a banishment leaves them, and for how long. */
    private static final int BANISH_EFFECT_TICKS = 40;
    private static final int BANISH_SLOWNESS_AMPLIFIER = 4;
    private static final double BANISH_DISTANCE = 30.0;

    /** How wide an arena is. */
    private static final int ARENA_RADIUS = 40;

    /**
     * The charge: Speed 100 and ten seconds of walking through the world.
     * <p>
     * The speed is a real {@code MobEffect} rather than an attribute modifier because the first
     * phase lets beneficial effects through for exactly this, and the walk reads
     * {@code MOVEMENT_SPEED} through {@code groundSpeed()} - so the number means something without
     * anything here knowing how the walk works. The phasing is the mod's own flag, and it comes with
     * {@code noGravity}: a thing that ignores walls ignores the floor as well, so without that the
     * charge would be ten seconds of falling.
     */
    private static final int CHARGE_TICKS = 200;
    private static final int CHARGE_SPEED_AMPLIFIER = 99;

    /** How far away a music weather has to be before it counts as gone. */
    private static final double WEATHER_RADIUS = 128.0;

    /** The mark on everything it calls up, so nothing it summons can be turned against it. */
    private static final String SUMMON_FLAG = "ucc:dragon_summon";

    /** And on every bolt of its own, so its own lightning heals it and a player's does not. */
    private static final String BOLT_FLAG = "ucc:dragon_bolt";

    private static final String TAG_SINGULARITY_COOLDOWN = "singularity_cooldown";
    private static final String TAG_KILL_COOLDOWN = "kill_cooldown";
    private static final String TAG_KILL_WINDUP = "kill_windup";
    private static final String TAG_CHARGE = "charge";

    /**
     * How long the two rare rows are out of the table for. Both start at zero: the health threshold
     * is what unlocks them, so the first of each is available the moment the fight reaches it.
     */
    private int singularityCooldown;
    private int killCooldown;

    /** Ticks left of a wind-up to a kill, or 0 when none is under way. */
    private int killWindup;

    /** Ticks left of the charge. */
    private int charge;

    public CompressedDragonTier2Entity(EntityType<? extends CompressedDragonTier2Entity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 2000;
    }

    /**
     * The first phase's numbers with the health doubled and the size raised.
     * <p>
     * {@code SCALE} is the whole of "bigger": vanilla multiplies the hitbox by it and the renderer
     * multiplies the drawing by it, so the two cannot drift apart. {@code STEP_HEIGHT} goes up with
     * it because step height is scaled by nothing.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return CompressedDragonEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.SCALE, BOSS_SCALE)
                .add(Attributes.STEP_HEIGHT, 3.0 * BOSS_SCALE);
    }

    @Override
    protected float damageTaken() {
        return DAMAGE_TAKEN;
    }

    /** No phrases: this death is the end of the fight rather than the middle of it. */
    @Override
    protected int deathPhrases() {
        return 0;
    }

    /** There is no third phase. What is left behind is an egg, which is the loot table's business. */
    @Nullable
    @Override
    protected EntityType<? extends Mob> nextPhase() {
        return null;
    }

    /* WHAT CAN HURT IT */

    /**
     * The two refusals that are not a matter of degree: {@code /kill} and its own Singularity.
     * <p>
     * {@code /kill} arrives as {@code minecraft:fell_out_of_world} - {@code Entity#kill} is one
     * {@code hurt} call with that source and {@code Float.MAX_VALUE} - so refusing that type is what
     * "immune to {@code /kill}" means in code. It also refuses the void, which is the honest cost of
     * the same rule.
     * <p>
     * Its own Singularity is two damage types now, since the TNT leaves a black hole before the blast:
     * the core's bite is refused as well, or a dragon drawn into the hole it threw would be eaten.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(ModDamageTypes.SINGULARITY)
                || source.is(ModDamageTypes.BLACK_HOLE) || super.isInvulnerableTo(source);
    }

    /**
     * The ceiling, as the blow arrives. A hit worth {@link #ABSOLUTE_LIMIT} or more is thrown away
     * whole, before anything else about it is looked at.
     * <p>
     * Only the figure as it arrives is measured, and the hundredth this dragon takes needs no second
     * test of its own: what lands is never larger than what arrived, so a blow that passes this
     * cannot fail it after the reduction. What a reduction cannot do is make a blow <em>bigger</em>,
     * and something else can - see {@link #absoluteLimit} for the half of the rule that answers
     * that.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (exceedsAbsoluteLimit(amount)) {
            refuseAbsoluteLimit();
            return false;
        }

        return super.hurt(source, amount);
    }

    /**
     * The size of blow this dragon refuses, and the one rule of its own that true damage does not
     * get past.
     * <p>
     * That is the whole distinction {@link AbsoluteLimit} exists to draw. A rule about what
     * <em>kind</em> of attack a boss answers is a statement about attacks, and true damage is the
     * thing that is not one; a rule about <em>how much</em> is a ceiling on the number itself, and
     * there is nothing for a damage type to bypass. So the hundredth above is bypassed by
     * {@code #ucc:true_damage} and this is not - which is exactly why the dev block is worth
     * 999 - a point under the ceiling - and lands every one of them.
     * <p>
     * Stated once here and enforced twice: {@code hurt} above refuses a blow this large outright,
     * and {@code ModEvents.onAbsoluteLimit} clips the final figure at {@code LivingDamageEvent.Pre}
     * and {@code LOWEST}, because a blow can be raised after {@code hurt} has judged it and the dev
     * sword's whole trick is setting it to {@code Float.MAX_VALUE} there.
     */
    @Override
    public float absoluteLimit() {
        return ABSOLUTE_LIMIT;
    }

    /** The sparks and the clang a refused hit makes, so it reads as turned away rather than missed. */
    @Override
    public void refuseAbsoluteLimit() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + getBbHeight() * 0.5,
                    getZ(), 30, getBbWidth() / 2.0, 1.0, getBbWidth() / 2.0, 0.2);
            serverLevel.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE,
                    6.0F, 0.4F);
        }
    }

    /**
     * Lightning. Its own costs it nothing; anybody else's hurts it exactly as it would hurt
     * anything.
     * <p>
     * Its own is ignored rather than healing it, and it cannot simply be let through: the calling
     * drops bolts around the dragon itself and a bolt hurts everything within three blocks of where
     * it lands, so a dragon that answered its own lightning would kill itself with it. Anything a
     * player brings is answered by {@code super}, which is the fire, the damage and the credit
     * exactly as vanilla wrote them.
     * <p>
     * The test is a mark on the bolt rather than the bolt's class, and that is the whole point: this
     * mod's own {@code VanillaLightningBoltEntity} is what a Lightning Core, a bolt item and a Bolt
     * Launcher all fire, so recognising the class would mean a player could not hurt it with
     * lightning at all. What is marked is what the dragon or its storm made. A bolt with no mark and
     * no player behind it is the sky's own - a real thunderstorm bolt - and counts as the weather's,
     * which is the other half of what the storm is for.
     */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        if (isDragonBolt(bolt) || bolt.getCause() == null && !(bolt instanceof VanillaLightningBoltEntity)) {
            return;
        }

        super.thunderHit(level, bolt);
    }

    /* THE FIGHT */

    /**
     * The two things that are true whichever style it is in: the storm exists, and there is nobody
     * watching from creative.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount == 1) {
            ensureWeather(serverLevel);
        }

        if (this.tickCount % CREATIVE_INTERVAL == 0) {
            clearSpectators(serverLevel);
            keepOutOfTheVoid(serverLevel);
        }

        tickCharge(serverLevel);

        // Ticked here rather than in the air table, so a rare attack spent just before a dive comes
        // back on the clock it was given rather than on however long the dragon stays on the ground.
        this.singularityCooldown--;
        this.killCooldown--;
    }

    /**
     * Summoning the storm again if it is gone, which is checked on spawn and on every change of
     * style. A weather that was killed, unloaded or removed by a command comes straight back; one
     * that is still there is left alone rather than doubled, or the music would be played twice over
     * itself.
     */
    private void ensureWeather(ServerLevel level) {
        List<BossMusicWeatherEntity> playing = level.getEntitiesOfClass(BossMusicWeatherEntity.class,
                getBoundingBox().inflate(WEATHER_RADIUS), BossMusicWeatherEntity::isAlive);
        if (!playing.isEmpty()) {
            return;
        }

        BossMusicWeatherEntity weather = ModEntities.BOSS_MUSIC_WEATHER.get().create(level);
        if (weather == null) {
            return;
        }

        weather.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
        level.addFreshEntity(weather);
    }

    /** The storm is checked on every change of style as well as on the tick it arrives. */
    @Override
    protected void setFlying(boolean flying) {
        super.setFlying(flying);

        if (level() instanceof ServerLevel serverLevel && this.tickCount > 0) {
            ensureWeather(serverLevel);
        }
    }

    /**
     * What it does about a player in creative: kills them.
     * <p>
     * It has to be a scan of the player list rather than anything to do with targeting, because a
     * creative player is invisible to every targeting rule in the game -
     * {@code canBeSeenAsEnemy} is false for them, which is what the mode is for. {@code kill()} is
     * {@code /kill} itself: one hit of {@code fell_out_of_world}, which is in
     * {@code #bypasses_invulnerability} and so is the one thing that reaches them.
     */
    private void clearSpectators(ServerLevel level) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(CREATIVE_RADIUS))) {
            if (player.isCreative()) {
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                        SoundSource.HOSTILE, 10.0F, 0.4F);
                player.kill();
            }
        }
    }

    /**
     * Puts it back on the ground if it has fallen out of the world.
     * <p>
     * This is the price of refusing {@code fell_out_of_world}: that damage type is also how vanilla
     * removes anything that falls past the bottom of the world, so a dragon immune to {@code /kill}
     * is a dragon that would otherwise fall for ever, ticking, invisible and unkillable. Refusing
     * the damage and answering the fall separately is the honest way round - the boss cannot be
     * deleted by dropping it down a hole, and it cannot be lost down one either.
     */
    private void keepOutOfTheVoid(ServerLevel level) {
        if (getY() >= level.getMinBuildHeight()) {
            return;
        }

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition());
        teleportTo(surface.getX() + 0.5, surface.getY() + 16.0, surface.getZ() + 0.5);
        setDeltaMovement(Vec3.ZERO);
        setFlying(true);
    }

    /** The charge's clock, and the two things it turns off when it runs out. */
    private void tickCharge(ServerLevel level) {
        if (this.charge <= 0) {
            return;
        }

        // Held level rather than falling: the phasing that lets it through a wall would let it
        // through the floor, so the vertical is taken away for as long as it lasts.
        setDeltaMovement(getDeltaMovement().x, 0.0, getDeltaMovement().z);
        setPhasing(true);

        if (--this.charge <= 0 && !isFlying()) {
            setPhasing(false);
            setNoGravity(false);
        }

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0, getZ(),
                6, getBbWidth() / 2.0, 1.0, getBbWidth() / 2.0, 0.05);
    }

    /* THE AIR */

    /** The only thing it holds in the air that is not an attack: a kill already being wound up. */
    @Override
    protected boolean flyExtras(ServerLevel level, LivingEntity target, double range) {
        return this.killWindup > 0 && tickKill(level, target, range);
    }

    /**
     * The air table. Thirty parts breath, twenty brood, twenty flock, fifteen thrown TNT and fifteen
     * dive, until the two rare ones unlock themselves off its health - and each of those is paid for
     * out of the breath rather than added on top, so the table is always a hundred parts: the
     * Singularity's five and the kill's one both come off the thirty.
     * <p>
     * Each is in the table only while it is both unlocked and off its cooldown, and the weight it is
     * not spending is the breath's. So the two rows come and go and the other four never change.
     */
    @Override
    protected List<AttackOption> flyAttacks(ServerLevel level, LivingEntity target, double range) {
        boolean singularity = getHealth() < getMaxHealth() * SINGULARITY_THRESHOLD
                && this.singularityCooldown <= 0;
        boolean kill = getHealth() < getMaxHealth() * KILL_THRESHOLD && this.killCooldown <= 0;

        int breath = BREATH_WEIGHT - (singularity ? SINGULARITY_WEIGHT : 0) - (kill ? KILL_WEIGHT : 0);

        List<AttackOption> table = new ArrayList<>(7);
        table.add(new AttackOption(breath, breathAttack()));
        table.add(new AttackOption(BROOD_WEIGHT, (l, t, r) -> {
            summonBrood(l);
            return true;
        }));

        table.add(new AttackOption(FLOCK_WEIGHT, (l, t, r) -> {
            summonFlock(l);
            return true;
        }));

        table.add(new AttackOption(TNT_WEIGHT, tntAttack()));
        table.add(new AttackOption(SLAM_WEIGHT, slamAttack()));

        if (singularity) {
            table.add(new AttackOption(SINGULARITY_WEIGHT, (l, t, r) -> {
                this.singularityCooldown = SINGULARITY_INTERVAL;
                throwSingularity(l, t);
                return true;
            }));
        }

        if (kill) {
            table.add(new AttackOption(KILL_WEIGHT, (l, t, r) -> {
                if (r > KILL_RANGE || !hasLineOfSight(t)) {
                    return false;
                }

                // Spent on the wind-up starting, not on it landing: one that the target walks out
                // of has still cost the dragon its four hundred ticks, which is the counterplay.
                this.killCooldown = KILL_INTERVAL;
                startKill(l);
                return true;
            }));
        }

        return table;
    }

    /** The wind-up to a kill: a tick of it, and whether it is still worth having. */
    private boolean tickKill(ServerLevel level, LivingEntity target, double range) {
        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0, target.getZ(),
                12, 0.4, 1.0, 0.4, 0.05);

        // Broken off rather than merely delayed: getting out of reach or behind something spends it.
        if (range > KILL_RANGE || !hasLineOfSight(target)) {
            this.killWindup = 0;
            return false;
        }

        if (--this.killWindup > 0) {
            return true;
        }

        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 10.0F, 1.4F);
        target.kill();
        return true;
    }

    private void startKill(ServerLevel level) {
        this.killWindup = KILL_WINDUP;
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 12.0F, 0.3F);
    }

    /**
     * One Singularity TNT, thrown the way every other charge is: as a {@link TntProjectileEntity},
     * which lights whatever block it is carrying wherever it stops. Nothing here knows what a
     * Singularity does - the block does.
     */
    private void throwSingularity(ServerLevel level, LivingEntity target) {
        ItemStack charge = new ItemStack(ModBlocks.SINGULARITY_TNT.get());
        TntProjectileEntity shell = new TntProjectileEntity(level, this, charge, ItemStack.EMPTY);
        shell.setPos(position().add(0.0, getBbHeight() * 0.5, 0.0));

        double dx = target.getX() - shell.getX();
        double dy = target.getY(0.5) - shell.getY();
        double dz = target.getZ() - shell.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        shell.shoot(dx, dy + flat * 0.1, dz, SINGULARITY_VELOCITY, 0.5F);
        level.addFreshEntity(shell);

        level.playSound(null, blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 10.0F, 0.3F);
    }

    /**
     * The brood: chickens carrying the infestation and something that hurts them, which is the
     * Compressed Chicken Boss's own trick and is borrowed on purpose - the infestation answers being
     * hurt rather than the clock, so the freezing under it is what makes silverfish come out, and
     * the regeneration is what decides how many.
     */
    private void summonBrood(ServerLevel level) {
        for (int i = 0; i < BROOD_SIZE; i++) {
            CompressedCobblestoneChickenEntity chicken =
                    ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get().create(level);
            if (chicken == null) {
                continue;
            }

            double angle = i * (Math.PI * 2.0 / BROOD_SIZE);
            double radius = 2.0 + this.random.nextDouble() * BROOD_SPREAD;
            chicken.moveTo(getX() + Math.cos(angle) * radius, getY() + 2.0,
                    getZ() + Math.sin(angle) * radius, this.random.nextFloat() * 360.0F, 0.0F);

            chicken.setConjured(BROOD_HEALTH);
            chicken.addEffect(new MobEffectInstance(ModMobEffects.COMPRESSED_INFESTATION,
                    BROOD_EFFECT_TICKS, 0, false, true, true));
            chicken.addEffect(new MobEffectInstance(ModMobEffects.FREEZING,
                    BROOD_EFFECT_TICKS, BROOD_DAMAGE_AMPLIFIER, false, true, true));
            chicken.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    BROOD_EFFECT_TICKS, BROOD_REGENERATION_AMPLIFIER, false, true, true));

            markSummon(chicken);
            level.addFreshEntity(chicken);
        }

        level.playSound(null, blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.HOSTILE, 6.0F, 0.4F);
    }

    /**
     * The flock: this mod's phantoms, which go through walls and hit like a diamond sword.
     * <p>
     * {@code finalizeSpawn} is not optional for one of these - a phantom reads its anchor point
     * there and flies off towards the world origin without it - and neither is the mark, which is
     * what keeps the flock off the thing that called it.
     */
    private void summonFlock(ServerLevel level) {
        for (int i = 0; i < FLOCK_SIZE; i++) {
            CompressedPhantomEntity phantom = ModEntities.COMPRESSED_PHANTOM.get().create(level);
            if (phantom == null) {
                continue;
            }

            double angle = i * (Math.PI * 2.0 / FLOCK_SIZE);
            double radius = 2.0 + this.random.nextDouble() * FLOCK_SPREAD;
            double x = getX() + Math.cos(angle) * radius;
            double y = getY() + FLOCK_HEIGHT;
            double z = getZ() + Math.sin(angle) * radius;

            phantom.moveTo(x, y, z, this.random.nextFloat() * 360.0F, 0.0F);
            phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                    MobSpawnType.MOB_SUMMONED, null);
            phantom.setPersistenceRequired();
            markSummon(phantom);
            level.addFreshEntity(phantom);
            level.sendParticles(ParticleTypes.PORTAL, x, y, z, 20, 0.4, 0.8, 0.4, 0.4);
        }

        level.playSound(null, blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 8.0F, 0.5F);
    }

    /* THE GROUND */

    /**
     * The ground table. The first phase's three at half the weight, and the four this phase adds:
     * a calling, a banishment, an arena and the charge, which is a fifth of it on its own.
     */
    @Override
    protected List<AttackOption> groundAttacks(ServerLevel level, LivingEntity target, double range) {
        return List.of(
                new AttackOption(BITE_WEIGHT, biteAttack()),
                new AttackOption(RUSH_WEIGHT, rushAttack()),
                new AttackOption(TAIL_WEIGHT, tailAttack()),
                new AttackOption(LIGHTNING_WEIGHT, (l, t, r) -> {
                    callLightning(l);
                    return true;
                }),
                new AttackOption(BANISH_WEIGHT, (l, t, r) -> {
                    if (r > ARENA_RADIUS) {
                        return false;
                    }

                    banish(l, t);
                    return true;
                }),
                new AttackOption(ARENA_WEIGHT, (l, t, r) -> {
                    flatten(l);
                    return true;
                }),
                new AttackOption(CHARGE_WEIGHT, (l, t, r) -> {
                    startCharge(l);
                    return true;
                }));
    }

    /**
     * The calling: bolts of its own around itself, worth {@link #LIGHTNING_DAMAGE} to anything they
     * catch and nothing at all to the dragon. The mark on these is what {@link #thunderHit} reads,
     * and it is not decoration - they fall around the dragon itself, and a bolt hurts everything
     * within three blocks of where it lands.
     */
    private void callLightning(ServerLevel level) {
        for (int i = 0; i < LIGHTNING_BOLTS; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(this.random.nextDouble()) * LIGHTNING_SPREAD;
            BlockPos pos = BlockPos.containing(getX() + Math.cos(angle) * distance, getY(),
                    getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(pos)) {
                continue;
            }

            VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
            if (bolt == null) {
                continue;
            }

            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
            bolt.moveTo(pos.getX() + 0.5, surface.getY(), pos.getZ() + 0.5);
            bolt.setDamage(LIGHTNING_DAMAGE);
            markBolt(bolt);
            level.addFreshEntity(bolt);
        }
    }

    /**
     * The banishment: blind, slowed to a crawl and thirty blocks away.
     * <p>
     * A player is moved by teleport rather than by velocity, because velocity makes the distance
     * depend on what they were standing on, and through their connection rather than
     * {@code teleportTo}, which would move the server's copy without telling their client. The
     * landing is taken off the heightmap rather than cast as a ray: a surface is somewhere a player
     * can stand, which is the one thing thirty blocks in a straight line cannot promise.
     */
    private void banish(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BANISH_EFFECT_TICKS,
                BANISH_SLOWNESS_AMPLIFIER, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BANISH_EFFECT_TICKS,
                0, false, true, true));

        Vec3 away = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        Vec3 heading = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
        Vec3 landing = target.position().add(heading.scale(BANISH_DISTANCE));
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(landing));

        double x = surface.getX() + 0.5;
        double y = surface.getY();
        double z = surface.getZ() + 0.5;

        if (target instanceof ServerPlayer player) {
            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        } else {
            target.teleportTo(x, y, z);
        }

        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 60, 0.6, 1.2, 0.6, 0.6);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 8.0F, 0.4F);
    }

    /**
     * The arena: the ground around it flattened into somewhere a fight can actually happen - every
     * hole in the floor filled and everything standing on it taken away, a layer a tick.
     * <p>
     * It is {@code DeferredFill.queueArena} rather than anything written here, which is the same job
     * every boss in this mod that brings its own ground uses. One layer only: a fight needs a
     * surface, not a solid.
     */
    private void flatten(ServerLevel level) {
        DeferredFill.queueArena(level, blockPosition(), ARENA_RADIUS);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 8.0F, 0.3F);
    }

    /** The charge: Speed 100 and ten seconds of ignoring the world. See {@link #CHARGE_TICKS}. */
    private void startCharge(ServerLevel level) {
        this.charge = CHARGE_TICKS;
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, CHARGE_TICKS,
                CHARGE_SPEED_AMPLIFIER, false, true, true));
        setNoGravity(true);
        setPhasing(true);

        level.playSound(null, blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.HOSTILE, 10.0F, 0.4F);
    }

    /* THE TWO MARKS */

    /** Marks something the dragon called up, so nothing it summons can be turned back on it. */
    public static void markSummon(Entity summon) {
        summon.getPersistentData().putBoolean(SUMMON_FLAG, true);
    }

    /** Whether {@code entity} was called up by one of these. Read by {@code ModEvents}. */
    public static boolean isSummon(Entity entity) {
        return entity.getPersistentData().getBoolean(SUMMON_FLAG);
    }

    /** Marks a bolt as the dragon's or its storm's, which is what makes it harmless to the dragon. */
    public static void markBolt(LightningBolt bolt) {
        bolt.getPersistentData().putBoolean(BOLT_FLAG, true);
    }

    private static boolean isDragonBolt(LightningBolt bolt) {
        return bolt.getPersistentData().getBoolean(BOLT_FLAG);
    }

    /** Nothing it calls up is its enemy, so it never picks one of them as a target either. */
    @Override
    public boolean isAlliedTo(Entity entity) {
        return isSummon(entity) || super.isAlliedTo(entity);
    }

    /** A player in creative is dealt with in {@link #clearSpectators}, not here. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Player player && player.isCreative()) && super.canAttack(target);
    }

    /* SAVING */

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_SINGULARITY_COOLDOWN, this.singularityCooldown);
        compound.putInt(TAG_KILL_COOLDOWN, this.killCooldown);
        compound.putInt(TAG_KILL_WINDUP, this.killWindup);
        compound.putInt(TAG_CHARGE, this.charge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.singularityCooldown = compound.getInt(TAG_SINGULARITY_COOLDOWN);
        this.killCooldown = compound.getInt(TAG_KILL_COOLDOWN);
        this.killWindup = compound.getInt(TAG_KILL_WINDUP);
        this.charge = compound.getInt(TAG_CHARGE);
    }
}
