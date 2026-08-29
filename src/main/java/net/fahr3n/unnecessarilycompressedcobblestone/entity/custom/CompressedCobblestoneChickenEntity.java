package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
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
                && !this.isChickenJockey() && this.eggTime <= 1;
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

    /** Two of these breed into another of these, not into a plain chicken. */
    @Nullable
    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get().create(level);
    }
}
