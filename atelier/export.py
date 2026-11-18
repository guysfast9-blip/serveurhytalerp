"""Assemblage du monde, export des cartes de données, relecture et aperçu."""
import json
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import maximum_filter

from .altitude import carte_altitude
from .eau import appliquer_eau
from .lieux import aplanir_lieux, lieux_json
from .monde import DECALAGE, GRAINE, HAUTEUR, LARGEUR
from .regions import carte_regions
from .routes import carte_routes

COULEURS = np.array([
    (44, 88, 120), (176, 196, 112), (150, 140, 125), (140, 108, 84), (58, 104, 56),
    (138, 132, 86), (196, 186, 132), (104, 92, 100), (70, 52, 84)], np.float32)


@dataclass
class Monde:
    altitude: np.ndarray    # float32, blocs au-dessus de la mer
    regions: np.ndarray     # uint8, indices de Region
    niveau: np.ndarray      # float32, surface de l'eau ; NaN = pas d'eau
    routes: np.ndarray      # bool
    lieux: list[dict]


def generer_monde(graine: int = GRAINE) -> Monde:
    regions = carte_regions(graine)
    alt, niveau = appliquer_eau(carte_altitude(regions, graine), regions, graine)
    alt = aplanir_lieux(alt, niveau)
    return Monde(alt, regions, niveau, carte_routes(alt, niveau), lieux_json())


def _encoder16(valeurs: np.ndarray, nom: str) -> np.ndarray:
    v = np.rint(valeurs) + DECALAGE
    if v.min() < 0 or v.max() > 65535:
        raise ValueError(f"{nom} : valeurs hors de la plage encodable ({v.min() - DECALAGE}..{v.max() - DECALAGE})")
    return v.astype(np.uint16)


def exporter(monde: Monde, dossier: Path) -> None:
    dossier = Path(dossier)
    dossier.mkdir(parents=True, exist_ok=True)
    Image.fromarray(_encoder16(monde.altitude, "altitude")).save(dossier / "altitude.png")
    eau = np.zeros(monde.niveau.shape, np.uint16)
    mouille = ~np.isnan(monde.niveau)
    if mouille.any():
        eau[mouille] = _encoder16(monde.niveau[mouille], "eau")
    if (eau[mouille] == 0).any():
        raise ValueError("eau : un niveau d'eau tombe sur la valeur réservée 0")
    Image.fromarray(eau).save(dossier / "eau.png")
    Image.fromarray(monde.regions.astype(np.uint8), mode="L").save(dossier / "regions.png")
    Image.fromarray(np.where(monde.routes, 255, 0).astype(np.uint8), mode="L").save(dossier / "routes.png")
    (dossier / "lieux.json").write_text(json.dumps(monde.lieux, ensure_ascii=False, indent=2), encoding="utf-8")


def lire(dossier: Path) -> Monde:
    dossier = Path(dossier)
    alt = np.asarray(Image.open(dossier / "altitude.png")).astype(np.float32) - DECALAGE
    eau = np.asarray(Image.open(dossier / "eau.png")).astype(np.float32)
    niveau = np.where(eau == 0, np.nan, eau - DECALAGE).astype(np.float32)
    regions = np.asarray(Image.open(dossier / "regions.png")).astype(np.uint8)
    routes = np.asarray(Image.open(dossier / "routes.png")) > 0
    lieux = json.loads((dossier / "lieux.json").read_text(encoding="utf-8"))
    return Monde(alt, regions, niveau, routes, lieux)


def apercu(monde: Monde, chemin: Path, echelle: int = 3) -> None:
    s = (slice(None, None, echelle), slice(None, None, echelle))
    alt = monde.altitude[s]
    gz, gx = np.gradient(alt)
    ombre = np.clip(1 - (gx + gz) * 0.12, 0.45, 1.4)[..., None]
    img = COULEURS[monde.regions[s]] * ombre
    neige = np.clip((alt - 150) / 25, 0, 1)[..., None]
    img = img * (1 - neige) + 235 * neige
    eau = ~np.isnan(monde.niveau[s])
    img[eau] = (52, 100, 160)
    img[maximum_filter(monde.routes, 3)[s]] = (105, 68, 35)
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    for l in monde.lieux:
        x = (l["x"] + LARGEUR // 2) / echelle
        z = (l["z"] + HAUTEUR // 2) / echelle
        r = {"capitale": 7, "ville": 5, "village": 3, "lieu": 4}[l["type"]]
        couleur = {"capitale": (170, 30, 30), "ville": (215, 120, 40), "village": (245, 232, 200), "lieu": (120, 60, 160)}[l["type"]]
        d.ellipse([x - r, z - r, x + r, z + r], fill=couleur, outline=(30, 20, 10))
    im.save(chemin)
