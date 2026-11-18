# Atelier de carte d'Aldermyr — plan de réalisation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**But :** produire, hors du jeu, les cartes de données d'Aldermyr (altitude, régions, eau, routes, lieux) qui serviront de référence au plugin de génération Hytale.

**Architecture :** un paquet Python `atelier/` découpé par responsabilité. `monde.py` décrit le monde (données pures), puis chaque module calcule une couche à partir des précédentes : régions → altitude → eau → routes et lieux. `export.py` assemble le tout, écrit les fichiers et un aperçu. Tout est déterministe à partir d'une graine.

**Technologies :** Python 3, numpy, scipy (`ndimage`), Pillow, pytest. Aucune installation : ces paquets sont déjà présents.

**Spec :** `docs/specs/2026-11-18-generateur-terrain-design.md` (sections 5.1 et 5.2). Le plugin Java (section 5.2) fera l'objet d'un second plan, après lecture de l'API de `HytaleServer.jar`.

**Hors de ce plan :** le plugin, les palettes de blocs, la végétation, le réglage de l'extrait de test (côté plugin).

## Contraintes globales

- Taille du monde : **4 800 × 3 600 blocs**, soit une carte de données par bloc : images de 4 800 px de large sur 3 600 px de haut.
- Repères : le pixel `(x, z)` (colonne, ligne, origine en haut à gauche) correspond au bloc `(x − 2400, z − 1800)`. Le centre de l'image est le bloc (0, 0). Le nord est en haut.
- Les coordonnées de description dans `monde.py` sont normalisées `(u, v)` ∈ [0, 1], reprises de l'ébauche v0.5 (`carte/gen_carte.py`) : `x = round(u × 4800)`, `z = round(v × 3600)`.
- Altitudes en blocs, **relatives au niveau de la mer** (0 = surface de la mer).
- Encodage `altitude.png` : niveaux de gris 16 bits, valeur = altitude + 1000.
- Encodage `eau.png` : niveaux de gris 16 bits, 0 = pas d'eau, sinon niveau de la surface de l'eau + 1000. La mer a le niveau 0, donc la valeur 1000.
- Encodage `regions.png` : niveaux de gris 8 bits, valeur = indice de région : 0 mer, 1 plaines, 2 montagnes, 3 steppes, 4 forêt, 5 Marches, 6 côte, 7 ruines, 8 Cratère.
- Encodage `routes.png` : niveaux de gris 8 bits, 255 = route, 0 = rien.
- `lieux.json` : liste d'objets `{"nom", "type", "x", "z", "rayon"}`, en coordonnées de bloc (centrées). `type` ∈ `capitale`, `ville`, `village`, `lieu`.
- Réglages d'altitude par région (spec, tableau 5.2) : plaines +6 à +20, montagnes +40 à +180, steppes +10 à +40, forêt +8 à +30, Marches +2 à +15, côte 0 à +8, ruines +10 à +35, Cratère −30 à +5.
- Mer : de −5 près des côtes à −40 au large. Le Cratère reste à sec.
- Rayons d'aplanissement : capitale 60, ville 35, village 15 blocs. Lieux des steppes : Mont Braise 0 (c'est un cône, pas une zone plate), Champs d'Obsidienne 40, Arène des Duels 25, Fosse aux Chaînes 30.
- Routes : 3 blocs de large.
- Graine par défaut : `7`.

## Points d'attention pour la relecture

1. **Frontières en ligne droite** entre régions (anti-objectif de la spec) : la frontière plaines/steppes doit onduler. Test dans la tâche 2.
2. **Un lieu nommé dans l'eau** (village noyé par un lac ou une rivière élargie) : aucun centre de lieu ne doit être sur de l'eau. Test dans la tâche 5.
3. **Une route qui traverse un lac ou la mer** : une route ne peut être sur l'eau que dans une rivière (un gué). Test dans la tâche 5.
4. **Débordement de l'encodage 16 bits** (altitudes extrêmes ou négatives écrêtées) : l'aller-retour export puis lecture doit rendre les mêmes valeurs. Test dans la tâche 6.
5. **Un monde qui ne finit pas en mer** : aucune terre à moins de 30 blocs du bord de l'image. Test dans la tâche 2.

