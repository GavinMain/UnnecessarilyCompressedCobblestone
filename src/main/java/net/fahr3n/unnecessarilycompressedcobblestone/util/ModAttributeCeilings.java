package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Raising the ceiling on {@code MAX_HEALTH}, because vanilla's is 1024 and this mod asks for more.
 * <p>
 * Every attribute value in the game goes through {@code RangedAttribute#sanitizeValue}, which clamps
 * it between the two figures the attribute was registered with. {@code MAX_HEALTH} is registered
 * with a maximum of 1024, so a boss declaring 5000 health is a boss with 1024 health and nothing
 * anywhere reports the difference - the number is taken, clamped and forgotten at the moment the
 * entity's {@code AttributeSupplier} is built. Eleven mobs in this mod declared more than 1024 and
 * every one of them had exactly 1024, which is why the dev block's 999 points of true damage killed
 * the Compressed Dragon's second phase in two hits: 999 and then 25, out of a bar that was supposed
 * to hold five thousand.
 * <p>
 * The ceiling is {@code private final} with no way to state another, so it is widened by an access
 * transformer and written once here. That is a smaller change than it sounds: a ceiling is not a
 * value, so nothing in the game moves because it was raised - every entity keeps whatever figure it
 * asked for, and the only ones that change are the ones that were being clamped, which are exactly
 * the ones that were lying. The alternative was renumbering eleven mobs down to 1024 and dividing
 * every damage figure in the mod to match.
 * <p>
 * It must run <b>before</b> {@code EntityAttributeCreationEvent}, since a supplier clamps its values
 * as it is built and a boss built under the old ceiling keeps 1024 whatever is raised afterwards.
 * The mod constructor is the first code that runs, so that is where {@link #raise} is called from.
 */
public final class ModAttributeCeilings {
    /**
     * The new ceiling on max health.
     * <p>
     * 2^20, chosen for being a power of two rather than for being a round number: health is carried
     * as a {@code float} from end to end, and at a million the gap between one float and the next is
     * about a sixteenth of a point, so arithmetic near the top of the range stays exact enough that
     * a bar cannot drift. It is far past anything this mod asks for, which is the point - the figure
     * a mob declares should be the figure it has, and this ceiling exists only so that there is one.
     */
    public static final double MAX_HEALTH_CEILING = 1048576.0;

    private ModAttributeCeilings() {
    }

    /** Raises the ceilings. Called from the mod constructor, before anything builds a supplier. */
    public static void raise() {
        raise(Attributes.MAX_HEALTH, MAX_HEALTH_CEILING);
    }

    /**
     * Raises one attribute's ceiling, and only ever raises it: an attribute already allowing more
     * than this - another mod got there first - is left exactly as it was found, since lowering a
     * ceiling would clamp values that mod has already stated.
     */
    private static void raise(Holder<Attribute> attribute, double ceiling) {
        if (!(attribute.value() instanceof RangedAttribute ranged) || ranged.maxValue >= ceiling) {
            return;
        }

        UnnecessarilyCompressedCobblestone.LOGGER.info("Raising the ceiling on {} from {} to {}",
                attribute.value().getDescriptionId(), ranged.maxValue, ceiling);
        ranged.maxValue = ceiling;
    }
}
