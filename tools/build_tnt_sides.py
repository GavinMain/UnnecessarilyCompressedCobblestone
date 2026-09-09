"""Builds every TNT side texture.

One design for all of them: a framed blue plate, the effect's own item icon in the middle where the
effect has an item and a hand-drawn mini figure where it does not, and a tier bar down the left and
right edges - green 1, yellow 2, red 3, purple 4. The plate, the frame and the bar geometry are
lifted off the hand-drawn chicken tile, which is also where the chicken figure itself comes from.

Adding a TNT is a line in TNTS, not a new drawing. Run with APPLY=1 to write into the mod; without
it, tiles and a contact sheet go to build/tnt_preview/ instead.
"""
import os, zipfile
from PIL import Image

JAR = os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar")
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
# Where the chicken tile the whole design is lifted off lives, and where previews are dropped.
CT = os.environ.get("COBBLE_TEXTURES", "C:/Users/gavin/OneDrive/Documents/cobble_textures")
PREVIEW = os.environ.get("PREVIEW_DIR", os.path.join(REPO, "build", "tnt_preview"))
os.makedirs(PREVIEW, exist_ok=True)
z = zipfile.ZipFile(JAR)


def vanilla(path):
    from io import BytesIO
    return Image.open(BytesIO(z.read("assets/minecraft/textures/" + path))).convert("RGBA")


FRAME = (0x46, 0x4c, 0xb7, 255)
FILL = (0x2d, 0x31, 0x6e, 255)
DASH = (0x14, 0x14, 0x21, 255)

TIER_COLOR = {
    1: (0x5b, 0xca, 0x51, 255),   # green - the user's own bar colour
    2: (0xf2, 0xd1, 0x3b, 255),   # yellow
    3: (0xe0, 0x4a, 0x3a, 255),   # red
    4: (0xa5, 0x58, 0xd6, 255),   # purple
}
BAR_ROWS = range(6, 10)
BAR_COLS = [0, 1, 2, 13, 14, 15]


def background():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y in (0, 15) or x in (0, 15):
                c = FRAME
            elif (x - 2) % 3 == 0 if y <= 9 else (x - 1) % 3 == 0:
                c = DASH
            else:
                c = FILL
            im.putpixel((x, y), c)
    return im


def bar(im, tier):
    c = TIER_COLOR[tier]
    for y in BAR_ROWS:
        for x in BAR_COLS:
            im.putpixel((x, y), c)


# ---------------------------------------------------------------- mini figures
# 12x12, drawn into x2..13 / y2..13. '.' is transparent.
FIGURES = {}

FIGURES["guardian"] = ("""
...######...
..#oooooo#..
.#oo####oo#.
#oo#EEEEE#o#
#oo#EEpEE#o#
#oo#EEEEE#o#
#oo##EEE##o#
.#oo#####o#.
.#oooooooo#.
..#oo##oo#..
...s#..#s...
..s..ss..s..
""", {"#": (0x1c, 0x3c, 0x3a, 255), "o": (0x4f, 0x8f, 0x86, 255),
      "E": (0xe8, 0xdc, 0xc4, 255), "p": (0xc0, 0x3a, 0x2a, 255),
      "s": (0x2e, 0x5c, 0x58, 255)})

FIGURES["golem"] = ("""
...######...
...#gEgE#...
...#gggg#...
...##gg##...
.##########.
##gggggggg##
##gggggggg##
.#g######g#.
.#g#gggg#g#.
....#gg#....
....#gg#....
...##..##...
""", {"#": (0x4a, 0x46, 0x42, 255), "g": (0xc4, 0xc0, 0xb8, 255),
      "E": (0x20, 0x20, 0x24, 255)})

FIGURES["anvil"] = ("""
............
.##########.
.#gggggggg#.
.##########.
..#gggggg#..
...######...
....####....
...######...
..########..
.#gggggggg#.
.##########.
............
""", {"#": (0x2a, 0x2a, 0x2e, 255), "g": (0x6e, 0x6e, 0x74, 255)})

