package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import net.fahr3n.unnecessarilycompressedcobblestone.component.ModDataComponents;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.NoteBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RestBoltItem;
import net.minecraft.world.item.ItemStack;

/**
 * A song written at a Composition Table, as the Composition Bolt carries it.
 * <p>
 * It is a list of <em>codes</em> read left to right, one per square of the sheet, and a code is one
 * of two things: a piano key (0 for A0 up to 87 for C8) or a rest, which is negative. That is the
 * whole format. It is small enough to sit in a data component, it survives being written and read
 * by anything that can copy an item, and it turns into the same {@link LightningSong} the datapack
 * songs parse to - so a composed bolt is played by exactly the machinery the Lightning TNTs use,
 * with nothing of its own but the writing.
 * <p>
 * The two rests are the two ways a player can say nothing: {@link #SHORT_REST} for a beat and
 * {@link #LONG_REST} for a bar of them. Nothing else can be written, which is why the sheet slots
 * take only note bolts and rests and why {@link #codeOf} is the one place that decides.
 */
public final class Composition {
    /** One beat of silence. */
    public static final int SHORT_REST = -1;

    /** Four of them, so a player can leave a real gap without spending four squares on it. */
    public static final int LONG_REST = -2;

    /** How long a square lasts. Five squares a second, which is quick enough to carry a tune. */
    public static final int BEAT_TICKS = 4;

    /** How many squares a sheet holds: two staves of nine. */
    public static final int SHEET_SIZE = 18;

    /** How loud a composed bolt strikes. Every note is the same: the sheet has no dynamics in it. */
    public static final float VOLUME = 1.0F;

    private Composition() {
    }

    /**
     * What this stack writes onto the sheet: a piano key, one of the two rests, or empty for
     * anything that is not a note or a rest.
     */
    public static OptionalInt codeOf(ItemStack stack) {
        if (stack.getItem() instanceof NoteBoltItem note) {
            return OptionalInt.of(note.key());
        }

        if (stack.getItem() instanceof RestBoltItem rest) {
            return OptionalInt.of(rest.code());
        }

        return OptionalInt.empty();
    }

    /** How many beats a code lasts. */
    public static int beats(int code) {
        return code == LONG_REST ? 4 : 1;
    }

    /** The song written on this stack, empty if nothing has been. */
    public static List<Integer> get(ItemStack stack) {
        List<Integer> song = stack.get(ModDataComponents.COMPOSITION.get());
        return song == null ? List.of() : song;
    }

    /** Writes a song onto a stack, or wipes it clean if there is nothing to write. */
    public static void set(ItemStack stack, List<Integer> codes) {
        if (codes.isEmpty()) {
            stack.remove(ModDataComponents.COMPOSITION.get());
        } else {
            stack.set(ModDataComponents.COMPOSITION.get(), List.copyOf(codes));
        }
    }

    /** Whether anything is written on this stack. */
    public static boolean isWritten(ItemStack stack) {
        return !get(stack).isEmpty();
    }

    /**
     * The song as lightning plays it: one strike per note, none for a rest, every note at the same
     * volume. A rest is not a silent bolt here - it is simply where no bolt falls - so what a rest
     * costs is the beats it takes up and nothing else.
     */
    public static LightningSong toSong(List<Integer> codes) {
        List<LightningSong.Note> notes = new ArrayList<>(codes.size());
        int beat = 0;

        for (int code : codes) {
            if (code >= 0) {
                notes.add(new LightningSong.Note(beat * BEAT_TICKS, VOLUME,
                        PianoNote.isKey(code) ? code : VanillaLightningBoltEntity.NO_NOTE));
            }

            beat += beats(code);
        }

        return new LightningSong(notes, beat * BEAT_TICKS);
    }

    /**
     * The other direction: a datapack song written back out as a sheet, so a piece the mod already
     * ships can be handed to a Composition Bolt without being typed out a second time. The Lightning
     * Staff's Composition engraving loads the same {@code fur_elise} file the Compressed Composer
     * plays, so a pack that edits that file changes both.
     * <p>
     * A sheet is a coarser thing than a song and the conversion says so. Time is quantised to
     * {@link #BEAT_TICKS}, which is exact for anything written in eighths at the tempo these songs
     * use and rounds otherwise; a square holds one code, so where two notes land on the same beat
     * the first is kept and the rest are dropped - a sheet cannot write a chord; and a strike with
     * no key at all becomes a rest, since a sheet has no way to say "thunder". Trailing rests are
     * added out to the song's own length, so the last note keeps its value.
     */
    public static List<Integer> fromSong(LightningSong song) {
        List<Integer> codes = new ArrayList<>();

        for (LightningSong.Note note : song.notes()) {
            int square = Math.round(note.tick() / (float) BEAT_TICKS);
            if (square < codes.size()) {
                continue;
            }

            while (codes.size() < square) {
                codes.add(SHORT_REST);
            }

            codes.add(PianoNote.isKey(note.key()) ? note.key() : SHORT_REST);
        }

        int squares = Math.round(song.lengthTicks() / (float) BEAT_TICKS);
        while (!codes.isEmpty() && codes.size() < squares) {
            codes.add(SHORT_REST);
        }

        return codes;
    }

    /** {@code C4 D4 - G4}: the sheet as a line of text, for the bolt's tooltip. */
    public static String describe(List<Integer> codes) {
        StringBuilder written = new StringBuilder();
        for (int code : codes) {
            if (!written.isEmpty()) {
                written.append(' ');
            }

            written.append(switch (code) {
                case SHORT_REST -> "-";
                case LONG_REST -> "--";
                default -> PianoNote.isKey(code) ? PianoNote.displayName(code).replace(" Sharp", "#").replace(" ", "") : "?";
            });
        }

        return written.toString();
    }
}
