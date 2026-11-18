"""Carte des régions : contour de l'île et régions aux frontières ondulées."""
import numpy as np
from scipy.ndimage import gaussian_filter, label, zoom

from .bruit import fbm
from .monde import (CRATERE, GRAINE, HAUTEUR, LARGEUR, LIEUX, RAYON_CRATERE, RAYON_RUINES,
                    Region)

MARGE_BORD = 0.12      # bande (en fraction de la hauteur) où la terre est progressivement repoussée vers l'intérieur
RAYON_CALME = 250      # pixels : autour des lieux, la déformation s'atténue pour garder chaque lieu dans sa région


def coordonnees() -> tuple[np.ndarray, np.ndarray]:
    v, u = np.mgrid[0:HAUTEUR, 0:LARGEUR].astype(np.float32)
    return u / LARGEUR, v / HAUTEUR


def masque_lieux(rayon_px: int) -> np.ndarray:
    """1 près des lieux nommés, décroissant doucement à 0."""
    f = 8  # calcul sur une grille réduite, puis agrandissement
    m = np.zeros((HAUTEUR // f, LARGEUR // f), np.float32)
    for l in LIEUX:
        x, z = l.pixel()
        m[min(z // f, m.shape[0] - 1), min(x // f, m.shape[1] - 1)] = 1.0
    m = gaussian_filter(m, rayon_px / 2.5 / f)
    m = zoom(m / m.max(), f, order=1)[:HAUTEUR, :LARGEUR]
    return np.clip(m, 0, 1).astype(np.float32)


def distance_cratere(wu: np.ndarray, wv: np.ndarray) -> np.ndarray:
    return np.hypot((wu - CRATERE[0]) * LARGEUR / HAUTEUR, wv - CRATERE[1])


def carte_regions(graine: int = GRAINE) -> np.ndarray:
    u, v = coordonnees()
    h, w = HAUTEUR, LARGEUR
    n1, n2 = fbm(h, w, 6, 4, graine), fbm(h, w, 6, 4, graine + 1)
    n3 = fbm(h, w, 7, 6, graine + 2)
    fin1, fin2 = fbm(h, w, 5, 12, graine + 3), fbm(h, w, 5, 12, graine + 4)
    cote = fbm(h, w, 6, 16, graine + 5)   # découpe fine du littoral (caps, anses)

    # déformation des coordonnées, atténuée près des lieux nommés
    calme = 1.0 - _calme()
    wu = u + ((n1 - 0.5) * 0.16 + (fin1 - 0.5) * 0.03) * calme
    wv = v + ((n2 - 0.5) * 0.16 + (fin2 - 0.5) * 0.03) * calme

    # contour de l'île : ellipse bruitée, repoussée des bords de l'image
    ell = ((u - 0.5) / 0.47) ** 2 + ((v - 0.47) / 0.45) ** 2
    bord = np.minimum.reduce([u * w / h, (1 - u) * w / h, v, 1 - v])
    penalite = np.clip(MARGE_BORD - bord, 0, None) * 3
    # près des lieux nommés, le bruit de la côte s'atténue aussi : ils restent sur la terre ferme
    bruit_cote = ((n3 - 0.5) * 0.45 + (cote - 0.5) * 0.5) * calme
    terre = (ell + bruit_cote + penalite < 1.0) & (wv + (cote - 0.5) * 0.04 * calme < 0.90)

    # les poches d'eau isolées dans les terres ne sont pas de la mer : on les rend à la terre
    poches, _ = label(~terre)
    terre |= poches != poches[0, 0]

    reg = np.full((h, w), Region.PLAINES, np.uint8)
    reg[wv >= 0.62] = Region.MARCHES
    reg[wv >= 0.76] = Region.COTE
    reg[(wu < 0.30) & (wv < 0.62)] = Region.STEPPES
    reg[(wu > 0.70) & (wv < 0.62)] = Region.FORET
    reg[wv < 0.22] = Region.MONTAGNES
    dcr = distance_cratere(wu, wv)
    reg[dcr < RAYON_RUINES] = Region.RUINES
    reg[dcr < RAYON_CRATERE] = Region.CRATERE
    reg[~terre] = Region.MER
    return reg


def _calme() -> np.ndarray:
    return masque_lieux(RAYON_CALME)
