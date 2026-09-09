package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Sweep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Dragon, first phase: the mod's last fight, and the only one that is two creatures
 * rather than two halves of one.
 * <p>
 * Everything about it hangs off one flag. It is either in the air or on the ground, it has three
 * attacks in each, and the only way between the two is an attack: the flying slam puts it on the
 * ground and the tail swipe puts it back in the air. So the fight is not a rotation of six things -
 * it is two fights with a door at each end, and which one is being fought is always the dragon's
 * choice rather than the clock's.
 * <p>
 * In the air it is a bombardment. It breathes {@link CompressedDragonBreathEntity}, which is
 * vanilla's dragon breath carrying every effect in {@code #ucc:damage_over_time} instead of Instant
 * Damage; it throws live TNT drawn out of {@code #ucc:tnt}, so what lands is whichever of this mod's
 * two dozen charges - or another mod's - the tag happened to hand over; and it dives, which is the
 * Smash engraving's landing turned round and pointed at the player.
 * <p>
 * On the ground it is a bulldozer. It bites, it rushes - crossing the ground at speed and taking
 * every block in the corridor it passes through - and it sweeps its tail, which empties a sphere of
 * the world around it and throws it back into the air.
 * <p>
 * Its two thousand five hundred health are worth twice that, because half of every hit is turned
 * away before armour is even consulted: see {@link #hurt}. What it does <em>not</em> have is armour
 * of its own, deliberately - see the same place.
 * <p>
 * It dies twice. The four phrases and the summon in {@link #tickDeath} are the seam between this
 * mob and the second phase, which is a mob of its own.
 */
public class CompressedDragonEntity extends AbstractCompressedDragonEntity implements Enemy {
    public static final float MAX_HEALTH = 2500.0F;

    /**
     * The share of every incoming hit that actually lands. Half, for every damage type there is.
     * <p>
     * It is a multiplier on the amount rather than {@code Attributes.ARMOR} because armour cannot
     * express this: {@code CombatRules} caps armour's contribution at four fifths however much of
     * it there is, does nothing at all against the third of the damage types in
     * {@code #bypasses_armor}, and above a hundred points starts <em>healing</em> what it is meant
     * to protect. A flat share of the incoming amount is exactly what was asked for, applies to
     * every source, and is one line - see {@link #hurt}.
     */
    public static final float DAMAGE_TAKEN = 0.5F;

    /** What the bite is worth, which is the whole of {@code ATTACK_DAMAGE}. */
    public static final float BITE_DAMAGE = 40.0F;

    /** How far it looks for something to fight, which is also its {@code FOLLOW_RANGE}. */
    private static final double SEARCH_RADIUS = 64.0;

    /* THE AIR */

    /** How high over its target it holds, and how far out it circles while it does. */
    private static final double HOVER_HEIGHT = 14.0;
    private static final double ORBIT_RADIUS = 18.0;

    /** How fast the circle turns, in radians a tick: a lap in about ten seconds. */
    private static final double ORBIT_SPEED = 0.03;

    /**
     * The step added to its velocity each tick, and the speed that step is allowed to build to.
     * <p>
     * The acceleration is the dial and the cap is only a ceiling on it, which is the way round that
     * works: flight is a step added to the delta every tick against a drag that takes a fixed share
     * back off, so terminal speed is step-over-drag and scaling the delta afterwards only fights
     * the next tick's step.
     */
    private static final double FLY_ACCELERATION = 0.06;
    private static final double FLY_SPEED = 0.9;

    /** How fast it may turn to face where it is going, in degrees a tick. */
    private static final float TURN_RATE = 6.0F;

    /** Its share of the air table, and how far a breath is worth taking. */
    private static final int BREATH_WEIGHT = 75;
    private static final double BREATH_RANGE = 48.0;

    /** Its share of the air table, how far a charge is thrown, and how hard. */
    private static final int TNT_WEIGHT = 15;
    private static final double TNT_RANGE = 40.0;
    private static final float TNT_VELOCITY = 1.6F;

    /** How many draws from the TNT tag before it gives the throw up as a bad job. */
    private static final int TNT_DRAWS = 8;

    /** Its share of the air table, and how close it has to be to start a dive. */
    private static final int SLAM_WEIGHT = 10;
    private static final double SLAM_RANGE = 32.0;

    /** How fast it comes down, how much of the dive is aimed sideways, and when it gives up. */
    private static final double SLAM_SPEED = 3.0;
    private static final double SLAM_LEAD = 0.8;
    private static final int SLAM_TIMEOUT = 80;

    /**
     * The landing, which is the Smash engraving's figures unchanged: five damage plus ten a block
     * fallen, in a radius of four blocks plus a quarter of one a block fallen, out to sixteen.
     * <p>
     * The one thing added is {@link #SLAM_MAX_FALL}. The engraving leaves the damage uncapped
     * because the player pays for every block of it with a climb; here the dragon picks its own
     * altitude and the player cannot, so the height <em>counted</em> stops at twenty-four blocks -
     * which puts the hit at two hundred and forty-five, the same order as every other boss's
     * biggest attack in this mod. The radius is bounded for the reason it is bounded there: it is
     * the size of an entity query, not a reward.
     */
    private static final float SLAM_BASE_DAMAGE = 5.0F;
    private static final float SLAM_DAMAGE_PER_BLOCK = 10.0F;
    private static final float SLAM_MAX_FALL = 24.0F;
    private static final double SLAM_MIN_RADIUS = 4.0;
    private static final double SLAM_MAX_RADIUS = 16.0;
    private static final double SLAM_RADIUS_PER_BLOCK = 0.25;

    /* THE GROUND */

    /** How fast it walks, and how close it has to be to bite. */
    private static final double GROUND_SPEED = 0.22;

    /**
     * The {@code MOVEMENT_SPEED} the walk above was tuned against, and the fastest the walk is ever
     * allowed to be however much of that attribute a dragon has. See {@link #groundSpeed}.
     */
    protected static final double BASE_MOVEMENT_SPEED = 0.3;
    private static final double MAX_GROUND_SPEED = 1.6;
    private static final double BITE_REACH = 7.0;
    private static final int BITE_WEIGHT = 50;

    /** Its share of the ground table, and the band of distances a rush is worth starting from. */
    private static final int RUSH_WEIGHT = 40;
    private static final double RUSH_MIN_RANGE = 6.0;
    private static final double RUSH_MAX_RANGE = 40.0;

    /** How fast a rush crosses ground, how long it may last, and what it does to what it catches. */
    private static final double RUSH_SPEED = 1.1;
    private static final int RUSH_TICKS = 40;
    private static final double RUSH_WIDTH = 5.0;
    private static final float RUSH_DAMAGE = 60.0F;

    /**
     * The corridor a rush takes the world out of, measured from the body's centre line and from its
     * feet. Not the whole hitbox: this thing is sixteen blocks wide, and clearing all of that would
     * be two thousand block reads a tick for as long as the rush lasted. Seven by six is the beast
     * itself rather than its wingspan, and is about two hundred reads a tick.
     */
    private static final int RUSH_PLOUGH_WIDTH = 3;
    private static final int RUSH_PLOUGH_HEIGHT = 6;

    /** Its share of the ground table, how close it has to be, and how far the sweep reaches. */
    private static final int TAIL_WEIGHT = 10;
    private static final double TAIL_RANGE = 12.0;
    private static final int TAIL_RADIUS = 8;

    /**
     * How long it will go on the ground with nothing in reach of the tail before it sweeps at the
     * air anyway. The swipe is the only door back into the air, so without this a player who simply
     * walks away from a landed dragon faces nothing but its walk for the rest of the fight.
     */
    private static final int TAIL_PATIENCE = 400;

    /** What the tail is worth, and how hard it throws. */
    private static final float TAIL_DAMAGE = 50.0F;
    private static final double TAIL_KNOCKBACK = 2.5;

    /** What it leaves the ground with when a swipe throws it back into the air. */
    private static final double TAKEOFF_LIFT = 1.2;

    /* THE CADENCE */

    /**
     * Ticks between attacks in the air and on the ground. What it attacks <em>with</em> is a draw
     * from {@link #flyAttacks} or {@link #groundAttacks}, so these two are the only things setting
     * how often anything happens, and every attack's frequency is its share of its own table.
     * <p>
     * A draw that turns out not to be usable - a breath with a hill in the way, a bite at something
     * out of reach - is not spent: the row is taken out and the rest are drawn between again, and if
     * nothing at all can be used the cooldown is left where it is and the tick is spent circling or
     * walking instead.
     */
    private static final int AIR_ATTACK_INTERVAL = 30;
    private static final int GROUND_ATTACK_INTERVAL = 20;

    /* THE FINALE */

    /** How long each of the four phrases holds the screen, and how many there are. */
    private static final int PHRASE_TICKS = 25;
    private static final int PHRASES = 4;

    /** And the beat after the last one, before the second phase arrives. */
    private static final int FINALE_TAIL = 20;

    /** How far the phrases carry. Anyone who could see the fight sees the end of it. */
    private static final double FINALE_RADIUS = 96.0;


    private static final String TAG_SLAMMING = "slamming";
    private static final String TAG_SLAM_FROM = "slam_from";
    private static final String TAG_RUSHING = "rushing";
    private static final String TAG_ATTACK_COOLDOWN = "attack_cooldown";
    private static final String TAG_SINCE_SWIPE = "since_swipe";


    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_dragon"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    /** Where it is in its circle. Scenery, so not saved. */
    private double orbit;

    private boolean slamming;
    private double slamFrom;
    private int slamTicks;
    private int rushing;

    /** Ticks until the next draw from whichever table it is fighting out of. */
    private int attackCooldown;

    /** Ticks on the ground since the last tail swipe. See {@link #TAIL_PATIENCE}. */
    private int sinceSwipe;

    public CompressedDragonEntity(EntityType<? extends CompressedDragonEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1000;
        setPersistenceRequired();
        setFlying(true);
    }

    /**
     * No armour and no toughness, and that is not an oversight. The half it turns away is stated
     * once, on the incoming amount, where it applies to every damage type in the game; armour on
     * top of that would only make the same hits arrive in a different order and would drag in the
     * whole of {@code CombatRules}' behaviour at the top of its range. {@code MOVEMENT_SPEED} is
     * deliberately modest - what makes it dangerous on the ground is the rush, not the walk.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return createDragonAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BITE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS)
                // It walks over anything it does not simply plough through.
                .add(Attributes.STEP_HEIGHT, 3.0);
    }

    /**
     * None at all, and {@code super} is not called.
     * <p>
     * Nothing in vanilla's goal library can move something sixteen blocks wide: a path is measured
     * in nodes the size of the mob's own hitbox, so a navigating dragon would find no path anywhere
     * and stand still. It flies, walks, rushes and dives out of {@link #customServerAiStep}
     * instead, which is what vanilla's own dragon does for the same reason.
     */
    @Override
    protected void registerGoals() {
    }


    /**
     * The base class holds the flag and the gravity that goes with it; what is added here is that
     * neither a dive nor a rush survives a change of element.
     */
    @Override
    protected void setFlying(boolean flying) {
        super.setFlying(flying);

        if (flying) {
            this.slamming = false;
            this.rushing = 0;
        } else {
            // A fresh stint on the ground gets the tail's patience back: see TAIL_PATIENCE.
            this.sinceSwipe = 0;
        }
    }

    /* WHAT IT FIGHTS */

    /** Its own kind, which is the one thing it will not turn on - the second phase included. */
    private static boolean isOwnKind(LivingEntity entity) {
        return entity instanceof CompressedDragonEntity;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof CompressedDragonEntity || super.isAlliedTo(entity);
    }

    /**
     * Everything alive that is not itself, an armour stand, or something that cannot be an enemy at
     * all - which is {@link BossTargeting}'s rule and every other boss in this mod's.
     */
    private boolean isEnemy(LivingEntity entity) {
        return BossTargeting.isValid(this, entity, SEARCH_RADIUS, CompressedDragonEntity::isOwnKind);
    }

    /**
     * Refuses every effect that is not a good one, filtered by category rather than by a list. It
     * breathes a cloud of every damaging effect in the game and then lands in it, so this is not a
     * favour to the boss so much as the only way its own breath is not the best weapon against it.
     * <p>
     * Beneficial ones are deliberately let through, and not as a kindness: the second phase grants
     * itself Speed 100 during its charge, and an effect it could not be given would have to become
     * an attribute modifier saying the same thing twice.
     */
    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
    }

    /**
     * Half of everything, before armour, Protection, Resistance or anything else is consulted.
     * <p>
     * Two kinds of damage are left alone. {@code #bypasses_invulnerability} is {@code /kill} and the
     * void, so the boss stays something a command can remove. {@code #ucc:true_damage} is this mod's
     * own name for the damage no protection answers, and this is a protection - it is the whole of
     * the boss's armour, stated as a multiplier because {@code Attributes.ARMOR} misbehaves at the
     * figures this mod reaches, and a reduction written that way would otherwise be the one form of
     * protection true damage did not get past. What still answers a hit this size is the
     * {@code AbsoluteLimit} the second phase states, which is a ceiling on the number rather than a
     * protection against the attack. See {@link #DAMAGE_TAKEN} for why this is not armour.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(ModTags.DamageTypes.TRUE_DAMAGE)) {
            return super.hurt(source, amount);
        }

        return super.hurt(source, amount * damageTaken());
    }

    /**
     * The share of a hit that lands. A dial rather than the constant itself, because the second
     * phase turns away ninety-nine hundredths of everything and reads it through here - and
     * everything that reduces a hit has to go through the one method, or the two would disagree.
     */
    protected float damageTaken() {
        return DAMAGE_TAKEN;
    }

    /* THE FIGHT */

    /** The wingbeat and the flight history are the base class's; the bar is the boss's own. */
    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Walls are only ignored where ignoring them is the point: in the air, where a
        // sixteen-block hitbox would otherwise catch on every hillside, and during a rush, which is
        // ploughing its own corridor anyway. A dive is neither - it has to be able to land.
        setPhasing(isFlying() ? !this.slamming : this.rushing > 0);

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !isEnemy(target)) {
            target = BossTargeting.choose(serverLevel, this, SEARCH_RADIUS, CompressedDragonEntity::isOwnKind);
            setTarget(target);
        }

        if (isFlying()) {
            flyStep(serverLevel, target);
        } else {
            groundStep(serverLevel, target);
        }
    }

    /* THE AIR */

    /** One tick in the air: hold the dive if there is one, otherwise circle and bombard. */
    private void flyStep(ServerLevel level, @Nullable LivingEntity target) {
        if (this.slamming) {
            slamStep(level);
            return;
        }

        this.attackCooldown--;

        if (target == null) {
            // Nothing to fight: hold station. Anything else here is a dragon that climbs to the
            // world's ceiling while nobody is looking, since a pull with no target has nothing to
            // pull towards and a step upwards every tick is a step upwards for ever.
            setDeltaMovement(getDeltaMovement().scale(0.8));
            face(getDeltaMovement().x, getDeltaMovement().z, TURN_RATE);
            return;
        }

        this.orbit += ORBIT_SPEED;
        drift(target.position().add(Math.cos(this.orbit) * ORBIT_RADIUS, HOVER_HEIGHT,
                Math.sin(this.orbit) * ORBIT_RADIUS));

        double range = distanceTo(target);

        // Whatever a deeper phase is in the middle of, offered the tick first: a wind-up that had to
        // win a draw to be carried on with would never finish.
        if (flyExtras(level, target, range)) {
            return;
        }

        if (this.attackCooldown <= 0 && draw(flyAttacks(level, target, range), level, target, range)) {
            this.attackCooldown = AIR_ATTACK_INTERVAL;
        }
    }

    /**
     * What a deeper phase is holding in the air that this one is not - a wind-up under way, and
     * nothing that is merely an attack, which belongs in {@link #flyAttacks}. Answering true means
     * the tick is spent.
     */
    protected boolean flyExtras(ServerLevel level, LivingEntity target, double range) {
        return false;
    }

    /** The same on the ground. See {@link #flyExtras}. */
    protected boolean groundExtras(ServerLevel level, LivingEntity target, double range) {
        return false;
    }

    /* THE TABLES */

    /**
     * One attack, tried. Answering false means the draw was not usable after all - out of range,
     * behind a hill - and the rest of the table is drawn between again rather than the tick being
     * thrown away, so a weight is a share of the attacks that <em>can</em> be made.
     */
    @FunctionalInterface
    protected interface DragonAttack {
        boolean fire(ServerLevel level, LivingEntity target, double range);
    }

    /** One row of a table: how often it is drawn against everything else in it, and what it is. */
    protected record AttackOption(int weight, DragonAttack attack) {
    }

    /**
     * What it may do in the air, and how often. Seventy-five parts breath, fifteen thrown TNT and
     * ten dive; a deeper phase overrides this with a table of its own rather than adding to it, so
     * that the shares of one table always read as the percentages they are.
     */
    protected List<AttackOption> flyAttacks(ServerLevel level, LivingEntity target, double range) {
        return List.of(
                new AttackOption(BREATH_WEIGHT, breathAttack()),
                new AttackOption(TNT_WEIGHT, tntAttack()),
                new AttackOption(SLAM_WEIGHT, slamAttack()));
    }

    /** The same on the ground: fifty parts bite, forty rush and ten tail. See {@link #flyAttacks}. */
    protected List<AttackOption> groundAttacks(ServerLevel level, LivingEntity target, double range) {
        return List.of(
                new AttackOption(BITE_WEIGHT, biteAttack()),
                new AttackOption(RUSH_WEIGHT, rushAttack()),
                new AttackOption(TAIL_WEIGHT, tailAttack()));
    }

    /*
     * The six attacks as rows, so that a deeper phase writes a table of its own out of these and its
     * own without restating what any of them costs or how far it reaches.
     */

    protected final DragonAttack breathAttack() {
        return (l, t, r) -> {
            if (r > BREATH_RANGE || !hasLineOfSight(t)) {
                return false;
            }

            breathe(l, t);
            return true;
        };
    }

    protected final DragonAttack tntAttack() {
        return (l, t, r) -> {
            if (r > TNT_RANGE) {
                return false;
            }

            throwCharge(l, t);
            return true;
        };
    }

    protected final DragonAttack slamAttack() {
        return (l, t, r) -> {
            if (r > SLAM_RANGE) {
                return false;
            }

            startSlam(l, t);
            return true;
        };
    }

    protected final DragonAttack biteAttack() {
        return (l, t, r) -> {
            if (r > BITE_REACH) {
                return false;
            }

            bite(l, t);
            return true;
        };
    }

    protected final DragonAttack rushAttack() {
        return (l, t, r) -> {
            if (r < RUSH_MIN_RANGE || r > RUSH_MAX_RANGE) {
                return false;
            }

            startRush(l, t);
            return true;
        };
    }

    protected final DragonAttack tailAttack() {
        return (l, t, r) -> {
            // Out of reach is only a refusal for as long as its patience lasts: see TAIL_PATIENCE
            // for why a landed dragon has to be able to sweep at nothing at all.
            if (r > TAIL_RANGE && this.sinceSwipe < TAIL_PATIENCE) {
                return false;
            }

            this.sinceSwipe = 0;
            tailSwipe(l);
            return true;
        };
    }

    /**
     * Draws one attack out of a table and fires it, answering whether anything was fired. A row that
     * refuses is taken out and the remainder drawn between again, so the table is walked at most
     * once however many of its rows turn out to be unusable.
     */
    private boolean draw(List<AttackOption> table, ServerLevel level, LivingEntity target, double range) {
        List<AttackOption> left = new ArrayList<>(table);
        int total = 0;
        for (AttackOption option : left) {
            total += option.weight();
        }

        while (!left.isEmpty() && total > 0) {
            int roll = this.random.nextInt(total);
            AttackOption chosen = left.get(left.size() - 1);
            for (AttackOption option : left) {
                roll -= option.weight();
                if (roll < 0) {
                    chosen = option;
                    break;
                }
            }

            if (chosen.attack().fire(level, target, range)) {
                return true;
            }

            left.remove(chosen);
            total -= chosen.weight();
        }

        return false;
    }

    /**
     * One tick of flight towards {@code want}: a step of acceleration towards it, a ceiling on what
     * that builds to, and the body turned to face where it is going rather than where it is aimed -
     * a dragon that pointed straight at its target while circling it would fly sideways.
     */
    private void drift(Vec3 want) {
        Vec3 pull = want.subtract(position());
        if (pull.lengthSqr() > 1.0E-4) {
            setDeltaMovement(getDeltaMovement().add(pull.normalize().scale(FLY_ACCELERATION)));
        }

        Vec3 movement = getDeltaMovement();
        if (movement.length() > FLY_SPEED) {
            setDeltaMovement(movement.normalize().scale(FLY_SPEED));
            movement = getDeltaMovement();
        }

        face(movement.x, movement.z, TURN_RATE);
    }

    /** One breath: vanilla's dragon fireball, carrying this mod's answer to "everything harmful". */
    private void breathe(ServerLevel level, LivingEntity target) {
        Vec3 mouth = position().add(getViewVector(1.0F).scale(4.0)).add(0.0, getBbHeight() * 0.6, 0.0);
        Vec3 heading = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(mouth);

        CompressedDragonBreathEntity breath = new CompressedDragonBreathEntity(level, this, heading);
        breath.setPos(mouth);
        level.addFreshEntity(breath);

        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 8.0F, 0.7F);
    }

    /**
     * One charge thrown: a block drawn out of {@code #ucc:tnt} and lobbed as a
     * {@link TntProjectileEntity}, which lights whatever it is carrying wherever it stops.
     * <p>
     * The draw is the tag rather than a list of this mod's TNTs, so a datapack that adds another
     * mod's charge has armed the dragon with it. Anything in the tag with no item form is passed
     * over - the projectile is drawn as the item it was fired as, and a block that is not an item
     * can be neither drawn nor lit.
     */
    private void throwCharge(ServerLevel level, LivingEntity target) {
        ItemStack charge = drawCharge(level);
        if (charge.isEmpty()) {
            return;
        }

        TntProjectileEntity shell = new TntProjectileEntity(level, this, charge, ItemStack.EMPTY);
        shell.setPos(position().add(0.0, getBbHeight() * 0.5, 0.0));

        double dx = target.getX() - shell.getX();
        double dy = target.getY(0.5) - shell.getY();
        double dz = target.getZ() - shell.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);

        // The extra tenth of the horizontal distance is the arc: a thrown block has gravity, so
        // aiming straight at somebody lands it short of them.
        shell.shoot(dx, dy + flat * 0.1, dz, TNT_VELOCITY, 1.0F);
        level.addFreshEntity(shell);

        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 6.0F, 0.6F);
    }

    /** One TNT out of the tag, or an empty stack if the tag holds nothing that can be thrown. */
    private ItemStack drawCharge(ServerLevel level) {
        Optional<HolderSet.Named<Block>> tag = BuiltInRegistries.BLOCK.getTag(ModTags.Blocks.TNT);
        if (tag.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int draw = 0; draw < TNT_DRAWS; draw++) {
            Optional<Holder<Block>> drawn = tag.get().getRandomElement(level.random);
            if (drawn.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = new ItemStack(drawn.get().value());
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * The dive. Straight down at three blocks a tick with a lean towards the target, gravity back
     * on and walls back in force, so it lands on the first thing under it rather than through it.
     */
    private void startSlam(ServerLevel level, LivingEntity target) {
        this.slamming = true;
        this.slamFrom = getY();
        this.slamTicks = SLAM_TIMEOUT;
        setNoGravity(false);

        // Now rather than next tick: see setPhasing, which applies both halves of it at once.
        setPhasing(false);

        Vec3 lead = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        lead = lead.lengthSqr() > 1.0E-4 ? lead.normalize().scale(SLAM_LEAD) : Vec3.ZERO;
        setDeltaMovement(lead.x, -SLAM_SPEED, lead.z);
        this.hasImpulse = true;

        face(lead.x, lead.z, TURN_RATE);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 10.0F, 0.5F);
    }

    /** One tick of a dive: hold the speed, and land on the first thing that stops it. */
    private void slamStep(ServerLevel level) {
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x, -SLAM_SPEED, movement.z);

        if (onGround() || this.verticalCollisionBelow || --this.slamTicks <= 0) {
            land(level);
        }
    }

    /**
     * The landing: the Smash engraving's own arithmetic, on everything standing around where it came
     * down, and the door into the second half of the fight.
     */
    private void land(ServerLevel level) {
        this.slamming = false;
        setFlying(false);
        setDeltaMovement(Vec3.ZERO);

        float fall = (float) Math.min(Math.max(this.slamFrom - getY(), 0.0), SLAM_MAX_FALL);
        float damage = SLAM_BASE_DAMAGE + SLAM_DAMAGE_PER_BLOCK * fall;
        double radius = Math.min(SLAM_MIN_RADIUS + fall * SLAM_RADIUS_PER_BLOCK, SLAM_MAX_RADIUS);

        for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius, radius / 2.0, radius), this::isEnemy)) {
            caught.hurt(damageSources().mobAttack(this), damage);
            shove(caught, 1.0, 0.5);
        }

        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(),
                (int) radius * 2, radius / 3.0, 0.2, radius / 3.0, 0.0);
        level.sendParticles(ParticleTypes.GUST, getX(), getY(), getZ(),
                (int) radius, radius / 2.0, 0.1, radius / 2.0, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.HOSTILE, 8.0F, 0.5F);
    }

    /* THE GROUND */

    /** One tick on the ground: hold the rush if there is one, otherwise walk, bite and sweep. */
    private void groundStep(ServerLevel level, @Nullable LivingEntity target) {
        if (this.rushing > 0) {
            rushStep(level);
            return;
        }

        this.attackCooldown--;
        this.sinceSwipe++;

        if (target == null) {
            return;
        }

        double range = distanceTo(target);
        face(target.getX() - getX(), target.getZ() - getZ(), TURN_RATE);

        if (groundExtras(level, target, range)) {
            return;
        }

        if (this.attackCooldown <= 0 && draw(groundAttacks(level, target, range), level, target, range)) {
            this.attackCooldown = GROUND_ATTACK_INTERVAL;
            return;
        }

        if (range <= BITE_REACH) {
            return;
        }

        // The walk. Written straight onto the delta rather than pathed, for the reason
        // registerGoals gives: nothing this wide can be navigated anywhere.
        Vec3 towards = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (towards.lengthSqr() > 1.0E-4) {
            Vec3 step = towards.normalize().scale(groundSpeed());
            setDeltaMovement(step.x, getDeltaMovement().y, step.z);
        }
    }

    /**
     * How far it walks in a tick, which is {@link #GROUND_SPEED} scaled by whatever
     * {@code MOVEMENT_SPEED} currently says against the base it was tuned at.
     * <p>
     * The walk is written straight onto the delta rather than pathed, so without this the attribute
     * would mean nothing at all and the second phase's Speed 100 would be a status with no effect.
     * The cap is what makes reading the attribute safe: at twenty-one times the base it would cross
     * four and a half blocks a tick, which is faster than the fight can be followed.
     */
    protected double groundSpeed() {
        double ratio = getAttributeValue(Attributes.MOVEMENT_SPEED) / BASE_MOVEMENT_SPEED;
        return Math.min(GROUND_SPEED * ratio, MAX_GROUND_SPEED);
    }

    /** The bite: vanilla's own melee, so {@code ATTACK_DAMAGE} and every enchantment effect apply. */
    private void bite(ServerLevel level, LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        doHurtTarget(target);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_HURT, SoundSource.HOSTILE, 6.0F, 0.6F);
    }

    /** The run-up: thrown at the target rather than walked, and it takes the world with it. */
    private void startRush(ServerLevel level, LivingEntity target) {
        Vec3 towards = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (towards.lengthSqr() < 1.0E-4) {
            return;
        }

        Vec3 aim = towards.normalize().scale(RUSH_SPEED);
        setDeltaMovement(aim.x, 0.1, aim.z);
        this.hasImpulse = true;
        this.rushing = RUSH_TICKS;

        // Now rather than next tick, for the same reason the dive turns them off now: the first
        // tick of a rush is the one that starts inside whatever it was standing against.
        setPhasing(true);

        face(aim.x, aim.z, TURN_RATE);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 10.0F, 1.2F);
    }

    /**
     * One tick of a rush. The speed is put back on every tick - drag would eat it before it arrived
     * - the corridor ahead is taken out of the world, and the hit is a <em>swept</em> test rather
     * than a bounding box overlap, because at this speed it covers a block a tick and would
     * otherwise step over whoever it was aimed at.
     */
    private void rushStep(ServerLevel level) {
        this.rushing--;

        Vec3 movement = getDeltaMovement();
        Vec3 flat = movement.multiply(1.0, 0.0, 1.0);
        if (flat.lengthSqr() > 1.0E-4) {
            Vec3 held = flat.normalize().scale(RUSH_SPEED);
            setDeltaMovement(held.x, movement.y, held.z);
        }

        Vec3 from = new Vec3(this.xo, this.yo, this.zo);
        Vec3 to = position();
        plough(level, to);

        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class,
                Sweep.bounds(from, to, RUSH_WIDTH).minmax(getBoundingBox()),
                entity -> isEnemy(entity) && Sweep.caught(entity, from, to, RUSH_WIDTH));

        if (caught.isEmpty()) {
            return;
        }

        for (LivingEntity entity : caught) {
            entity.hurt(damageSources().mobAttack(this), RUSH_DAMAGE);
            shove(entity, 1.4, 0.6);
        }

        // A rush is spent on whatever it connected with; it does not carry on through the far side.
        this.rushing = 0;
        setPhasing(false);
        setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.6F);
    }

    /**
     * Takes the corridor it is standing in out of the world.
     * <p>
     * What may be taken is read off the block rather than listed: unbreakable is
     * {@code getDestroySpeed} below zero, which is bedrock, the barrier and every other mod's
     * version of them; a container is anything with a block entity, so a rush through somebody's
     * base does not eat what was in it; and this mod's own hardened stone is left alone from
     * {@code HARDENED_LEVEL} up, which is the same line an explosion cannot cross - so an arena
     * built out of it is an arena that survives the fight. Vanilla's {@code #dragon_immune} is
     * honoured too, since this is a dragon.
     */
    private void plough(ServerLevel level, Vec3 centre) {
        BlockPos origin = BlockPos.containing(centre);
        boolean took = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -RUSH_PLOUGH_WIDTH; dx <= RUSH_PLOUGH_WIDTH; dx++) {
            for (int dz = -RUSH_PLOUGH_WIDTH; dz <= RUSH_PLOUGH_WIDTH; dz++) {
                for (int dy = 0; dy < RUSH_PLOUGH_HEIGHT; dy++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (isSpared(level, pos, state)) {
                        continue;
                    }

                    took = level.removeBlock(pos, false) || took;
                }
            }
        }

        if (took) {
            // 2008 is the puff of dust vanilla's own dragon leaves where it has broken through.
            level.levelEvent(2008, origin, 0);
        }
    }

    /** Whether a block survives being ploughed through. See {@link #plough}. */
    private static boolean isSpared(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(BlockTags.DRAGON_IMMUNE) || state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0.0F) {
            return true;
        }

        Integer compression = ModBlocks.levelOf(state.getBlock());
        return compression != null && compression >= ModBlocks.HARDENED_LEVEL;
    }

    /**
     * The tail. Everything within reach is hit and thrown, the world inside that reach is emptied a
     * layer at a time, and the swipe carries it back into the air.
     * <p>
     * The digging is a {@link DeferredFill} crater rather than a loop here: a sphere of radius eight
     * is a couple of thousand block writes, and a layer a tick turns that into the sweep visibly
     * travelling outwards - which is what a tail looks like anyway.
     */
    private void tailSwipe(ServerLevel level) {
        for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(TAIL_RANGE), this::isEnemy)) {
            caught.hurt(damageSources().mobAttack(this), TAIL_DAMAGE);
            shove(caught, TAIL_KNOCKBACK, 0.8);
        }

        DeferredFill.queueCrater(level, position(), TAIL_RADIUS);

        setFlying(true);
        setDeltaMovement(getDeltaMovement().x, TAKEOFF_LIFT, getDeltaMovement().z);
        this.hasImpulse = true;

        level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.0, getZ(),
                40, TAIL_RANGE / 2.0, 0.5, TAIL_RANGE / 2.0, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 10.0F, 0.5F);
    }

    /**
     * Throws something away from the dragon. The mark matters as much as the push: a delta written
     * on the server only reaches a player's own client when the tracker is told the velocity
     * changed, and their client is what actually moves them.
     */
    private void shove(LivingEntity entity, double strength, double lift) {
        Vec3 away = entity.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            return;
        }

        Vec3 push = away.normalize().scale(strength);
        entity.push(push.x, lift, push.z);
        entity.hurtMarked = true;
    }

    /* THE FINALE */

    /**
     * The end of the first phase: four phrases, one at a time, and then the second.
     * <p>
     * It is {@code tickDeath} rather than {@code die} because the phrases need ticks - vanilla's own
     * death is twenty of them and this one is a hundred and twenty, which is the four titles plus a
     * beat. The loot has already dropped by the time this starts, and the corpse is removed by the
     * last tick of it rather than by vanilla's twentieth.
     */
    @Override
    protected void tickDeath() {
        this.deathTime++;

        if (!(level() instanceof ServerLevel level)) {
            return;
        }

        // One phrase per PHRASE_TICKS, sent on the first tick of each so it is sent exactly once.
        int elapsed = this.deathTime - 1;
        int phrase = elapsed / PHRASE_TICKS;
        if (phrase < deathPhrases() && elapsed % PHRASE_TICKS == 0) {
            flash(level, phrase);
        }

        if (this.deathTime % 5 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2.0, getZ(),
                    1, 4.0, 2.0, 4.0, 0.0);
        }

        if (this.deathTime >= deathPhrases() * PHRASE_TICKS + FINALE_TAIL && !isRemoved()) {
            summonSecondPhase(level);
            level.broadcastEntityEvent(this, (byte) 60);
            remove(Entity.RemovalReason.KILLED);
        }
    }

    /** One phrase, across the screen of everyone close enough to have been in the fight. */
    private void flash(ServerLevel level, int phrase) {
        Component text = Component.translatable(
                "entity." + UnnecessarilyCompressedCobblestone.MOD_ID + ".compressed_dragon.death." + (phrase + 1));

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(FINALE_RADIUS))) {
            // Held for exactly as long as the phrase is due, so the four read as one sentence being
            // spoken rather than as four titles overlapping.
            player.connection.send(new ClientboundSetTitlesAnimationPacket(2, PHRASE_TICKS - 4, 2));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            player.connection.send(new ClientboundSetTitleTextPacket(text));
        }

        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.HOSTILE,
                10.0F, 0.5F + phrase * 0.15F);
    }

    /**
     * How many phrases this dragon's death spells out. Four for the first phase, whose death is the
     * handover; none for a dragon that is the end of the fight rather than the middle of it.
     */
    protected int deathPhrases() {
        return PHRASES;
    }

    /** What arrives out of the burst, or null for a dragon that is the last of them. */
    @Nullable
    protected EntityType<? extends Mob> nextPhase() {
        return ModEntities.COMPRESSED_DRAGON_TIER_2.get();
    }

    /**
     * The next phase, which is a mob of its own rather than another life of this one - so its boss
     * bar, its health and every one of its attacks start rather than being reset.
     * <p>
     * It is put down where this one fell, facing the same way, and through {@code finalizeSpawn} the
     * way everything summoned in this mod is.
     */
    private void summonSecondPhase(ServerLevel level) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2.0, getZ(),
                8, 6.0, 3.0, 6.0, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 10.0F, 0.5F);

        EntityType<? extends Mob> next = nextPhase();
        if (next == null) {
            return;
        }

        Mob arrival = next.create(level);
        if (arrival == null) {
            return;
        }

        arrival.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        arrival.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.TRIGGERED, null);
        level.addFreshEntity(arrival);
    }

    /* THE BOSS BAR */

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

    /* SAVING */

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(TAG_SLAMMING, this.slamming);
        compound.putDouble(TAG_SLAM_FROM, this.slamFrom);
        compound.putInt(TAG_RUSHING, this.rushing);
        compound.putInt(TAG_ATTACK_COOLDOWN, this.attackCooldown);
        compound.putInt(TAG_SINCE_SWIPE, this.sinceSwipe);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.slamming = compound.getBoolean(TAG_SLAMMING);
        this.slamFrom = compound.getDouble(TAG_SLAM_FROM);
        this.slamTicks = SLAM_TIMEOUT;
        this.rushing = compound.getInt(TAG_RUSHING);
        this.attackCooldown = compound.getInt(TAG_ATTACK_COOLDOWN);
        this.sinceSwipe = compound.getInt(TAG_SINCE_SWIPE);

        if (this.slamming) {
            setNoGravity(false);
        }
    }
}
