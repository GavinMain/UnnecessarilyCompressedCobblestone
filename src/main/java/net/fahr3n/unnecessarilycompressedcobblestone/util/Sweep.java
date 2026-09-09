package net.fahr3n.unnecessarilycompressedcobblestone.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Hit tests against a line something travelled along rather than against the box it ended in.
 * <p>
 * Anything that crosses more than about a block in a tick needs this. Vanilla lays a whole tick of
 * movement down in one step, so a bounding-box overlap taken afterwards misses everything the mover
 * passed clean through on the way - which at the speeds this mod reaches is most of the path. The
 * Compressed Chicken Boss's ram covers nearly three blocks a tick and the Dash engraving covers five
 * in one, and both would otherwise connect only with whatever happened to be standing on the exact
 * square they stopped on.
 */
public final class Sweep {
    private Sweep() {
    }

    /** How far {@code point} is from the segment between {@code from} and {@code to}. */
    public static double distanceToSegment(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 along = to.subtract(from);
        double lengthSqr = along.lengthSqr();
        if (lengthSqr < 1.0E-6) {
            return point.distanceTo(from);
        }

        // Where along the segment the nearest point is, clamped to its ends so the test is against
        // the segment and not against the infinite line through it.
        double t = Math.max(0.0, Math.min(1.0, point.subtract(from).dot(along) / lengthSqr));
        return point.distanceTo(from.add(along.scale(t)));
    }

    /** Whether {@code entity} was within {@code width} of the path from {@code from} to {@code to}. */
    public static boolean caught(Entity entity, Vec3 from, Vec3 to, double width) {
        return distanceToSegment(entity.getBoundingBox().getCenter(), from, to) <= width;
    }

    /**
     * The box to ask the level for candidates in before testing them properly. It is the whole path
     * grown by {@code width}, which is a generous over-estimate on purpose: it is only there to keep
     * the entity query cheap, and {@link #caught} is what actually decides.
     */
    public static AABB bounds(Vec3 from, Vec3 to, double width) {
        return new AABB(from, to).inflate(width);
    }
}
