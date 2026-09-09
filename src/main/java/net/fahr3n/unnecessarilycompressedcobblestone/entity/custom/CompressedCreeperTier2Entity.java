package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * The Compressed Creeper Tier 2. The first one's whole pattern - blink in, commit to a spot, wind up,
 * go off, never die of it - at four times the blast, twice the pace and eight times the health, with
 * one thing of its own: it calls TNT down around whoever it is fighting.
 * <p>
 * The three numbers move together and that is the fight. A blast four times as wide and four times
 * as hard is not survivable in the way the first one's is - there is no gear in this mod that stands
 * in it - so the answer stops being "take the hit in good armour" and becomes "be somewhere else",
 * which is what the first tier was teaching. The wind-up being half as long is what makes that hard
 * rather than routine: {@link #CHARGE_TICKS} of warning is a second and a half to read where it
 * landed and clear ten blocks, and Jump Boost or the Zoom TNT are suddenly worth carrying.
 * <p>
 * The summon is what stops the answer being a single memorised distance. TNT drawn from
 * {@code #ucc:tnt} lands around the target on its own clock, so the ground a player was going to
 * retreat onto is not reliably there - and because the draw is the tag rather than a list, a
 * datapack that adds another mod's TNT adds it to this boss's repertoire too.
 * <p>
 * It keeps the first tier's immunity to explosions, so its own summons cannot be farmed into it.
 */
public class CompressedCreeperTier2Entity extends CompressedCreeperEntity {
    public static final float MAX_HEALTH = 2000.0F;

    /**
     * Armour at the attribute's own ceiling of 30, and eighteen toughness under it.
     * <p>
     * The toughness is the real change from the first tier, which deliberately runs 30 and zero.
     * Vanilla's {@code armour - damage / (2 + toughness / 4)} means a big hit chews through armour
     * that has no toughness behind it, which is what made the first one four times as vulnerable to
     * one heavy smash as to four light ones. At eighteen that term is divided by 6.5 instead of 2,
     * so the mace build loses most of its edge and the fight stops having one correct weapon.
     */
    public static final double ARMOR = 30.0;
    public static final double ARMOR_TOUGHNESS = 18.0;

    /** Four times the first tier's blast, in reach and in what it does where it reaches. */
    private static final float SIZE_MULTIPLIER = 4.0F;

    /** Half the first tier's wind-up: a second and a half between arriving and going off. */
    private static final int CHARGE_TICKS = 30;

    /** Ticks between summons, how many arrive, and how far out around the target they land. */
    private static final int SUMMON_INTERVAL = 200;
    private static final int SUMMON_COUNT = 3;
    private static final double SUMMON_SPREAD = 7.0;

    /** How far it will reach to summon at all; past this there is nothing worth burying. */
    private static final double SUMMON_RANGE = 32.0;

    private static final String TAG_SUMMON_COOLDOWN = "summon_cooldown";

    private int summonCooldown = SUMMON_INTERVAL;

    public CompressedCreeperTier2Entity(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 500;
        this.bossEvent.setName(Component.translatable(
                "entity.unnecessarilycompressedcobblestone.compressed_creeper_tier_2"));
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        setPersistenceRequired();
    }

    /** The first tier's numbers with the health at two thousand and real toughness behind the plate. */
    public static AttributeSupplier.Builder createAttributes() {
        return CompressedCreeperEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS);
    }

    /* THE THREE DIALS */

    @Override
    protected int chargeTicks() {
        return CHARGE_TICKS;
    }

    @Override
    protected float explosionRadius() {
        return super.explosionRadius() * SIZE_MULTIPLIER;
    }

    @Override
    protected float damageMultiplier() {
        return super.damageMultiplier() * SIZE_MULTIPLIER;
    }

    /* THE SUMMON */

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > SUMMON_RANGE * SUMMON_RANGE) {
            return;
        }

        if (this.summonCooldown-- <= 0) {
            this.summonCooldown = SUMMON_INTERVAL;
            summonTnt(serverLevel, target);
        }
    }

    /**
     * Lights {@link #SUMMON_COUNT} TNT around the target, each one drawn afresh from
     * {@code #ucc:tnt}.
     * <p>
     * The draw goes through {@link CompressedTntEffect#lightRandom}, which is the same code the
     * Random TNT itself uses - so whatever a datapack has added to that tag is in this boss's
     * repertoire on the same day, and the generic path for lighting another mod's TNT (set the
     * block, catch fire, put the block back) does not exist twice.
     * <p>
     * They are lit on the ground rather than dropped from overhead. A summon that arrived falling
     * would land wherever the target had moved to by the time it got there, which is the first
     * tier's blast again; lit where they stand, they are three fuses burning in known places, and
     * reading them is the same skill the boss's own wind-up asks for.
     */
    private void summonTnt(ServerLevel level, LivingEntity target) {
        boolean lit = false;

        for (int i = 0; i < SUMMON_COUNT; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = level.random.nextDouble() * SUMMON_SPREAD;
            BlockPos pos = BlockPos.containing(
                    target.getX() + Math.cos(angle) * radius,
                    target.getY(),
                    target.getZ() + Math.sin(angle) * radius);

            // A TNT lit inside a mountain is a TNT nobody sees; anything solid is passed over rather
            // than dug out, the same rule the geyser follows.
            if (!level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced()) {
                continue;
            }

            lit |= CompressedTntEffect.lightRandom(level, pos, this);
        }

        if (lit) {
            level.playSound(null, blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE,
                    3.0F, 0.4F);
        }
    }

    /** It will not fight its own kind, whichever tier that is. */
    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_CREEPER_TIER_2.get()
                && type != ModEntities.COMPRESSED_CREEPER.get()
                && super.canAttackType(type);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_SUMMON_COOLDOWN, this.summonCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.summonCooldown = compound.getInt(TAG_SUMMON_COOLDOWN);
    }
}
