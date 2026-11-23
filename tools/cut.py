"""Cuts the Codex sheets in art/sheets into app drawables (the Leyline cutter, plus round paintings).

Magenta is keyed by a flood fill from the cell edges, so lavender and pink inside the art survive.

    python tools/cut.py [sheet names]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

# Same order as the prompts in gen_art.py.
ICONS = ["far", "track", "nearfar", "jump", "blink", "third", "side", "dark", "hands", "back", "breath", "candle", "orb", "sun", "basket", "scroll"]
HERBS = ["tea", "jasmine", "chrys", "osmanthus", "plum", "lotus", "ginger", "mint", "goji", "bamboo", "pine", "camellia"]
FOLK = ["goatherd", "teahouse", "potter", "bride", "fisher", "merchant", "weaver", "girl"]

SPRITES = [  # sheet, names, prefix, cols, rows, max px
    ("icons", ICONS, "ic", 4, 4, 240), ("herbs", HERBS, "herb", 4, 3, 220),
    ("gifts", FOLK, "gift", 4, 2, 240), ("master", ["brush", "moon", "mirror", "orb"], "master", 4, 1, 260), ("tabs", ["hall", "practice", "scroll", "path"], "tab", 4, 1, 160),
]
SCENES = {"hall": 1024}
# Round paintings on indigo-black: cut to a circle.
ROUND = {"people": (3, 3, ["seer"] + FOLK, "face", 360)}
TALES = [f"t{t}_{i}" for t in range(1, 7) for i in range(1, 9)] + ["path_end"]


def is_key(arr):
    r, g, b = arr[..., 0].astype(int), arr[..., 1].astype(int), arr[..., 2].astype(int)
    return (np.minimum(r, b) - g > 70) & (r > 150) & (b > 150)


def key_cell(cell):
    """Transparent where magenta touches the edges (flood fill); soft fringe by colour distance."""
    arr = np.array(cell.convert("RGBA")).astype(np.int32)
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    d = np.sqrt((r - 255) ** 2 + g ** 2 + (b - 255) ** 2)
    near = d < 120
    lab, _ = ndimage.label(near)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    bg = np.isin(lab, list(edge))
    # small enclosed magenta holes (between legs, inside handles) go too
    hole = ((d < 60) | ((np.minimum(r, b) - g > 110) & (r > 180) & (b > 180))) & ~bg
    bg |= hole
    out = arr.copy()
    alpha = np.where(bg, 0, 255)
    # the fringe: pixels next to the background lose their pink and some alpha
    grown = ndimage.binary_dilation(bg, iterations=4) & ~bg
    touch = ndimage.binary_dilation(bg, iterations=1) & ~bg
    k = np.clip((160 - d) / 160, 0, 1)
    fr = grown & (d < 200)
    alpha = np.where(fr, (255 * (1 - k * 0.85)).astype(np.int32), alpha)
    # the outermost pixel is half see-through, so no hard pink line is left
    alpha = np.where(touch, alpha // 2, alpha)
    # spill: near the edge, magenta tint (red and blue both above green) is pulled back to grey-ish
    m = np.minimum(r, b) - g
    pinky = grown & (m > 12)
    keep = np.maximum(r - b, 0)  # warm reds (orange fur) keep their red
    out[..., 0] = np.where(pinky, np.minimum(r, g + keep + (r - g - keep).clip(0) * 0.15), r)
    out[..., 2] = np.where(pinky, g + (b - g) * 0.15, b)
    out[..., 3] = alpha
    return Image.fromarray(out.clip(0, 255).astype(np.uint8))


def despill(cell):
    """Watercolour washes bleed into the magenta: near the edge, magenta-ness turns into transparency and its tint goes."""
    arr = np.array(cell).astype(np.int32)
    r, g, b, a = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]
    clear = a < 10
    near = ndimage.binary_dilation(clear, iterations=18) & ~clear
    m = np.clip((np.minimum(r, b) - g - 8) / 120, 0, 1) * near
    a2 = (a * (1 - m) ** 1.5).astype(np.int32)
    # pull the tint back: red and blue down toward the green, warm reds keep their red
    keep = np.maximum(r - b, 0)
    tint = m > 0
    r2 = np.where(tint, (r - (r - np.maximum(g + keep, g)) * m).astype(np.int32), r)
    b2 = np.where(tint, (b - (b - g) * m * 0.9).astype(np.int32), b)
    out = np.stack([r2, g, b2, a2], -1).clip(0, 255).astype(np.uint8)
    return Image.fromarray(out)


def biggest_only(cell):
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    if n == 0:
        return cell
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = int(np.argmax(sizes[1:]) + 1)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0, big}
    for k in edge:
        arr[lab == k, 3] = 0
    for k in range(1, n + 1):
        if sizes[k] < 30:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)


def frames(path, cols, rows):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    cw, ch = w / cols, h / rows
    for j in range(rows):
        for i in range(cols):
            cell = im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
            cell = biggest_only(despill(key_cell(cell)))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            yield cell.crop(box) if box else cell


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = set(sys.argv[1:])
    for sheet, names, prefix, cols, rows, size in SPRITES:
        f = SHEETS / f"{sheet}.png"
        if not f.exists() or (want and sheet not in want):
            continue
        for name, fr in zip(names, frames(f, cols, rows)):
            fr.thumbnail((size, size), Image.LANCZOS)
            fr.save(OUT / f"{prefix}_{name}.webp", "WEBP", quality=90, method=6)
        print(sheet, flush=True)
    for name, width in SCENES.items():
        f = SHEETS / f"{name}.png"
        if not f.exists() or (want and name not in want):
            continue
        g = Image.open(f).convert("RGB")
        g = g.resize((width, round(width * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / f"{name}.webp", "WEBP", quality=86, method=6)
        print(name, flush=True)
    rounds(want)


def roundel(img, size):
    """The painted circle on its dark ground: found by brightness, cropped square, masked round."""
    g = np.array(img.convert("L")).astype(int)
    lit = g > 40
    ys, xs = np.nonzero(lit)
    if len(xs) == 0:
        return None
    # the circle's box: robust to stray dark parts of the painting
    x0, x1 = np.percentile(xs, [0.3, 99.7]); y0, y1 = np.percentile(ys, [0.3, 99.7])
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    r = max(x1 - x0, y1 - y0) / 2 * 0.985
    sq = img.convert("RGBA").crop((int(cx - r), int(cy - r), int(cx + r), int(cy + r))).resize((size, size), Image.LANCZOS)
    m = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(m).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    sq.putalpha(m.resize((size, size), Image.LANCZOS))
    return sq


def rounds(want):
    for sheet, (cols, rows, names, prefix, size) in ROUND.items():
        f = SHEETS / f"{sheet}.png"
        if not f.exists() or (want and sheet not in want):
            continue
        im = Image.open(f).convert("RGB")
        cw, ch = im.width / cols, im.height / rows
        k = 0
        for j in range(rows):
            for i in range(cols):
                c = roundel(im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch))), size)
                c.save(OUT / f"{prefix}_{names[k]}.webp", "WEBP", quality=88, method=6)
                k += 1
        print(sheet, flush=True)
    for t in TALES:
        f = SHEETS / f"{t}.png"
        if not f.exists() or (want and t not in want and "tales" not in want):
            continue
        roundel(Image.open(f), 820).save(OUT / f"v_{t}.webp", "WEBP", quality=84, method=6)
        print(t, flush=True)


if __name__ == "__main__":
    main()