FIGURES["random"] = ("""
...######...
..##wwww##..
.##ww##ww##.
.##ww##ww##.
.######ww##.
.....##ww#..
....##ww##..
....##ww#...
....#ww#....
............
....####....
....#ww#....
""", {"#": (0x2a, 0x14, 0x3e, 255), "w": (0xff, 0xff, 0xff, 255)})

FIGURES["spiral"] = ("""
...######...
..#wwwwww#..
.#w######w#.
.#w#wwww#w#.
.#w#w##w#w#.
.#w#w#ww#w#.
.#w#w####w#.
.#w#wwwwww#.
.#w########.
.#wwwwwwww#.
..########..
............
""", {"#": (0x1a, 0x2a, 0x3e, 255), "w": (0xd8, 0xe8, 0xf4, 255)})

FIGURES["flash"] = ("""
.....##.....
..#..ww..#..
..##.ww.##..
...#.ww.#...
....#ww#....
##.##ww##.##
##wwwwwwww##
....#ww#....
...#.ww.#...
..##.ww.##..
..#..ww..#..
.....##.....
""", {"#": (0xa8, 0x96, 0x30, 255), "w": (0xff, 0xff, 0xd8, 255)})

FIGURES["song"] = ("""
....b#......
...bb#......
..bb##......
.bbb####nnnn
.####bb#nnnn
...bbb#.n##n
..bbb#..n##n
.bbb#...n##n
..b#..nnn##n
.....nn##nn.
.....nn##nn.
......nnnn..
""", {"b": (0xff, 0xf0, 0x8a, 255), "#": (0x2a, 0x2a, 0x44, 255),
      "n": (0xff, 0xff, 0xff, 255)})


def draw_figure(im, name):
    art, pal = FIGURES[name]
    rows = [r for r in art.strip("\n").split("\n")]
    assert len(rows) == 12 and all(len(r) == 12 for r in rows), (name, len(rows), [len(r) for r in rows])
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch != ".":
                im.putpixel((2 + i, 2 + j), pal[ch])


def mini_block(im, tex, backing=None):
    """A 12x12 'block' plate: the texture squeezed to 10x10 inside a dark 1px border."""
    small = tex.resize((10, 10), Image.NEAREST)
    if backing:
        plate = Image.new("RGBA", (10, 10), backing)
        plate.alpha_composite(small)
        small = plate
    for y in range(12):
        for x in range(12):
            if x in (0, 11) or y in (0, 11):
                im.putpixel((2 + x, 2 + y), DASH)
    im.alpha_composite(small, (3, 3))


# ---------------------------------------------------------------- the roster
# name -> (kind, payload, tier)
ITEM, BLOCKMINI, FIGURE, CHICKEN = "item", "block", "figure", "chicken"

# Two chevrons, the fast-forward mark: the Zoom TNT's effect is Speed X and a step height, and
# neither has an item to put in the middle of the plate.
FIGURES["zoom"] = ("""
............
............
.##...##....
.###..###...
.####.####..
.#####.####.
.#####.####.
.####.####..
.###..###...
.##...##....
............
............
""", {"#": (0x5b, 0xca, 0x51, 255)})


# A silverfish seen from above: a segmented grey body with legs down both sides. The second nest
# flies and bites through armour, and the tier bar is the only thing saying so.
FIGURES["silverfish"] = ("""
............
.....##.....
....#bb#....
..l#bbbb#l..
...#bbbb#...
..l#bbbb#l..
...#bbbb#...
..l#bbbb#l..
...#bbbb#...
....#bb#....
.....##.....
............
""", {"#": (0x2b, 0x2b, 0x30, 255), "b": (0x9a, 0x9a, 0xa2, 255),
      "l": (0x6a, 0x6a, 0x72, 255)})


# Three columns of deep stone standing to different heights: the Pillar TNT plants level 213 through
# the chunks around it, and there is no item anywhere in the mod that says "a pillar".
FIGURES["pillars"] = ("""
.##.....##..
.##.....##..
.##.##..##..
s##s##..##.s
.##.##.s##..
.##.##..##..
.##.##..##.s
s##.##..##..
.##.##..##..
.##.##.s##..
.##.##..##..
.##.##..##..
""", {"#": (0x8a, 0x8f, 0xb0, 255), "s": (0x5a, 0x5f, 0x80, 255)})


