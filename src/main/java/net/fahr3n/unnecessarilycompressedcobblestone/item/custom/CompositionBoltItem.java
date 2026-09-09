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
    /**
     * How wide it plays. The same reasoning as every other song here - below a volume of 1 a sound
     * carries a flat sixteen blocks and fades over them, so a wide ring costs the music more than it
     * gains - and tighter still than the Moonlight Bolt's, because a composed song is short and
     * usually played at something.
     */
    private static final double RADIUS = 2.0;

    public CompositionBoltItem(Properties properties) {
        super(properties);
    }

    /**
     * A written bolt plays its sheet; a blank one does nothing at all, rather than falling back on
     * some default strike - a blank sheet is blank.
     */
    @Override
    public void strike(ServerLevel level, BlockPos pos, ItemStack stack, int signal, float bonusDamage) {
        List<Integer> song = Composition.get(stack);
        if (!song.isEmpty()) {
            DeferredStrikes.queueSong(level, pos, Composition.toSong(song), RADIUS, strength(signal));
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
