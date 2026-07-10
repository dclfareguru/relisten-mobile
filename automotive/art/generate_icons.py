#!/usr/bin/env python3
"""Regenerate launcher icon PNGs from the source art. Requires Pillow.

    python3 automotive/art/generate_icons.py

Adaptive foreground: art recentered and scaled so its farthest opaque pixel sits
~330/1024 of the canvas from center (inside Android's 66dp safe circle, allowing for
the up-to-72dp mask). Legacy: art at ~445/1024 over the dark squircle background.
"""
from PIL import Image, ImageDraw
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")
CANVAS = 1024
SIZES = {"mdpi": (108, 48), "hdpi": (162, 72), "xhdpi": (216, 96),
         "xxhdpi": (324, 144), "xxxhdpi": (432, 192)}


def center_on(size, img):
    c = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c.paste(img, ((size - img.width) // 2, (size - img.height) // 2), img)
    return c


def max_extent(img, thresh=40, step=2):
    a = img.getchannel("A").load()
    cx, cy = img.width / 2, img.height / 2
    m = 0
    for y in range(0, img.height, step):
        for x in range(0, img.width, step):
            if a[x, y] > thresh:
                m = max(m, math.hypot(x - cx, y - cy))
    return m


def main():
    src = Image.open(os.path.join(HERE, "ttb_crossed_guitars_1024.png")).convert("RGBA")
    art = src.crop(src.getchannel("A").getbbox())
    centered = center_on(max(art.size) + 8, art)
    radius = max_extent(centered)

    def scaled_layer(target):
        k = target / radius
        resized = centered.resize(
            (round(centered.width * k), round(centered.height * k)), Image.LANCZOS)
        return center_on(CANVAS, resized)

    fg = scaled_layer(330)
    bg = Image.open(os.path.join(HERE, "icon_background_1024.png")).convert("RGBA")

    mask = Image.new("L", (CANVAS, CANVAS), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, CANVAS - 1, CANVAS - 1], radius=200, fill=255)
    legacy = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    legacy.paste(bg, (0, 0), mask)
    legacy.alpha_composite(scaled_layer(445))

    for density, (adaptive, legacy_px) in SIZES.items():
        d = os.path.join(RES, f"mipmap-{density}")
        fg.resize((adaptive, adaptive), Image.LANCZOS).save(
            os.path.join(d, "ic_launcher_foreground.png"))
        bg.resize((adaptive, adaptive), Image.LANCZOS).save(
            os.path.join(d, "ic_launcher_background.png"))
        lg = legacy.resize((legacy_px, legacy_px), Image.LANCZOS)
        lg.save(os.path.join(d, "ic_launcher.png"))
        lg.save(os.path.join(d, "ic_launcher_round.png"))
        print(density, "done")


if __name__ == "__main__":
    main()
