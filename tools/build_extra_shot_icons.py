"""Placeholder icons for the two Extra Shot engravings.

The Sniper plate recoloured, since the Sniper is the other engraving that goes on any bow in the
game and these two are its neighbours on the shelf: the first rung to a warm amber, the second to a
deeper orange, so the pair reads as a ladder the way the Vapor Slashes and the Damage augments do.

Recorded in TEXTURE.md as the copy it is.
"""
import colorsys
import os

from PIL import Image

MOD = "src/main/resources/assets/unnecessarilycompressedcobblestone/textures/item"
SRC = os.path.join(MOD, "sniper_engraving.png")


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


sniper = Image.open(SRC).convert("RGBA")
restain(sniper, 0.11, 0.55, 1.10).save(os.path.join(MOD, "extra_shot_engraving.png"))
restain(sniper, 0.05, 0.75, 0.90).save(os.path.join(MOD, "extra_shot_2_engraving.png"))
print("ok")
