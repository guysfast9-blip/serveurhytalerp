"""Rivières et lacs : niveaux d'eau cohérents (jamais croissants vers l'aval) et lits creusés."""
from functools import lru_cache

import numpy as np
from scipy.ndimage import binary_dilation, distance_transform_edt, uniform_filter1d
from scipy.spatial import cKDTree

from .bruit import fbm
from .monde import GRAINE, HAUTEUR, LACS, LARGEUR, RIVIERES, Region, Riviere

PENTE_VALLEE = 0.8      # blocs de dénivelé par bloc de distance sur les flancs des vallées
LISSAGE_TRACE = 41      # échantillons : lissage du terrain le long du tracé avant le calcul du niveau


def _catmull_rom(points: np.ndarray, pas: int) -> np.ndarray:
    p = np.vstack([points[:1], points, points[-1:]])
    sortie = []
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        t = np.linspace(0, 1, pas, endpoint=False)[:, None]
        sortie.append(0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t ** 2
                             + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    sortie.append(p[-2][None, :])
    return np.vstack(sortie)


CONTACT = 3            # pixels : un affluent s'arrête dès qu'il touche une rivière décrite avant lui


@lru_cache(maxsize=None)
def trace_riviere(r: Riviere) -> np.ndarray:
    """Tracé lissé de la rivière, en pixels (x, z), de l'amont vers l'aval.

    Un affluent s'arrête au premier contact avec une rivière décrite avant lui dans RIVIERES.
    """
    pix = _trace_brut(r)
    precedentes = [trace_riviere(p) for p in RIVIERES[:RIVIERES.index(r)]] if r in RIVIERES else []
    if precedentes:
        dist, _ = cKDTree(np.vstack(precedentes)).query(pix)
        contact = np.flatnonzero(dist <= CONTACT)
        if contact.size:
            pix = pix[:contact[0] + 1]
    pix.setflags(write=False)
    return pix


def _trace_brut(r: Riviere) -> np.ndarray:
    pts = np.array([(u * LARGEUR, v * HAUTEUR) for u, v in r.points], np.float64)
    longueur = np.hypot(*np.diff(pts, axis=0).T).max()
    courbe = _catmull_rom(pts, int(longueur * 2) + 2)
    pix = np.round(courbe).astype(np.int64)
    pix[:, 0] = np.clip(pix[:, 0], 0, LARGEUR - 1)
    pix[:, 1] = np.clip(pix[:, 1], 0, HAUTEUR - 1)
    garde = np.ones(len(pix), bool)
    garde[1:] = np.any(pix[1:] != pix[:-1], axis=1)
    return pix[garde]


def masques_lacs(regions: np.ndarray | None = None, graine: int = GRAINE) -> list[np.ndarray]:
    """Un masque par lac de LACS (hors Cratère et ruines si les régions sont fournies)."""
    masques = _masques_lacs(graine)
    if regions is not None:
        interdit = np.isin(regions, [Region.CRATERE, Region.RUINES])
        masques = [m & ~interdit for m in masques]
    return masques


def _masques_lacs(graine: int) -> list[np.ndarray]:
    bruit = fbm(HAUTEUR, LARGEUR, 4, 40, graine + 40)   # grain fin : rives découpées
    v, u = np.ogrid[0:HAUTEUR, 0:LARGEUR]
    masques = []
    for lac in LACS:
        m = ((u / LARGEUR - lac.u) / lac.ru) ** 2 + ((v / HAUTEUR - lac.v) / lac.rv) ** 2
        masques.append((m + (bruit - 0.5) * 0.9) < 1.0)
    return masques


def appliquer_eau(alt: np.ndarray, regions: np.ndarray, graine: int = GRAINE) -> tuple[np.ndarray, np.ndarray]:
    alt = alt.copy()
    mer = regions == Region.MER
    interdit = np.isin(regions, [Region.CRATERE, Region.RUINES])
    lacs = masques_lacs(regions, graine)
    niveau_lac = []
    for m in lacs:
        contour = binary_dilation(m, iterations=2) & ~m
        niveau_lac.append(float(alt[contour].min()) - 1.0)

    # rasters des rivières : niveau, rayon et profondeur sur les pixels du tracé
    trace = np.zeros(alt.shape, bool)
    r_niv = np.zeros(alt.shape, np.float32)
    r_ray = np.zeros(alt.shape, np.float32)
    r_prof = np.zeros(alt.shape, np.float32)

    for riv in RIVIERES:   # les rivières principales sont décrites en premier
        pix = trace_riviere(riv)
        xs, zs = pix[:, 0], pix[:, 1]
        terrain = uniform_filter1d(alt[zs, xs].astype(np.float64), LISSAGE_TRACE, mode="nearest")
        dans_lac = np.full(len(pix), -1)
        for k, m in enumerate(lacs):
            dans_lac[m[zs, xs]] = k
        niveau_confluent = None
        if trace.any() and not mer[zs[-1], xs[-1]]:
            deja = np.argwhere(trace)
            dist, j = cKDTree(deja).query((zs[-1], xs[-1]))
            if dist <= CONTACT + 1:
                niveau_confluent = float(r_niv[deja[j][0], deja[j][1]])

        niv = np.empty(len(pix))
        courant = np.inf
        for i in range(len(pix)):
            k = dans_lac[i]
            if k >= 0:
                if courant < niveau_lac[k]:
                    niveau_lac[k] = courant
                courant = niveau_lac[k]
            else:
                courant = min(courant, terrain[i] - 1.0)
            niv[i] = courant
        if mer[zs[-1], xs[-1]]:
            niv = np.maximum(niv, 0.0)
        if niveau_confluent is not None:
            niv = np.maximum(niv, niveau_confluent)
            niv[-1] = niveau_confluent

        f = np.linspace(0, 1, len(pix))
        rayon = (riv.largeur_amont + (riv.largeur_aval - riv.largeur_amont) * f) / 2
        prof = 2 + 3 * f
        libre = ~trace[zs, xs]          # ne pas écraser une rivière déjà tracée (confluents)
        trace[zs[libre], xs[libre]] = True
        r_niv[zs[libre], xs[libre]] = niv[libre]
        r_ray[zs[libre], xs[libre]] = rayon[libre]
        r_prof[zs[libre], xs[libre]] = prof[libre]

    # creusement : lit sous l'eau, puis vallée aux flancs en pente douce
    d, (iz, ix) = distance_transform_edt(~trace, return_indices=True)
    d = d.astype(np.float32)
    lv, ry, pf = r_niv[iz, ix], r_ray[iz, ix], r_prof[iz, ix]
    del iz, ix
    flanc = lv + 0.5 + np.maximum(d - ry, 0) * PENTE_VALLEE
    alt = np.minimum(alt, np.where(interdit, np.inf, flanc)).astype(np.float32)
    lit = (d <= ry) & ~interdit
    fond = lv - np.maximum(1.0, pf * (1 - (d / np.maximum(ry, 1e-3)) ** 2))
    alt[lit] = np.minimum(alt[lit], fond[lit])

    niveau = np.full(alt.shape, np.nan, np.float32)
    niveau[lit] = lv[lit]

    # lacs : surface plate, fond en pente douce jusqu'à niveau − 4 au centre
    for m, L in zip(lacs, niveau_lac):
        profondeur = distance_transform_edt(m)
        t = np.clip(profondeur / max(profondeur.max(), 1), 0, 1)
        niveau[m] = L
        alt[m] = np.minimum(alt[m], L - 1 - 3 * t[m])

    niveau[mer] = 0.0
    niveau[interdit] = np.nan
    return alt.astype(np.float32), niveau
