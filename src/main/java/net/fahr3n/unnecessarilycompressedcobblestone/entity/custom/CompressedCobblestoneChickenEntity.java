package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A chicken that has been through the compressor. It is a chicken in every respect that matters -
 * seeds still lure it and still breed it, it still flaps down from a fall unhurt, and it still
 * carries a baby zombie as a jockey - and the one thing it does differently is what it leaves on
 * the floor: a level {@link #LAID_LEVEL} block instead of an egg.
 */
public class CompressedCobblestoneChickenEntity extends Chicken {
    /** What it lays. */
    public static final int LAID_LEVEL = 34;

    /** Vanilla's laying interval: somewhere between five and ten minutes. */
    private static final int LAY_INTERVAL = 6000;

    /** NBT key for {@link #conjured}. */
    private static final String CONJURED_TAG = "Conjured";

    /**
     * Whether this one was called up rather than bred, hatched or placed.
     * <p>
     * It is the same guard the silverfish's brood mark is, and for the same reason: the Compressed
     * Chicken Boss calls ten of these into the air every half minute of its second half, and a
     * conjured chicken that laid blocks and dropped meat would make that ability a printer. One
     * lays nothing and drops nothing; a chicken that was paid for is untouched.
     */
    private boolean conjured;

    public CompressedCobblestoneChickenEntity(EntityType<? extends Chicken> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Lays a block where a chicken would lay an egg.
     * <p>
     * {@link Chicken#aiStep()} runs the countdown and drops the egg itself, and it is not split into
     * anything overridable, so the clock is read one tick early here and wound straight back on. By
     * the time the inherited code looks, its own countdown is nowhere near zero and no egg is laid;
     * the block goes down afterwards, on exactly the tick the egg would have appeared.
     */
    @Override
    public void aiStep() {
        boolean laying = !this.level().isClientSide() && this.isAlive() && !this.isBaby()
                && !this.conjured && !this.isChickenJockey() && this.eggTime <= 1;
        if (laying) {
            this.eggTime = this.random.nextInt(LAY_INTERVAL) + LAY_INTERVAL;
        }

        super.aiStep();

        if (laying) {
            playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            spawnAtLocation(ModBlocks.byLevel(LAID_LEVEL).get());
            gameEvent(GameEvent.ENTITY_PLACE);
        }
    }

    /**
     * Never hurt by a landing. A vanilla chicken has no such immunity - it only descends slowly, and
     * dropped from any real height it dies like anything else - but these arrive by being thrown off
     * a TNT twenty-five blocks up, so the flap has to actually mean something.
     */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /**
     * Marks this one as summoned, and gives it the health the summoner wants it to have.
     * <p>
     * The health is not decoration: what the boss does with these is hurt them on a cadence so the
     * infestation they carry hatches, and how much health they have is exactly how long that goes
     * on for.
     */
    public void setConjured(float maxHealth) {
        this.conjured = true;
        setPersistenceRequired();

        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(maxHealth);
        }

        setHealth(getMaxHealth());
    }

    @Override
    protected boolean shouldDropLoot() {
        return !this.conjured && super.shouldDropLoot();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(CONJURED_TAG, this.conjured);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.conjured = compound.getBoolean(CONJURED_TAG);
    }

    /** Two of these breed into another of these, not into a plain chicken. */
    @Nullable
    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get().create(level);
    }
}
