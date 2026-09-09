"""Builds the pictures the tier 236-242 batch needs.

Three things, and all three are restains or copies of art that already exists here - which is the
honest description rather than an apology, because in each case the new thing IS a rung of something
already drawn:

* ``item/incremental_engraving.png``            - the engraving plate every engraving in the mod
                                                  shares, stained the pale gold of a thing going up
                                                  a level. Every engraving icon in this mod is that
                                                  one plate at a different hue; a new one that drew
                                                  its own would be the odd one out.
* ``item/tier_4_compressed_cobblestone_apple.png`` - the tier 3 apple stained warmer and brighter,
                                                  the way tiers 1 to 3 are one ramp of the same
                                                  drawing. Entry 10 of TEXTURE.md already asks for
                                                  those three to differ by more than their fill and
                                                  this makes it four; that is the thing to fix, and
                                                  fixing it means one new silhouette for all four
                                                  rather than a fourth exception.

The two TNT sides are NOT built here - ``build_tnt_sides.py`` owns every one of those and the two new
charges are entries in its own table. This script only does the two restains and copies the shared
TNT top and bottom faces, which are the same image for every recent charge in the mod.

Run with APPLY=1 to write into the mod; without it a preview goes to build/.
"""
import colorsys
import os
import shutil

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
PREVIEW = os.path.join(REPO, "build", "tier_236_242_preview")

# Whichever charge's faces the recent ones all share; every TNT since the arrow ones uses these.
TOP_SOURCE = "bee_nt_top.png"
BOTTOM_SOURCE = "bee_nt_bottom.png"

NEW_TNTS = ["dome_tnt", "blackhole_tnt"]


def restain(image, hue, sat_floor, value_scale):
    """Re-hues a sprite, leaving its alpha and its shading exactly where they were."""
    out = Image.new("RGBA", image.size)
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            # Flat greys carry no hue of their own, so the saturation floor is what stains them.
            s = max(s, sat_floor)
            r, g, b = colorsys.hsv_to_rgb(hue, s, min(1.0, v * value_scale))
            out.putpixel((x, y), (int(r * 255), int(g * 255), int(b * 255), a))
    return out


def write(relative, image):
    path = os.path.join(root, *relative.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)
    print("wrote", path)


root = MOD if os.environ.get("APPLY") else PREVIEW

# 0.13 is a pale gold, which is as far from the Haste engraving's orange and the Extra Shot's amber
# as the warm half of the wheel goes while still reading as "one rung up".
write("item/incremental_engraving.png",
      restain(Image.open(os.path.join(MOD, "item", "super_reach_engraving.png")).convert("RGBA"),
              0.13, 0.5, 1.15))

# The three apples below this run warm stone, neutral grey, pale violet. 0.08 with the value pushed
# is the warm end again but brighter than tier 1, so the ramp reads as coming back round rather than
# as a fourth point on one line.
write("item/tier_4_compressed_cobblestone_apple.png",
      restain(Image.open(os.path.join(MOD, "item", "tier_3_compressed_cobblestone_apple.png")).convert("RGBA"),
              0.08, 0.45, 1.25))

for name in NEW_TNTS:
    for face, source in (("top", TOP_SOURCE), ("bottom", BOTTOM_SOURCE)):
        destination = os.path.join(root, "block", "%s_%s.png" % (name, face))
        os.makedirs(os.path.dirname(destination), exist_ok=True)
        shutil.copyfile(os.path.join(MOD, "block", source), destination)
        print("copied", destination)
