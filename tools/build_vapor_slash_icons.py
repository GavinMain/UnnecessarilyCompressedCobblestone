"""Placeholder icons for the two Vapor Slash engravings.

The Dash plate recoloured, since the Dash is the ability these two are a deeper version of: the
first slash taken to a pale cyan (the vapour), the second to a deeper blue so the two rungs read as
a ladder the way the Damage augments do. Recorded in TEXTURE.md as the copy it is.
"""
import colorsys
import os

from PIL import Image

MOD = "src/main/resources/assets/unnecessarilycompressedcobblestone/textures/item"
SRC = os.path.join(MOD, "dash_engraving.png")


def restain(im, hue, sat_floor, value_scale):
    out = Image.new("RGBA", im.size)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            # Flat greys in the plate carry no hue of their own, so a straight rotation leaves them
            # grey - the saturation floor is what actually stains them.
            s = max(s, sat_floor)
            r, g, b = colorsys.hsv_to_rgb(hue, s, min(1.0, v * value_scale))
            out.putpixel((x, y), (int(r * 255), int(g * 255), int(b * 255), a))
    return out


dash = Image.open(SRC).convert("RGBA")
restain(dash, 0.50, 0.45, 1.10).save(os.path.join(MOD, "vapor_slash_engraving.png"))
restain(dash, 0.60, 0.60, 0.85).save(os.path.join(MOD, "vapor_slash_2_engraving.png"))
print("ok")
