"""Placeholder art for the tiers 200-205 batch: the shield, the bone and the two engravings.

Everything here is derived rather than drawn, and TEXTURE.md says so for each. Vanilla's own shield
and bone are the sources for the first two, restained to the compression level each item is cut
from, which at least means the silhouettes are correct and the UV layout of the shield is exactly
the one ShieldModel expects. The engraving plates are the usual copy of the Reach plate.

Vanilla's textures are read out of the NeoForge client-extra jar under build/moddev/artifacts, so
this only runs after a gradle build has fetched it. Point MC_JAR somewhere else if that moves.
"""
import colorsys
import glob
import os
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
MC_JAR = os.environ.get("MC_JAR") or sorted(glob.glob(os.path.join(
    REPO, "build/moddev/artifacts/*client-extra*.jar")))[-1]


def vanilla(path):
    with zipfile.ZipFile(MC_JAR) as jar:
        with jar.open("assets/minecraft/textures/" + path) as handle:
            return Image.open(handle).convert("RGBA")


def stone(level):
    name = "compressed_cobblestone.png" if level == 1 else f"compressed_cobblestone_{level}.png"
    return Image.open(os.path.join(TEX, "block", name)).convert("RGBA")


def average_hue(im):
    """The hue and saturation the given stone reads as, so a restain lands on that level's colour."""
    hs, ss, n = 0.0, 0.0, 0
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            hs += h
            ss += s
            n += 1
    return (hs / n, ss / n) if n else (0.0, 0.0)


def restain(im, hue, sat_floor, value_scale=1.0):
    out = Image.new("RGBA", im.size)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            # A flat grey carries no hue of its own, so a rotation alone would leave it grey; the
            # saturation floor is what actually stains it.
            r, g, b = colorsys.hsv_to_rgb(hue, max(s, sat_floor), min(1.0, v * value_scale))
            out.putpixel((x, y), (int(r * 255), int(g * 255), int(b * 255), a))
    return out


def emit(rel, im):
    path = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    print(rel)


# ------------------------------------------------------------------ the shield, tier 200
# Vanilla's plain shield restained to level 200's stone. Keeping vanilla's own file as the source is
# not laziness here: ShieldModel's plate and handle read fixed UVs out of this 64x64 layout, so a
# drawing that is not laid out exactly like it would come out folded.
shield_hue, shield_sat = average_hue(stone(200))
emit("entity/shield/compressed_shield.png",
     restain(vanilla("entity/shield_base_nopattern.png"), shield_hue, max(shield_sat, 0.12), 0.95))

# ------------------------------------------------------------------ the bone, tier 201
bone_hue, bone_sat = average_hue(stone(201))
emit("item/compressed_bone.png",
     restain(vanilla("item/bone.png"), bone_hue, max(bone_sat, 0.15), 0.9))

# ------------------------------------------------------------------ the two engravings
# The Reach plate again, which is the seventh and eighth time - see TEXTURE.md entries 88, 91 and 99.
plate = Image.open(os.path.join(TEX, "item", "reach_engraving.png")).convert("RGBA")
emit("item/uppercut_engraving.png", restain(plate, 0.13, 0.55, 1.15))
emit("item/exploding_sword_engraving.png", restain(plate, 0.03, 0.65, 1.05))
