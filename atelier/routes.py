"""Routes : chemins de 3 blocs entre les lieux, qui contournent les lacs et la mer."""
import numpy as np
from PIL import Image, ImageDraw
from skimage.graph import route_through_array

from .eau import masques_lacs, trace_riviere
from .monde import HAUTEUR, LARGEUR, LIEUX, RIVIERES, ROUTES

LARGEUR_ROUTE = 3
ZONE_RIVIERE = 15       # pixels autour d'un tracé de rivière où la route peut passer à gué
REDUCTION = 8           # taille des cases de la grille de contournement


def _eau_interdite(niveau: np.ndarray) -> np.ndarray:
    zone = np.zeros(niveau.shape, bool)
    for r in RIVIERES:
        for x, z in trace_riviere(r):
            zone[max(z - ZONE_RIVIERE, 0):z + ZONE_RIVIERE + 1, max(x - ZONE_RIVIERE, 0):x + ZONE_RIVIERE + 1] = True
    # le passage à gué n'est permis que sur les rivières, jamais à travers un lac
    for lac in masques_lacs():
        zone &= ~lac
    return ~np.isnan(niveau) & ~zone


def _segment(a: tuple[float, float], b: tuple[float, float]) -> list[tuple[int, int]]:
    """Segment légèrement ondulé, comme sur l'ébauche, en pixels."""
    (x0, z0), (x1, z1) = a, b
    n = max(int(np.hypot(x1 - x0, z1 - z0) / 20), 2)
    pts = []
    for t in np.linspace(0, 1, n + 1):
        ond = np.sin(t * np.pi) * np.sin(t * 7 + x0 / 240) * 20
        nx, nz = -(z1 - z0), x1 - x0
        norme = max(np.hypot(nx, nz), 1e-9)
        pts.append((round(x0 + (x1 - x0) * t + nx / norme * ond), round(z0 + (z1 - z0) * t + nz / norme * ond)))
    return pts


def _dessin(points: list[tuple[int, int]]) -> np.ndarray:
    im = Image.new("1", (LARGEUR, HAUTEUR), 0)
    ImageDraw.Draw(im).line(points, fill=1, width=LARGEUR_ROUTE)
    for x, z in (points[0], points[-1]):
        ImageDraw.Draw(im).ellipse([x - 1, z - 1, x + 1, z + 1], fill=1)
    return np.asarray(im, bool)


def _detour(a, b, cout: np.ndarray) -> list[tuple[int, int]]:
    f = REDUCTION
    debut = (int(a[1]) // f, int(a[0]) // f)
    fin = (int(b[1]) // f, int(b[0]) // f)
    chemin, _ = route_through_array(cout, debut, fin, fully_connected=True, geometric=True)
    centres = np.array([(c * f + f // 2, r * f + f // 2) for r, c in chemin], float)
    pts = [(int(a[0]), int(a[1]))] + [tuple(p) for p in centres[1:-1].astype(int)] + [(int(b[0]), int(b[1]))]
    return pts


def _lisser(points: list[tuple[int, int]], fenetre: int = 9) -> list[tuple[int, int]]:
    """Adoucit les marches d'escalier d'un détour, extrémités conservées."""
    p = np.array(points, float)
    if len(p) <= fenetre:
        return points
    noyau = np.ones(fenetre) / fenetre
    lisse = np.column_stack([np.convolve(np.pad(p[:, i], fenetre // 2, mode="edge"), noyau, "valid") for i in (0, 1)])
    lisse[0], lisse[-1] = p[0], p[-1]
    return [tuple(q) for q in np.round(lisse).astype(int)]


def carte_routes(alt: np.ndarray, niveau: np.ndarray) -> np.ndarray:
    interdit = _eau_interdite(niveau)
    f = REDUCTION
    h, w = HAUTEUR // f, LARGEUR // f
    bloque = interdit[:h * f, :w * f].reshape(h, f, w, f).any(axis=(1, 3))
    pente = np.abs(np.gradient(alt[:h * f:f, :w * f:f])).sum(axis=0)
    cout = np.where(bloque, np.inf, 1.0 + pente)

    pos = {l.nom: l.pixel() for l in LIEUX}
    routes = np.zeros(alt.shape, bool)
    for route in ROUTES:
        pts = [pos[p] if isinstance(p, str) else (p[0] * LARGEUR, p[1] * HAUTEUR) for p in route]
        for a, b in zip(pts, pts[1:]):
            seg = _dessin(_segment(a, b))
            if (seg & interdit).any():
                detour = _detour(a, b, cout)
                seg = _dessin(_lisser(detour))
                if (seg & interdit).any():
                    seg = _dessin(detour)
            routes |= seg
    return routes
