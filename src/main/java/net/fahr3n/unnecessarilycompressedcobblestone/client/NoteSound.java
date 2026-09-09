package net.fahr3n.unnecessarilycompressedcobblestone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Plays one of the eighty-eight note samples so that it carries as far as the player can see.
 * <p>
 * {@code Level#playLocalSound} cannot do this. It builds a {@code SimpleSoundInstance} with
 * {@code Attenuation.LINEAR}, and the sound engine then hands OpenAL a linear model over
 * {@code max(volume, 1) * 16} blocks: the gain falls from full at the source to nothing at that
 * distance, and the only dial is where the nothing is. Volume is the same dial - above 1 it is
 * clamped to full gain and multiplies that distance instead - so with linear attenuation a note can
 * be loud or it can be quiet, but it always fades, and it always fades to silence somewhere.
 * <p>
 * {@code Attenuation.NONE} is the other branch of that same code, and it calls
 * {@code Channel#disableAttenuation}, which turns the distance model off for the source outright.
 * The gain is then whatever it was set to, at any range, and the position is left doing nothing but
 * panning: a bolt three hundred blocks away is heard exactly as loudly as one underfoot, which is
 * what a song played across a field needs. How far a note reaches at all is then not a sound
 * question but a tracking one - a client plays this off its own copy of the bolt, so the answer is
 * the entity's client tracking range.
 * <p>
 * That also gives the volume back its natural meaning. With attenuation off it is loudness and
 * nothing else, over the whole range, so a song's dynamics are heard as dynamics wherever the
 * listener is standing rather than as how far each note happened to carry.
 */
@OnlyIn(Dist.CLIENT)
public final class NoteSound {
    private NoteSound() {
    }

    public static void play(SoundEvent sound, double x, double y, double z, float volume) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                sound.getLocation(), SoundSource.RECORDS, volume, 1.0F, RandomSource.create(),
                false, 0, SoundInstance.Attenuation.NONE, x, y, z, false));
    }
}
