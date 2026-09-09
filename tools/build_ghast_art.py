"""Builds the three icons the Compressed Ghast and the healing staff's effect need.

* ``item/tier_18_compressed_heart.png`` - the family drawing with a ghast-white core. That makes
  nine of the eighteen hearts one drawing in nine cores, which TEXTURE.md has been complaining about
  since the third and which this does nothing to fix.
* ``mob_effect/ghasted.png`` - the ghast's own face, cut out of the entity texture and framed the
  way the boss-spoil icons are. It is the one icon in the mod that is a real picture of the thing it
  means, because the mob it draws already has a face in a 64x32 sheet and a ghast face is instantly
  readable at 16 pixels.
* ``mob_effect/mending.png`` - the healing staff's own effect. Vanilla ships no Instant Health icon
  that can be reused (its effect textures are not in a namespace this can reach at build time
  without the client jar's own path), so it is drawn: a plain cross on the potion red.

Run with APPLY=1 to write into the mod; without it a preview goes to build/.
"""
import os
import zipfile
from io import BytesIO

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(REPO, "src/main/resources/assets/unnecessarilycompressedcobblestone/textures")
PREVIEW = os.path.join(REPO, "build", "ghast_preview")
JAR = os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar")

OUTLINE = (0x20, 0x1C, 0x18, 255)

# The ghast's core colour, and the lit middle of it: the mob's own off-white.
GHAST_CORE = (0xE8, 0xE8, 0xE8, 255)
GHAST_CORE_LIT = (0xFF, 0xFF, 0xFF, 255)

# The Potion of Healing's colour, which is also what the staff's bottle is tinted.
HEAL = (0xF8, 0x24, 0x23, 255)
HEAL_LIT = (0xFF, 0xC8, 0xC8, 255)


def vanilla(path):
    with zipfile.ZipFile(JAR) as jar:
        return Image.open(BytesIO(jar.read("assets/minecraft/textures/" + path))).convert("RGBA")


def framed(face):
    """A 16x16 face centred in the 18x18 canvas vanilla's mob effect icons use, with an outline."""
    out = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    out.paste(face.resize((16, 16), Image.NEAREST), (1, 1))
    for i in range(18):
        for pos in ((i, 0), (i, 17), (0, i), (17, i)):
            out.putpixel(pos, OUTLINE)

    return out


def heart():
    base = Image.open(os.path.join(TEX, "item/tier_2_compressed_heart.png")).convert("RGBA")
    out = base.copy()
    for y in range(6, 10):
        for x in range(6, 10):
            out.putpixel((x, y), GHAST_CORE_LIT if 7 <= x <= 8 and 7 <= y <= 8 else GHAST_CORE)

    return out


def ghast_face():
    """The ghast's face out of its entity sheet: the 16x16 block at (0, 0) of the 64x32 texture."""
    sheet = vanilla("entity/ghast/ghast.png")
    return sheet.crop((0, 0, 16, 16))


def mending():
    """A cross, because a cross is what every health icon in every game already is."""
    face = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            arm = 6 <= x <= 9 or 6 <= y <= 9
            middle = 6 <= x <= 9 and 6 <= y <= 9
            if arm and 2 <= x <= 13 and 2 <= y <= 13:
                face.putpixel((x, y), HEAL_LIT if middle else HEAL)

    return face


def write(relative, image):
    root = TEX if os.environ.get("APPLY") else PREVIEW
    path = os.path.join(root, *relative.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)
    print("wrote", path)


write("item/tier_18_compressed_heart.png", heart())
write("mob_effect/ghasted.png", framed(ghast_face()))
write("mob_effect/mending.png", framed(mending()))