---

### Task 1: description du monde et mise en place du projet

**Fichiers :**
- Créer : `atelier/__init__.py` (vide), `atelier/monde.py`, `tests/test_monde.py`, `pytest.ini` (`testpaths = tests`), `.gitignore` (`donnees/`, `__pycache__/`)
- Initialiser le dépôt git dans `/home/claude/aldermyr` (la spec et ce plan y sont déjà)

**Interfaces :**
- Produit, dans `atelier/monde.py` :
  - `LARGEUR = 4800`, `HAUTEUR = 3600`, `GRAINE = 7`, `DECALAGE = 1000`
  - `class Region(IntEnum)` : `MER=0, PLAINES=1, MONTAGNES=2, STEPPES=3, FORET=4, MARCHES=5, COTE=6, RUINES=7, CRATERE=8`
  - `ALTITUDES: dict[Region, tuple[int, int]]`, d'après les contraintes globales (sans MER)
  - `@dataclass(frozen=True) class Lieu(nom: str, type: str, u: float, v: float, rayon: int)` avec `pixel(self) -> tuple[int, int]` et `bloc(self) -> tuple[int, int]`
  - `@dataclass(frozen=True) class Riviere(nom: str, points: tuple[tuple[float, float], ...], largeur_amont: int, largeur_aval: int)`, largeurs en blocs
  - `@dataclass(frozen=True) class Lac(nom: str, u: float, v: float, ru: float, rv: float)`, rayons normalisés comme dans l'ébauche
  - `LIEUX: tuple[Lieu, ...]`, `RIVIERES: tuple[Riviere, ...]`, `LACS: tuple[Lac, ...]`
  - `ROUTES: tuple[tuple[str | tuple[float, float], ...], ...]` : points de passage (nom de lieu ou coordonnées `(u, v)`)
  - `CRATERE = (0.50, 0.36)`, `RAYON_CRATERE = 0.07`, `RAYON_RUINES = 0.105`, en unités de hauteur d'image comme dans l'ébauche
- Les valeurs `(u, v)` des lieux, rivières, lacs et routes sont **copiées de `carte/gen_carte.py`** (version v0.5). Largeurs des rivières en blocs : grand fleuve 6 → 24, affluent ouest 4 → 12, rivière de Sylvaë 6 → 20, affluent forestier 3 → 9, émissaire du lac des Brumes 3 → 8. Lieux des steppes (`type="lieu"`) : Mont Braise (0.06, 0.37), Champs d'Obsidienne (0.27, 0.20), Arène des Duels (0.075, 0.47), Fosse aux Chaînes (0.25, 0.36).

- [ ] **Étape 1 : écrire les tests qui échouent** dans `tests/test_monde.py`

```python
from atelier.monde import LIEUX, RIVIERES, LACS, ROUTES, LARGEUR, HAUTEUR, Lieu

def compte(t): return sum(1 for l in LIEUX if l.type == t)

def test_inventaire_des_lieux():
    assert (compte("capitale"), compte("ville"), compte("village"), compte("lieu")) == (5, 1, 13, 4)

def test_noms_uniques():
    noms = [l.nom for l in LIEUX]
    assert len(noms) == len(set(noms))

def test_conversion_des_coordonnees():
    l = Lieu("Test", "village", 0.5, 0.5, 15)
    assert l.pixel() == (2400, 1800) and l.bloc() == (0, 0)

def test_tout_est_dans_la_carte():
    for l in LIEUX:
        x, z = l.pixel()
        assert 0 <= x < LARGEUR and 0 <= z < HAUTEUR

def test_routes_referencent_des_lieux_existants():
    noms = {l.nom for l in LIEUX}
    for route in ROUTES:
        for p in route:
            assert not isinstance(p, str) or p in noms

def test_eau_decrite():
    assert len(RIVIERES) == 5 and len(LACS) == 4
```

