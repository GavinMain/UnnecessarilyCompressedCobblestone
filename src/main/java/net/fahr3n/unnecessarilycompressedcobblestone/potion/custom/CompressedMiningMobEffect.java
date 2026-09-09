package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.util.BossSpoil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A boss's spoils: while it is held, stone broken by the holder comes up as one particular
 * compression level instead of as cobblestone.
 * <p>
 * The effect itself does nothing at all - it has no tick, no attribute and no damage. It is a flag
 * with a duration on it, and the whole of the behaviour is in {@code ModEvents}, which reads it off
 * the breaker when a block's drops are decided. That is deliberate rather than lazy: a drop is
 * something that happens once, at a moment the effect is not being ticked, so there is nothing for
 * an {@code applyEffectTick} to do and a handler on the drop event is where the question is actually
 * asked.
 * <p>
 * There is one registered effect per {@link BossSpoil} rather than one effect whose amplifier is the
 * level. That is not the shape most of this mod's tables take, and the reason is the display: the
 * inventory screen writes an amplifier out as a numeral only up to nine and prints nothing at all
 * above it, so a single effect at amplifier 196 would read as an unnumbered "Spoils" and the one
 * fact a player needs - which stone they are about to get - would be invisible. A registered effect
 * per boss puts the tier in the name.
 */
public class CompressedMiningMobEffect extends MobEffect {
    private final BossSpoil spoil;

    public CompressedMiningMobEffect(BossSpoil spoil) {
        // Beneficial, so milk is the only thing that takes it off and a Compression Rain set does
        // not refuse it: that set is written to sweep away harmful effects by category, and a spoil
        // arriving at the end of a boss fight is the last thing it should be throwing out.
        super(MobEffectCategory.BENEFICIAL, spoil.color());
        this.spoil = spoil;
    }

    /** Which boss this came off, and so which compression level it hands out. */
    public BossSpoil spoil() {
        return this.spoil;
    }
}
