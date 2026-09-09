"""Placeholder art for the Ray of Laser, its augments and the Laser Augmentation Table.

Everything here is a placeholder in the same sense the rest of TEXTURE.md means it: the shapes
are right, the pixels are not drawn by hand, and the file says so. What it writes:

* ``item/<name>_augment.png`` - one plate per augment, cut from the engravings' own plate drawing
  with the mark recoloured. The colour is the augment's family, so the six effect augments are told
  apart by the colour of the thing they inflict, and the damage and rate ladders each run one hue
  through four (or three) brightnesses, which is as much as a 16x16 icon can say about a level.
* ``item/ray_of_laser.png`` - a barrel with a lens on the end, in the beam's own teal.
* ``block/laser_augmentation_table_top.png`` and ``_side.png`` - the Engraving Table's two faces
  with the worked part recoloured to the beam's teal, so the fourth table of that family reads as
  one of them and still not as any of the other three.
* ``gui/laser_augmentation_table/laser_augmentation_table_gui.png`` - the container panel, drawn
  rather than copied, because this one is 186 tall instead of 166 and carries a list.

Run from the repository root: ``python tools/build_laser_textures.py``.
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/unnecessarilycompressedcobblestone/textures"

# The plate every engraving icon is drawn on. The augments borrow it so the two upgrade systems
# look like siblings, which they are.
PLATE = ASSETS / "item/quick_draw_engraving.png"

# One colour per augment family. The three ladders keep one hue and change value with the level.
AUGMENT_COLORS = {
    "damage_1": (255, 150, 120),
    "damage_2": (255, 110, 80),
    "damage_3": (235, 70, 40),
    "damage_4": (190, 30, 10),
    "rate_1": (200, 230, 255),
    "rate_2": (140, 200, 255),
    "rate_3": (80, 165, 255),
    "poison": (135, 200, 60),
    "wither": (60, 55, 60),
    "lightning": (126, 206, 255),
    "freeze": (154, 215, 240),
    "burn": (224, 123, 28),
    "explosive": (224, 85, 43),
    "blindness": (40, 40, 48),
    "slowness": (95, 105, 160),
    "nausea": (85, 30, 110),
    "hunger": (88, 118, 84),
    "swiftness": (124, 175, 198),
}

# The beam's own colour, which the laser and its table are drawn in.
BEAM = (120, 220, 220)

# Vanilla's container palette, which every GUI in the game is built out of.
PANEL = (198, 198, 198, 255)
PANEL_LIGHT = (255, 255, 255, 255)
PANEL_SHADOW = (85, 85, 85, 255)
SLOT_FILL = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)

PANEL_WIDTH = 176
PANEL_HEIGHT = 186


def is_grey(pixel):
    """Whether a pixel is part of the plate rather than the mark scratched into it."""
    r, g, b, a = pixel
    return a == 0 or (abs(r - g) <= 8 and abs(g - b) <= 8 and abs(r - b) <= 8)


def augment_icon(colour):
    """The engraving plate with its mark recoloured, keeping the mark's own shading."""
    plate = Image.open(PLATE).convert("RGBA")
    out = plate.copy()

    marks = [(x, y, plate.getpixel((x, y)))
             for y in range(plate.height) for x in range(plate.width)
             if not is_grey(plate.getpixel((x, y)))]
    if not marks:
        raise SystemExit(f"{PLATE} has no mark to recolour")

    # The mark is shaded, so it is scaled rather than replaced: the brightest pixel of the original
    # becomes the new colour and everything below it keeps its share of that.
    brightest = max(sum(pixel[:3]) for _, _, pixel in marks)
    for x, y, pixel in marks:
        share = sum(pixel[:3]) / brightest
        out.putpixel((x, y), tuple(int(c * share) for c in colour) + (pixel[3],))

    return out


