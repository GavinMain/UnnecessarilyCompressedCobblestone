package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.LightningSong;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * A bolt that is not one strike but a whole piece of music, played in note bolts.
 * <p>
 * A core loaded with one calls down every note of its song around itself, in time, over however
 * long the song runs - so where a note bolt is one key of a piano, this is a pianist. The core's
 * redstone signal is the dynamic the whole performance is played at, the way it is the dynamic of a
 * single note bolt's strike.
 * <p>
 * The core strikes once a second while it is powered and a song is minutes long, so a second strike
 * arriving mid-performance is dropped rather than layered. The core holds that rule itself, through
 * {@link DeferredStrikes#isPlayingAt}, since it is the repeating trigger: a bolt shot twice at the
 * same spot is two performances.
 * <p>
 * Which song is a datapack file under {@code data/<namespace>/songs/<name>.txt}, so a pack can
 * replace what this plays without touching the mod. A new song bolt is this class again with
 * another name in it.
 */
public class SongBoltItem extends BoltItem {
    /**
     * How wide the performance falls. Kept tight for the same reason the Lightning TNT's is: below a
     * volume of 1 the sound engine carries a sound a flat sixteen blocks and fades it linearly over
     * them, so bolts scattered wide lose more of the music to where the listener stands than the
     * music's own dynamics are worth. This is tighter still than the TNT's, because a core is a
     * thing you stand next to.
     */
    private static final double RADIUS = 3.0;

    private final ResourceLocation song;

    public SongBoltItem(ResourceLocation song, Properties properties) {
        super(properties);
        this.song = song;
    }

    /** The bonus damage is not passed on: a song is a hundred bolts, and none of them is the shot. */
    @Override
    public void strike(ServerLevel level, BlockPos pos, ItemStack stack, int signal, float bonusDamage) {
        DeferredStrikes.playSong(level, pos, LightningSong.get(level.getServer(), this.song), RADIUS,
                strength(signal), DeferredStrikes.DEFAULT_DAMAGE);
    }
}
