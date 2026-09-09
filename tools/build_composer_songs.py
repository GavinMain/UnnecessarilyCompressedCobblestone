"""Writes the five songs the Composer boss plays.

Each is the opening phrase of something well known, typed out as (midi, beats) pairs where a beat is
a quarter note, and rendered into the `tick volume midi` files LightningSong reads. A quarter is
QUARTER_TICKS ticks, so a song's length is fixed by its own note values and nothing here has to
count ticks by hand - which is what keeps every one of the five inside the nine seconds the boss has
to play it in.

Every note is written at full volume. A song is dynamics only below a volume of 1, and these are
attacks rather than performances: what a player has to hear is which key was struck, from wherever
they are standing.

Run it from anywhere; it writes into src/main/resources/data/<modid>/songs/.
"""
import os

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(REPO, "src/main/resources/data/unnecessarilycompressedcobblestone/songs")

# 8 ticks to a quarter note, which is 150 beats a minute: quick enough that a sixteen-beat phrase
# fits inside the boss's nine second window, slow enough that consecutive notes are separate bolts.
QUARTER_TICKS = 8

# A rest is a note value with no pitch.
REST = None

SONGS = {
    # Beethoven, Symphony No. 9, "Ode to Joy". The first phrase, in C.
    "ode_to_joy": ("Beethoven, Symphony No. 9 - Ode to Joy", [
        (64, 1), (64, 1), (65, 1), (67, 1),
        (67, 1), (65, 1), (64, 1), (62, 1),
        (60, 1), (60, 1), (62, 1), (64, 1),
        (64, 1.5), (62, 0.5), (62, 2),
    ]),

    # Beethoven, Symphony No. 5. The fate motif, twice: once where it is written and once an
    # octave up, which is what gives it somewhere to go inside eight seconds.
    "fate": ("Beethoven, Symphony No. 5 - the fate motif", [
        (67, 0.5), (67, 0.5), (67, 0.5), (63, 2), (REST, 1),
        (65, 0.5), (65, 0.5), (65, 0.5), (62, 2), (REST, 1),
        (79, 0.5), (79, 0.5), (79, 0.5), (75, 2), (REST, 1),
        (77, 0.5), (77, 0.5), (77, 0.5), (74, 2), (REST, 1),
    ]),

    # Beethoven, Bagatelle in A minor, "Fur Elise". The opening figure and the phrase under it.
    "fur_elise": ("Beethoven, Bagatelle in A minor - Fur Elise", [
        (76, 0.5), (75, 0.5), (76, 0.5), (75, 0.5),
        (76, 0.5), (71, 0.5), (74, 0.5), (72, 0.5),
        (69, 1),
        (60, 0.5), (64, 0.5), (69, 0.5),
        (71, 1),
        (64, 0.5), (68, 0.5), (71, 0.5), (72, 0.5),
        (76, 1),
    ]),

    # Mozart, Serenade No. 13, "Eine kleine Nachtmusik". Both halves of the opening.
    "eine_kleine": ("Mozart, Serenade No. 13 - Eine kleine Nachtmusik", [
        (67, 1), (62, 1), (67, 0.5), (67, 0.5), (62, 0.5), (67, 0.5), (71, 0.5), (74, 1.5),
        (74, 1), (69, 1), (74, 0.5), (74, 0.5), (69, 0.5), (74, 0.5), (78, 0.5), (81, 1.5),
    ]),

    # Pachelbel, Canon in D. The ground bass, twice round - the eight notes everything else in that
    # piece is built over, and the lowest thing any of these five songs strikes.
    "canon_in_d": ("Pachelbel, Canon in D - the ground bass", [
        (50, 1), (45, 1), (47, 1), (42, 1), (43, 1), (38, 1), (43, 1), (45, 1),
        (50, 1), (45, 1), (47, 1), (42, 1), (43, 1), (38, 1), (43, 1), (45, 1),
    ]),
}


def render(name, title, notes):
    lines = []
    tick = 0.0
    for midi, beats in notes:
        if midi is not None:
            lines.append("%d 1.0 %d" % (round(tick), midi))
        tick += beats * QUARTER_TICKS

    length = round(tick)
    header = [
        "# Written by tools/build_composer_songs.py. One strike per line: tick volume midi.",
        "# %s" % title,
        "# %d strikes over %d ticks." % (len(lines), length),
        "length %d" % length,
    ]

    path = os.path.join(OUT, name + ".txt")
    with open(path, "w", encoding="utf-8") as out:
        out.write("\n".join(header + lines) + "\n")

    return length, len(lines)


os.makedirs(OUT, exist_ok=True)
for name, (title, notes) in SONGS.items():
    length, count = render(name, title, notes)
    print("%-14s %3d strikes, %3d ticks (%.1fs)" % (name, count, length, length / 20.0))