- [ ] **Étape 2 : lancer les tests et vérifier qu'ils échouent.** `python3 -m pytest tests/test_monde.py -v` → échec à l'import (`ModuleNotFoundError: atelier.monde`).
- [ ] **Étape 3 : écrire `atelier/monde.py`** avec les interfaces ci-dessus.
- [ ] **Étape 4 : relancer les tests.** Ils passent.
- [ ] **Étape 5 : commit** (`git init` au préalable, puis commit de la spec, du plan, et de la tâche) : `feat(atelier): description du monde d'Aldermyr`.

---

### Task 2: bruit et carte des régions

**Fichiers :** créer `atelier/bruit.py`, `atelier/regions.py`, `tests/conftest.py`, `tests/test_regions.py`

**Interfaces :**
- Consomme : `monde.py` (tâche 1)
- Produit :
  - `bruit.fbm(hauteur: int, largeur: int, octaves: int, base: int, graine: int) -> np.ndarray` : `float32`, forme `(hauteur, largeur)`, valeurs dans [0, 1], déterministe pour une même graine. Même principe que `fbm` dans l'ébauche (grilles aléatoires agrandies avec `scipy.ndimage.zoom`, ordre 3), mais avec un `np.random.default_rng(graine)` local.
  - `regions.carte_regions(graine: int = GRAINE) -> np.ndarray` : `uint8`, forme `(3600, 4800)`, valeurs = `Region`
  - `tests/conftest.py` : une fixture `monde` de portée `session` qui appelle `export.generer_monde()` (tâche 6). Jusqu'à la tâche 6, chaque fichier de test appelle directement les fonctions dont il a besoin, via ses propres fixtures `scope="module"`.
- Règles : reprendre la logique de l'ébauche (contour de l'île en ellipse bruitée, régions par seuils sur des coordonnées déformées par le bruit, anneau des ruines et Cratère par distance au centre), à pleine résolution. La déformation doit être assez forte pour respecter le test d'ondulation, et le contour de l'île doit laisser au moins 30 blocs de mer au bord de l'image.

- [ ] **Étape 1 : écrire les tests qui échouent** dans `tests/test_regions.py`

```python
import numpy as np, pytest
from atelier.regions import carte_regions
from atelier.bruit import fbm
from atelier.monde import LIEUX, Region

@pytest.fixture(scope="module")
def reg(): return carte_regions()

def region_de(reg, nom):
    x, z = next(l for l in LIEUX if l.nom == nom).pixel()
    return Region(reg[z, x])

def test_fbm_deterministe_et_borne():
    a, b = fbm(64, 80, 4, 4, 3), fbm(64, 80, 4, 4, 3)
    assert a.dtype == np.float32 and np.array_equal(a, b) and a.min() >= 0 and a.max() <= 1

def test_forme(reg):
    assert reg.shape == (3600, 4800) and reg.dtype == np.uint8

@pytest.mark.parametrize("nom,region", [
    ("Aurelia", Region.PLAINES), ("Durmarak", Region.MONTAGNES), ("Cœur-Sylve", Region.FORET),
    ("Gorrath", Region.STEPPES), ("Valmont", Region.PLAINES), ("Carrefour des Pendus", Region.MARCHES)])
def test_lieux_dans_leur_region(reg, nom, region):
    assert region_de(reg, nom) == region

def test_centre_du_cratere(reg):
    assert reg[int(0.36 * 3600), int(0.50 * 4800)] == Region.CRATERE

def test_le_monde_finit_en_mer(reg):
    b = 30
    assert (reg[:b] == 0).all() and (reg[-b:] == 0).all() and (reg[:, :b] == 0).all() and (reg[:, -b:] == 0).all()

def test_frontiere_plaines_steppes_ondule(reg):
    # pour chaque ligne entre v=0.25 et v=0.55, x de la dernière case steppes avant les plaines
    xs = []
    for z in range(int(0.25 * 3600), int(0.55 * 3600), 10):
        ligne = reg[z, :2400]
        st = np.where(ligne == Region.STEPPES)[0]
        if st.size: xs.append(st.max())
    assert max(xs) - min(xs) >= 30
```

