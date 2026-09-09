"""Builds the Compressed Scythe's icon.

Hand-typed rather than restained, because there is nothing in the mod or in vanilla shaped like a
scythe to restain - the whole point of the weapon is that it is not an axe. It is drawn on the same
bottom-left-to-top-right diagonal every vanilla tool uses, so the handheld model holds it the way it
holds a pickaxe: a dark stone haft with a light stone blade curving off the top of it.

Run with APPLY=1 to write into the mod; without it a preview goes to build/.
"""
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures/item")
PREVIEW = os.path.join(REPO, "build", "scythe_preview")

# The mod's stone ramp: the same four colours the Bolt Launcher and the Compressed Saddle are in.
COLORS = {
    "h": (0x22, 0x25, 0x3c, 255),  # haft, darkest
    "H": (0x2d, 0x31, 0x6e, 255),  # haft highlight
    "#": (0x6d, 0x74, 0xc8, 255),  # blade
    "*": (0x9a, 0xa0, 0xda, 255),  # blade edge, lightest
}

FIGURE = """
................
....****....H...
..**......#.h...
.*#......#.Hh...
.*#.....#.hh....
..*#..##.hh.....
...*###..h......
.........h......
........hh......
........h.......
.......hh.......
.......h........
......hh........
......h.........
.....hh.........
................
"""


def build():
    rows = [row for row in FIGURE.split("\n") if row]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in COLORS:
                im.putpixel((x, y), COLORS[ch])

    return im


root = MOD if os.environ.get("APPLY") else PREVIEW
os.makedirs(root, exist_ok=True)
path = os.path.join(root, "compressed_scythe.png")
build().save(path)
print("wrote", path)
