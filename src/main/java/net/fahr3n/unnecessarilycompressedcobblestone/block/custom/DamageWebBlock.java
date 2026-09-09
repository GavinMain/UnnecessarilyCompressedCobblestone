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
 * The {@link RegenWebBlock}'s opposite number: a cobweb that holds whatever walks into it and puts
 * Instant Damage II on it every tick it is held.
 * <p>
 * It is written as the same block with the effect swapped, and that is worth stating rather than
 * factoring out, because the two are not symmetrical in what they mean. A web that heals is a place
 * to stand; a web that harms is a trap, and the trap works because a web is the one block in the
 * game that makes leaving slower than arriving. Instant Damage lands the moment it is added, so the
 * count of ticks something spends stuck is the damage - which is what turns "slows you down" into
 * "kills you", and is the whole of the block.
 * <p>
 * Going through the vanilla effect rather than calling {@code hurt} is the same choice the Regen Web
 * makes and buys the same things: the particles, whatever another mod has to say about Instant
 * Damage, immunity through {@code MobEffectEvent.Applicable}, and above all the inversion - the
 * undead are <em>healed</em> by this web, exactly as they are harmed by the other one. A trap that
 * mends skeletons is a more interesting block than one that kills everything, and none of it is
 * written here.
 * <p>
 * The damage itself is {@code minecraft:magic}, which is inside {@code #bypasses_armor}, so armour
 * does nothing about it and Resistance and Protection do. That is Instant Damage's own bargain and
 * is not this block's to change.
 * <p>
 * {@code WebBlock.codec()} is declared {@code MapCodec<WebBlock>}, which a {@code MapCodec} of a
 * subclass cannot override, so the field is declared at the supertype and {@code simpleCodec} infers
 * the rest from the constructor - the same shape as {@code CompressedTntBlock}.
 */
public class DamageWebBlock extends WebBlock {
    private static final MapCodec<WebBlock> CODEC = simpleCodec(DamageWebBlock::new);

    /**
     * Instant Damage II - the amplifier is one less than the numeral. Vanilla's Instant Damage is
     * {@code 6 << amplifier}, so this is twelve points a tick against the Regen Web's eight of
     * healing: a trap ought to win the exchange with the thing that undoes it.
     */
    private static final int AMPLIFIER = 1;

    @Override
    public MapCodec<WebBlock> codec() {
        return CODEC;
    }

    public DamageWebBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // The web half first: this is still a web, and anything that stops being stuck in it stops
        // being hurt by it on the same tick.
        super.entityInside(state, level, pos, entity);

        if (level.isClientSide() || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        // Duration 1 because the effect is instantaneous: it is applied and gone within the tick,
        // so nothing accumulates and the damage stops the moment the entity steps out.
        living.addEffect(new MobEffectInstance(MobEffects.HARM, 1, AMPLIFIER, false, false, false));
    }
}