TNTS = {
    "compressed_tnt":       (BLOCKMINI, "block/tnt_side.png", 1),
    "super_compressed_tnt": (BLOCKMINI, "block/tnt_side.png", 2),
    "flower_tnt":           (ITEM, "block/poppy.png", 1),
    "guardian_tnt":         (FIGURE, "guardian", 1),
    "chunk_tnt":            (BLOCKMINI, "block/cobblestone.png", 1),
    "glass_tnt":            (BLOCKMINI, "block/glass.png", 1),
    "glass_tnt_tier_2":     (BLOCKMINI, "block/glass.png", 2),
    "chicken_tnt":          (CHICKEN, None, 1),
    "chicken_tnt_tier_2":   (CHICKEN, None, 2),
    "chicken_tnt_tier_3":   (CHICKEN, None, 3),
    "golem_tnt":            (FIGURE, "golem", 1),
    "apple_tnt":            (ITEM, "item/apple.png", 1),
    "random_tnt":           (FIGURE, "random", 1),
    "trident_enchant_tnt":  (ITEM, "item/trident.png", 1),
    "binding_tnt":          (ITEM, "@leather", 1),
    "anvil_tnt":            (FIGURE, "anvil", 1),
    "levitation_tnt":       (ITEM, "item/shulker_shell.png", 1),
    "treent_tnt":           (ITEM, "block/oak_sapling.png", 1),
    "arrow_tnt":            (ITEM, "item/arrow.png", 1),
    "arrow_spiral_tnt":     (FIGURE, "spiral", 1),
    "breeding_tnt":         (ITEM, "item/wheat.png", 1),
    "effect_tnt":           (ITEM, "@potion", 1),
    "flash_tnt":            (FIGURE, "flash", 1),
    "lightning_song_tnt":   (FIGURE, "song", 1),
    "pinball_tnt":          (ITEM, "item/wind_charge.png", 1),
    "bolt_tnt":             (ITEM, "@mod:item/compressed_vanilla_bolt.png", 1),
    "moonlight_tnt":        (FIGURE, "song", 1),
    "pool_tnt":             (BLOCKMINI, "block/water_still.png", 1),
    "lightning_song_tnt_2": (FIGURE, "song", 2),
    "zoom_tnt":             (FIGURE, "zoom", 1),
    "web_tnt":              (BLOCKMINI, "block/cobweb.png", 1),
    "portal_tnt":           (BLOCKMINI, "block/nether_portal.png", 1),
    "spawner_tnt":          (BLOCKMINI, "block/spawner.png", 1),
    "snow_tnt":             (BLOCKMINI, "block/snow.png", 1),
    "silverfish_tnt":       (FIGURE, "silverfish", 1),
    "silverfish_tnt_tier_2":(FIGURE, "silverfish", 2),
    "geyser_tnt":           (BLOCKMINI, "block/water_still.png", 1),
    "geyser_tnt_tier_2":    (BLOCKMINI, "block/lava_still.png", 2),
    "debris_tnt":           (BLOCKMINI, "block/ancient_debris_top.png", 1),
    "arrow_tnt_tier_2":     (ITEM, "item/arrow.png", 2),
    "succ_tnt":             (ITEM, "item/ender_eye.png", 1),
    "village_tnt":          (ITEM, "item/emerald.png", 1),
    "mansion_tnt":          (BLOCKMINI, "block/dark_oak_planks.png", 1),
    "pyramid_tnt":          (BLOCKMINI, "block/prismarine_bricks.png", 1),
    "pillar_tnt_tier_2":    (FIGURE, "pillars", 2),
    "bee_nt":               (ITEM, "item/honeycomb.png", 1),
    # Its own floor, so the icon is what the charge leaves behind rather than what it takes away.
    "flat_tnt":             (BLOCKMINI, "block/smooth_stone.png", 1),
    "damage_web_tnt":       (BLOCKMINI, "block/cobweb.png", 2),
    "laser_tnt":            (FIGURE, "guardian", 1),
    # A dome is a shell of the mod's own stone, so the icon is the stone rather than a shape.
    "dome_tnt":             (BLOCKMINI, "block/stone_bricks.png", 1),
    # The Succ TNT's eye on a yellow bar: this is that charge's second tier and nothing else.
    "blackhole_tnt":        (ITEM, "item/ender_eye.png", 2),
}


