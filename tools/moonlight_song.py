"""Writes the opening of Beethoven's Op. 27 No. 2 as a note-bolt song file.

There is no MIDI of it in the repository and none to download, so the score is typed out here
instead - which is fine, because the well-known part of it is a texture rather than a tune: an
unbroken line of triplets on one hand, an octave in the bass under each bar, and the melody
entering above them in the fifth. That is what this holds, twelve bars of it, ending where it
began.

The output is the same song format ``tools/mid_to_song.py`` writes and the Lightning TNT already
reads - ``tick volume midi`` a line - so the Moonlight Bolt plays it through exactly the same
machinery as everything else.

    python tools/moonlight_song.py \
        src/main/resources/data/unnecessarilycompressedcobblestone/songs/moonlight.txt

Dynamics are the score's: it is marked *pp* and stays there, so the three voices are separated by
how they sit under each other - the melody over the bass over the triplets - rather than by any
swell. Everything is scaled again by the core's redstone signal when it is played.
"""

import sys
import pathlib

TICKS_PER_SECOND = 20.0

# Adagio sostenuto. The marking is a tempo nobody agrees on; this is the middle of the range
# recordings actually sit in.
BPM = 54.0

# Written in cut common time but felt in four, twelve triplet eighths to the bar.
BEATS_PER_BAR = 4
TRIPLETS_PER_BAR = 12

# How loud each voice is against the others. The piece never leaves pp, so this is balance and
# not dynamics.
ARPEGGIO = 0.34
BASS = 0.52
MELODY = 0.78

# The pitches, as MIDI notes, named the way the score names them.
GS1, B1, CS2, GS2, B2, CS3 = 32, 35, 37, 44, 47, 49
GS3, A3, BS3, CS4, DS4, E4, FS4, GS4, A4 = 56, 57, 60, 61, 63, 64, 66, 68, 69

# The four chords the opening is built out of, each as the triplet figure the right hand plays.
TONIC = [GS3, CS4, E4]
SUBMEDIANT = [A3, CS4, E4]
NEAPOLITAN = [A3, 62, FS4]
DOMINANT = [GS3, BS3, FS4]
SIX_FOUR = [GS3, CS4, E4]
DOMINANT_CLOSE = [GS3, BS3, DS4]

# One bar each: the bass octave under it, the four triplet figures across it, and whatever the
# melody does over it, as ``(beat, note)``. The melody's dotted eighth and sixteenth do not line
# up with the triplets underneath, and that is the point of it - it is written across them.
BARS = [
    # i, twice, alone. The whole piece is set up by these two bars and nothing else happens in them.
    (CS2, [TONIC] * 4, []),
    (CS2, [TONIC] * 4, []),

    # VI, then the Neapolitan over the same bass.
    (B1, [SUBMEDIANT, SUBMEDIANT, NEAPOLITAN, NEAPOLITAN], []),

    # V, and a passing six-four inside it.
    (GS1, [DOMINANT, DOMINANT, SIX_FOUR, DOMINANT_CLOSE], []),

    # The melody enters on one note and stays on it - a repeated G#, which is the whole of the tune
    # for its first two bars.
    (CS2, [TONIC] * 4, [(0.0, GS4)]),
    (CS2, [TONIC] * 4, [(0.0, GS4), (0.75, GS4), (1.0, GS4)]),

    (B1, [SUBMEDIANT, SUBMEDIANT, NEAPOLITAN, NEAPOLITAN], [(0.0, A4), (0.75, A4), (1.0, A4)]),
    (GS1, [DOMINANT, DOMINANT, SIX_FOUR, DOMINANT_CLOSE], [(0.0, GS4), (0.75, GS4), (1.0, GS4)]),

    (CS2, [TONIC] * 4, [(0.0, GS4)]),
    (CS2, [TONIC] * 4, []),

    # Home, and left ringing.
    (B1, [SUBMEDIANT, SUBMEDIANT, NEAPOLITAN, NEAPOLITAN], []),
    (CS2, [TONIC] * 4, [(0.0, GS4)]),
]


def main():
    destination = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else
                               "src/main/resources/data/unnecessarilycompressedcobblestone/songs/moonlight.txt")

    ticks_per_beat = TICKS_PER_SECOND * 60.0 / BPM
    ticks_per_bar = ticks_per_beat * BEATS_PER_BAR
    strikes = []

    for bar, (bass, figures, melody) in enumerate(BARS):
        start = bar * ticks_per_bar

        # The bass is an octave, struck as one: two bolts on the same tick.
        strikes.append((round(start), BASS, bass))
        strikes.append((round(start), BASS, bass + 12))

        # Onsets are rounded off the absolute position rather than accumulated, so a triplet that
        # does not divide into ticks cannot drift over twelve bars.
        for index, figure in enumerate(figures):
            for step, note in enumerate(figure):
                offset = (index * 3 + step) * ticks_per_bar / TRIPLETS_PER_BAR
                strikes.append((round(start + offset), ARPEGGIO, note))

        for beat, note in melody:
            strikes.append((round(start + beat * ticks_per_beat), MELODY, note))

    strikes.sort()
    length = round(len(BARS) * ticks_per_bar + 2.0 * TICKS_PER_SECOND)

    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(
        "# Written by tools/moonlight_song.py. One strike per line: tick volume midi.\n"
        "# Beethoven, Piano Sonata No. 14 in C# minor, Op. 27 No. 2, first movement, opening.\n"
        f"# {len(strikes)} strikes over {length} ticks at {BPM:g} bpm.\n"
        f"length {length}\n"
        + "\n".join(f"{tick} {volume:.2f} {note}" for tick, volume, note in strikes) + "\n",
        encoding="utf-8")

    print(f"{len(strikes)} strikes over {length} ticks")


if __name__ == "__main__":
    main()
