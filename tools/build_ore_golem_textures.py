"""Builds the Compressed Ore Golem's three drawings.

All of them are the mod's own art restated rather than drawn: the golem's skin is the existing
compressed golem speckled with ore, the carved head is the tier 2 head restained to the stone the
new golem is built out of, and the seventeenth heart is the family drawing with a green core.

Recorded in TEXTURE.md as the copies they are. Run with APPLY=1 to write into the mod.
"""
import os
import random

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
PREVIEW = os.path.join(REPO, "build", "ore_golem_preview")

ROOT = MOD if os.environ.get("APPLY") else PREVIEW


def emit(rel, im):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    print("wrote", path)


# ----------------------------------------------------------------- 1. the golem
# Seven ores, in vanilla's own colours. Coal is in the list and is the one that reads at distance,
# because the golem's own stone is mid-grey and only the very dark and the very saturated show.
ORES = [
    (0x18, 0x18, 0x1c),  # coal
    (0xd8, 0xaf, 0x93),  # copper
    (0xd8, 0xc8, 0xa8),  # iron
    (0xfc, 0xdc, 0x50),  # gold
    (0xff, 0x2f, 0x2f),  # redstone
    (0x3b, 0x63, 0xcf),  # lapis
    (0x17, 0xdd, 0x62),  # emerald
    (0x5c, 0xdb, 0xd5),  # diamond
]

golem = Image.open(os.path.join(MOD, "entity/compressed_golem/compressed_golem.png")).convert("RGBA")
ore = golem.copy()

# Seeded, so the same skin comes out every run and a rebuild is not a silent change to the mod.
rng = random.Random(222)
for y in range(ore.height):
    for x in range(ore.width):
        r, g, b, a = ore.getpixel((x, y))
        if a == 0:
            continue

        # A speck every fortieth pixel, in ore-sized clusters of one or two. Anything denser reads
        # as static rather than as stone with something in it.
        if rng.random() < 0.025:
            vein = ORES[rng.randrange(len(ORES))]
            # Kept slightly under full saturation and blended with the stone under it, so the
            # golem still looks like rock with ore in it rather than a golem made of gemstones.
            ore.putpixel((x, y), (
                (vein[0] * 3 + r) // 4, (vein[1] * 3 + g) // 4, (vein[2] * 3 + b) // 4, a))

emit("entity/compressed_golem/compressed_ore_golem.png", ore)

# ------------------------------------------------------------ 2. the carved head
# The tier 2 face, recoloured onto the tier 222 stone's own palette so the head matches the body it
# sits on. The face is whatever is darker than the stone around it, which is how the carving reads.
head = Image.open(os.path.join(MOD, "block/carved_cobblestone_tier_2.png")).convert("RGBA")
body_2 = Image.open(os.path.join(MOD, "block/compressed_cobblestone_115.png")).convert("RGBA")
body_3 = Image.open(os.path.join(MOD, "block/compressed_cobblestone_222.png")).convert("RGBA")


def average(im):
    pixels = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width)]
    n = len(pixels)
    return tuple(sum(p[i] for p in pixels) // n for i in range(3))


old, new = average(body_2), average(body_3)
carved = Image.new("RGBA", head.size)
for y in range(head.height):
    for x in range(head.width):
        r, g, b, a = head.getpixel((x, y))
        # Shift the whole tile by the difference between the two stones' average colours, which
        # keeps the carved face's contrast exactly where it was and only moves the hue under it.
        carved.putpixel((x, y), (
            max(0, min(255, r - old[0] + new[0])),
            max(0, min(255, g - old[1] + new[1])),
            max(0, min(255, b - old[2] + new[2])), a))

emit("block/carved_cobblestone_tier_3.png", carved)

# ------------------------------------------------------------ 3. the heart
# The family drawing again, in the green of the boss bar. That is sixteen hearts sharing one
# silhouette now; see TEXTURE.md, which has been asking about this since the third.
heart = Image.open(os.path.join(MOD, "item/tier_2_compressed_heart.png")).convert("RGBA")
CORE, CORE_LIT = (0x3c, 0xd0, 0x6e, 255), (0xb4, 0xf5, 0xcb, 255)
h = heart.copy()
for y in range(6, 10):
    for x in range(6, 10):
        h.putpixel((x, y), CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else CORE)

emit("item/tier_17_compressed_heart.png", h)
