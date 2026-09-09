package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.fahr3n.unnecessarilycompressedcobblestone.util.SelectiveImmunity;

/**
 * The Compressed Snow Golem. A snow golem in everything the renderer draws and in how it fights -
 * it strolls, it looks around, it throws snowballs - and a boss because of one rule and two
 * abilities.
 * <p>
 * <b>The rule is that nothing but a fall costs it anything.</b> Not "very little": nothing. Swords,
 * arrows, lava and lightning all land on it and all come to nothing, through
 * {@link #refusesDamageFrom}, which spares exactly {@code #minecraft:is_fall}. That is a stronger
 * statement than any amount of armour and, unlike armour, it has no ceiling to run into and no
 * arithmetic to go wrong at the top - see the armour note in CLAUDE.md for why the other route
 * cannot get here. It also means the fight has exactly one answer, and the answer is the Uppercut
 * engraving: hit it with an engraved melee weapon, watch it go a hundred blocks up, and let the
 * ground do the work.
 * <p>
 * The rule is a {@code SelectiveImmunity} and emphatically not an {@code isInvulnerableTo}, and that
 * is the difference between a fight and a wall: the second makes {@code hurt} return before the blow
 * exists, so the Uppercut - which is hung off a landed hit - could never fire on the one boss it was
 * written for. What is left over from that is this mod's true damage, which is past the rule by
 * design: a boss refusing kinds of attack is making a statement about attacks, and true damage is
 * the thing that is not one.
 * <p>
 * <b>The two abilities are both answers to that.</b> The slam is what it does with the fall the
 * player just gave it - it fires itself at the ground and everything standing near the landing takes
 * the same rising-with-distance damage the Smash engraving deals, and the fall costs the golem
 * nothing. Forty seconds is the whole of the counterplay: most uppercuts land, and roughly one in
 * five is answered. The dome is a shell of tier 205 stone thrown up around itself, which is
 * unbreakable to every ordinary tool in the game - and is exactly the tier the Exploding Sword
 * engraving is built to clear, which is the other half of the fight and the reason that engraving
 * names the number it does.
 */
public class CompressedSnowGolemEntity extends SnowGolem implements SelectiveImmunity {
    public static final float MAX_HEALTH = 1000.0F;

    /** How far it looks for something to fight, which is also how far it can throw. */
    private static final double SEARCH_RADIUS = 32.0;

    /** How hard it drives itself at the ground, in blocks per tick. The Smash engraving's figure. */
    private static final double SLAM_SPEED = 3.0;

    /** How far off the ground it has to be before a slam is worth starting. */
    private static final double SLAM_MIN_HEIGHT = 6.0;

    /** Forty seconds. */
    private static final int SLAM_COOLDOWN = 800;

    /**
     * The Smash engraving's damage curve, repeated rather than shared: a flat base plus a straight
     * line in how far it fell, with no ceiling on it. It is safe uncapped for the reason that
     * engraving's note gives - the input is a distance somebody had to be thrown, and it approaches
     * no ratio.
     */
    private static final float SLAM_BASE_DAMAGE = 5.0F;
    private static final float SLAM_DAMAGE_PER_BLOCK = 10.0F;

    /** The radius <em>is</em> bounded, because it is the size of an entity query and not a reward. */
    private static final double SLAM_MIN_RADIUS = 4.0;
    private static final double SLAM_MAX_RADIUS = 16.0;
    private static final double SLAM_RADIUS_PER_BLOCK = 0.25;

    /** Ten seconds, and how big the shell is. */
    private static final int DOME_COOLDOWN = 200;
    private static final int DOME_RADIUS = 4;

    /** What the dome is built of - and what the Exploding Sword engraving is cut to reach. */
    public static final int DOME_LEVEL = 205;

