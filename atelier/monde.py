"""Description du monde d'Aldermyr : données pures, reprises de l'ébauche v0.5 (carte/gen_carte.py).

Coordonnées normalisées (u, v) dans [0, 1] : u vers l'est, v vers le sud.
"""
from dataclasses import dataclass
from enum import IntEnum

LARGEUR = 4800
HAUTEUR = 3600
GRAINE = 7
DECALAGE = 1000


class Region(IntEnum):
    MER = 0
    PLAINES = 1
    MONTAGNES = 2
    STEPPES = 3
    FORET = 4
    MARCHES = 5
    COTE = 6
    RUINES = 7
    CRATERE = 8


# Altitude (min, max) en blocs au-dessus du niveau de la mer, d'après la spec (tableau 5.2).
ALTITUDES: dict[Region, tuple[int, int]] = {
    Region.PLAINES: (6, 20),
    Region.MONTAGNES: (40, 180),
    Region.STEPPES: (10, 40),
    Region.FORET: (8, 30),
    Region.MARCHES: (2, 15),
    Region.COTE: (0, 8),
    Region.RUINES: (10, 35),
    Region.CRATERE: (-30, 5),
}


def _vers_pixel(u: float, v: float) -> tuple[int, int]:
    return round(u * LARGEUR), round(v * HAUTEUR)


@dataclass(frozen=True)
class Lieu:
    nom: str
    type: str  # capitale, ville, village, lieu
    u: float
    v: float
    rayon: int

    def pixel(self) -> tuple[int, int]:
        return _vers_pixel(self.u, self.v)

    def bloc(self) -> tuple[int, int]:
        x, z = self.pixel()
        return x - LARGEUR // 2, z - HAUTEUR // 2


@dataclass(frozen=True)
class Riviere:
    nom: str
    points: tuple[tuple[float, float], ...]  # de l'amont vers l'aval
    largeur_amont: int  # en blocs
    largeur_aval: int


@dataclass(frozen=True)
class Lac:
    nom: str
    u: float
    v: float
    ru: float
    rv: float


def _capitale(nom, u, v):
    return Lieu(nom, "capitale", u, v, 60)


def _village(nom, u, v):
    return Lieu(nom, "village", u, v, 15)


LIEUX: tuple[Lieu, ...] = (
    _capitale("Aurelia", 0.50, 0.50),
    _capitale("Durmarak", 0.42, 0.12),
    _capitale("Cœur-Sylve", 0.86, 0.40),
    _capitale("Gorrath", 0.14, 0.42),
    _capitale("Port-Écume", 0.475, 0.895),
    Lieu("Valmont", "ville", 0.385, 0.335, 35),
    # Empire
    _village("Gué-Rouge", 0.555, 0.52),
    _village("Pont-Vieux", 0.43, 0.475),
    _village("Blé-d'Or", 0.68, 0.25),
    _village("Lisière", 0.685, 0.385),
    # Durmarak
    _village("Forgeval", 0.30, 0.14),
    _village("Mine-Profonde", 0.79, 0.165),
    # Sylvaë
    _village("Feuillombre", 0.75, 0.33),
    _village("Clairbois", 0.93, 0.60),
    # Gorrath
    _village("Croc-Sec", 0.19, 0.23),
    _village("Os-Blanc", 0.20, 0.47),
    # Port-Écume
    _village("Sel-Marin", 0.30, 0.86),
    _village("Anse-Grise", 0.62, 0.875),
    # Marches brisées
    _village("Carrefour des Pendus", 0.335, 0.74),
    # Lieux des steppes
    Lieu("Mont Braise", "lieu", 0.06, 0.37, 0),
    Lieu("Champs d'Obsidienne", "lieu", 0.27, 0.20, 40),
    Lieu("Arène des Duels", "lieu", 0.075, 0.47, 25),
    Lieu("Fosse aux Chaînes", "lieu", 0.25, 0.36, 30),
)

RIVIERES: tuple[Riviere, ...] = (
    Riviere("Grand fleuve",
            ((0.58, 0.13), (0.60, 0.24), (0.62, 0.36), (0.58, 0.47), (0.53, 0.52), (0.52, 0.62),
             (0.49, 0.74), (0.50, 0.84), (0.49, 0.92), (0.50, 0.99)), 6, 24),
    Riviere("Rivière de Sylvaë",
            ((0.80, 0.14), (0.78, 0.25), (0.82, 0.34), (0.79, 0.45), (0.83, 0.55), (0.80, 0.66),
             (0.84, 0.76), (0.86, 0.86), (0.88, 0.97)), 6, 20),
    Riviere("Affluent ouest",
            ((0.33, 0.24), (0.36, 0.34), (0.40, 0.45), (0.47, 0.52), (0.53, 0.53)), 4, 12),
    Riviere("Affluent forestier",
            ((0.94, 0.30), (0.90, 0.38), (0.88, 0.47), (0.83, 0.54)), 3, 9),
    Riviere("Émissaire du lac des Brumes",
            ((0.655, 0.785), (0.60, 0.84), (0.53, 0.86), (0.50, 0.87)), 3, 8),
)

LACS: tuple[Lac, ...] = (
    Lac("Lac de Givre", 0.585, 0.165, 0.030, 0.028),
    Lac("Miroir d'Onde", 0.815, 0.545, 0.040, 0.035),
    Lac("Lac de Cendre", 0.105, 0.585, 0.035, 0.030),
    Lac("Lac des Brumes", 0.655, 0.785, 0.045, 0.030),
)

ROUTES: tuple[tuple[str | tuple[float, float], ...], ...] = (
    ("Aurelia", (0.48, 0.70), "Port-Écume"),
    ("Aurelia", (0.38, 0.40), (0.40, 0.22), "Durmarak"),
    ("Aurelia", (0.70, 0.46), "Cœur-Sylve"),
    ("Aurelia", (0.32, 0.47), "Gorrath"),
    ("Gorrath", (0.28, 0.68), "Port-Écume"),
    ("Cœur-Sylve", (0.74, 0.68), "Port-Écume"),
)

# Cratère : centre normalisé, rayons en unités de hauteur d'image (comme dans l'ébauche).
CRATERE = (0.50, 0.36)
RAYON_CRATERE = 0.07
RAYON_RUINES = 0.105
