package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The plainest bolt there is: what a core loaded with this throws is exactly what a thunderstorm
 * throws, fire and all. It is the reference the others are measured against.
 * <p>
 * The one thing the core's signal changes is how far the strike is heard. At a full signal it is a
 * storm bolt, thunder and all, audible to every player in the dimension the way vanilla's is; at a
 * signal of 1 the same strike does the same damage and lights the same fires, but nobody more than a
 * few blocks away knows it happened. See {@link VanillaLightningBoltEntity} for why volume is
 * distance rather than loudness.
 */
public class CompressedVanillaBoltItem extends BoltItem {
    public CompressedVanillaBoltItem(Properties properties) {
        super(properties);
    }

    @Override
    public void strike(ServerLevel level, BlockPos core, int signal) {
        VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
        if (bolt == null) {
            return;
        }

        // On top of the core rather than inside it, so what it hits is whatever is standing there.
        bolt.moveTo(core.getX() + 0.5, core.getY() + 1.0, core.getZ() + 0.5);
        bolt.setVolumeScale(strength(signal));
        level.addFreshEntity(bolt);
    }
}
