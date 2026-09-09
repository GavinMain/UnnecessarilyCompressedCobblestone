"""Placeholder art for the Teleportation Gate and the tier 2 pickaxe.

Everything here is derived from art that is already in the repo rather than drawn, and TEXTURE.md
says so for each:

  * the two gate halves are the level 96 stone the gate is crafted from, darkened and shot through
    with a violet haze that is brightest in the middle of the doorway - the alpha is what makes it
    read as something to walk into rather than a wall, and it is highest at the edges so the frame
    stays solid;
  * the gate's GUI panel is the Composition Table's, cropped to the gate's own 176x110 and with the
    slot squares painted out, since the gate has no slots;
  * the tier 2 pickaxe is the tier 1 icon restained to the level 207 stone it is cut from, with the
    head brightened so the two read apart at inventory size.

Run with `python tools/build_teleporter_textures.py`; it needs nothing but Pillow.
"""
import colorsys
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")

# The level the gate is crafted from and the level the tier 2 pickaxe is cut from.
GATE_LEVEL = 96
PICKAXE_LEVEL = 207

# The haze in the doorway. A violet, because nothing else in this mod is one and a gate should not
# be mistaken for a TNT at a glance.
HAZE = (150, 90, 235)


def stone(level):
    name = "compressed_cobblestone.png" if level == 1 else f"compressed_cobblestone_{level}.png"
    return Image.open(os.path.join(TEX, "block", name)).convert("RGBA")


def save(image, path):
    full = os.path.join(TEX, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    image.save(full)
    print("wrote", os.path.relpath(full, REPO))


def gate_half(upper):
    """One half of the doorway: the frame at the edges, the haze in the middle.

    The two halves differ in which edge is solid - the lower one is closed at the bottom and the
    upper one at the top - so a gate reads as a single opening rather than as two panes stacked.
    """
    base = stone(GATE_LEVEL)
    out = Image.new("RGBA", base.size)

    for y in range(base.height):
        for x in range(base.width):
            r, g, b, _ = base.getpixel((x, y))

            # How far into the opening this pixel is, 0 at the frame and 1 in the middle. The
            # vertical half of it is one-sided: the opening runs off the top of the lower half and
            # off the bottom of the upper one, so the two meet with no frame between them.
            edge_x = min(x, base.width - 1 - x) / (base.width / 2.0)
            edge_y = (y if upper else base.height - 1 - y) / float(base.height - 1)
            depth = max(0.0, min(1.0, min(edge_x * 2.2, edge_y * 2.2)))

            haze = depth ** 1.5
            r = int(r * (1.0 - haze) + HAZE[0] * haze)
            g = int(g * (1.0 - haze) + HAZE[1] * haze)
            b = int(b * (1.0 - haze) + HAZE[2] * haze)

            # The frame is opaque and the middle of the doorway is not, but never fully clear: a
            # gate you cannot see is a gate you walk into by accident.
            alpha = int(255 - 105 * haze)
            out.putpixel((x, y), (r, g, b, alpha))

    return out


def panel():
    """The gate's screen: the Composition Table's panel with its slots painted out."""
    source = Image.open(os.path.join(TEX, "gui/composition_table/composition_table_gui.png")).convert("RGBA")
    out = Image.new("RGBA", (256, 256))
    out.paste(source.crop((0, 0, 176, 110)), (0, 0))

    # The plate colour vanilla containers use, which is what the slot squares are painted over with.
    plate = (198, 198, 198, 255)
    for y in range(16, 110):
        for x in range(4, 172):
            r, g, b, a = out.getpixel((x, y))
            # Slot squares are the dark sunken pixels; the panel itself is light. Anything darker
            # than the plate by a clear margin inside the body of the panel is a slot.
            if a and r < 170 and abs(r - g) < 12 and abs(g - b) < 12:
                out.putpixel((x, y), plate)

    return out


def restain(image, hue, saturation_floor, value_scale=1.0):
    out = Image.new("RGBA", image.size)
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            # A flat grey carries no hue of its own, so a rotation alone would leave it grey; the
            # saturation floor is what actually stains it.
            r, g, b = colorsys.hsv_to_rgb(hue, max(s, saturation_floor), min(1.0, v * value_scale))
            out.putpixel((x, y), (int(r * 255), int(g * 255), int(b * 255), a))

    return out


def average_hue(image):
    hues, saturations, count = 0.0, 0.0, 0
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                continue
            h, s, _ = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            hues += h
            saturations += s
            count += 1

    return (hues / count, saturations / count) if count else (0.0, 0.0)


def pickaxe():
    source = Image.open(os.path.join(TEX, "item/compressed_cobblestone_pickaxe.png")).convert("RGBA")
    hue, saturation = average_hue(stone(PICKAXE_LEVEL))
    return restain(source, hue, max(saturation, 0.35), 1.15)


if __name__ == "__main__":
    save(gate_half(False), "block/teleportation_gate_lower.png")
    save(gate_half(True), "block/teleportation_gate_upper.png")
    save(panel(), "gui/teleportation_gate/teleportation_gate_gui.png")
    save(pickaxe(), "item/compressed_cobblestone_pickaxe_tier_2.png")