Le test `Port-Écume` est volontairement absent ici : sa région (côte) est vérifiée dans la tâche 5, après l'aplanissement.

- [ ] **Étape 2 : lancer les tests.** `python3 -m pytest tests/test_regions.py -v` → échec à l'import.
- [ ] **Étape 3 : écrire `fbm` dans `atelier/bruit.py` et `carte_regions` dans `atelier/regions.py`.**
- [ ] **Étape 4 : relancer les tests.** Ils passent. Si un lieu tombe dans la mauvaise région, ajuster les seuils dans `regions.py`, jamais les coordonnées des lieux.
- [ ] **Étape 5 : commit** : `feat(atelier): bruit et carte des régions`.

---

### Task 3: carte d'altitude

**Fichiers :** créer `atelier/altitude.py`, `tests/test_altitude.py`

**Interfaces :**
- Consomme : `carte_regions`, `fbm`, `ALTITUDES`, `CRATERE`, `RAYON_CRATERE`, `RAYON_RUINES`
- Produit : `altitude.carte_altitude(regions: np.ndarray, graine: int = GRAINE) -> np.ndarray` : `float32`, forme `(3600, 4800)`, altitude relative à la mer
- Règles :
  - Pour chaque région, altitude = `min + bruit × (max − min)`, avec un bruit propre à la région : crêtes (`1 − |2·fbm − 1|`) pour les montagnes, plateaux pour les steppes (bruit quantifié puis adouci), bruit doux ailleurs.
  - Mélange aux frontières : flou gaussien des altitudes de chaque région pondérées par un masque de région adouci, sur une bande de 20 à 60 blocs (sigma ≈ 15 blocs).
  - Cratère : cuvette dont le fond est à −30 au centre, remontant à +5 au bord, avec un rebord (ruines) jusqu'à +35.
  - Mont Braise : cône jusqu'à +60 au centre, rayon 90 blocs, ajouté par-dessus les steppes.
  - Mer : de −5 au contact de la côte à −40 à 150 blocs et plus du rivage (distance via `scipy.ndimage.distance_transform_edt`).
  - Les terres doivent rester strictement au-dessus de 0. Écrêter à +1 minimum partout où la région n'est pas MER ni CRATERE.

- [ ] **Étape 1 : écrire les tests qui échouent** dans `tests/test_altitude.py`

```python
import numpy as np, pytest
from atelier.regions import carte_regions
from atelier.altitude import carte_altitude
from atelier.monde import Region, ALTITUDES, LIEUX

@pytest.fixture(scope="module")
def cartes():
    reg = carte_regions()
    return reg, carte_altitude(reg)

def test_mer_sous_l_eau_et_profondeur(cartes):
    reg, alt = cartes
    mer = alt[reg == Region.MER]
    assert mer.max() <= -5 + 0.5 and mer.min() >= -40 - 0.5

def test_terres_hors_mer_au_dessus_de_zero(cartes):
    reg, alt = cartes
    terre = (reg != Region.MER) & (reg != Region.CRATERE)
    assert alt[terre].min() > 0

def test_bornes_par_region(cartes):
    # les cœurs de région (loin des frontières) respectent le tableau, à 2 blocs près
    from scipy.ndimage import binary_erosion
    reg, alt = cartes
    for r, (lo, hi) in ALTITUDES.items():
        coeur = binary_erosion(reg == r, iterations=80)
        x, z = next(l for l in LIEUX if l.nom == "Mont Braise").pixel()
        coeur[max(z - 120, 0):z + 121, max(x - 120, 0):x + 121] = False   # le cône dépasse exprès
        if coeur.any():
            v = alt[coeur]
            assert v.min() >= lo - 2 and v.max() <= hi + 2, r.name

def test_fond_du_cratere(cartes):
    reg, alt = cartes
    assert -31 <= alt[int(0.36 * 3600), int(0.50 * 4800)] <= -25

def test_mont_braise_domine_les_steppes(cartes):
    reg, alt = cartes
    x, z = next(l for l in LIEUX if l.nom == "Mont Braise").pixel()
    assert alt[z, x] >= 55

def test_pas_de_falaise_hors_montagnes(cartes):
    reg, alt = cartes
    dz = np.maximum(np.abs(np.diff(alt, axis=0))[:, :-1], np.abs(np.diff(alt, axis=1))[:-1, :])
    doux = np.isin(reg[:-1, :-1], [Region.PLAINES, Region.COTE, Region.MARCHES, Region.FORET])
    assert dz[doux].max() <= 3
```

