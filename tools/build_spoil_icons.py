"""Draws the boss-spoil effect icons.

One per BossSpoil constant, and each one is simply that spoil's own block texture: the icon shows
the stone the effect makes, which is the only fact about it a player needs and the one thing no
amount of drawing would say more clearly. The 16x16 face is centred in the 18x18 canvas vanilla's
mob effect icons use, and a one-pixel dark outline is laid round it so the icon reads as an item
rather than as a patch of the HUD.

The names and levels are read out of BossSpoil.java rather than repeated here, so a new constant in
that enum is picked up by rerunning this and nothing has to be typed twice.
"""
import os
import re

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
ENUM = os.path.join(
    REPO, "src/main/java/net/fahr3n/unnecessarilycompressedcobblestone/util/BossSpoil.java")

OUTLINE = (0x20, 0x1C, 0x18, 255)

# The one level in the enum that is an expression rather than a literal.
GOLEM_TIER_2_LEVEL = 115


def levels():
    """Every (effect name, compression level) pair, off the enum's own constants."""
    source = open(ENUM, encoding="utf-8").read()
    found = []
    for name, level in re.findall(
            r'^\s{4}[A-Z_0-9]+\("([a-z_0-9]+)",\s*([^,]+),', source, re.MULTILINE):
        level = level.strip()
        if level == "ModBlocks.GOLEM_TIER_2_LEVEL + 1":
            level = GOLEM_TIER_2_LEVEL + 1
        found.append((name + "_spoils", int(level)))

    return found


def block_texture(level):
    name = "compressed_cobblestone.png" if level == 1 else f"compressed_cobblestone_{level}.png"
    return Image.open(os.path.join(TEX, "block", name)).convert("RGBA")


def icon(level):
    out = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    out.paste(block_texture(level).resize((16, 16), Image.NEAREST), (1, 1))

    # A border drawn round the face rather than over it, so none of the stone is lost to it.
    for i in range(18):
        for pos in ((i, 0), (i, 17), (0, i), (17, i)):
            out.putpixel(pos, OUTLINE)

    return out


out_dir = os.path.join(TEX, "mob_effect")
os.makedirs(out_dir, exist_ok=True)
for name, level in levels():
    icon(level).save(os.path.join(out_dir, name + ".png"))
    print(f"{name}.png  (level {level})")
