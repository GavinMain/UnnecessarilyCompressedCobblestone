package net.fahr3n.unnecessarilycompressedcobblestone.potion.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * A step height of one extra block, for as long as it is held.
 * <p>
 * There is no vanilla effect for this, and the obvious alternative - adding a transient
 * {@code STEP_HEIGHT} modifier straight onto whatever is nearby - has nowhere to take it off again:
 * the Compression Jump set gets away with it because a tick handler watches the set and removes the
 * modifier when a piece comes off, and nothing watches a mob a TNT went off next to. An effect owns
 * its own modifier and hands it back when it lapses, which is the whole reason this is one.
 * <p>
 * {@code STEP_HEIGHT} is a {@link net.minecraft.world.entity.ai.attributes.RangedAttribute} capped
 * at 10, so nothing here can run away however deep the amplifier goes.
 */
public class StepHeightMobEffect extends MobEffect {
    /** How much higher a step is per level. */
    private static final double PER_LEVEL = 1.0;

    public StepHeightMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
        addAttributeModifier(Attributes.STEP_HEIGHT,
                ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "effect.zoom"),
                PER_LEVEL, AttributeModifier.Operation.ADD_VALUE);
    }
}
