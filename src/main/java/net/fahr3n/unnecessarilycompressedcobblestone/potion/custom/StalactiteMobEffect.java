package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/**
 * The ceiling gives way. Every few seconds a pointed dripstone forms in the air overhead and drops
 * on whoever is carrying the potion.
 * <p>
 * The damage is not dealt here at all: a falling stalactite already hurts what it lands on, and
 * because {@code PointedDripstoneBlock} is {@code Fallable} the game credits it as
 * {@code minecraft:falling_stalactite} without being asked. That is worth knowing before drinking
 * one - unlike most of what a potion does, this damage is <em>not</em> in {@code #bypasses_armor},
 * so armour helps, and a helmet takes the hit and the wear along with the head under it.
 * <p>
 * How much it hurts is therefore how far it fell, which is the one number this sets.
 */
public class StalactiteMobEffect extends MobEffect implements TickingDamage {
    /** How often one forms at level I; each level up halves it. */
    private static final int BASE_INTERVAL = 60;

    /** How far overhead, which is most of how hard it lands. */
    private static final int HEIGHT = 10;

    /** Damage per block fallen, and the most one may ever do. */
    private static final float DAMAGE_PER_BLOCK = 1.0F;
    private static final int MAX_DAMAGE = 30;

    public StalactiteMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }

        // One at level I, two at level II, and so on: a deeper brew is a heavier ceiling.
        for (int stalactite = 0; stalactite <= amplifier; stalactite++) {
            BlockPos pos = entity.blockPosition().offset(
                    level.random.nextInt(3) - 1, HEIGHT, level.random.nextInt(3) - 1);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced()) {
                continue;
            }

            BlockState state = Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                    .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);

            // It has to exist as a block for a moment: FallingBlockEntity.fall takes the block at a
            // position and turns that into the entity, rather than being handed a state.
            level.setBlock(pos, state, Block.UPDATE_INVISIBLE);
            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
            falling.setHurtsEntities(DAMAGE_PER_BLOCK, MAX_DAMAGE);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = BASE_INTERVAL >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    /**
     * One stalactite per level, each worth its whole fall - {@link #HEIGHT} blocks at
     * {@link #DAMAGE_PER_BLOCK}, capped the way the falling block itself is capped.
     * <p>
     * An upper bound, like the detonation's: a stalactite that finds no room overhead never forms
     * and one that lands beside its target hits nothing. The Compression Bomb takes the ceiling
     * away, so the ceiling is what it pays for.
     */
    @Override
    public float damagePerApplication(int amplifier) {
        return Math.min(HEIGHT * DAMAGE_PER_BLOCK, MAX_DAMAGE) * (amplifier + 1);
    }
}
