"""Builds the two pictures the Omni Slash needs.

The first is the crescent itself, drawn the way the Scythe Wave's is - the difference of two
circles, which is exactly what a crescent is - and then given a second, thinner blade behind the
first, offset along the same axis. That is the whole of what separates the two drawings, and it is
there because this crescent is drawn five blocks tall rather than one and a half: at that size a
single clean arc reads as a plate, and a leading edge with a trail behind it reads as a cut. Like
every sheet in this mod that is going to be tinted, it carries its shape entirely in the alpha
channel and sits near white everywhere it is opaque at all - a tint is a multiply, so a sheet
carrying its shape in the brightness could only ever be tinted darker.

The second is the engraving's item icon, the Vapor Slash plate restained hot gold: the Omni Slash is
the end of the line the two Vapor Slashes started - a swing that leaves the blade and takes the
world with it - so it belongs on that shelf, and gold against their blue and the Scythe Wave's
blue-white is what says it is the deepest of the four.

Run with APPLY=1 to write into the mod; without it a preview goes to build/.
"""
import colorsys
import math
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
PREVIEW = os.path.join(REPO, "build", "omni_slash_preview")

SIZE = 128

# The leading blade: an outer edge and the bite taken out of it, in sheet coordinates.
OUTER_R = 60.0
INNER_R = 55.0
INNER_DX = 20.0

# The trailing one, thinner and pulled back along the same axis, so the cut has a wake.
TRAIL_OUTER_R = 48.0
TRAIL_INNER_R = 45.0
TRAIL_INNER_DX = 15.0
TRAIL_DX = 16.0
TRAIL_ALPHA = 0.55

# How soft each edge is, in pixels. The outer is the sharper of the two: a blade is keen on the
# cutting side and fades on the trailing one.
OUTER_FEATHER = 2.0
INNER_FEATHER = 5.0

CENTRE = (SIZE - 1) / 2.0


def coverage(distance, radius, feather, inside):
    """How much of a pixel a circle of `radius` covers, smoothed over `feather` pixels."""
    edge = (radius - distance) / feather if inside else (distance - radius) / feather
    return min(1.0, max(0.0, edge))


def blade(dx, dy, outer_r, inner_r, inner_dx):
    """The alpha of one crescent - the difference of two circles - at an offset from its centre."""
    outer = coverage(math.hypot(dx, dy), outer_r, OUTER_FEATHER, True)
    inner = coverage(math.hypot(dx - inner_dx, dy), inner_r, INNER_FEATHER, False)
    return outer * inner


def crescent():
    image = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - CENTRE
            dy = y - CENTRE

            lead = blade(dx, dy, OUTER_R, INNER_R, INNER_DX)
            trail = TRAIL_ALPHA * blade(dx + TRAIL_DX, dy, TRAIL_OUTER_R, TRAIL_INNER_R,
                                        TRAIL_INNER_DX)
            alpha = min(1.0, max(lead, trail))
            if alpha <= 0.0:
                continue

            # A touch of heat in the body of the blade, white at the edges. The shading is in the
            # colour rather than in the alpha, so the shape stays exactly the two arcs above.
            depth = 1.0 - alpha
            green = int(255 - 25 * depth)
            blue = int(255 - 70 * depth)
            image.putpixel((x, y), (255, green, blue, int(alpha * 255)))

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

slash = os.path.join(root, "entity", "omni_slash.png")
os.makedirs(os.path.dirname(slash), exist_ok=True)
crescent().save(slash)
print("wrote", slash)

source = os.path.join(MOD, "item", "vapor_slash_2_engraving.png")
icon = os.path.join(root, "item", "omni_slash_engraving.png")
os.makedirs(os.path.dirname(icon), exist_ok=True)
restain(Image.open(source).convert("RGBA"), 0.11, 0.75, 1.2).save(icon)
print("wrote", icon)