    private int slamCooldown;
    private int domeCooldown;
    private boolean slamming;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_snow_golem"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    public CompressedSnowGolemEntity(EntityType<? extends SnowGolem> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1000;
        setPersistenceRequired();
    }

    /**
     * A thousand health and nothing else raised. There is deliberately no armour on it: armour would
     * be a second answer to damage it is already immune to, and the invulnerability note in
     * CLAUDE.md is explicit that "only X hurts it" should be paired with no armour rather than with
     * more - the point of the rule is that one answer exists, not that the answer is slow.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return SnowGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS);
    }

    /**
     * A snow golem's own goals with this mod's boss targeting in place of vanilla's: everything
     * alive and in reach rather than only things that implement {@code Enemy}, players first, and
     * never its own kind or the things that only stand there.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.25, 20, (float) SEARCH_RADIUS));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0, 1.0000001E-5F));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> BossTargeting.isValid(this, target, SEARCH_RADIUS,
                        entity -> entity instanceof CompressedSnowGolemEntity)));
    }

    /**
     * Only a fall costs it anything.
     * <p>
     * This is {@link SelectiveImmunity} rather than {@code isInvulnerableTo}, and on this boss above
     * all the difference is the fight. {@code isInvulnerableTo} makes {@code hurt} return before the
     * blow exists, so no {@code LivingDamageEvent} fires - and the Uppercut engraving, which is the
     * one intended answer to this golem, is hung off exactly that event. Written the other way the
     * rule cancelled its own counterplay: a sword passed through it and nothing was ever thrown.
     * Now the sword lands, costs the golem nothing, and the engraving does the rest.
     * <p>
     * {@code /kill}, the void and this mod's true damage are all past this by {@link
     * SelectiveImmunity#refusable}, which is where the argument for each of the three is.
     */
    @Override
    public boolean refusesDamageFrom(DamageSource source) {
        return SelectiveImmunity.refusable(source) && !source.is(DamageTypeTags.IS_FALL);
    }

    /** A hundred a hit, thrown the way a snow golem throws. */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        CompressedSnowballEntity snowball = new CompressedSnowballEntity(level(), this);
        double aimY = target.getEyeY() - 1.1F;
        double dx = target.getX() - getX();
        double dy = aimY - snowball.getY();
        double dz = target.getZ() - getZ();
        double arc = Math.sqrt(dx * dx + dz * dz) * 0.2;

        snowball.shoot(dx, dy + arc, dz, 1.6F, 12.0F);
        playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F,
                0.4F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(snowball);
    }

    /** The two cooldowns, and the two things they let it do. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }

        if (this.domeCooldown > 0) {
            this.domeCooldown--;
        }

        // A slam that ended in water, in lava or on a boat never reaches causeFallDamage, so the
        // mark has to be dropped here or the landing would go off later at the wrong moment. Same
        // hole the Smash engraving has a tick handler for.
        if (this.slamming && (onGround() || isInWater() || isInLava())) {
            this.slamming = false;
        }

        if (getTarget() == null) {
            return;
        }

        // Only on the way down. Started on the way up it would fire the golem back at the ground
        // from four blocks and waste the whole throw - and the throw is the only thing that hurts
        // it, so a slam that went off at the bottom of the arc would make it unkillable.
        if (this.slamCooldown == 0 && !this.slamming && !onGround()
                && getDeltaMovement().y < 0.0 && heightAboveGround() >= SLAM_MIN_HEIGHT) {
            startSlam();
        }

        if (this.domeCooldown == 0 && onGround()) {
            raiseDome();
        }
    }

    /**
     * Fires itself at the ground.
     * <p>
     * Straight down and nothing else, exactly as the Smash engraving does it: whatever sideways
     * speed it had would otherwise carry the landing away from whoever it was aimed at - and being
     * thrown a hundred blocks by an Uppercut leaves a great deal of sideways speed.
     */
    private void startSlam() {
        this.slamming = true;
        this.slamCooldown = SLAM_COOLDOWN;

        setDeltaMovement(0.0, -SLAM_SPEED, 0.0);
        this.hasImpulse = true;

        level().playSound(null, blockPosition(), SoundEvents.WIND_CHARGE_THROW,
                SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    /**
     * The landing.
     * <p>
     * {@code causeFallDamage} is where this belongs rather than a tick handler, because its argument
     * is the distance actually fallen at the moment of the landing - so a slam cut short by a ledge
     * is worth the ledge, which is the rule the Smash engraving's note gives. Returning false
     * without calling {@code super} is what spares the golem its own fall damage, and that is the
     * whole meaning of the forty second cooldown: while it is down, a fall is a fall and the golem
     * takes it, which is the only way it can be hurt at all.
     */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (!this.slamming) {
            return super.causeFallDamage(distance, multiplier, source);
        }

        this.slamming = false;
        if (level().isClientSide()) {
            return false;
        }

        float damage = SLAM_BASE_DAMAGE + SLAM_DAMAGE_PER_BLOCK * distance;
        double radius = Math.min(SLAM_MIN_RADIUS + distance * SLAM_RADIUS_PER_BLOCK, SLAM_MAX_RADIUS);

        for (LivingEntity caught : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius, radius / 2.0, radius),
                entity -> entity != this && entity.isAlive() && !isAlliedTo(entity)
                        && !(entity instanceof CompressedSnowGolemEntity))) {
            caught.hurt(damageSources().mobAttack(this), damage);

            Vec3 away = caught.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 shove = away.normalize();
                caught.push(shove.x, 0.4, shove.z);
            }
        }

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(),
                    (int) radius * 2, radius / 3.0, 0.2, radius / 3.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(),
                    (int) radius * 8, radius / 2.0, 0.3, radius / 2.0, 0.2);
        }

        level().playSound(null, blockPosition(), SoundEvents.MACE_SMASH_GROUND,
                SoundSource.HOSTILE, 4.0F, 0.6F);

        // The fall is spent on the slam, so the golem takes none of it.
        return false;
    }

    /**
     * The dome: a hollow shell of tier 205 stone thrown up over itself.
     * <p>
     * Only blocks that were going to be replaced anyway are written - air, grass, snow, water - so
     * the dome covers the golem without eating the ground it is standing on or the wall somebody
     * built. The shell is one block thick and open underneath, which is what makes it a dome rather
     * than a tomb, and it is drawn from the golem's feet upwards.
     * <p>
     * Tier 205 is unbreakable to every ordinary tool there is. What clears it is this mod's own
     * pickaxe, slowly, or the Exploding Sword engraving, instantly - that engraving reaches exactly
     * this tier and no deeper, and this is the reason it is written that way.
     */
    private void raiseDome() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }

        this.domeCooldown = DOME_COOLDOWN;

        BlockState shell = ModBlocks.byLevel(DOME_LEVEL).get().defaultBlockState();
        BlockPos feet = blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int inner = (DOME_RADIUS - 1) * (DOME_RADIUS - 1);
        int outer = DOME_RADIUS * DOME_RADIUS;

        for (int x = -DOME_RADIUS; x <= DOME_RADIUS; x++) {
            for (int y = 0; y <= DOME_RADIUS; y++) {
                for (int z = -DOME_RADIUS; z <= DOME_RADIUS; z++) {
                    int distance = x * x + y * y + z * z;
                    if (distance <= inner || distance > outer) {
                        continue;
                    }

                    pos.set(feet.getX() + x, feet.getY() + y, feet.getZ() + z);
                    if (level.isLoaded(pos) && level.getBlockState(pos).canBeReplaced()) {
                        level.setBlock(pos, shell, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }

        level.playSound(null, feet, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 3.0F, 0.6F);
    }

    /** How far it is off whatever is under it, so a slam is only started from somewhere worth it. */
    private double heightAboveGround() {
        BlockPos.MutableBlockPos pos = blockPosition().mutable();
        int floor = level().getMinBuildHeight();

        while (pos.getY() > floor && level().getBlockState(pos).isAir()) {
            pos.move(0, -1, 0);
        }

        return getY() - pos.getY();
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_SNOW_GOLEM.get() && type != EntityType.ARMOR_STAND;
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
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SlamCooldown", this.slamCooldown);
        compound.putInt("DomeCooldown", this.domeCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.slamCooldown = compound.getInt("SlamCooldown");
        this.domeCooldown = compound.getInt("DomeCooldown");
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
