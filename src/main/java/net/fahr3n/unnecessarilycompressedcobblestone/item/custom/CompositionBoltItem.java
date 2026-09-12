package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.util.Composition;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A blank sheet of music, and then whatever was written on it.
 * <p>
 * It is the {@link SongBoltItem} with the song taken out of the mod and put in the player's hands:
 * where that one names a datapack file, this one carries the notes themselves in a data component,
 * written at a Composition Table. Fired or set on a core, it plays them.
 * <p>
 * One bolt is one song. Writing a new sheet over an old one replaces it, which is what the table's
 * second button is for.
 */
public class CompositionBoltItem extends BoltItem {
    public CompositionBoltItem(Properties properties) {
        super(properties);
    }

    /**
     * A written bolt plays its sheet; a blank one does nothing at all, rather than falling back on
     * some default strike - a blank sheet is blank.
     * <p>
     * Every note is a note bolt in all but name: it lands exactly where the strike was called, at
     * that height, and hits for what a note bolt would - vanilla lightning plus whatever the launcher
     * added. Scattering the notes in a ring on the ground, the way the datapack songs are played,
     * made a composed bolt shot at something in the air miss it entirely.
     */
    @Override
    public void strike(ServerLevel level, BlockPos pos, ItemStack stack, int signal, float bonusDamage) {
        List<Integer> song = Composition.get(stack);
        if (!song.isEmpty()) {
            DeferredStrikes.playSong(level, pos, Composition.toSong(song), 0.0, strength(signal),
                    CompressedVanillaBoltItem.VANILLA_DAMAGE + Math.max(0.0F, bonusDamage));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        List<Integer> song = Composition.get(stack);
        tooltip.add(song.isEmpty()
                ? Component.translatable("item.unnecessarilycompressedcobblestone.composition_bolt.blank")
                : Component.literal(Composition.describe(song)));
    }
}
