"""Carte conceptuelle d'Aldermyr (ébauche visuelle, pas encore la génération Hytale)."""
import numpy as np
from scipy.ndimage import zoom, gaussian_filter
from PIL import Image, ImageDraw, ImageFont

W, H = 1600, 1200
rng = np.random.default_rng(7)


def fbm(octaves=6, base=4, persistence=0.5):
    out = np.zeros((H, W))
    amp, total = 1.0, 0.0
    for o in range(octaves):
        n = base * 2 ** o
        grid = rng.random((n + 3, int(n * W / H) + 3))
        z = zoom(grid, (H / n, W / n), order=3)[:H, :W]
        out += amp * z
        total += amp
        amp *= persistence
    out /= total
    return (out - out.min()) / (out.max() - out.min())


yy, xx = np.mgrid[0:H, 0:W]
x, y = xx / W, yy / H
n1, n2, n3 = fbm(), fbm(), fbm(octaves=7, base=6)
wx = x + (n1 - 0.5) * 0.16 + (fbm(5, 12) - 0.5) * 0.03
wy = y + (n2 - 0.5) * 0.16 + (fbm(5, 12) - 0.5) * 0.03

# --- terre / mer -------------------------------------------------------------
ell = ((x - 0.5) / 0.47) ** 2 + ((y - 0.47) / 0.45) ** 2
land = (ell + (n3 - 0.5) * 0.55 < 1.0) & (wy < 0.90)

# --- régions -----------------------------------------------------------------
CRATER = (0.50, 0.36)
dcr = np.hypot((wx - CRATER[0]) * W / H, wy - CRATER[1])
region = np.full((H, W), "plaines", dtype=object)
region[wy >= 0.62] = "marches"
region[wy >= 0.76] = "cote"
region[(wx < 0.30) & (wy < 0.62)] = "steppes"
region[(wx > 0.70) & (wy < 0.62)] = "foret"
region[wy < 0.22] = "montagnes"
region[dcr < 0.105] = "ruines"
region[dcr < 0.07] = "cratere"
region[~land] = "mer"

# --- relief ------------------------------------------------------------------
elev = 0.38 + (n3 - 0.5) * 0.12
mount = np.clip((0.26 - wy) / 0.22, 0, 1)
ridge = 1 - np.abs(fbm(6, 6) * 2 - 1)
elev += mount * (0.30 + ridge * 0.55)
elev += np.where(region == "steppes", (fbm(5, 8) - 0.5) * 0.18, 0)
rim = np.exp(-((dcr - 0.075) / 0.018) ** 2) * 0.22
bowl = np.where(dcr < 0.07, -0.18 * (1 - dcr / 0.07), 0)
elev += rim + bowl
elev = np.where(land, elev, 0.15)
elev = gaussian_filter(elev, 1.2)

# --- couleurs ----------------------------------------------------------------
PAL = {
    "plaines": (176, 196, 112), "montagnes": (128, 116, 100), "steppes": (140, 108, 84),
    "foret": (58, 104, 56), "marches": (138, 132, 86), "cote": (196, 186, 132),
    "ruines": (104, 92, 100), "cratere": (70, 52, 84), "mer": (44, 88, 120),
}
img = np.zeros((H, W, 3))
for k, c in PAL.items():
    img[region == k] = c
img = gaussian_filter(img, (2.5, 2.5, 0))
# neige sur les sommets
snow = np.clip((elev - 0.85) / 0.12, 0, 1)[..., None] * land[..., None]
img = img * (1 - snow) + np.array([236, 236, 240]) * snow
# variation de texture
img *= (0.88 + n3[..., None] * 0.24)
# ombrage du relief
gy, gx = np.gradient(elev * 40)
shade = np.clip(1 - (gx * 0.7 + gy * 0.7) * 1.6, 0.45, 1.45)[..., None]
img = np.where(land[..., None], img * shade, img)
# mer : plus sombre au large
deep = np.clip(gaussian_filter((~land).astype(float), 25), 0, 1)[..., None]
img = np.where(land[..., None], img, img * (1.15 - deep * 0.45))
# côtes
coast = gaussian_filter(land.astype(float), 2)
edge = ((coast > 0.2) & (coast < 0.8))[..., None]
img = np.where(edge & ~land[..., None], img * 0.6 + np.array([220, 210, 170]) * 0.4, img)
# lueur du cratère
glow = np.exp(-(dcr / 0.05) ** 2)[..., None]
img = img * (1 - glow * 0.5) + np.array([170, 110, 230]) * glow * 0.5
# teinte parchemin
img = img * 0.85 + np.array([235, 220, 180]) * 0.15
im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


