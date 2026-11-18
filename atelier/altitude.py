"""Carte d'altitude : chaque région a son relief, mélangé en douceur aux frontières."""
import numpy as np
from scipy.ndimage import distance_transform_edt, gaussian_filter

from .bruit import fbm
from .monde import ALTITUDES, GRAINE, HAUTEUR, LARGEUR, LIEUX, Region

SIGMA_MELANGE = 15          # blocs : largeur de la transition entre deux régions
PIEMONT = 300               # blocs : distance sur laquelle les montagnes s'élèvent depuis leur bord
LARGEUR_RUINES = 126        # blocs : largeur de l'anneau des ruines (0,035 × 3600)
CONE_HAUTEUR, CONE_RAYON = 60, 90
FOND_CRATERE = 60           # blocs : distance au bord du Cratère où l'on atteint le fond
RAMPE_COTE = 120            # blocs : les terres descendent en pente douce vers la mer sur cette distance


def _relief(region: Region, graine: int, distances: dict) -> np.ndarray:
    """Valeur 0..1 par pixel décrivant le relief propre à une région."""
    h, w = HAUTEUR, LARGEUR
    if region == Region.MONTAGNES:
        crete = 1.0 - np.abs(fbm(h, w, 6, 6, graine + 10) * 2 - 1)
        massif = fbm(h, w, 4, 3, graine + 11)
        b = np.clip(0.6 * crete + 0.4 * massif, 0, 1)
        return b * np.clip(distances["montagnes"] / PIEMONT, 0, 1)
    if region == Region.STEPPES:
        f = fbm(h, w, 5, 8, graine + 12)
        plateaux = gaussian_filter(np.round(f * 4) / 4, 6)
        return np.clip(0.7 * plateaux + 0.3 * f, 0, 1)
    if region == Region.CRATERE:
        # 1 sur le rebord (altitude max), 0 au fond, atteint à 60 blocs du bord
        return 1.0 - np.clip(distances["cratere"] / FOND_CRATERE, 0, 1) ** 0.7
    if region == Region.RUINES:
        return 1.0 - np.clip(distances["hors_cratere"] / LARGEUR_RUINES, 0, 1)
    return fbm(h, w, 5, 5, graine + 20 + int(region))


def carte_altitude(regions: np.ndarray, graine: int = GRAINE) -> np.ndarray:
    mer = regions == Region.MER
    cratere = regions == Region.CRATERE
    distances = {
        "montagnes": distance_transform_edt(regions == Region.MONTAGNES).astype(np.float32),
        "cratere": distance_transform_edt(cratere).astype(np.float32),
        "hors_cratere": distance_transform_edt(~cratere).astype(np.float32),
    }

    num = np.zeros(regions.shape, np.float32)
    den = np.zeros(regions.shape, np.float32)
    for region, (lo, hi) in ALTITUDES.items():
        masque = regions == region
        if not masque.any():
            continue
        poids = gaussian_filter(masque.astype(np.float32), SIGMA_MELANGE)
        cible = lo + _relief(region, graine, distances) * (hi - lo)
        num += poids * cible
        den += poids
    alt = num / np.maximum(den, 1e-6)

    # les terres descendent en pente douce vers le rivage (pas de falaise au bord de la mer)
    vers_mer = distance_transform_edt(~mer).astype(np.float32)
    rampe = np.clip(vers_mer / RAMPE_COTE, 0, 1)
    # (les montagnes, elles, peuvent tomber à pic dans la mer ; la transition reste progressive)
    poids_montagne = gaussian_filter((regions == Region.MONTAGNES).astype(np.float32), SIGMA_MELANGE)
    rampe = rampe + (1 - rampe) * poids_montagne
    alt = np.where(mer | cratere, alt, 1.0 + (alt - 1.0) * rampe * (2 - rampe))

    # Mont Braise : cône volcanique posé sur les steppes
    braise = next(l for l in LIEUX if l.nom == "Mont Braise")
    x, z = braise.pixel()
    vv, uu = np.ogrid[0:HAUTEUR, 0:LARGEUR]
    d = np.sqrt((uu - x) ** 2 + (vv - z) ** 2).astype(np.float32)
    alt += CONE_HAUTEUR * np.clip(1 - d / CONE_RAYON, 0, 1) ** 1.3


    terre_ferme = ~mer & ~cratere
    alt[terre_ferme] = np.maximum(alt[terre_ferme], 1.0)

    # mer : de −5 au contact du rivage à −40 au large
    au_large = distance_transform_edt(mer).astype(np.float32)
    alt[mer] = -5.0 - 35.0 * np.clip((au_large[mer] - 1) / 150, 0, 1)
    return alt.astype(np.float32)
