package net.fahr3n.unnecessarilycompressedcobblestone.util;

/**
 * The eighty-eight keys of a grand piano, as the mod names them.
 * <p>
 * Everything about a note bolt that is not its sound is worked out here rather than written down
 * eighty-eight times: its item id, its display name and the sound event its sample is registered
 * under. A key is only ever an index, 0 for A0 up to 87 for C8, so nothing downstream has to know
 * how a piano is laid out.
 * <p>
 * None of the eighty-eight is crafted. They come out of the Bolt TNT and nowhere else, which is why
 * there are no tiers or ingredients here - the split between pitch class and octave now does nothing
 * but name a key and colour its icon, twelve hues by twelve semitones with the octave as brightness.
 */
public final class PianoNote {
    /** How many keys a grand piano has. */
    public static final int KEYS = 88;

    /** A0, the bottom key, in MIDI numbering. C8 is {@code LOWEST_MIDI + KEYS - 1}. */
    public static final int LOWEST_MIDI = 21;

    /** The pitch classes as an id names them, from A because that is where a keyboard starts. */
    private static final String[] NAMES = {
            "a", "a_sharp", "b", "c", "c_sharp", "d", "d_sharp", "e", "f", "f_sharp", "g", "g_sharp"};

    private static final String[] DISPLAY = {
            "A", "A Sharp", "B", "C", "C Sharp", "D", "D Sharp", "E", "F", "F Sharp", "G", "G Sharp"};

    private PianoNote() {
    }

    /** @return true if {@code key} is a key on the keyboard */
    public static boolean isKey(int key) {
        return key >= 0 && key < KEYS;
    }

    /** The key a MIDI note is played on, or -1 for anything off the keyboard. */
    public static int fromMidi(int midi) {
        int key = midi - LOWEST_MIDI;
        return isKey(key) ? key : -1;
    }

    /** {@code a_sharp_3}: the id half of everything, from the item to the sound event. */
    public static String name(int key) {
        return NAMES[key % 12] + "_" + octave(key);
    }

    /** {@code A Sharp 3}, which with " Bolt" on the end is the item's name. */
    public static String displayName(int key) {
        return DISPLAY[key % 12] + " " + octave(key);
    }

    /** The item id of this key's bolt. */
    public static String itemName(int key) {
        return name(key) + "_bolt";
    }

    /**
     * The twelve dye colours a pitch class is drawn in, in the same order {@link #NAMES} is, so a
     * key's index into either is its pitch class. These are the dyes {@code tools/build_note_bolts.py}
     * paints the icons with, repeated here rather than read off anything, because the icon is a PNG
     * built once and this is needed every frame a bolt is on screen.
     */
    private static final int[] DYES = {
            0x5E7C16, // a
            0x169C9C, // a sharp
            0x3C44AA, // b
            0xF9FFFE, // c
            0x9D9D97, // c sharp
            0x474F52, // d
            0x1D1D21, // d sharp
            0x835432, // e
            0xB02E26, // f
            0xF9801D, // f sharp
            0xFED83D, // g
            0x80C71F, // g sharp
    };

    /**
     * The lowest any channel of a bolt's colour is allowed to fall. The icons are free to draw D
     * Sharp 0 at 0x1D1D21 on a bright inventory slot, but lightning is drawn additively against the
     * night sky, so a colour that dark is a bolt nobody sees strike. Every channel is lifted toward
     * this floor by the same fraction, which keeps the hue and only stops the bolt going out.
     */
    private static final float MIN_CHANNEL = 0.28F;

    /**
     * What key {@code key} is drawn in, packed 0xRRGGBB: its pitch class as the hue and its octave
     * as the brightness, which is the same pair of facts its icon is painted from. Two bolts a
     * semitone apart are different colours and two an octave apart are the same colour at different
     * brightnesses, so a song is legible as a colour as well as a sound.
     */
    public static int colour(int key) {
        if (!isKey(key)) {
            return 0x7373FF;
        }

        int dye = DYES[key % 12];
        // The icons' own shade curve: the bottom of the keyboard dark, the top nearly white.
        float shade = 0.55F + 0.09F * octave(key);
        return pack(channel((dye >> 16) & 0xFF, shade), channel((dye >> 8) & 0xFF, shade),
                channel(dye & 0xFF, shade));
    }

    /** One channel of {@link #colour}: shaded by the octave, then lifted off the floor. */
    private static float channel(int value, float shade) {
        float lit = Math.min(1.0F, value / 255.0F * shade);
        return MIN_CHANNEL + lit * (1.0F - MIN_CHANNEL);
    }

    private static int pack(float red, float green, float blue) {
        return (Math.round(red * 255.0F) << 16) | (Math.round(green * 255.0F) << 8) | Math.round(blue * 255.0F);
    }

    /**
     * Which octave the key is in, numbered as a piano is: octaves turn over at C, and the keyboard
     * opens three semitones below C1 on A0.
     */
    public static int octave(int key) {
        return (key + 9) / 12;
    }
}