# --- rivières ----------------------------------------------------------------
def catmull(pts, n=30):
    pts = [pts[0]] + list(pts) + [pts[-1]]
    out = []
    for i in range(1, len(pts) - 2):
        p0, p1, p2, p3 = map(np.array, pts[i - 1:i + 3])
        for t in np.linspace(0, 1, n, endpoint=False):
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(np.array(pts[-2]))
    return out


RIVERS = [
    # (points, largeur départ, largeur arrivée)
    ([(0.58, 0.13), (0.60, 0.24), (0.62, 0.36), (0.58, 0.47), (0.53, 0.52), (0.52, 0.62),
      (0.49, 0.74), (0.50, 0.84), (0.49, 0.92), (0.50, 0.99)], 3, 11),           # grand fleuve
    ([(0.33, 0.24), (0.36, 0.34), (0.40, 0.45), (0.47, 0.52), (0.53, 0.53)], 2, 6),  # affluent ouest
    ([(0.80, 0.14), (0.78, 0.25), (0.82, 0.34), (0.79, 0.45), (0.83, 0.55), (0.80, 0.66),
      (0.84, 0.76), (0.86, 0.86), (0.88, 0.97)], 3, 9),                           # rivière de Sylvaë
    ([(0.655, 0.785), (0.60, 0.84), (0.53, 0.86), (0.50, 0.87)], 2, 4),          # émissaire du lac des Marches
    ([(0.94, 0.30), (0.90, 0.38), (0.88, 0.47), (0.83, 0.54)], 2, 5),             # affluent forestier
]
riv = Image.new("L", (W * 2, H * 2), 0)
rd = ImageDraw.Draw(riv)
for pts, w0, w1 in RIVERS:
    curve = catmull(pts)
    for i in range(len(curve) - 1):
        t = i / (len(curve) - 1)
        (ax, ay), (bx, by) = curve[i], curve[i + 1]
        wob = np.sin(i * 0.9) * 0.0025
        w = int((w0 + (w1 - w0) * t) * 2)
        rd.line([(int((ax + wob) * W * 2), int(ay * H * 2)), (int((bx + wob) * W * 2), int(by * H * 2))],
                fill=255, width=w)
        rd.ellipse([int((bx + wob) * W * 2) - w // 2, int(by * H * 2) - w // 2,
                    int((bx + wob) * W * 2) + w // 2, int(by * H * 2) + w // 2], fill=255)
riv = np.asarray(riv.resize((W, H), Image.LANCZOS)).astype(float) / 255
riv *= gaussian_filter(land.astype(float), 1.5)
LAKES = [  # (centre x, centre y, rayon x, rayon y)
    (0.585, 0.165, 0.030, 0.028),   # lac de montagne, source du fleuve
    (0.815, 0.545, 0.040, 0.035),   # lac sacré elfique, confluence
    (0.105, 0.585, 0.035, 0.030),   # lac des steppes
    (0.655, 0.785, 0.045, 0.030),   # lac des Marches
]
nl = fbm(5, 10)
for cx, cy, rx, ry in LAKES:
    m = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + (nl - 0.5) * 1.1
    riv = np.maximum(riv, np.clip((1.0 - m) * 6, 0, 1))
bank = np.clip(gaussian_filter(riv, 2.0) * 2.2, 0, 1) - riv
arr = np.asarray(im).astype(float)
arr = arr * (1 - bank[..., None] * 0.25)
arr = arr * (1 - riv[..., None]) + np.array([60, 112, 150]) * riv[..., None]
im = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))

# --- routes, villes, textes ----------------------------------------------------
d = ImageDraw.Draw(im)
F = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
f_big, f_mid, f_small = (ImageFont.truetype(F, s) for s in (40, 26, 20))
P = lambda a, b: (int(a * W), int(b * H))

cities = {
    "Aurelia": (0.50, 0.50), "Durmarak": (0.42, 0.12), "Cœur-Sylve": (0.86, 0.40),
    "Gorrath": (0.14, 0.42), "Port-Écume": (0.475, 0.895),
}
roads = [["Aurelia", (0.48, 0.70), "Port-Écume"], ["Aurelia", (0.38, 0.40), (0.40, 0.22), "Durmarak"],
         ["Aurelia", (0.70, 0.46), "Cœur-Sylve"], ["Aurelia", (0.32, 0.47), "Gorrath"],
         ["Gorrath", (0.28, 0.68), "Port-Écume"], ["Cœur-Sylve", (0.74, 0.68), "Port-Écume"]]


