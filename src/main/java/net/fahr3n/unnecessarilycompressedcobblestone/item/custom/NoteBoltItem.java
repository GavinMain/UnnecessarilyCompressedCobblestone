package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * One key of a grand piano, as a bolt. There are eighty-eight of them and they differ in nothing
 * but the key they carry.
 * <p>
 * What a core loaded with one throws is an ordinary strike - it burns what it lands on and kills
 * what is standing there, exactly as {@link CompressedVanillaBoltItem}'s does - and the only thing
 * that changes is that it says a note instead of thunder. That makes a rank of cores a keyboard: a
 * bolt each and a redstone signal each, and the signal is the key's dynamic, from barely audible at
 * a signal of 1 to as loud as the sound engine will play anything at 15.
 */
public class NoteBoltItem extends BoltItem {
    /** What a signal of 1 comes out at: quiet, rather than a key nobody hears struck. */
    private static final float MIN_VOLUME = 0.1F;

    private final int key;

    /** @param key a piano key, 0 for A0 up to 87 for C8 */
    public NoteBoltItem(int key, Properties properties) {
        super(properties);
        this.key = key;
    }

    public int key() {
        return this.key;
    }

    @Override
    public void strike(ServerLevel level, BlockPos pos, ItemStack stack, int signal, float bonusDamage) {
        VanillaLightningBoltEntity bolt = ModEntities.VANILLA_LIGHTNING_BOLT.get().create(level);
        if (bolt == null) {
            return;
        }

        bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // A note's volume is loudness, not range, so the signal is a dynamic mark and nothing else.
        // A key struck below this floor is a key nobody hears over the strike itself.
        bolt.setNote(this.key, Math.max(strength(signal), MIN_VOLUME));
        bolt.setDamage(CompressedVanillaBoltItem.VANILLA_DAMAGE + Math.max(0.0F, bonusDamage));
        level.addFreshEntity(bolt);
    }
}
