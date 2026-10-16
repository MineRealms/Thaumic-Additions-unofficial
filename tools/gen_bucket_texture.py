# Derives the crystal water bucket's icon from vanilla's water bucket.
#
# Upstream never shipped a bucket for this fluid, so there is no 1.12 art to bring across, and the
# fluid's own textures are animated 16x16 tiles — no use as an item icon. What upstream did have is
# a colour: FluidCrystalWater#getColor picked a teal, 0xFF3D9999, and the fluid's own still texture
# is drawn in it. So the bucket's art is vanilla's, turned to that teal and nothing else touched: a
# hue rotation on every pixel leaves the metal and its shading exactly as they were, because the
# metal has almost no saturation for a rotation to move.
#
# Usage: python -I tools/gen_bucket_texture.py <vanilla water_bucket.png> <output.png>
import colorsys
import sys

from PIL import Image

# Vanilla's water reads about 220 degrees; the crystal water teal reads 180.
HUE_SHIFT = -40.0 / 360.0


def tint(pixel):
    red, green, blue, alpha = pixel
    hue, saturation, value = colorsys.rgb_to_hsv(red / 255.0, green / 255.0, blue / 255.0)
    red, green, blue = colorsys.hsv_to_rgb((hue + HUE_SHIFT) % 1.0, saturation, value)
    return (round(red * 255), round(green * 255), round(blue * 255), alpha)


def main(source, target):
    image = Image.open(source).convert("RGBA")
    image.putdata([tint(pixel) for pixel in image.getdata()])
    image.save(target)
    print("wrote", target)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