def road(way):
    way = [cities[w] if isinstance(w, str) else w for w in way]
    pts = []
    for (x0, y0), (x1, y1) in zip(way, way[1:]):
        for t in np.linspace(0, 1, 24, endpoint=False):
            px = x0 + (x1 - x0) * t + np.sin(t * 7 + x0 * 20) * 0.005
            py = y0 + (y1 - y0) * t + np.cos(t * 6 + y0 * 20) * 0.004
            pts.append(P(px, py))
    pts.append(P(*way[-1]))
    for i in range(0, len(pts) - 1, 2):
        d.line([pts[i], pts[i + 1]], fill=(92, 60, 30), width=4)


for way in roads:
    road(way)


def label(xy, text, font, fill=(250, 244, 225)):
    d.text(xy, text, font=font, fill=fill, anchor="mm", stroke_width=3, stroke_fill=(40, 28, 20))


for name, (cx, cy) in cities.items():
    px, py = P(cx, cy)
    d.rectangle([px - 9, py - 9, px + 9, py + 9], fill=(170, 30, 30), outline=(40, 20, 10), width=3)
    label((px, py + 26), name, f_small)


# --- éléments des steppes ----------------------------------------------------
f_feat = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-BoldItalic.ttf", 15)
# volcan et coulées de lave
vx, vy = P(0.06, 0.37)
for ang, ln in [(0.4, 90), (1.1, 70), (-0.3, 60), (1.7, 55)]:
    pts = [(vx, vy)]
    for k in range(1, 8):
        r = ln * k / 7
        pts.append((vx + int(np.cos(ang) * r + np.sin(k * 2.1) * 4), vy + int(np.sin(ang) * r * 0.9 + 10)))
    d.line(pts, fill=(235, 110, 30), width=3)
d.polygon([(vx - 30, vy + 22), (vx - 7, vy - 22), (vx + 7, vy - 22), (vx + 30, vy + 22)],
          fill=(70, 50, 45), outline=(30, 20, 15))
d.polygon([(vx - 7, vy - 22), (vx + 7, vy - 22), (vx + 4, vy - 14), (vx - 4, vy - 14)], fill=(255, 140, 40))
label((vx + 8, vy + 40), "Mont Braise", f_feat, (255, 210, 170))
# champs d'obsidienne (disputés avec les nains)
ox, oy = P(0.27, 0.20)
for dx, dy, r in [(-14, 0, 9), (0, -6, 8), (12, 3, 10), (-2, 9, 7), (22, -5, 6)]:
    d.polygon([(ox + dx, oy + dy - r), (ox + dx + r, oy + dy), (ox + dx, oy + dy + r), (ox + dx - r, oy + dy)],
              fill=(25, 20, 35), outline=(120, 110, 150))
label((ox + 4, oy + 24), "Champs d'Obsidienne", f_feat, (215, 205, 240))
# arène des duels du Grand Chef
ax, ay = P(0.075, 0.47)
d.ellipse([ax - 18, ay - 12, ax + 18, ay + 12], fill=(205, 170, 120), outline=(60, 35, 20), width=4)
d.ellipse([ax - 9, ay - 5, ax + 9, ay + 5], outline=(120, 40, 30), width=2)
label((ax + 10, ay + 26), "Arène des Duels", f_feat, (255, 225, 190))
# ruines de la Fosse aux Chaînes (ancienne forteresse d'Arcanthis)
rx_, ry_ = P(0.25, 0.36)
for dx, h in [(-14, 16), (-4, 24), (6, 12), (16, 20)]:
    d.rectangle([rx_ + dx - 4, ry_ - h, rx_ + dx + 4, ry_ + 6], fill=(150, 140, 145), outline=(50, 40, 45))
d.line([(rx_ - 18, ry_ + 6), (rx_ + 20, ry_ + 6)], fill=(50, 40, 45), width=3)
label((rx_, ry_ + 22), "Fosse aux Chaînes", f_feat, (230, 220, 225))

# villes secondaires et villages
f_vil = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Italic.ttf", 15)
f_town = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf", 18)
TOWNS = [("Valmont", (0.385, 0.335))]          # ville de l'Empire, frontière ouest
VILLAGES = [
    # Empire
    ("Gué-Rouge", (0.555, 0.52)), ("Pont-Vieux", (0.43, 0.475)), ("Blé-d'Or", (0.68, 0.25)),
    ("Lisière", (0.685, 0.385)),
    # Durmarak
    ("Forgeval", (0.30, 0.14)), ("Mine-Profonde", (0.79, 0.165)),
    # Sylvaë
    ("Feuillombre", (0.75, 0.33)), ("Clairbois", (0.93, 0.60)),
    # Gorrath
    ("Croc-Sec", (0.19, 0.23)), ("Os-Blanc", (0.20, 0.47)),
    # Port-Écume
    ("Sel-Marin", (0.30, 0.86)), ("Anse-Grise", (0.62, 0.875)),
    # Marches brisées
    ("Carrefour des Pendus", (0.335, 0.74)),
]
for name, (vx, vy) in VILLAGES:
    px, py = P(vx, vy)
    d.ellipse([px - 6, py - 6, px + 6, py + 6], fill=(245, 232, 200), outline=(50, 30, 15), width=2)
    label((px, py + 17), name, f_vil, (250, 244, 225))
