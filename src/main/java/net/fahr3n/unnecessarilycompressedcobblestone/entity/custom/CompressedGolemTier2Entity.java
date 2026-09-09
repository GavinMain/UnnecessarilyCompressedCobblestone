package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Golem Tier 2. Everything the first one is, an order of magnitude harder, and with
 * two things of its own: it heals as long as it has ground under it, and it pounds.
 * <p>
 * The healing is the fight. A thousand health behind netherite plate and Resistance would already be
 * a long afternoon; Regeneration V on top of it outruns most damage a player can put out, and there
 * is exactly one thing to do about it - take its footing away. Anything that puts water where it is
 * standing turns the regeneration off, which is what the Pool TNT is for, and the golem is heavy
 * enough that it will not simply walk out of a pool it is fighting in.
 * <p>
 * Its Resistance stops at IV, and that is a real ceiling rather than a taste: vanilla's reduction is
 * {@code (amplifier + 1) * 5} out of 25, so Resistance V is the whole hit and a boss nothing can
 * hurt is not a boss. Four levels is 80%, which is as deep as the effect goes while still leaving
 * something to fight.
 */
public class CompressedGolemTier2Entity extends CompressedGolemEntity {
    public static final float MAX_HEALTH = 1000.0F;

    /** A full set of netherite: twenty armour and twelve toughness. */
    public static final double ARMOR = 20.0;
    public static final double ARMOR_TOUGHNESS = 12.0;

    /** Resistance IV: four fifths off every hit, and the deepest the effect goes without closing. */
    private static final int RESISTANCE_AMPLIFIER = 3;

    /** Regeneration V, for as long as it has ground under it and no water around it. */
    private static final int REGENERATION_AMPLIFIER = 4;

    /**
     * Both effects are re-applied on a short duration rather than held on a long one, so either
     * lapses on its own the moment the condition behind it does - the same shape as the armour
     * sets' full-set bonus.
     */
    private static final int EFFECT_DURATION = 40;

    /** Ticks between ground pounds, and how far one reaches. */
    private static final int GROUND_POUND_INTERVAL = 100;
    private static final double GROUND_POUND_RADIUS = 6.0;
    private static final float GROUND_POUND_DAMAGE = 30.0F;

    /** Ticks between jump pounds. Rare, because the answer to one is to not be under it. */
    private static final int JUMP_POUND_INTERVAL = 300;

    /** How far a jump pound reaches, and what it hits for. */
    private static final double JUMP_POUND_RADIUS = 8.0;

    /**
     * Ten hearts through a full set of netherite with Protection IV on every piece, worked back
     * through vanilla's two reductions the same way the Compressed Spirit's is. Unarmoured, it is
     * fifty.
     */
    public static final float JUMP_POUND_DAMAGE = 100.0F;

    /** How high it goes, and how far the crater it lands in reaches. */
    private static final double JUMP_POUND_LIFT = 1.6;
    private static final int JUMP_POUND_CRATER_RADIUS = 4;
    private static final int JUMP_POUND_CRATER_DEPTH = 2;

    /** How long it may hang in the air before the landing is called anyway. */
    private static final int JUMP_POUND_TIMEOUT = 100;

    private static final String TAG_GROUND_POUND = "ground_pound_cooldown";
    private static final String TAG_JUMP_POUND = "jump_pound_cooldown";

    private int groundPoundCooldown = GROUND_POUND_INTERVAL;
    private int jumpPoundCooldown = JUMP_POUND_INTERVAL;

    /** Ticks left of a jump that is in the air, or 0 when it is not jumping. */
    private int airborne;

