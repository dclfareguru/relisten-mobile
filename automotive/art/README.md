# Icon sources

Crossed-guitars TTB launcher icon: red Gibson SG x teal Fender Telecaster.

- `icon_fg.svg` — adaptive-icon foreground layer (transparent; art within the 66dp safe circle)
- `icon_bg.svg` — adaptive-icon background layer (stage-dark glow + groove rings)
- `icon_legacy.svg` — composite for legacy square icons

Regenerate PNGs by rendering each SVG at 1024x1024 with a transparent-capable renderer
(e.g. `chrome --headless=new --default-background-color=00000000 --window-size=1024,1024
--screenshot=...`) and downscaling: adaptive layers to 108/162/216/324/432 px, legacy to
48/72/96/144/192 px, into `app/src/main/res/mipmap-*`. Note: Chrome headless crops (not
scales) below ~432 px windows — always render at 1024 and downscale.
