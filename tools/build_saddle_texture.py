"""Builds the Compressed Saddle icon.

Vanilla's saddle restained to the mod's deep-stone blue, the same trick the Compressed Bone and the
Compressed Shield are drawn with: the leather's browns are pushed onto a cold grey-blue ramp and the
iron rings are left alone, so the item reads as a saddle at a glance and as *this mod's* saddle on a
second look. Run with APPLY=1 to write into the mod; without it a preview goes to build/.
"""
import os
import zipfile
from io import BytesIO

from PIL import Image

JAR = os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar")
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
PREVIEW = os.path.join(REPO, "build", "saddle_preview")

z = zipfile.ZipFile(JAR)
src = Image.open(BytesIO(z.read("assets/minecraft/textures/item/saddle.png"))).convert("RGBA")

# The stone ramp. Dark to light, and cold: the same family the compressed blocks and the TNT plates
# are in, so a saddle in an inventory beside them looks like it came out of the same mod.
RAMP = [
    (0x14, 0x14, 0x21),
    (0x22, 0x25, 0x3c),
    (0x2d, 0x31, 0x6e),
    (0x46, 0x4c, 0xb7),
    (0x6d, 0x74, 0xc8),
    (0x9a, 0xa0, 0xda),
]


def restain(px):
    r, g, b, a = px
    if a == 0:
        return px

    # The iron rings and buckles are already grey - leave anything unsaturated as it is, so the
    # hardware still reads as metal against the restained leather.
    if max(r, g, b) - min(r, g, b) < 20:
        return px

    # Luminance onto the ramp. Vanilla's leather runs dark-to-mid, so the ramp is walked over the
    # range the source actually uses rather than over the whole of 0-255.
    lum = (r * 299 + g * 587 + b * 114) // 1000
    step = min(len(RAMP) - 1, max(0, (lum - 40) * len(RAMP) // 110))
    return RAMP[step] + (a,)


out = Image.new("RGBA", src.size)
for y in range(src.height):
    for x in range(src.width):
        out.putpixel((x, y), restain(src.getpixel((x, y))))

root = MOD if os.environ.get("APPLY") else PREVIEW
path = os.path.join(root, "item", "compressed_saddle.png")
os.makedirs(os.path.dirname(path), exist_ok=True)
out.save(path)
print("wrote", path)
