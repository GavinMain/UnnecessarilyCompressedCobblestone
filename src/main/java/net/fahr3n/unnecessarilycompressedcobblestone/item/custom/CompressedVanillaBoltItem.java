package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

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
    /** What a storm bolt does. It is private on {@code LightningBolt}, so it is written out here. */
    public static final float VANILLA_DAMAGE = 5.0F;


    public CompressedVanillaBoltItem(Properties properties) {
        super(properties);
    }

    @Override
    public void strike(ServerLevel level, BlockPos pos, ItemStack stack, int signal, float bonusDamage) {
        VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
        if (bolt == null) {
            return;
        }

        bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        bolt.setVolumeScale(strength(signal));
        bolt.setDamage(VANILLA_DAMAGE + Math.max(0.0F, bonusDamage));
        level.addFreshEntity(bolt);
    }
}
