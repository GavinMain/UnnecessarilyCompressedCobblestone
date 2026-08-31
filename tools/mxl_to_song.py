"""Turns a MusicXML score into a Lightning TNT song file.

The Lightning TNT plays a song in lightning and in nothing else, so what it needs out of a score is
not a melody. Lightning has no pitch to give - Minecraft rolls a fresh random one for every bolt and
nothing can set it - so the only thing a bolt carries is how loud it is. A song for it is a rhythm
and a dynamic line played on a single note, one bolt per note, which is why the score this was
written for is a one-note reduction.

The song file is plain text in a datapack:

    data/<namespace>/songs/<name>.txt

one strike per line as ``tick volume``, volume being 0 to 1, plus a ``length <ticks>`` line saying
how long the performance runs for. Point this script at a ``.mxl`` (a zipped MusicXML, which is what
every notation program exports) or a bare ``.musicxml``:

    python tools/mxl_to_song.py "path/to/score.musicxml" ^
        src/main/resources/data/unnecessarilycompressedcobblestone/songs/lightning_song.txt

Options worth knowing:

``--tempo``      beats per minute to play at, overriding whatever the score says. Worth measuring off
                 a rendering of the score rather than trusting the marking - see ``--length``.
``--length``     seconds the finished performance should run for, tail included.
``--part``       which part of the score to take, when there is more than one.
``--floor``      the volume the quietest dynamic in the score comes out at.

Dynamics are read as they are marked and every note takes whatever was last marked before it. The
ladder below is geometric, about three decibels a step, and the whole line is then normalised so the
loudest mark in the score plays at 1 - which is as loud as the sound engine will play anything, and
the point above which volume stops being loudness and turns into range.
"""

import argparse
import pathlib
import zipfile
import xml.etree.ElementTree as ElementTree

TICKS_PER_SECOND = 20.0

# Relative amplitudes, roughly three decibels a step. Only the ratios matter: the line is normalised
# against the loudest mark the score actually uses.
DYNAMICS = {
    "pppp": 0.05, "ppp": 0.08, "pp": 0.11, "p": 0.16,
    "mp": 0.23, "mf": 0.32, "f": 0.45, "ff": 0.64, "fff": 0.90, "ffff": 1.27,
    "sf": 0.64, "sfz": 0.64, "fp": 0.45, "rf": 0.45, "rfz": 0.45, "sffz": 0.90,
}

DEFAULT_DYNAMIC = "mf"


def read_score(path):
    """The MusicXML document, out of a .mxl container or a bare file."""
    path = pathlib.Path(path)
    if path.suffix.lower() != ".mxl":
        return ElementTree.parse(path).getroot()

    with zipfile.ZipFile(path) as archive:
        # The container names the real score; falling back on the first .xml covers exports that
        # leave the container out.
        name = None
        if "META-INF/container.xml" in archive.namelist():
            container = ElementTree.fromstring(archive.read("META-INF/container.xml"))
            root_file = container.find(".//{*}rootfile")
            if root_file is not None:
                name = root_file.get("full-path")

        if name is None:
            name = next(entry for entry in archive.namelist()
                        if entry.endswith((".xml", ".musicxml")) and not entry.startswith("META-INF"))

        return ElementTree.fromstring(archive.read(name))


def collect(part):
    """Every note in one part, as (beat, duration, dynamic, ties) tuples, with the tempo and length."""
    notes = []
    divisions = 1.0
    tempo = None
    dynamic = DEFAULT_DYNAMIC
    position = 0.0
    end = 0.0

    for measure in part.findall("{*}measure"):
        for element in measure:
            tag = element.tag.split("}")[-1]

            if tag == "attributes":
                text = element.findtext("{*}divisions")
                if text:
                    divisions = float(text)

            elif tag in ("direction", "sound"):
                sound = element if tag == "sound" else element.find(".//{*}sound[@tempo]")
                if sound is not None and sound.get("tempo"):
                    tempo = float(sound.get("tempo"))

                marks = element.find(".//{*}dynamics")
                if marks is not None:
                    for mark in marks:
                        name = mark.tag.split("}")[-1]
                        if name in DYNAMICS:
                            dynamic = name

            elif tag == "note":
                duration = float(element.findtext("{*}duration", default="0")) / divisions
                chord = element.find("{*}chord") is not None
                start = position - duration if chord else position

                if element.find("{*}rest") is None:
                    ties = {tie.get("type") for tie in element.findall("{*}tie")}
                    notes.append((start, duration, dynamic, ties))

                if not chord:
                    position += duration

                end = max(end, position)

            elif tag == "backup":
                position -= float(element.findtext("{*}duration", default="0")) / divisions

            elif tag == "forward":
                position += float(element.findtext("{*}duration", default="0")) / divisions

    return notes, tempo, end