- [ ] **Étape 2 : lancer les tests.** `python3 -m pytest tests/test_altitude.py -v` → échec à l'import.
- [ ] **Étape 3 : écrire `carte_altitude` dans `atelier/altitude.py`.**
- [ ] **Étape 4 : relancer les tests.** Ils passent.
- [ ] **Étape 5 : commit** : `feat(atelier): carte d'altitude`.

---

### Task 4: rivières et lacs

**Fichiers :** créer `atelier/eau.py`, `tests/test_eau.py`

**Interfaces :**
- Consomme : `carte_regions`, `carte_altitude`, `RIVIERES`, `LACS`
- Produit : `eau.appliquer_eau(alt: np.ndarray, regions: np.ndarray) -> tuple[np.ndarray, np.ndarray]` qui renvoie `(altitude_modifiee, niveau)`. `niveau` est en `float32`, avec `np.nan` là où il n'y a pas d'eau et 0.0 sur la mer. `altitude_modifiee` contient les lits creusés.
- Aussi : `eau.trace_riviere(r: Riviere) -> np.ndarray`, la polyligne lissée (Catmull-Rom, comme dans l'ébauche) en pixels `(N, 2)` dans l'ordre amont → aval. Elle sert aux tests et à `routes.py`.
- Règles :
  - **Lacs** : surface plate. Niveau = altitude minimale du terrain sur le contour du lac, moins 1. Fond = niveau − 4 au centre, en pente douce vers les bords.
  - **Rivières** : le long du tracé, niveau = minimum cumulé de l'altitude du terrain lissée, moins 1, donc **jamais croissant vers l'aval**. Il vaut 0 à l'embouchure (mer) ou le niveau du lac/de la rivière qu'elle rejoint. Largeur interpolée de `largeur_amont` à `largeur_aval`. Lit = niveau − profondeur, avec une profondeur de 2 en amont à 5 en aval, et des berges en pente sur 4 blocs.
  - Si un tracé passe dans un lac, la rivière prend le niveau du lac sur la portion concernée. Le niveau d'un lac traversé est `min(niveau calculé au contour, niveau de la rivière qui y entre)`, pour que la descente reste continue.
  - Ordre de calcul : d'abord les rivières qui finissent en mer (grand fleuve, rivière de Sylvaë), puis les affluents. Pour un affluent : `niveau = max(minimum cumulé, niveau au confluent)`. La suite reste ainsi décroissante et rejoint exactement la rivière principale.
  - Aucune eau dans le Cratère ni dans l'anneau des ruines.

- [ ] **Étape 1 : écrire les tests qui échouent** dans `tests/test_eau.py`

```python
import numpy as np, pytest
from atelier.regions import carte_regions
from atelier.altitude import carte_altitude
from atelier.eau import appliquer_eau, trace_riviere
from atelier.monde import Region, RIVIERES, LACS

@pytest.fixture(scope="module")
def cartes():
    reg = carte_regions()
    alt, niv = appliquer_eau(carte_altitude(reg), reg)
    return reg, alt, niv

def test_mer_au_niveau_zero(cartes):
    reg, alt, niv = cartes
    assert np.all(niv[reg == Region.MER] == 0)

def test_lac_plat(cartes):
    reg, alt, niv = cartes
    for lac in LACS:
        x, z = round(lac.u * 4800), round(lac.v * 3600)
        zone = niv[z - 20:z + 21, x - 20:x + 21]
        assert not np.isnan(zone).any() and np.ptp(zone) == 0, lac.nom

def test_rivieres_descendent_vers_l_aval(cartes):
    reg, alt, niv = cartes
    for r in RIVIERES:
        pts = trace_riviere(r)
        n = np.array([niv[z, x] for x, z in pts if not np.isnan(niv[z, x])])
        assert len(n) > 0.9 * len(pts) and np.all(np.diff(n) <= 1e-3), r.nom

def test_grand_fleuve_atteint_la_mer(cartes):
    reg, alt, niv = cartes
    x, z = trace_riviere(RIVIERES[0])[-1]
    assert niv[z, x] == 0

def test_lit_sous_la_surface(cartes):
    reg, alt, niv = cartes
    eau = ~np.isnan(niv)
    assert np.all(alt[eau] < niv[eau])

def test_cratere_a_sec(cartes):
    reg, alt, niv = cartes
    assert np.isnan(niv[np.isin(reg, [Region.CRATERE, Region.RUINES])]).all()
```

- [ ] **Étape 2 : lancer les tests.** `python3 -m pytest tests/test_eau.py -v` → échec à l'import.
- [ ] **Étape 3 : écrire `trace_riviere` et `appliquer_eau` dans `atelier/eau.py`.**
- [ ] **Étape 4 : relancer les tests.** Ils passent.
- [ ] **Étape 5 : commit** : `feat(atelier): rivières et lacs`.

---

### Task 5: routes et aplanissement des lieux

**Fichiers :** créer `atelier/routes.py`, `atelier/lieux.py`, `tests/test_routes_lieux.py`

**Interfaces :**
- Consomme : `ROUTES`, `LIEUX`, `trace_riviere`, les sorties des tâches 2 à 4
- Produit :
  - `lieux.aplanir_lieux(alt: np.ndarray, niveau: np.ndarray) -> np.ndarray` : pour chaque lieu de rayon > 0, la zone de rayon `rayon` est mise à l'altitude médiane du terrain sur ce disque, puis raccordée au terrain par une pente sur `rayon / 2` blocs. Exception : le Mont Braise (rayon 0) n'est pas touché. Si la médiane tombe sous le niveau de l'eau voisine, on utilise niveau + 2.
  - `lieux.lieux_json() -> list[dict]` : au format des contraintes globales, en coordonnées de bloc
  - `routes.carte_routes(alt: np.ndarray, niveau: np.ndarray) -> np.ndarray` : `bool`, forme `(3600, 4800)`. Les routes suivent les points de passage de `ROUTES`, avec de légères ondulations comme dans l'ébauche, sur 3 blocs de large.
  - Le croisement d'une rivière est autorisé (gué). Si un segment de route traverse un lac ou la mer, le tracé de ce segment est **dévié** pour contourner l'eau : chemin de moindre coût sur une grille réduite (×8), avec un coût infini sur les lacs et la mer.
  - Ordre d'appel imposé par `export.generer_monde` (tâche 6) : régions → altitude → eau → aplanissement → routes.

- [ ] **Étape 1 : écrire les tests qui échouent** dans `tests/test_routes_lieux.py`

```python
import numpy as np, pytest
from scipy.ndimage import label
from atelier.regions import carte_regions
from atelier.altitude import carte_altitude
from atelier.eau import appliquer_eau
from atelier.lieux import aplanir_lieux, lieux_json
from atelier.routes import carte_routes
from atelier.monde import LIEUX, ROUTES, Region

@pytest.fixture(scope="module")
def cartes():
    reg = carte_regions()
    alt, niv = appliquer_eau(carte_altitude(reg), reg)
    alt = aplanir_lieux(alt, niv)
    return reg, alt, niv, carte_routes(alt, niv)

def test_lieux_plats(cartes):
    reg, alt, niv, rou = cartes
    for l in LIEUX:
        if l.rayon == 0: continue
        x, z = l.pixel(); r = l.rayon // 2
        assert np.ptp(alt[z - r:z + r + 1, x - r:x + r + 1]) <= 1, l.nom

def test_aucun_lieu_dans_l_eau(cartes):
    reg, alt, niv, rou = cartes
    for l in LIEUX:
        x, z = l.pixel()
        assert np.isnan(niv[z, x]), l.nom

def test_port_ecume_sur_la_cote(cartes):
    reg, alt, niv, rou = cartes
    x, z = next(l for l in LIEUX if l.nom == "Port-Écume").pixel()
    assert reg[z, x] == Region.COTE

def test_routes_relient_leurs_extremites(cartes):
    reg, alt, niv, rou = cartes
    lab, _ = label(rou, structure=np.ones((3, 3)))
    pos = {l.nom: l.pixel() for l in LIEUX}
    for route in ROUTES:
        (xa, za), (xb, zb) = pos[route[0]], pos[route[-1]]
        assert lab[za, xa] != 0 and lab[za, xa] == lab[zb, xb], route

def test_routes_jamais_sur_lac_ni_mer(cartes):
    reg, alt, niv, rou = cartes
    from atelier.eau import trace_riviere
    from atelier.monde import RIVIERES
    riv = np.zeros_like(rou)
    for r in RIVIERES:
        for x, z in trace_riviere(r):
            riv[max(z - 15, 0):z + 16, max(x - 15, 0):x + 16] = True
    eau_interdite = ~np.isnan(niv) & ~riv
    assert not (rou & eau_interdite).any()

def test_lieux_json():
    d = lieux_json()
    assert len(d) == 23 and set(d[0]) == {"nom", "type", "x", "z", "rayon"}
    a = next(e for e in d if e["nom"] == "Aurelia")
    assert (a["x"], a["z"]) == (0, 0) and a["rayon"] == 60
```

Exception à la règle « ne pas toucher aux coordonnées » : **Pont-Vieux** est dessiné exactement sur l'affluent ouest. S'il échoue à `test_aucun_lieu_dans_l_eau`, on déplace ses coordonnées dans `monde.py` vers la berge la plus proche, à 30 blocs de l'eau, et on le signale au porteur du projet. Faire de même pour tout autre village dans ce cas.

Le test `test_lieux_json` suppose Aurelia en (0.50, 0.50), c'est-à-dire le bloc (0, 0). C'est sa position dans l'ébauche v0.5.

- [ ] **Étape 2 : lancer les tests.** `python3 -m pytest tests/test_routes_lieux.py -v` → échec à l'import.
- [ ] **Étape 3 : écrire `aplanir_lieux`, `lieux_json` et `carte_routes`.**
- [ ] **Étape 4 : relancer les tests.** Ils passent.
- [ ] **Étape 5 : commit** : `feat(atelier): routes et aplanissement des lieux`.

---

### Task 6: assemblage, export, lecture et aperçu

**Fichiers :** créer `atelier/export.py`, `atelier/__main__.py`, `tests/test_export.py`. Compléter `tests/conftest.py` avec la fixture `monde` (session).

**Interfaces :**
- Consomme : tout ce qui précède
- Produit :
  - `@dataclass class Monde(altitude: np.ndarray, regions: np.ndarray, niveau: np.ndarray, routes: np.ndarray, lieux: list[dict])`
  - `export.generer_monde(graine: int = GRAINE) -> Monde`, dans l'ordre imposé par la tâche 5
  - `export.exporter(monde: Monde, dossier: Path) -> None` : écrit `altitude.png`, `regions.png`, `eau.png`, `routes.png`, `lieux.json` (UTF-8, `ensure_ascii=False`) selon les encodages des contraintes globales. L'altitude est arrondie à l'entier. Lève `ValueError` si une valeur sort de la plage encodable (altitude + 1000 hors de [0, 65535]).
  - `export.lire(dossier: Path) -> Monde` : l'inverse exact de `exporter`
  - `export.apercu(monde: Monde, chemin: Path, echelle: int = 3) -> None` : PNG réduit (1 600 × 1 200 par défaut) coloré par région, avec ombrage du relief, eau, routes, et un point par lieu
  - `python3 -m atelier donnees/` : génère, exporte et écrit `donnees/apercu.png`
- Les PNG 16 bits s'écrivent avec Pillow en mode `I;16` à partir d'un tableau `uint16`.

- [ ] **Étape 1 : écrire les tests qui échouent** dans `tests/test_export.py`

```python
import json, numpy as np, pytest
from PIL import Image
from atelier.export import exporter, lire, apercu, Monde

def test_fichiers_et_formats(monde, tmp_path):
    exporter(monde, tmp_path)
    for nom, mode in [("altitude.png", "I;16"), ("eau.png", "I;16"), ("regions.png", "L"), ("routes.png", "L")]:
        im = Image.open(tmp_path / nom)
        assert im.size == (4800, 3600) and im.mode == mode, nom
    assert len(json.loads((tmp_path / "lieux.json").read_text(encoding="utf-8"))) == 23

def test_aller_retour(monde, tmp_path):
    exporter(monde, tmp_path)
    m = lire(tmp_path)
    assert np.array_equal(m.altitude, np.round(monde.altitude))
    assert np.array_equal(m.regions, monde.regions) and np.array_equal(m.routes, monde.routes)
    assert np.array_equal(np.isnan(m.niveau), np.isnan(monde.niveau))

def test_valeurs_extremes_encodees(tmp_path):
    z = np.zeros((3600, 4800), np.float32)
    alt = z.copy(); alt[0, 0], alt[0, 1] = -40, 180
    niv = np.full_like(z, np.nan); niv[0, 0] = 0
    m = Monde(alt, np.zeros_like(z, np.uint8), niv, np.zeros_like(z, bool), [])
    exporter(m, tmp_path); r = lire(tmp_path)
    assert (r.altitude[0, 0], r.altitude[0, 1], r.niveau[0, 0]) == (-40, 180, 0)

def test_debordement_refuse(tmp_path):
    z = np.zeros((3600, 4800), np.float32); z[0, 0] = -2000
    m = Monde(z, np.zeros_like(z, np.uint8), np.full_like(z, np.nan), np.zeros_like(z, bool), [])
    with pytest.raises(ValueError):
        exporter(m, tmp_path)

def test_apercu(monde, tmp_path):
    apercu(monde, tmp_path / "apercu.png")
    assert Image.open(tmp_path / "apercu.png").size == (1600, 1200)
```

- [ ] **Étape 2 : lancer les tests.** `python3 -m pytest tests/test_export.py -v` → échec à l'import.
- [ ] **Étape 3 : écrire `atelier/export.py`, `atelier/__main__.py` et la fixture `monde`.**
- [ ] **Étape 4 : relancer toute la suite.** `python3 -m pytest -v` → tout passe.
- [ ] **Étape 5 : générer pour de vrai.** `python3 -m atelier donnees/`, puis ouvrir `donnees/apercu.png` et le comparer à `carte/aldermyr_carte_v05.png` : mêmes régions, rivières, lacs et routes aux mêmes endroits. Envoyer l'aperçu au porteur du projet.
- [ ] **Étape 6 : commit** : `feat(atelier): export des cartes de données et aperçu`.
