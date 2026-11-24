#!/usr/bin/env python3
"""Regenera catalog.json (raíz del repo) a partir de las carpetas locales de la familia.

Uso (después de cada versión de cualquier app):
    python3 tools/update_catalog.py            # escribe catalog.json
    python3 tools/update_catalog.py --check    # además comprueba que cada APK ya está en GitHub (HEAD)

Para cada app busca el APK que existe en su carpeta (`android/<Prefijo>-<versión>.apk`), lee el paquete, versionCode y
versionName del propio APK con `aapt2 dump badging` (si no hay aapt2, del `build.gradle.kts` confirmado en git) y toma
las novedades de la sección de esa versión en su CHANGELOG.md. Nunca escribe en los otros repos.
"""
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HOME = os.path.dirname(ROOT)  # las carpetas de la familia viven junto a esta
BASE = "https://raw.githubusercontent.com/SiriusMagic/"  # el mismo que Sources.BASE en la app

# id, nombre, carpeta local, repo en GitHub, carpeta del APK, nombre del APK (con {v}), paquete, color
APPS = [
    ("stone", "Stone", "Stone", "Stone", "android-native", "Stone-{v}-optimized.apk", "com.zirakusa.stone.kotlin.preview", "#8E7CF0"),
    ("stone-video", "Stone Video", "stone-v-deo-mcp", "Stone-v-deo-mcp", "android-native", "StoneVideo-{v}.apk", "com.zirakusa.stone.video", "#E85D9A"),
    ("gateway-hub", "Gateway Hub", "gateway-hub-mcp", "Gateway-Hub-MCP", "android", "GatewayHub-{v}.apk", "com.zirakusa.gatewayhub", "#E8B931"),
    ("telefono", "Teléfono", "tel-fono.mcp", "Tel-fono.mcp", "android", "Telefono-{v}.apk", "com.zirakusa.telefono", "#4CE08A"),
    ("sms", "SMS", "sms", "SMS", "android", "SMS-{v}.apk", "com.zirakusa.sms", "#4FC3F7"),
    ("reloj", "Reloj", "reloj-mcp", "Reloj-mcp", "android", "Reloj-{v}.apk", "com.zirakusa.reloj", "#FF9A1F"),
    ("calendario", "Calendario", "calendario-mcp", "Calendario-mcp", "android", "Calendario-{v}.apk", "com.zirakusa.calendario", "#EB4D3D"),
    ("notas", "Notas", "notas.mcp", "Notas.mcp", "android", "Notas-{v}.apk", "com.zirakusa.notas", "#F5C518"),
    ("clima", "Clima", "clima-mcp", "Clima-mcp", "android", "Clima-{v}.apk", "com.zirakusa.clima", "#2F80ED"),
    ("camara", "Cámara", "c-mara-mcp", "C-mara-mcp", "android", "Camara-{v}.apk", "com.zirakusa.camara", "#9E9E9E"),
    ("qr", "QR", "qr-mcp", "Qr-mcp", "android", "QR-{v}.apk", "com.zirakusa.qr", "#26A69A"),
    ("redes", "Redes", "analizador-de-redes-mcp", "Analizador-de-redes-mcp", "android", "Redes-{v}.apk", "com.zirakusa.redes", "#2E9E5B"),
    ("brujula", "Brújula", "br-jula-mcp...", "Br-jula-mcp...", "android", "Brujula-{v}.apk", "com.zirakusa.brujula", "#FF3B30"),
    ("nivel", "Nivel", "nivel-burbuja-mcp", "Nivel-burbuja-mcp", "android", "Nivel-{v}.apk", "com.zirakusa.nivel", "#C6F135"),
    ("calculadora", "Calculadora", "calculadora.mcp", "Calculadora.mcp", "android", "Calculadora-{v}.apk", "com.zirakusa.calculadora", "#FFB300"),
    ("filem", "Filem", "filem.mcp", "Filem.mcp", "android", "Filem-{v}.apk", "com.zirakusa.filem", "#7FD49B"),
    ("luma", "Luma", "galer-a-mcp", "Galer-a-mcp", "android", "Luma-{v}.apk", "app.luma.gallery", "#B388FF"),
    ("inicio", "Inicio", "laucher-mcp", "Laucher-mcp", "android", "Inicio-{v}.apk", "com.zirakusa.launcher", "#F06292"),
    # Tienda: este repo. En GitHub se llama Contactos-mcp hasta que se renombre a Tienda-mcp; la app prueba las dos.
    ("tienda", "Tienda", os.path.basename(ROOT), "Contactos-mcp", "android", "Tienda-{v}.apk", "com.zirakusa.tienda", "#3DDC97"),
]


