# Générateur v0.2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Relief de détail, surfaces variées et végétation par prefabs dans le générateur Aldermyr.

**Architecture:** Tout le calcul (bruit, relief, surfaces, positions des prefabs) reste dans le cœur pur
`fr.aldermyr.coeur`, testé en JUnit. L'adaptateur `fr.aldermyr.hytale` charge les prefabs et découpe
leurs blocs au chunk courant.

**Tech Stack:** Java 21 (compilation), JUnit 4.13.2, scripts `plugin/outils/*.sh`.

**Spec:** `docs/specs/2026-11-18-generateur-v02-design.md`

## Global Constraints
- Le cœur n'importe rien de `com.hypixel`.
- Toute valeur réglable vit dans `palette-defaut.json` ; `version` = 2.
- Seuls des noms de blocs déjà présents dans la palette v0.1 sont utilisés.
- Déterminisme : mêmes (x, z) → même résultat, quel que soit le chunk qui calcule.

## Review Focus
1. Colonne de berge à côté d'une rivière : jamais abaissée sous le niveau de l'eau.
2. Arbre ancré dans un chunk voisin : sa partie est posée dans le chunk courant (coordonnées locales hors 0–31 transmises).
3. Ancienne `palette.json` (sans `version`) : renommée et remplacée, pas fusionnée.
4. Dossier de prefab inexistant : un avertissement, aucun plantage.
5. Terre basse de la côte : reste au-dessus de la mer.

---

### Task 1: Bruit
**Files:** Create `coeur/Bruit.java`, Test `BruitTest.java`
**Produces:** `static double valeur(int graine, int x, int z)` ∈ [0,1) ; `static double fbm(int graine, double x, double z, double onde, int octaves)` ∈ [-1,1].
- [ ] Tests : déterminisme, bornes, variation sur 100 points ; RED puis GREEN.

### Task 2: Palette v2
**Files:** Modify `palette-defaut.json`, `coeur/Palette.java`, `PaletteTest.java`
**Produces:** `Palette.Region region(int)` (relief, crete, falaise, rocheAuDessus, variantes, vegetation),
`version()`, `penteFalaise()`, `limiteArbres()` ; `record Variante(String bloc, double part)`,
`record Prefab(String dossier, double poids)`, `record Vegetation(double densite, List<Prefab> prefabs)`.

### Task 3: Relief
**Files:** Create `coeur/Relief.java`, `ReliefTest.java` ; Modify `CartesMonde` (`poserZone` pour les tests).
**Produces:** `new Relief(CartesMonde, Palette)`, `int altitude(int x, int z)`, `int pente(int x, int z)`.
Tests : eau inchangée, berge (≤ 2 blocs d'une eau) inchangée, variation présente et bornée en plaine,
terre ≥ 1, lieu quasi plat (écart ≤ 1).

### Task 4: Plan de colonne et surfaces
**Files:** Modify `PlanColonne.java`, `Remplisseur.java`, `AldermyrWorldGen.java`, tests.
`planifier(CartesMonde, Palette, Relief, int x, int z)` ; falaise si pente ≥ `penteFalaise` ;
roche au-dessus de `rocheAuDessus` ; variantes ; teinte modulée.

### Task 5: Végétation (cœur)
**Files:** Create `coeur/Vegetation.java`, `VegetationTest.java` ; Modify `Cible`, `Remplisseur`.
**Produces:** `record Placement(int x, int y, int z, String dossier, long tirage)` ;
`List<Placement> placements(int cx, int cz)` (ancres des 3×3 chunks) ;
`Cible.prefab(String dossier, long tirage, int lx, int y, int lz)`.
Tests : pas dans l'eau/route/lieu, forêt plus dense que plaine, déterminisme, ancre voisine transmise.

### Task 6: Adaptateur Hytale et corrections
**Files:** Create `hytale/Prefabs.java` ; Modify `CibleHytale`, `AldermyrWorldGenProvider`, `AldermyrPlugin`, `manifest.json`, `INSTALLATION.md`.
Prefabs mis en cache par dossier (triés par chemin), avertissement unique si absent ; blocs posés seulement
dans 0–31 et sur l'air. Palette sans `version` ≥ 2 → `palette-v1.json`. `donnees/` créé au démarrage.

### Task 7: Construction et vérification sur les vraies données
`outils/tester.sh` vert (dont `DonneesReellesTest` : temps par chunk, aperçu du relief), `outils/construire.sh`, livraison zip.
