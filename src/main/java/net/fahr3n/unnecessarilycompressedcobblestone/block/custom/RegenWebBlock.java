package net.fahr3n.unnecessarilycompressedcobblestone.block.custom;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cobweb that mends whatever is caught in it: it holds an entity exactly as an ordinary web does,
 * and puts Instant Health on it every tick it is held.
 * <p>
 * The healing is the vanilla effect rather than a call to {@link LivingEntity#heal}, and that is the
 * whole difference between the two. Instant Health is an {@code InstantenousMobEffect}, so it lands
 * the moment it is added and needs no duration; going through it means everything that reads a
 * potion effect reads this too - the particles, and above all the fact that Instant Health
 * <em>hurts</em> the undead. A web that heals the living and burns the undead is a far more
 * interesting block than one that heals everything, and none of that had to be written here.
 * <p>
 * {@code WebBlock.codec()} is declared {@code MapCodec<WebBlock>}, which a {@code MapCodec} of a
 * subclass cannot override, so the field is declared at the supertype and {@code simpleCodec} infers
 * the rest from the constructor - the same shape as {@code CompressedTntBlock}.
 */
public class RegenWebBlock extends WebBlock {
    private static final MapCodec<WebBlock> CODEC = simpleCodec(RegenWebBlock::new);

    @Override
    public MapCodec<WebBlock> codec() {
        return CODEC;
    }

    public RegenWebBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // The web half first: this is still a web, and anything that stops being stuck in it stops
        // being healed by it on the same tick.
        super.entityInside(state, level, pos, entity);

        if (level.isClientSide() || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        // Duration 1 because the effect is instantaneous: it is applied and gone within the tick,
        // so nothing accumulates and the heal stops the moment the entity steps out.
        living.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 0, false, false, false));
    }
}
