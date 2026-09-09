"""Builds the two pictures the Scythe Wave needs.

The first is the crescent itself, drawn rather than restained: a 64x64 sheet holding the difference
of two circles, which is exactly what a crescent is, with the inner circle pushed off to one side so
the blade is thin at the tips and thick through the belly. It carries its shape entirely in the
alpha channel and sits at near-white everywhere it is opaque at all - a tint is a multiply, so a
sheet that carried the shape in its brightness could only ever be tinted darker.

The second is the engraving's item icon, which is the Vapor Slash plate restained: the two are the
same idea on two different weapons - a swing that leaves the blade and travels - so they belong on
the same shelf, and a cold blue-white separates the scythe's cut from the katana's.

Run with APPLY=1 to write into the mod; without it a preview goes to build/.
"""
import colorsys
import math
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
PREVIEW = os.path.join(REPO, "build", "scythe_wave_preview")

SIZE = 64

# The outer edge of the blade, and the bite taken out of it. Both are in sheet coordinates.
OUTER_R = 30.0
INNER_R = 27.0
INNER_DX = 11.0

# How soft the two edges are, in pixels. The outer edge is the sharper of the two: a blade is keen
# on the cutting side and fades on the trailing one.
OUTER_FEATHER = 1.6
INNER_FEATHER = 3.2

CENTRE = (SIZE - 1) / 2.0


def coverage(distance, radius, feather, inside):
    """How much of a pixel a circle of `radius` covers, smoothed over `feather` pixels."""
    edge = (radius - distance) / feather if inside else (distance - radius) / feather
    return min(1.0, max(0.0, edge))


def crescent():
    image = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - CENTRE
            dy = y - CENTRE

            outer = coverage(math.hypot(dx, dy), OUTER_R, OUTER_FEATHER, True)
            inner = coverage(math.hypot(dx - INNER_DX, dy), INNER_R, INNER_FEATHER, False)
            alpha = outer * inner
            if alpha <= 0.0:
                continue

            # A touch of cold in the body of the blade, white at the edges: the shading is in the
            # colour rather than in the alpha, so the shape stays exactly the crescent above.
            depth = 1.0 - alpha
            red = int(210 + 45 * depth)
            green = int(232 + 23 * depth)
            image.putpixel((x, y), (red, green, 255, int(alpha * 255)))

    return image


def restain(image, hue, sat_floor, value_scale):
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


root = MOD if os.environ.get("APPLY") else PREVIEW

wave = os.path.join(root, "entity", "scythe_wave.png")
os.makedirs(os.path.dirname(wave), exist_ok=True)
crescent().save(wave)
print("wrote", wave)

source = os.path.join(MOD, "item", "vapor_slash_engraving.png")
icon = os.path.join(root, "item", "scythe_wave_engraving.png")
os.makedirs(os.path.dirname(icon), exist_ok=True)
restain(Image.open(source).convert("RGBA"), 0.55, 0.35, 1.15).save(icon)
print("wrote", icon)
