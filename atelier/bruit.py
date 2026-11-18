"""Bruit fractal déterministe (somme d'octaves de grilles aléatoires agrandies)."""
import numpy as np
from scipy.ndimage import zoom


def fbm(hauteur: int, largeur: int, octaves: int, base: int, graine: int) -> np.ndarray:
    # Les grandes cartes sont calculées à demi-résolution puis agrandies : le détail le plus fin
    # (quelques pixels par cellule) n'en souffre pas, et c'est quatre fois plus rapide.
    if hauteur >= 1000 and hauteur % 2 == 0 and largeur % 2 == 0:
        petit = fbm(hauteur // 2, largeur // 2, octaves, base, graine)
        return np.clip(zoom(petit, 2, order=1), 0, 1).astype(np.float32)
    rng = np.random.default_rng(graine)
    sortie = np.zeros((hauteur, largeur), np.float32)
    amp, total = 1.0, 0.0
    for o in range(octaves):
        n = base * 2 ** o
        m = max(1, round(n * largeur / hauteur))
        grille = rng.random((n + 3, m + 3)).astype(np.float32)
        z = zoom(grille, (hauteur / n, largeur / m), order=3)[:hauteur, :largeur]
        sortie += amp * z
        total += amp
        amp *= 0.5
    sortie /= total
    sortie -= sortie.min()
    sortie /= max(float(sortie.max()), 1e-9)
    return sortie.astype(np.float32)
