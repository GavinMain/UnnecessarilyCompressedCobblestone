"""Builds the pictures the tier 229-234 batch needs.

Four things, and three of them are restains of art that already exists here - which is the honest
description rather than an apology, because in each case the new thing IS the old thing with one
property inverted or swapped, and a drawing that said otherwise would be lying about the item:

* ``block/damage_web.png``     - the Regen Web restained from green to red. The two blocks are the
                                 same cobweb with the sign of the effect flipped, and red-for-harm
                                 against green-for-heal is the one colour convention this mod can
                                 rely on a player already knowing.
* ``item/compressed_healing_staff.png`` - the Summoning Staff's shaft restained warm red, since a
                                 staff is a shaft with a head and the head is the only part that
                                 ever differs.

The TNT sides are NOT built here - ``build_tnt_sides.py`` owns every one of those, and the three new
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
PREVIEW = os.path.join(REPO, "build", "tier_229_234_preview")

# Whichever charge's faces the recent ones all share; every TNT since the arrow ones uses these.
TOP_SOURCE = "bee_nt_top.png"
BOTTOM_SOURCE = "bee_nt_bottom.png"

NEW_TNTS = ["flat_tnt", "damage_web_tnt", "laser_tnt"]


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

# Hue 0.0 is red; the Regen Web sits in the greens, so the floor is what actually moves it.
write("block/damage_web.png",
      restain(Image.open(os.path.join(MOD, "block", "regen_web.png")).convert("RGBA"),
              0.0, 0.55, 1.0))

# 0.03 is a warm red rather than a pure one - the staff is a heal, and blood red would read as the
# web above it rather than as a potion.
write("item/compressed_healing_staff.png",
      restain(Image.open(os.path.join(MOD, "item", "compressed_summoning_staff.png")).convert("RGBA"),
              0.03, 0.45, 1.1))

for name in NEW_TNTS:
    for face, source in (("top", TOP_SOURCE), ("bottom", BOTTOM_SOURCE)):
        destination = os.path.join(root, "block", "%s_%s.png" % (name, face))
        os.makedirs(os.path.dirname(destination), exist_ok=True)
        shutil.copyfile(os.path.join(MOD, "block", source), destination)
        print("copied", destination)
