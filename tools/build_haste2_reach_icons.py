"""Placeholder icons for the Haste II and Super Reach engravings.

Each is its own first rung recoloured, so the pair reads as a ladder rather than as two unrelated
plates: Haste II is the Haste plate pushed a shade deeper and warmer, and Super Reach is the Reach
plate taken to a brighter, colder blue - further away being the thing it means.

Recorded in TEXTURE.md as the copies they are.
"""
import colorsys
import os

from PIL import Image

MOD = "src/main/resources/assets/unnecessarilycompressedcobblestone/textures/item"


def restain(src, dst, hue, sat_floor, value_scale):
    im = Image.open(os.path.join(MOD, src)).convert("RGBA")
    out = Image.new("RGBA", im.size)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            # Flat greys carry no hue of their own, so the saturation floor is what stains them.
            s = max(s, sat_floor)
            r, g, b = colorsys.hsv_to_rgb(hue, s, min(1.0, v * value_scale))
            out.putpixel((x, y), (int(r * 255), int(g * 255), int(b * 255), a))

    out.save(os.path.join(MOD, dst))
    print("wrote", dst)


restain("haste_engraving.png", "haste_2_engraving.png", 0.13, 0.70, 0.88)
restain("reach_engraving.png", "super_reach_engraving.png", 0.55, 0.65, 1.15)
