"""Derives the gear textures that are not drawn by hand.

Five things, all off art the mod already ships: the tier 1 and tier 8 hearts (tier 2's drawing with
a red and a soul-blue core), the tier 1 apple (tier 2's drawing warmed and darkened), the jump/lightning/arrow armour sets (the compressed set with its black line network taken
to a hue - green, yellow, grey), and the three weapons (vanilla stone gear with black seams cut
across it). Run with APPLY=1 to write into the mod; without it everything and a contact sheet go
to build/gear_preview/ instead.

The armour reads its source from the hand-drawn _after files in the texture scratch folder, which
is not in the repo - point COBBLE_TEXTURES at it, or re-point CT at the mod's own icons once the
compressed set stops moving.
"""
import colorsys, os, shutil
from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
CT = os.environ.get("COBBLE_TEXTURES", "C:/Users/gavin/OneDrive/Documents/cobble_textures")
PREVIEW = os.environ.get("PREVIEW_DIR", os.path.join(REPO, "build", "gear_preview"))
os.makedirs(PREVIEW, exist_ok=True)
APPLY = bool(os.environ.get("APPLY"))

out_files = {}


def emit(rel, im):
    out_files[rel] = im
    im.save(os.path.join(PREVIEW, rel.replace("/", "__")))


# ---------------------------------------------------------------- 1. heart tier 1
heart = Image.open(os.path.join(MOD, "item/tier_2_compressed_heart.png")).convert("RGBA")
CORE, CORE_LIT = (0xff, 0x3b, 0x3b, 255), (0xff, 0xc4, 0xc4, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else CORE)
emit("item/tier_1_compressed_heart.png", h)

# The Compressed Spirit's heart, the eighth of the family and the same drawing again: the soul-fire
# blue-green of the spirit itself, which is also what its egg is coloured. That makes eight hearts
# that are one drawing in eight cores, which TEXTURE.md has been asking to fix since the third.
SPIRIT_CORE, SPIRIT_CORE_LIT = (0x6b, 0xe3, 0xe8, 255), (0xd6, 0xfa, 0xfc, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), SPIRIT_CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else SPIRIT_CORE)
emit("item/tier_8_compressed_heart.png", h)

# And the second golem's, the ninth: the yellow of its boss bar.
GOLEM_2_CORE, GOLEM_2_CORE_LIT = (0xf2, 0xd1, 0x3b, 255), (0xff, 0xf3, 0xc4, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), GOLEM_2_CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else GOLEM_2_CORE)
emit("item/tier_9_compressed_heart.png", h)

# And the Compressed Witch's, the tenth: witch purple.
WITCH_CORE, WITCH_CORE_LIT = (0xb0, 0x50, 0xd0, 255), (0xee, 0xcc, 0xff, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), WITCH_CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else WITCH_CORE)
emit("item/tier_10_compressed_heart.png", h)


# And the Compressed Guardian's, the fourteenth: prismarine teal, the colour of the thing itself.
GUARDIAN_CORE, GUARDIAN_CORE_LIT = (0x3f, 0xa0, 0x96, 255), (0xba, 0xf5, 0xea, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), GUARDIAN_CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else GUARDIAN_CORE)
emit("item/tier_14_compressed_heart.png", h)


# And the Compressed Husk's, the fifteenth: the pale dry sand of the desert it comes out of. That is
# now seven of the fifteen hearts that are one drawing in seven cores, which TEXTURE.md has been
# asking to fix since the third and which this rung makes no better.
HUSK_CORE, HUSK_CORE_LIT = (0xc2, 0xa8, 0x66, 255), (0xf2, 0xe6, 0xc4, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), HUSK_CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else HUSK_CORE)
emit("item/tier_15_compressed_heart.png", h)

# And the Compressed Snow Golem's, the sixteenth: the pale blue-white of the thing itself. Eight of
# the sixteen hearts are now one drawing in eight cores.
SNOW_GOLEM_CORE, SNOW_GOLEM_CORE_LIT = (0xa8, 0xc8, 0xe0, 255), (0xee, 0xf6, 0xff, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), SNOW_GOLEM_CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else SNOW_GOLEM_CORE)
emit("item/tier_16_compressed_heart.png", h)


# ---------------------------------------------------------------- 1b. apple tier 1
# Tiers 2 and 3 are one drawing in two shades - tier 3 is tier 2 plus a flat (34, 26, 44), which
# lifts it and turns it violet. Tier 1 goes the other way: the same drawing scaled down and warmed,
# so the three read as one dark-to-light ramp instead of three separate apples.
apple = Image.open(os.path.join(MOD, "item/tier_2_compressed_cobblestone_apple.png")).convert("RGBA")
WARM = (0.86, 0.76, 0.62)
a1 = apple.copy()
for y in range(apple.height):
    for x in range(apple.width):
        r, g, b, al = apple.getpixel((x, y))
        if al:
            a1.putpixel((x, y), (int(r * WARM[0]), int(g * WARM[1]), int(b * WARM[2]), al))
