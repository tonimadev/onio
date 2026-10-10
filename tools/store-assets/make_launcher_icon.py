#!/usr/bin/env python3
"""Generates the launcher icon: adaptive layers (vector), legacy webps and the Play Store PNG.

The mark is a shopping bag with a check cut out ("shop, then keep your money"), white on a coral gradient.
Outputs (relative to the repo root):
  app/src/main/res/drawable/ic_launcher_{foreground,background}.xml
  app/src/main/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml
  app/src/main/res/mipmap-*dpi/ic_launcher{,_round}.webp      (API < 26)
  app/src/main/ic_launcher-playstore.png                      (512x512)
Requires rsvg-convert and ImageMagick. Usage: python3 tools/store-assets/make_launcher_icon.py
"""
import os
import subprocess
import tempfile

from generate_store_assets import BAG_WITH_HOLES, HANDLE, ROOT

START, END = "#FF7A59", "#F23F5D"
RES = os.path.join(ROOT, "app", "src", "main", "res")


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def vector_layers():
    write(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Shopping bag with a check cut out: "shop, then walk away with your money". -->
    <path
        android:fillColor="#FFFFFFFF"
        android:fillType="evenOdd"
        android:pathData="{BAG_WITH_HOLES}" />
    <path
        android:pathData="{HANDLE}"
        android:strokeWidth="4"
        android:strokeColor="#FFFFFFFF"
        android:strokeLineCap="round"
        android:fillColor="#00000000" />
</vector>
''')
    write(os.path.join(RES, "drawable", "ic_launcher_background.xml"), f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:type="linear">
                <item android:color="#FF{START[1:]}" android:offset="0.0" />
                <item android:color="#FF{END[1:]}" android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
''')
    adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'''
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        write(os.path.join(RES, "mipmap-anydpi-v26", name), adaptive)


def svg(view, shape):
    x, y, w = view
    clip = {"full": "", "round": f'<clipPath id="c"><circle cx="54" cy="54" r="{w / 2}"/></clipPath>',
            "squircle": f'<clipPath id="c"><rect x="{x}" y="{y}" width="{w}" height="{w}" rx="{w * 0.22}"/></clipPath>'}[shape]
    g = ' clip-path="url(#c)"' if shape != "full" else ""
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x} {y} {w} {w}" width="{w}" height="{w}">
<defs><linearGradient id="g" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="{START}"/><stop offset="1" stop-color="{END}"/></linearGradient>{clip}</defs>
<g{g}><rect x="0" y="0" width="108" height="108" fill="url(#g)"/>
<path d="{BAG_WITH_HOLES}" fill="#fff" fill-rule="evenodd"/>
<path d="{HANDLE}" fill="none" stroke="#fff" stroke-width="4" stroke-linecap="round"/></g></svg>'''


def render(tmp, name, view, shape, px):
    path = os.path.join(tmp, f"{name}.svg")
    write(path, svg(view, shape))
    out = os.path.join(tmp, f"{name}_{px}.png")
    subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), path, "-o", out], check=True)
    return out


def rasters():
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    with tempfile.TemporaryDirectory() as tmp:
        for density, px in sizes.items():
            for fname, shape in (("ic_launcher", "squircle"), ("ic_launcher_round", "round")):
                png = render(tmp, f"{fname}_{density}", (14, 14, 80), shape, px)
                target = os.path.join(RES, f"mipmap-{density}", f"{fname}.webp")
                subprocess.run(["magick", png, "-define", "webp:lossless=true", target], check=True)
        store = render(tmp, "store", (0, 0, 108), "full", 512)
        subprocess.run(["magick", store, os.path.join(ROOT, "app", "src", "main", "ic_launcher-playstore.png")], check=True)


if __name__ == "__main__":
    vector_layers()
    rasters()
    print("launcher icon generated")