def ray_of_laser():
    """A barrel pointing up and to the right, with a lens on the end of it."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dark = (26, 24, 22, 255)
    body = (110, 110, 118, 255)
    lens = BEAM + (255,)

    # The barrel: a three-wide diagonal from the bottom left to the top right.
    for step in range(11):
        x = 2 + step
        y = 12 - step
        for offset in range(3):
            image.putpixel((min(15, x + offset), min(15, y + offset)), body if offset == 1 else dark)

    # The grip, hanging off the bottom of the barrel.
    for y in range(11, 14):
        for x in range(2, 5):
            image.putpixel((x, y), dark if x == 2 or y == 13 else body)

    # The lens, which is the only part of it that is the colour of the beam.
    for x, y in ((12, 2), (13, 2), (12, 3), (13, 3), (14, 2), (13, 1)):
        image.putpixel((x, y), lens)

    return image


def table_face(source, out_path):
    """One of the Engraving Table's faces with everything not grey turned the colour of the beam."""
    face = Image.open(source).convert("RGBA")
    out = face.copy()

    for y in range(face.height):
        for x in range(face.width):
            pixel = face.getpixel((x, y))
            if is_grey(pixel):
                continue
            share = sum(pixel[:3]) / 765.0
            out.putpixel((x, y), tuple(min(255, int(c * (0.4 + share))) for c in BEAM) + (pixel[3],))

    out.save(out_path)


def bevel(image, x, y, width, height, light, shadow):
    """A vanilla panel edge: light down the top and left, shadow down the bottom and right."""
    for i in range(width):
        image.putpixel((x + i, y), light)
        image.putpixel((x + i, y + height - 1), shadow)
    for i in range(height):
        image.putpixel((x, y + i), light)
        image.putpixel((x + width - 1, y + i), shadow)


def inset(image, x, y, width, height):
    """A vanilla sunken box: dark down the top and left, white down the bottom and right."""
    for i in range(width):
        image.putpixel((x + i, y), SLOT_DARK)
        image.putpixel((x + i, y + height - 1), PANEL_LIGHT)
    for i in range(height):
        image.putpixel((x, y + i), SLOT_DARK)
        image.putpixel((x + width - 1, y + i), PANEL_LIGHT)
    for row in range(1, height - 1):
        for column in range(1, width - 1):
            image.putpixel((x + column, y + row), SLOT_FILL)


def gui():
    """The container panel: two boxes, a sunken list, and the player's own inventory below it."""
    image = Image.new("RGBA", (256, 256), (0, 0, 0, 0))

    for y in range(PANEL_HEIGHT):
        for x in range(PANEL_WIDTH):
            image.putpixel((x, y), PANEL)
    bevel(image, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_LIGHT, PANEL_SHADOW)

    # The laser and the augment, at the positions LaserAugmentationTableMenu places its slots.
    inset(image, 15, 21, 18, 18)
    inset(image, 15, 53, 18, 18)

    # The list, and the scrollbar track down its right-hand edge. The screen draws the rows and the
    # thumb itself, off what is actually on the table.
    inset(image, 43, 15, 120, 62)
    inset(image, 163, 15, 8, 62)

    # The player's inventory: three rows and a hotbar, at the positions the menu places them.
    for row in range(3):
        for column in range(9):
            inset(image, 7 + column * 18, 103 + row * 18, 18, 18)
    for column in range(9):
        inset(image, 7 + column * 18, 161, 18, 18)

    return image


def main():
    (ASSETS / "gui/laser_augmentation_table").mkdir(parents=True, exist_ok=True)

    for name, colour in AUGMENT_COLORS.items():
        augment_icon(colour).save(ASSETS / f"item/{name}_augment.png")

    ray_of_laser().save(ASSETS / "item/ray_of_laser.png")

    table_face(ASSETS / "block/engraving_table_top.png",
               ASSETS / "block/laser_augmentation_table_top.png")
    table_face(ASSETS / "block/engraving_table_side.png",
               ASSETS / "block/laser_augmentation_table_side.png")

    gui().save(ASSETS / "gui/laser_augmentation_table/laser_augmentation_table_gui.png")

    print(f"wrote {len(AUGMENT_COLORS)} augment icons, the laser, two block faces and the panel")


if __name__ == "__main__":
    main()