emit("item/tier_1_compressed_cobblestone_apple.png", a1)

# ---------------------------------------------------------------- 2. armour sets
# The compressed set is vanilla iron grey broken up by a near-black line network. The other sets
# are that same art with the line network taken to a hue; the grey plates are left alone so the
# four sets stay one family.
def line_recolour(im, hue, sat, vlo, vhi):
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if not a:
                continue
            lum = (r * 299 + g * 587 + b * 114) // 1000
            if lum >= 64 or max(r, g, b) - min(r, g, b) > 24:
                continue  # a plate, or already coloured - leave it
            v = vlo + (vhi - vlo) * (lum / 63.0)
            nr, ng, nb = colorsys.hsv_to_rgb(hue / 360.0, sat, v)
            out.putpixel((x, y), (int(nr * 255), int(ng * 255), int(nb * 255), a))
    return out


SETS = {
    "compression_jump":      (120, 0.85, 0.30, 0.78),   # green lines
    "compression_lightning": (48, 0.95, 0.34, 0.86),    # yellow lines
    "compression_arrow":     (0, 0.0, 0.22, 0.55),      # grey lines
    "compression_magic":     (285, 0.80, 0.32, 0.84),   # violet lines
    "compression_rain":      (205, 0.85, 0.30, 0.82),   # rain blue lines
    # The last set, and the last hue left that reads as its own at inventory size: everything else
    # here is green, yellow, grey, violet or blue, so red is the one that cannot be mistaken for a
    # neighbour.
    "ultimate_compressed":   (0, 0.90, 0.34, 0.88),     # red lines
}
PIECES = ["helmet", "chestplate", "leggings", "boots"]

for prefix, (hue, sat, vlo, vhi) in SETS.items():
    for piece in PIECES:
        src = Image.open(os.path.join(
            CT, "item/compressed_cobblestone_%s_after.png" % piece)).convert("RGBA")
        emit("item/%s_%s.png" % (prefix, piece), line_recolour(src, hue, sat, vlo, vhi))
    # the worn leggings layer, off the compressed layer the user drew
    layer = Image.open(os.path.join(CT, "armor/compressed_cobblestone_layer_2_after.png")).convert("RGBA")
    emit("armor/%s_layer_2.png" % prefix, line_recolour(layer, hue, sat, vlo, vhi))


# ---------------------------------------------------------------- 3. weapons
# Vanilla art with black seams cut across the stone, the same way the armour is broken up.
BLACK = (0x00, 0x00, 0x00, 255)
SEAMS = {
    "compressed_cobblestone_sword": [(12, 2), (13, 2), (14, 2),
                                     (9, 5), (10, 5), (11, 5),
                                     (6, 8), (7, 8)],
    "compressed_cobblestone_pickaxe": [(8, 3), (9, 3),
                                       (11, 4),
                                       (13, 7)],
    "compressed_cobblestone_mace": [(8, 2), (9, 2), (10, 2), (11, 2),
                                    (6, 5), (7, 5), (8, 5), (9, 5), (10, 5)],
}
for name, seam in SEAMS.items():
    base = os.path.join(CT, "item/%s.png" % name)
    im = Image.open(base if os.path.exists(base) else
                    os.path.join(MOD, "item/%s.png" % name)).convert("RGBA")
    for x, y in seam:
        if im.getpixel((x, y))[3]:
            im.putpixel((x, y), BLACK)
    emit("item/%s.png" % name, im)


# ---------------------------------------------------------------- contact sheet
order = sorted(out_files)
cols = 6
rows = (len(order) + cols - 1) // cols
sheet = Image.new("RGBA", (cols * 96, rows * 96), (255, 255, 255, 255))
for i, rel in enumerate(order):
    im = out_files[rel]
    if im.size != (16, 16):
        im = im.crop((0, 0, 64, 32)).resize((96, 48), Image.NEAREST)
    else:
        im = im.resize((96, 96), Image.NEAREST)
    sheet.alpha_composite(im, ((i % cols) * 96, (i // cols) * 96))
sheet.save(os.path.join(PREVIEW, "_sheet.png"))

if APPLY:
    for rel, im in out_files.items():
        im.save(os.path.join(MOD, *rel.split("/")))
    print("applied %d files" % len(out_files))
else:
    print("preview only: %d files" % len(out_files))
for rel in order:
    print("  ", rel)
