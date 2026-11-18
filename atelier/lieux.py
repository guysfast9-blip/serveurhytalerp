"""Lieux nommés : aplanissement des emplacements et export en coordonnées de bloc."""
import numpy as np

from .monde import HAUTEUR, LARGEUR, LIEUX


def aplanir_lieux(alt: np.ndarray, niveau: np.ndarray) -> np.ndarray:
    """Aplanit le disque de chaque lieu (rayon > 0) et le raccorde au terrain sur rayon / 2 blocs.

    L'eau n'est jamais comblée : seuls les pixels secs sont modifiés.
    """
    alt = alt.copy()
    sec = np.isnan(niveau)
    for l in LIEUX:
        if l.rayon == 0:
            continue
        x, z = l.pixel()
        R = l.rayon
        ext = int(R * 1.5) + 2
        z0, z1, x0, x1 = max(z - ext, 0), min(z + ext + 1, HAUTEUR), max(x - ext, 0), min(x + ext + 1, LARGEUR)
        vv, uu = np.ogrid[z0:z1, x0:x1]
        d = np.sqrt((uu - x) ** 2 + (vv - z) ** 2)
        fen_alt, fen_sec = alt[z0:z1, x0:x1], sec[z0:z1, x0:x1]
        disque = (d <= R) & fen_sec
        cible = float(np.median(fen_alt[disque]))
        eau_proche = niveau[z0:z1, x0:x1][~fen_sec]
        if eau_proche.size:
            cible = max(cible, float(np.nanmax(eau_proche)) + 2)
        t = np.clip((d - R) / (R / 2), 0, 1)
        t = t * t * (3 - 2 * t)                 # raccord en douceur
        nouveau = cible + (fen_alt - cible) * t
        fen_alt[fen_sec] = nouveau[fen_sec]
    return alt


def lieux_json() -> list[dict]:
    sortie = []
    for l in LIEUX:
        x, z = l.bloc()
        sortie.append({"nom": l.nom, "type": l.type, "x": x, "z": z, "rayon": l.rayon})
    return sortie
