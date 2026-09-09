"""Builds the eighty-eight note bolts: one sample, one texture and one sound entry per piano key.

A note bolt is an ordinary lightning bolt that plays a piano note instead of thunder, so the mod
needs a real sample for every key of a grand piano - and there is no way to get eighty-eight of
them out of Minecraft. Vanilla's harp is one sample stretched over two octaves, and the sound
engine clamps pitch to [0.5, 2], so nothing that starts from a vanilla sound can cover the seven
and a quarter octaves a piano has. They are synthesised here instead.

The tone is plain additive synthesis of a struck string: partials at slightly stretched multiples
of the fundamental (a real string is stiff, so its overtones run sharp - that stretch is most of
what makes a piano sound like a piano rather than an organ), each decaying on its own and the high
ones decaying fastest, over a short noise burst for the hammer. It is not a sampled Steinway, and
it is not meant to be: what it has to do is be unmistakably *that* key, in tune, at any dynamic.

Everything this writes is generated, so nothing here is edited by hand:

    assets/<modid>/sounds/note/<key>.ogg     eighty-eight samples
    assets/<modid>/sounds.json               the sound events they are registered under
    assets/<modid>/textures/item/<key>_bolt.png   the item icons
    assets/<modid>/lang/en_us.json           the item and sound-event names (merged, not replaced)

Run it from the repository root:

    python tools/build_note_bolts.py
"""

import json
import pathlib

import numpy as np
import soundfile as sf
from PIL import Image

MOD_ID = "unnecessarilycompressedcobblestone"
ASSETS = pathlib.Path("src/main/resources/assets") / MOD_ID

SAMPLE_RATE = 44100

# A0, the bottom key of a piano, is MIDI 21. Every key up from it is a twelfth of an octave.
KEYS = 88
A0_HZ = 27.5

# The twelve pitch classes as the keys are named, starting from A because the piano does.
NAMES = ["a", "a_sharp", "b", "c", "c_sharp", "d", "d_sharp", "e", "f", "f_sharp", "g", "g_sharp"]
DISPLAY = ["A", "A Sharp", "B", "C", "C Sharp", "D", "D Sharp", "E", "F", "F Sharp", "G", "G Sharp"]

# What each pitch class is drawn in: the twelve dye colours its recipe is keyed on, so the icon
# says which dye made it. Octave is brightness on top of this, plus the digit in the corner.
DYES = {
    "c": (249, 255, 254), "c_sharp": (157, 157, 151), "d": (71, 79, 82), "d_sharp": (29, 29, 33),
    "e": (131, 84, 50), "f": (176, 46, 38), "f_sharp": (249, 128, 29), "g": (254, 216, 61),
    "g_sharp": (128, 199, 31), "a": (94, 124, 22), "a_sharp": (22, 156, 156), "b": (60, 68, 170),
}

# A 3x5 pixel font for the octave digit in the icon's corner. Nothing else is small enough to read.
DIGITS = [
    "111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
    "111100111001111", "111100111101111", "111001010010010", "111101111101111",
]


def key_name(key):
    """``a0`` through ``c8``: the pitch class and the octave, the way a piano key is named."""
    pitch_class = key % 12
    # Octaves turn over at C, and the keyboard starts three semitones below C1.
    octave = (key + 9) // 12
    return f"{NAMES[pitch_class]}_{octave}", f"{DISPLAY[pitch_class]} {octave}", NAMES[pitch_class], octave


