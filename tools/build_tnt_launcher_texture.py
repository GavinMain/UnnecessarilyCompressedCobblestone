"""Builds the TNT Launcher's four icons.

The Bolt Launcher's own drawing with the string restained TNT red. That is deliberate rather than
lazy: the two launchers are the same weapon holding different ammunition, so they should read as a
pair, and the string is the part of a bow that the ammunition sits against. The stave keeps the deep
stone blue every other piece of gear here is drawn in.

Recorded in TEXTURE.md as the copy it is - two weapons that differ by one colour is thin, and the
family wants a real second drawing.

Run with APPLY=1 to write into the mod; without it a preview goes to build/.
"""
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures/item")
PREVIEW = os.path.join(REPO, "build", "tnt_launcher_preview")

FRAMES = ["", "_pulling_0", "_pulling_1", "_pulling_2"]

# The stave, left exactly as the Bolt Launcher's is so the two are visibly the same weapon.
STAVE = (26, 30, 64, 255)

# The string and the nocked charge, on a red ramp: dark to bright, the colours of a TNT block's band.
RAMP = [
    (0x8b, 0x1f, 0x18),
    (0xc0, 0x33, 0x28),
    (0xdb, 0x4a, 0x38),
    (0xf2, 0x7a, 0x64),
]


def restain(px):
    r, g, b, a = px
    if a == 0 or px == STAVE:
        return px

    lum = (r * 299 + g * 587 + b * 114) // 1000
    # The pale half of the source runs roughly 215-255, so the ramp is walked over that band rather
    # than over the whole of 0-255 - otherwise every string pixel lands on the brightest rung.
    step = min(len(RAMP) - 1, max(0, (lum - 210) * len(RAMP) // 46))
    return RAMP[step] + (a,)


root = MOD if os.environ.get("APPLY") else PREVIEW
os.makedirs(root, exist_ok=True)

for frame in FRAMES:
    src = Image.open(os.path.join(MOD, "bolt_launcher%s.png" % frame)).convert("RGBA")
    out = Image.new("RGBA", src.size)
    for y in range(src.height):
        for x in range(src.width):
            out.putpixel((x, y), restain(src.getpixel((x, y))))

    path = os.path.join(root, "tnt_launcher%s.png" % frame)
    out.save(path)
    print("wrote", path)
