"""Turns a MIDI file into a note-bolt song file.

The note bolts gave lightning a pitch it never had, so a song for them is no longer a rhythm and a
dynamic line - it is the score. This reads a plain Standard MIDI File and writes the same song
format the Lightning TNT already uses, with the note added:

    data/<namespace>/songs/<name>.txt

one strike per line as ``tick volume [midi]``, where ``midi`` is the key struck (21 to 108, the
eighty-eight keys of a piano) and a line without one is the old unpitched bolt. A ``length <ticks>``
line says how long the performance runs for.

    python tools/mid_to_song.py "path/to/score.mid" ^
        src/main/resources/data/unnecessarilycompressedcobblestone/songs/railgun.txt

Every note is one real lightning bolt, so the two things worth turning down are both here:
``--voices`` caps how many fall on any one tick (the loudest of them win), and ``--transpose``
folds anything outside the keyboard back into it an octave at a time rather than dropping it.

No MIDI library is needed or wanted - the format is a handful of chunks and a variable-length
integer, and parsing it here keeps the tool runnable on a bare Python.
"""

import argparse
import collections
import pathlib
import struct

TICKS_PER_SECOND = 20.0

# The keyboard: A0 to C8.
LOWEST = 21
HIGHEST = 108


def read_varint(data, index):
    value = 0
    while True:
        byte = data[index]
        index += 1
        value = (value << 7) | (byte & 0x7F)
        if not byte & 0x80:
            return value, index


def read_events(path):
    """Every note-on in the file, as ``(seconds, midi note, velocity)``, in time order.

    Tempo changes are applied as they are met, which is why the whole file is merged into one
    stream first: a tempo event on track 1 governs the notes on track 4.
    """
    data = pathlib.Path(path).read_bytes()
    if data[:4] != b"MThd":
        raise SystemExit(f"{path} is not a MIDI file")

    _, fmt, tracks, division = struct.unpack(">IHHH", data[4:14])
    if division & 0x8000:
        raise SystemExit("SMPTE timing is not supported")

    index = 14
    events = []
    for _ in range(tracks):
        if data[index:index + 4] != b"MTrk":
            break

        (size,) = struct.unpack(">I", data[index + 4:index + 8])
        index += 8
        end = index + size

        tick = 0
        status = 0
        while index < end:
            delta, index = read_varint(data, index)
            tick += delta

            if data[index] & 0x80:
                status = data[index]
                index += 1

            if status == 0xFF:
                kind = data[index]
                index += 1
                size_meta, index = read_varint(data, index)
                if kind == 0x51:
                    events.append((tick, "tempo", struct.unpack(">I", b"\0" + data[index:index + 3])[0]))
                index += size_meta
            elif status in (0xF0, 0xF7):
                size_sysex, index = read_varint(data, index)
                index += size_sysex
            else:
                kind = status & 0xF0
                first = data[index]
                index += 1
                second = data[index] if kind not in (0xC0, 0xD0) else 0
                if kind not in (0xC0, 0xD0):
                    index += 1

                # Channel 10 is percussion, whose "notes" are drums rather than pitches.
                if kind == 0x90 and second > 0 and (status & 0x0F) != 9:
                    events.append((tick, "note", (first, second)))

        index = end

    events.sort(key=lambda event: event[0])

    # Ticks to seconds, honouring every tempo change along the way.
    notes = []
    seconds = 0.0
    last_tick = 0
    micros_per_beat = 500000
    for tick, kind, value in events:
        seconds += (tick - last_tick) / division * micros_per_beat / 1_000_000.0
        last_tick = tick
        if kind == "tempo":
            micros_per_beat = value
        else:
            notes.append((seconds, value[0], value[1]))

    return notes


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source")
    parser.add_argument("destination")
    parser.add_argument("--voices", type=int, default=6,
                        help="most bolts on any one tick; the loudest win")
    parser.add_argument("--floor", type=float, default=0.18,
                        help="what the quietest note in the file comes out at")
    parser.add_argument("--tail", type=float, default=2.0,
                        help="seconds of silence left on the end")
    arguments = parser.parse_args()

    notes = read_events(arguments.source)
    if not notes:
        raise SystemExit("no notes in that file")

    loudest = max(velocity for _, _, velocity in notes)

    # Everything on the same tick, so the voice cap can pick between simultaneous notes rather
    # than between a chord and the note after it.
    by_tick = collections.defaultdict(list)
    for seconds, note, velocity in notes:
        # An octave at a time back into the keyboard, so a part written out of range is still
        # played rather than dropped.
        while note < LOWEST:
            note += 12
        while note > HIGHEST:
            note -= 12

        volume = arguments.floor + (1.0 - arguments.floor) * (velocity / loudest)
        by_tick[round(seconds * TICKS_PER_SECOND)].append((volume, note))

    lines = []
    strikes = 0
    for tick in sorted(by_tick):
        voices = sorted(by_tick[tick], reverse=True)[:arguments.voices]
        for volume, note in sorted(voices, key=lambda voice: voice[1]):
            lines.append(f"{tick} {volume:.2f} {note}")
            strikes += 1

    length = max(by_tick) + round(arguments.tail * TICKS_PER_SECOND)

    destination = pathlib.Path(arguments.destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(
        f"# Written by tools/mid_to_song.py. One strike per line: tick volume midi.\n"
        f"# Source: {pathlib.Path(arguments.source).name}\n"
        f"# {strikes} strikes over {length} ticks, at most {arguments.voices} a tick.\n"
        f"length {length}\n" + "\n".join(lines) + "\n", encoding="utf-8")

    print(f"{strikes} strikes over {length} ticks")


if __name__ == "__main__":
    main()
