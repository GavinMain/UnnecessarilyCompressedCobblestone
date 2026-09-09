"""Derives a Material Compressor tier's five faces from an existing tier's.

Every tier of the machine is the same drawing in a different stain: a plate with a dark outline, a
blue arrow pointing in on the top and the input side, an orange arrow pointing out on the bottom and
the output side. The arrows are the only thing a player reads a face by - they are what tell a hopper
which side it may use - so a restain must leave them exactly where and what they are, and move only
the plate underneath.

That is the whole of what this does: pixels that are near-neutral are the plate and get stained,
pixels with any real colour in them are an arrow and are left alone. Tier 4 was made from tier 3 this
way by hand; tier 5 is made from tier 3 here, so the two deep tiers are stained off the same neutral
source rather than one off the other, which would compound the tint.

Run with APPLY=1 to write into the mod; without it everything goes to build/compressor_preview/.
"""
import os
from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
PREVIEW = os.environ.get("PREVIEW_DIR", os.path.join(REPO, "build", "compressor_preview"))
os.makedirs(PREVIEW, exist_ok=True)
APPLY = bool(os.environ.get("APPLY"))

FACES = ["top", "side", "bottom", "input", "output"]

# How far apart a pixel's channels may be and still count as plate rather than arrow. The plate is
# built from true greys (127, 127, 127) and (40, 40, 46); the arrows are (90, 190, 255) and
# (255, 170, 60), which are nowhere near.
NEUTRAL_TOLERANCE = 24

# Per-channel multipliers, applied to the plate only. Netherite is a dark grey-brown with the red
# left in and the green taken out, which is what separates it from the cool blue of the diamond tier
# above it - the two deepest machines must not read as the same block.
STAINS = {
    ("material_compressor_tier_3", "material_compressor_tier_5"): (0.66, 0.56, 0.60),
}


def stain(im, mult):
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if not a or max(r, g, b) - min(r, g, b) > NEUTRAL_TOLERANCE:
                continue  # transparent, or an arrow - leave it exactly as it is
            out.putpixel((x, y), (int(r * mult[0]), int(g * mult[1]), int(b * mult[2]), a))
    return out


written = []
for (src_name, dst_name), mult in STAINS.items():
    for face in FACES:
        src = Image.open(os.path.join(MOD, "block/%s_%s.png" % (src_name, face))).convert("RGBA")
        out = stain(src, mult)
        rel = "block/%s_%s.png" % (dst_name, face)
        out.save(os.path.join(PREVIEW, rel.replace("/", "__")))
        if APPLY:
            out.save(os.path.join(MOD, *rel.split("/")))
        written.append(rel)

print(("applied" if APPLY else "preview only") + ": %d files" % len(written))
for rel in written:
    print("  ", rel)
