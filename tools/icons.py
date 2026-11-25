#!/usr/bin/env python3
"""Dibuja el icono de fábrica de cada app de la familia como PNG (lo usa update_catalog.py para `icons/<id>.png`).

Lee del repo local de la app (nunca lo cambia): el `android:icon` de `<application>` en el manifiesto, su icono adaptable
(`mipmap-anydpi*/…xml`) y sus capas. Las capas raster (webp/png) se componen tal cual; las vectoriales de Android se pasan
a SVG (rutas, rellenos, trazos y grupos con translate/scale/rotate) y las dibuja cairosvg (`pip install cairosvg`). Se queda con la parte
visible (72 de 108 dp) y redondea las esquinas, como un lanzador.
"""
import io
import os
import re
import xml.etree.ElementTree as ET

import cairosvg  # pip install cairosvg
from PIL import Image, ImageDraw

A = "{http://schemas.android.com/apk/res/android}"
LAYER = 432          # 108 dp a xxxhdpi
SIZE = 144           # el PNG final


def _res(folder, apk_dir):
    return os.path.join(folder, apk_dir, "app", "src", "main", "res")


def _manifest_icon(folder, apk_dir):
    root = ET.parse(os.path.join(folder, apk_dir, "app", "src", "main", "AndroidManifest.xml")).getroot()
    app = root.find("application")
    ref = app.get(A + "icon") if app is not None else None
    if not ref or not ref.startswith("@mipmap/"):
        raise ValueError("sin android:icon @mipmap en <application>")
    return ref.split("/", 1)[1]


def _colors(res):
    found = {}
    folder = os.path.join(res, "values")
    for f in sorted(os.listdir(folder)) if os.path.isdir(folder) else []:
        if f.endswith(".xml"):
            for c in ET.parse(os.path.join(folder, f)).getroot().iter("color"):
                found[c.get("name")] = (c.text or "").strip()
    return found


def _rgba(value, colors, alpha=1.0):
    """«#RGB», «#RRGGBB», «#AARRGGBB» o «@color/x» → (r, g, b, a 0..1)."""
    if value is None:
        return None
    if value.startswith("@color/"):
        return _rgba(colors.get(value.split("/", 1)[1]), colors, alpha)
    v = value.lstrip("#")
    if len(v) == 3:
        v = "".join(ch * 2 for ch in v)
    if len(v) == 6:
        v = "FF" + v
    if len(v) != 8:
        return None
    a, r, g, b = (int(v[i:i + 2], 16) for i in (0, 2, 4, 6))
    return r, g, b, a / 255 * alpha


def _css(c):
    return "none" if c is None else f"rgba({c[0]},{c[1]},{c[2]},{c[3]:.3f})"


def _svg_children(node, colors):
    out = []
    for child in node:
        tag = child.tag
        if tag == "group":
            tx, ty = float(child.get(A + "translateX", 0)), float(child.get(A + "translateY", 0))
            sx, sy = float(child.get(A + "scaleX", 1)), float(child.get(A + "scaleY", 1))
            rot = float(child.get(A + "rotation", 0))
            px, py = float(child.get(A + "pivotX", 0)), float(child.get(A + "pivotY", 0))
            t = (f"translate({tx + px} {ty + py}) rotate({rot}) scale({sx} {sy}) translate({-px} {-py})")
            out.append(f'<g transform="{t}">{_svg_children(child, colors)}</g>')
        elif tag == "path":
            d = child.get(A + "pathData")
            if not d:
                continue
            fill = _rgba(child.get(A + "fillColor"), colors, float(child.get(A + "fillAlpha", 1)))
            stroke = _rgba(child.get(A + "strokeColor"), colors, float(child.get(A + "strokeAlpha", 1)))
            width = child.get(A + "strokeWidth", "0")
            cap = child.get(A + "strokeLineCap", "butt"); join = child.get(A + "strokeLineJoin", "miter")
            rule = "evenodd" if child.get(A + "fillType") == "evenOdd" else "nonzero"
            out.append(f'<path d="{d}" fill="{_css(fill)}" fill-rule="{rule}" stroke="{_css(stroke)}" '
                       f'stroke-width="{width}" stroke-linecap="{cap}" stroke-linejoin="{join}"/>')
    return "".join(out)


def _vector(path, colors):
    root = ET.parse(path).getroot()
    vw, vh = root.get(A + "viewportWidth"), root.get(A + "viewportHeight")
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{LAYER}" height="{LAYER}" viewBox="0 0 {vw} {vh}">'
           f'{_svg_children(root, colors)}</svg>')
    png = cairosvg.svg2png(bytestring=svg.encode(), output_width=LAYER, output_height=LAYER)
    return Image.open(io.BytesIO(png)).convert("RGBA")


def _layer(res, ref, colors):
    """Una capa (fondo o primer plano) a LAYER×LAYER."""
    if ref.startswith("@color/") or ref.startswith("#"):
        return Image.new("RGBA", (LAYER, LAYER), tuple(int(x) for x in _rgba(ref, colors)[:3]) + (255,))
    kind, name = ref[1:].split("/", 1)
    for density in ("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi", "nodpi"):
        for ext in ("webp", "png"):
            p = os.path.join(res, f"{kind}-{density}", f"{name}.{ext}")
            if os.path.exists(p):
                return Image.open(p).convert("RGBA").resize((LAYER, LAYER), Image.LANCZOS)
    p = os.path.join(res, kind, f"{name}.xml")
    if os.path.exists(p):
        return _vector(p, colors)
    raise ValueError(f"no encuentro {ref}")


def render(folder, apk_dir):
    """El icono de la app como PNG (bytes)."""
    res = _res(folder, apk_dir)
    colors = _colors(res)
    name = _manifest_icon(folder, apk_dir)
    adaptive = next((os.path.join(res, d, f"{name}.xml") for d in sorted(os.listdir(res))
                     if d.startswith("mipmap-anydpi") and os.path.exists(os.path.join(res, d, f"{name}.xml"))), None)
    if adaptive:
        root = ET.parse(adaptive).getroot()
        canvas = Image.new("RGBA", (LAYER, LAYER), (0, 0, 0, 0))
        for part in ("background", "foreground"):
            node = root.find(part)
            if node is not None and node.get(A + "drawable"):
                canvas.alpha_composite(_layer(res, node.get(A + "drawable"), colors))
        cut = LAYER * 18 // 108
        icon = canvas.crop((cut, cut, LAYER - cut, LAYER - cut))
    else:
        icon = _layer(res, f"@mipmap/{name}", colors)
    icon = icon.resize((SIZE, SIZE), Image.LANCZOS)
    mask = Image.new("L", (SIZE * 4, SIZE * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, SIZE * 4 - 1, SIZE * 4 - 1), radius=SIZE * 4 * 22 // 100, fill=255)
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    out.paste(icon, (0, 0), mask.resize((SIZE, SIZE), Image.LANCZOS))
    buf = io.BytesIO()
    out.save(buf, "PNG", optimize=True)
    return buf.getvalue()