def tinted(path, rgb):
    tex = vanilla(path)
    out = Image.new("RGBA", tex.size, rgb + (255,))
    out.putalpha(tex.getchannel("A"))
    px, ox = tex.load(), out.load()
    for y in range(tex.height):
        for x in range(tex.width):
            r, g, b, a = px[x, y]
            if a:
                ox[x, y] = (rgb[0] * r // 255, rgb[1] * g // 255, rgb[2] * b // 255, a)
    return out


def potion_icon():
    overlay = vanilla("item/potion_overlay.png")
    tint = Image.new("RGBA", (16, 16), (0x9b, 0x50, 0xd6, 255))
    tint.putalpha(overlay.getchannel("A"))
    out = Image.alpha_composite(Image.new("RGBA", (16, 16)), tint)
    out.alpha_composite(vanilla("item/potion.png"))
    return out


chicken_src = Image.open(CT + "/block/chicken_tnt_tier_2_side_after.png").convert("RGBA")


def near(c, ref, tol=12):
    return all(abs(a - b) <= tol for a, b in zip(c[:3], ref[:3]))


def chicken_figure():
    """The user's chicken, lifted off their tile and cleaned of editor strays.

    Anything that is not the frame, the fill, the dash or their green bar is the bird; the two
    stray shades of white/yellow and the one half-transparent bar pixel are snapped back.
    """
    green = TIER_COLOR[1]
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = chicken_src.getpixel((x, y))
            if c[3] < 255 or any(near(c, r) for r in (FRAME, FILL, DASH, green)):
                continue
            if near(c, (0xff, 0xff, 0xff), 8):
                c = (0xff, 0xff, 0xff, 255)
            elif near(c, (0xf1, 0xdb, 0x15), 8):
                c = (0xf1, 0xdb, 0x15, 255)
            out.putpixel((x, y), c)
    return out


CHICKEN_FIG = chicken_figure()

written = []
for name, (kind, payload, tier) in TNTS.items():
    im = background()
    if kind == CHICKEN:
        bar(im, tier)
        im.alpha_composite(CHICKEN_FIG)
    else:
        bar(im, tier)
        if kind == FIGURE:
            draw_figure(im, payload)
        elif kind == BLOCKMINI:
            mini_block(im, vanilla(payload),
                       (0xbe, 0xd8, 0xe4, 255) if "glass" in payload else None)
        else:
            if payload == "@potion":
                icon = potion_icon()
            elif payload.startswith("@mod:"):
                # The mod's own art, for an effect whose item is one of ours rather than vanilla's.
                icon = Image.open(os.path.join(MOD, *payload[5:].split("/"))).convert("RGBA")
            elif payload == "@leather":
                icon = tinted("item/leather_chestplate.png", (0xa0, 0x65, 0x40))
            else:
                icon = vanilla(payload)
            im.alpha_composite(icon)
            bar(im, tier)  # keep the tier readable through a full-bleed icon

    rel = "block/%s_side.png" % name
    for root in ([MOD] if os.environ.get("APPLY") else [PREVIEW]):
        p = os.path.join(root, *rel.split("/"))
        os.makedirs(os.path.dirname(p), exist_ok=True)
        im.save(p)
    written.append(rel)

# contact sheet
sheet = Image.new("RGBA", (16 * 8 * 6, 128 * ((len(written) + 5) // 6)), (255, 255, 255, 255))
for i, r in enumerate(written):
    root = (MOD if os.environ.get("APPLY") else PREVIEW)
    t = Image.open(os.path.join(root, *r.split("/"))).convert("RGBA").resize((128, 128), Image.NEAREST)
    sheet.alpha_composite(t, ((i % 6) * 128, (i // 6) * 128))
sheet.save(os.path.join(PREVIEW, "_sheet.png"))
print("wrote %d TNT sides" % len(written))
for r in written:
    print("  ", r)