def attacks(notes):
    """
    The notes that are actually struck. A note tied to the one before it is held rather than played
    again, so it is one bolt and not two - which is what turns a final chord held over four bars into
    a single strike rather than four.

    The tie is only believed when the note before it really does end where this one starts and really
    does carry a matching tie start. Transcriptions are careless with tie tags - the score this was
    written against marks a tie stop on every note in the piece and a tie start on almost none - and a
    note wrongly swallowed is a note the song loses.
    """
    kept = []
    for index, (beat, duration, dynamic, ties) in enumerate(notes):
        if index > 0 and "stop" in ties:
            previous = notes[index - 1]
            if "start" in previous[3] and abs(previous[0] + previous[1] - beat) < 1e-6:
                continue

        kept.append((beat, dynamic))

    return kept


def name_of(level):
    """The dynamic mark closest to a level, for the header."""
    return min(DYNAMICS, key=lambda mark: abs(DYNAMICS[mark] - level))


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("score", help="the .mxl or .musicxml to read")
    parser.add_argument("output", help="the song file to write")
    parser.add_argument("--tempo", type=float, default=None, help="beats per minute, overriding the score")
    parser.add_argument("--length", type=float, default=None, help="seconds the performance runs for")
    parser.add_argument("--part", type=int, default=0, help="which part to take, 0 for the first")
    parser.add_argument("--floor", type=float, default=0.0,
                        help="what the quietest dynamic comes out at, 0 to leave the ladder alone")
    arguments = parser.parse_args()

    root = read_score(arguments.score)
    parts = root.findall("{*}part")
    if not parts:
        raise SystemExit("no parts in that score")

    part = parts[min(arguments.part, len(parts) - 1)]
    notes, marked_tempo, end = collect(part)
    if not notes:
        raise SystemExit("no notes in that part - try another --part")

    tempo = arguments.tempo or marked_tempo or 120.0
    struck = attacks(notes)

    levels = [DYNAMICS[dynamic] for _, dynamic in struck]
    loudest = max(levels)
    quietest = min(levels)

    seconds = arguments.length or end * 60.0 / tempo

    lines = [
        "# Written by tools/mxl_to_song.py. One strike per line: tick volume.",
        "# Source: " + pathlib.Path(arguments.score).name,
        "# {0} strikes at {1:g} bpm, {2} to {3}, normalised to 1.".format(
            len(struck), tempo, name_of(quietest), name_of(loudest)),
        "length {0}".format(int(round(seconds * TICKS_PER_SECOND))),
    ]

    for beat, dynamic in struck:
        # Rounded off the beat rather than accumulated, so a rhythm the tick rate cannot divide
        # evenly - a sixteenth at this tempo is 2.1 ticks - jitters by half a tick and never drifts.
        tick = int(round(beat * 60.0 / tempo * TICKS_PER_SECOND))

        volume = DYNAMICS[dynamic] / loudest
        if arguments.floor:
            span = quietest / loudest
            volume = arguments.floor + (volume - span) * (1.0 - arguments.floor) / (1.0 - span)

        lines.append("{0} {1:.2f}".format(tick, volume))

    output = pathlib.Path(arguments.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print("wrote {0} strikes to {1}, {2:.1f}s at {3:g} bpm".format(len(struck), output, seconds, tempo))


if __name__ == "__main__":
    main()