def aapt2():
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "/opt/android-sdk"
    tools = os.path.join(sdk, "build-tools")
    if not os.path.isdir(tools):
        return None
    for v in sorted(os.listdir(tools), reverse=True):
        path = os.path.join(tools, v, "aapt2")
        if os.access(path, os.X_OK):
            return path
    return None


def badging(tool, apk):
    out = subprocess.run([tool, "dump", "badging", apk], capture_output=True, text=True, check=True).stdout
    m = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", out)
    if not m:
        raise SystemExit(f"No se pudo leer {apk}")
    return m.group(1), int(m.group(2)), m.group(3)


def from_gradle(folder, module):
    text = subprocess.run(["git", "-C", folder, "show", f"HEAD:{module}/app/build.gradle.kts"],
                          capture_output=True, text=True, check=True).stdout
    pkg = re.search(r'applicationId\s*=\s*"([^"]+)"', text).group(1)
    code = int(re.search(r"versionCode\s*=\s*(\d+)", text).group(1))
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1)
    return pkg, code, name


def find_apk(folder, apk_dir, pattern):
    regex = re.compile("^" + re.escape(pattern).replace(re.escape("{v}"), r"([0-9][0-9A-Za-z.\-]*?)") + "$")
    found = []
    for f in os.listdir(os.path.join(folder, apk_dir)):
        m = regex.match(f)
        if m:
            found.append((m.group(1), f))
    if len(found) != 1:
        raise SystemExit(f"{folder}/{apk_dir}: se esperaba un APK «{pattern}» y hay {len(found)}")
    return found[0]


def notes(folder, version):
    """Las novedades de esa versión en el CHANGELOG: los títulos en negrita, o la primera frase."""
    path = os.path.join(folder, "CHANGELOG.md")
    if not os.path.exists(path):
        return ""
    lines = open(path, encoding="utf-8").read().splitlines()
    section, inside = [], False
    for line in lines:
        if line.startswith("#"):
            if inside:
                break
            inside = re.search(r"(^|[^0-9.])" + re.escape(version) + r"([^0-9.]|$)", line) is not None
            continue
        if inside:
            section.append(line)
    text = " ".join(s.strip() for s in section)
    # Los títulos en negrita al principio de cada punto («- **Conectar todas** …»); si no hay, cualquier negrita.
    heads = [m.group(1) for s in section for m in [re.match(r"\s*-\s+\*\*([^*]+)\*\*", s)] if m]
    bold = [b.strip().rstrip(":").strip() for b in (heads or re.findall(r"\*\*([^*]+)\*\*", text))]
    if bold:
        seen = []
        for b in bold:
            if b not in seen:
                seen.append(b)
        out = ", ".join(seen[:4])
    else:
        first = text.lstrip("- ").replace("`", "")
        out = re.split(r"(?<=[.!?])\s", first, maxsplit=1)[0]
    out = out.replace("`", "")
    return out if len(out) <= 160 else out[:157].rstrip() + "…"


def main():
    check = "--check" in sys.argv
    tool = aapt2()
    apps = []
    for app_id, name, local, repo, apk_dir, pattern, package, color in APPS:
        folder = os.path.join(HOME, local)
        version, file_name = find_apk(folder, apk_dir, pattern)
        apk_path = os.path.join(folder, apk_dir, file_name)
        pkg, code, vname = badging(tool, apk_path) if tool else from_gradle(folder, apk_dir)
        if pkg != package or vname != version:
            raise SystemExit(f"{file_name}: es {pkg} {vname}, se esperaba {package} {version}")
        url = f"{BASE}{repo}/main/{apk_dir}/{file_name}"
        if check:
            try:
                status = urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=30).status
            except urllib.error.HTTPError as e:
                status = e.code
            print(f"{status} {url}" + ("" if status == 200 else "   <- todavía no está en GitHub (¿falta subirlo?)"))
        apps.append({
            "id": app_id, "name": name, "package": pkg, "versionCode": code, "versionName": vname,
            "apk": url, "notes": notes(folder, vname), "color": color,
        })
    catalog = {"format": 1, "apps": apps}
    with open(os.path.join(ROOT, "catalog.json"), "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
        f.write("\n")
    for a in apps:
        print(f"{a['name']:<12} {a['versionName']:<8} ({a['versionCode']})  {a['notes'][:70]}")


if __name__ == "__main__":
    main()