def sample(key):
    """One struck string at this key's pitch, as float32 mono."""
    f0 = A0_HZ * 2.0 ** (key / 12.0)

    # How long the note rings for. A piano's bass strings ring for the better part of a minute and
    # its top octave for barely a second; this is that shape, cut short enough that eighty-eight
    # samples stay a couple of megabytes rather than fifty.
    decay = float(np.interp(np.log2(f0), [np.log2(27.5), np.log2(440.0), np.log2(4186.0)], [1.9, 1.0, 0.32]))
    length = min(3.2, decay * 2.6)
    t = np.arange(int(length * SAMPLE_RATE), dtype=np.float64) / SAMPLE_RATE

    # Stiffness: the nth partial of a real string sits above n * f0 rather than on it, and thicker
    # (lower) strings are stiffer. This is the whole difference between a piano and an organ.
    stiffness = 0.00035 * (110.0 / max(f0, 27.5)) ** 0.5

    out = np.zeros_like(t)
    rng = np.random.default_rng(key)
    for n in range(1, 41):
        freq = n * f0 * np.sqrt(1.0 + stiffness * n * n)
        if freq > SAMPLE_RATE * 0.45:
            break

        # Odd partials a touch stronger than even, which is where the hollowness in a piano's
        # bottom two octaves comes from; high partials both start quieter and die faster.
        amplitude = (1.0 / n ** 1.35) * (1.0 if n % 2 else 0.75)
        out += amplitude * np.sin(2.0 * np.pi * freq * t + rng.uniform(0.0, 2.0 * np.pi)) \
            * np.exp(-t / (decay / n ** 0.55))

    # The hammer itself: a few milliseconds of noise, which is what stops the attack sounding like
    # a sine turning on.
    thump = int(0.006 * SAMPLE_RATE)
    out[:thump] += rng.uniform(-0.35, 0.35, thump) * np.linspace(1.0, 0.0, thump) ** 2

    # A short attack ramp and a short release, so nothing clicks at either end.
    attack = int(0.003 * SAMPLE_RATE)
    out[:attack] *= np.linspace(0.0, 1.0, attack)
    release = int(0.02 * SAMPLE_RATE)
    out[-release:] *= np.linspace(1.0, 0.0, release)

    peak = np.max(np.abs(out))
    return (out / peak * 0.85).astype(np.float32) if peak > 0 else out.astype(np.float32)


def icon(base, name, pitch_class, octave):
    """The vanilla bolt's own zigzag in this key's dye colour, with the octave in the corner."""
    pixels = np.array(base, dtype=np.float32)
    red, green, blue = DYES[pitch_class]

    # Octave as brightness: the bottom of the keyboard is dark and the top is nearly white, so two
    # bolts of the same pitch class an octave apart are told apart at inventory size.
    shade = 0.55 + 0.09 * octave
    tint = np.clip(np.array([red, green, blue], dtype=np.float32) * shade, 0.0, 255.0)
    pixels[:, :, :3] = tint

    # The digit sits bottom left, where the zigzag is not, in whichever of black or white the
    # colour behind it is not.
    ink = 20 if shade > 0.95 else 245
    glyph = DIGITS[octave]
    for row in range(5):
        for column in range(3):
            if glyph[row * 3 + column] == "1":
                pixels[10 + row, 1 + column] = (ink, ink, ink, 255)

    Image.fromarray(pixels.astype(np.uint8), "RGBA").save(
        ASSETS / "textures" / "item" / f"{name}_bolt.png")


def main():
    sounds_dir = ASSETS / "sounds" / "note"
    sounds_dir.mkdir(parents=True, exist_ok=True)

    base = Image.open(ASSETS / "textures" / "item" / "compressed_vanilla_bolt.png").convert("RGBA")

    sounds = {}
    lang = {}
    for key in range(KEYS):
        name, display, pitch_class, octave = key_name(key)

        sf.write(sounds_dir / f"{name}.ogg", sample(key), SAMPLE_RATE, format="OGG", subtype="VORBIS")
        icon(base, name, pitch_class, octave)

        sounds[f"note.{name}"] = {
            "sounds": [{"name": f"{MOD_ID}:note/{name}", "stream": False}],
            "subtitle": f"subtitles.{MOD_ID}.note",
        }
        lang[f"item.{MOD_ID}.{name}_bolt"] = f"{display} Bolt"

    # The Moonlight Bolt is not a key of the piano but it is drawn from the same base, in the pale
    # blue the piece is named for.
    moonlight = np.array(base, dtype=np.float32)
    moonlight[:, :, :3] = (188, 214, 255)
    Image.fromarray(moonlight.astype(np.uint8), "RGBA").save(
        ASSETS / "textures" / "item" / "moonlight_bolt.png")

    lang[f"subtitles.{MOD_ID}.note"] = "Piano note"

    (ASSETS / "sounds.json").write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")

    # Merged rather than written: everything else in the lang file is hand-written.
    lang_path = ASSETS / "lang" / "en_us.json"
    existing = json.loads(lang_path.read_text(encoding="utf-8"))
    existing.update(lang)
    lang_path.write_text(json.dumps(existing, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"{KEYS} samples, icons and sound events written")


if __name__ == "__main__":
    main()