    public CompressedGolemTier2Entity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 500;
        this.bossEvent.setName(Component.translatable(
                "entity.unnecessarilycompressedcobblestone.compressed_golem_tier_2"));
        this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        setPersistenceRequired();
    }

    /**
     * The first golem's numbers with the health at a thousand and a full set of netherite's
     * protection. The armour is well clear of the 100 the armour note warns about, so the ordinary
     * vanilla curve applies and nothing here can produce a reduction over 100%.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return CompressedGolemEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.ATTACK_DAMAGE, 40.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    /** It will not fight its own kind, whichever tier that is. */
    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_GOLEM_TIER_2.get() && super.canAttackType(type);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, RESISTANCE_AMPLIFIER,
                false, false, true));

        if (standsOnGround()) {
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, REGENERATION_AMPLIFIER,
                    false, false, true));
        }

        if (this.airborne > 0) {
            // The landing, or the timeout that covers a jump that found nowhere to land.
            if (--this.airborne <= 0 || onGround()) {
                this.airborne = 0;
                jumpPound(serverLevel);
            }

            return;
        }

        LivingEntity target = getTarget();
        if (target == null) {
            return;
        }

        if (this.jumpPoundCooldown-- <= 0 && onGround()) {
            this.jumpPoundCooldown = JUMP_POUND_INTERVAL;
            leap(target);
            return;
        }

        if (this.groundPoundCooldown-- <= 0 && distanceToSqr(target) <= GROUND_POUND_RADIUS * GROUND_POUND_RADIUS) {
            this.groundPoundCooldown = GROUND_POUND_INTERVAL;
            groundPound(serverLevel);
        }
    }

    /**
     * Whether there is ground under it, which is the whole of its healing.
     * <p>
     * Two things have to be true and the second is the one that matters: the block below has to be
     * something it counts as ground, and it must not be standing in water. A pool laid over dirt
     * leaves the dirt exactly where it was - it is the water around its feet that stops the healing,
     * which is what makes flooding the fight an answer rather than a landscaping job.
     */
    public boolean standsOnGround() {
        if (isInWaterOrBubble()) {
            return false;
        }

        BlockState below = level().getBlockState(blockPosition().below());
        return below.is(ModTags.Blocks.GOLEM_GROUND);
    }

    /** Both hands into the ground: everything close by is hit, and nothing is broken. */
    private void groundPound(ServerLevel level) {
        hurtAround(level, GROUND_POUND_RADIUS, GROUND_POUND_DAMAGE);

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        level.getBlockState(blockPosition().below())),
                getX(), getY(), getZ(), 120, GROUND_POUND_RADIUS / 2.0, 0.1, GROUND_POUND_RADIUS / 2.0, 0.4);
        level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 4.0F, 0.6F);
    }

    /** Straight up, and towards whatever it was fighting, so it comes down on top of it. */
    private void leap(LivingEntity target) {
        Vec3 towards = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        Vec3 lead = towards.lengthSqr() < 1.0E-4 ? Vec3.ZERO : towards.normalize().scale(0.35);

        setDeltaMovement(lead.x, JUMP_POUND_LIFT, lead.z);
        this.airborne = JUMP_POUND_TIMEOUT;
        this.hasImpulse = true;

        playSound(SoundEvents.IRON_GOLEM_ATTACK, 4.0F, 0.5F);
    }

    /**
     * The landing. Everything within {@link #JUMP_POUND_RADIUS} takes {@link #JUMP_POUND_DAMAGE},
     * and the ground it lands on is dug out - which is as much a favour as a threat, since the
     * crater is where the water goes if anyone thinks to put it there.
     */
    private void jumpPound(ServerLevel level) {
        hurtAround(level, JUMP_POUND_RADIUS, JUMP_POUND_DAMAGE);

        BlockPos centre = blockPosition();
        for (int x = -JUMP_POUND_CRATER_RADIUS; x <= JUMP_POUND_CRATER_RADIUS; x++) {
            for (int z = -JUMP_POUND_CRATER_RADIUS; z <= JUMP_POUND_CRATER_RADIUS; z++) {
                if (x * x + z * z > JUMP_POUND_CRATER_RADIUS * JUMP_POUND_CRATER_RADIUS) {
                    continue;
                }

                for (int y = 0; y > -JUMP_POUND_CRATER_DEPTH; y--) {
                    BlockPos pos = centre.offset(x, y, z);
                    // Anything unbreakable is left alone: the hardened levels are unbreakable to
                    // every other force in the game and this is not the exception.
                    if (level.isLoaded(pos) && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0.0F) {
                        level.destroyBlock(pos, false);
                    }
                }
            }
        }

        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 12,
                JUMP_POUND_RADIUS / 3.0, 0.5, JUMP_POUND_RADIUS / 3.0, 0.0);
        level.playSound(null, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.5F);
    }

    /**
     * One pound's worth of damage to everything around it. Armour stands and its own kind are left
     * out, the same two exceptions its targeting makes, and the damage is dealt as a mob attack so
     * armour, Protection and Resistance all apply to it.
     */
    private void hurtAround(ServerLevel level, double radius, float damage) {
        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius),
                entity -> entity != this && entity.isAlive() && !(entity instanceof ArmorStand)
                        && !(entity instanceof CompressedGolemEntity) && !isAlliedTo(entity));

        for (LivingEntity entity : caught) {
            entity.hurt(damageSources().mobAttack(this), damage);

            // Thrown outwards from the pound rather than backwards from the golem's swing, which is
            // what makes a landing read as a shockwave.
            Vec3 away = entity.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4) {
                entity.push(away.normalize().x * 0.6, 0.4, away.normalize().z * 0.6);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_GROUND_POUND, this.groundPoundCooldown);
        compound.putInt(TAG_JUMP_POUND, this.jumpPoundCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.groundPoundCooldown = compound.getInt(TAG_GROUND_POUND);
        this.jumpPoundCooldown = compound.getInt(TAG_JUMP_POUND);
    }
}
