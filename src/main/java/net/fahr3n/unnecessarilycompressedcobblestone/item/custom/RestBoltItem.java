package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * A rest: the silence between two notes, as a bolt.
 * <p>
 * There are two of them and they differ only in how long the silence is - one beat, or a bar of
 * four - which is what makes them two items rather than one with a number on it: the Composition
 * Table lays a sheet out square by square, and a rest that could be any length would need a
 * counter on the square to say which.
 * <p>
 * Struck on its own it is still real lightning, and still burns what it lands on. What it does not
 * do is make a sound - {@code setVolume(0)} is a bolt that is seen and not heard - which is the one
 * honest reading of "a silent bolt".
 */
public class RestBoltItem extends BoltItem {
    private final int code;

    /** @param code the rest this writes onto a sheet: {@code Composition.SHORT_REST} or {@code LONG_REST} */
    public RestBoltItem(int code, Properties properties) {
        super(properties);
        this.code = code;
    }

    public int code() {
        return this.code;
    }

    @Override
    public void strike(ServerLevel level, BlockPos pos, ItemStack stack, int signal, float bonusDamage) {
        VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
        if (bolt == null) {
            return;
        }

        bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        bolt.setVolume(0.0F);
        bolt.setDamage(CompressedVanillaBoltItem.VANILLA_DAMAGE + Math.max(0.0F, bonusDamage));
        level.addFreshEntity(bolt);
    }
}
