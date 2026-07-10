# Icon sources

Launcher icon: red Gibson SG x teal Fender Telecaster crossed like swords with TTB
across the crossing point.

- `ttb_crossed_guitars_1024.png` — source artwork (AI-generated, transparent background)
- `icon_background_1024.png` — adaptive-icon background layer (stage-dark glow + groove rings)
- `generate_icons.py` — regenerates all `mipmap-*` PNGs from the two files above
  (adaptive foreground/background 108–432 px, legacy square icons 48–192 px).
  Run `python3 automotive/art/generate_icons.py` (needs Pillow).

To change the icon, replace `ttb_crossed_guitars_1024.png` (keep a transparent
background) and re-run the script. It automatically recenters the art and scales it
into the adaptive-icon safe zone so nothing clips under circular launcher masks.