for name, (vx, vy) in TOWNS:
    px, py = P(vx, vy)
    d.rectangle([px - 7, py - 7, px + 7, py + 7], fill=(215, 120, 40), outline=(40, 20, 10), width=3)
    label((px, py + 22), name, f_town)

label(P(0.68, 0.10), "MONTAGNES DE DURMARAK", f_mid)
label(P(0.68, 0.145), "nains", f_small, (230, 230, 230))
label(P(0.15, 0.30), "STEPPES DE GORRATH", f_mid)
label(P(0.15, 0.345), "orcs", f_small, (230, 230, 230))
label(P(0.86, 0.27), "FORÊT DE SYLVAË", f_mid)
label(P(0.86, 0.315), "elfes", f_small, (230, 230, 230))
label(P(0.50, 0.585), "EMPIRE D'AURELION", f_mid)
label(P(0.50, 0.625), "humains", f_small, (230, 230, 230))
label(P(0.50, 0.36), "LE CRATÈRE", f_mid, (225, 200, 255))
label(P(0.30, 0.69), "~ MARCHES BRISÉES ~", f_mid, (240, 225, 190))
label(P(0.72, 0.71), "terres sans loi", f_small, (240, 225, 190))
label(P(0.40, 0.955), "MER", f_mid, (200, 225, 240))

f_tiny = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-BoldItalic.ttf", 16)
for (lx, ly), name in [((0.585, 0.215), "Lac de Givre"), ((0.815, 0.605), "Miroir d'Onde"),
                       ((0.105, 0.635), "Lac de Cendre"), ((0.655, 0.835), "Lac des Brumes")]:
    label(P(lx, ly), name, f_tiny, (215, 235, 250))


# légende
lx, ly = 40, H - 175
d.rectangle([lx, ly, lx + 230, ly + 135], fill=(235, 222, 190), outline=(60, 40, 20), width=3)
f_leg = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf", 16)
d.rectangle([lx + 18, ly + 18, lx + 34, ly + 34], fill=(170, 30, 30), outline=(40, 20, 10), width=2)
d.text((lx + 48, ly + 26), "Capitale", font=f_leg, fill=(40, 28, 20), anchor="lm")
d.rectangle([lx + 19, ly + 49, lx + 33, ly + 63], fill=(215, 120, 40), outline=(40, 20, 10), width=2)
d.text((lx + 48, ly + 56), "Ville", font=f_leg, fill=(40, 28, 20), anchor="lm")
d.ellipse([lx + 20, ly + 80, lx + 32, ly + 92], fill=(245, 232, 200), outline=(50, 30, 15), width=2)
d.text((lx + 48, ly + 86), "Village", font=f_leg, fill=(40, 28, 20), anchor="lm")
d.line([(lx + 16, ly + 115), (lx + 26, ly + 115)], fill=(92, 60, 30), width=4)
d.line([(lx + 30, ly + 115), (lx + 38, ly + 115)], fill=(92, 60, 30), width=4)
d.text((lx + 48, ly + 115), "Route", font=f_leg, fill=(40, 28, 20), anchor="lm")

# titre + cadre
d.rectangle([0, 0, W - 1, H - 1], outline=(60, 40, 20), width=14)
d.rectangle([14, 14, W - 15, H - 15], outline=(190, 160, 110), width=3)
label((170, 70), "ALDERMYR", f_big, (250, 236, 200))
label((W - 120, H - 50), "ébauche v0.5", f_small, (250, 236, 200))
# rose des vents
cx, cy = W - 110, 120
d.polygon([(cx, cy - 50), (cx - 12, cy), (cx + 12, cy)], fill=(250, 240, 210), outline=(40, 28, 20))
d.polygon([(cx, cy + 50), (cx - 12, cy), (cx + 12, cy)], fill=(90, 60, 30), outline=(40, 28, 20))
label((cx, cy - 66), "N", f_small)

im.save("/home/claude/carte/aldermyr_carte_v05.png")
print("ok")
