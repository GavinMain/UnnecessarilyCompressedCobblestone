package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedPrimedTntEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedTntEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * TNT that does something other than what vanilla's does. Everything about how it is lit - redstone,
 * fire, a flaming arrow, being caught in another blast - is inherited; only what happens at the end
 * of the fuse changes, so every variant is this one class with a different {@link #effect}.
 */
public class CompressedTntBlock extends TntBlock {
    /** What the primed entity will do when the fuse runs out. */
    private final CompressedTntEffect effect;

    /**
     * Built per instance because the effect is part of what this block is, and {@code simpleCodec}
     * only takes a one-argument constructor. The lambda closes over the parameter rather than the
     * field so it does not read {@code this} while the object is still being built.
     */
    private final MapCodec<TntBlock> codec;

    public CompressedTntBlock(Properties properties, CompressedTntEffect effect) {
        super(properties);
        this.effect = effect;
        this.codec = simpleCodec(blockProperties -> new CompressedTntBlock(blockProperties, effect));
    }

    @Override
    public MapCodec<TntBlock> codec() {
        return this.codec;
    }

    public CompressedTntEffect getEffect() {
        return this.effect;
    }

    /**
     * Every way this block can be set off funnels through here in {@link TntBlock} - redstone, fire,
     * a flaming projectile, breaking an unstable one - so overriding it alone is enough to make all
     * of them spawn the stronger entity. Vanilla's equivalent is a private static method, which is
     * why this repeats the sound and the game event rather than delegating.
     */
    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (level.isClientSide) {
            return;
        }

        CompressedPrimedTntEntity tnt = new CompressedPrimedTntEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                igniter, state, this.effect);
        level.addFreshEntity(tnt);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }

    /**
     * Caught in someone else's blast. The staggered fuse is vanilla's, and it is what keeps a pile
     * of these from all going off on the same tick.
     */
    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide) {
            return;
        }

        CompressedPrimedTntEntity tnt = new CompressedPrimedTntEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                explosion.getIndirectSourceEntity(), defaultBlockState(), this.effect);
        int fuse = tnt.getFuse();
        tnt.setFuse((short) (level.random.nextInt(fuse / 4) + fuse / 8));
        level.addFreshEntity(tnt);
    }
}
