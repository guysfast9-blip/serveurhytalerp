# Générateur Aldermyr v0.2 — relief et végétation

## Pourquoi
Premier essai en jeu (v0.1) : le monde suit bien la carte, mais il est lisse (marches d'un bloc
qui dessinent des courbes de niveau), les montagnes sont molles, les zones rocheuses sont
uniformes et il n'y a aucune végétation. Le fondateur a validé l'ordre : relief d'abord,
puis végétation par région, puis les corrections promises (manifeste, dossier `donnees/`).

## Objectifs
1. **Relief de détail** : bruit déterministe ajouté à l'altitude de la carte, amplitude par région
   (`relief`), crêtes pour les montagnes (`crete`, croissant avec l'altitude). Transitions douces
   entre régions (moyenne sur 3×3 échantillons espacés de 12 blocs).
2. **Rien ne casse l'eau ni les lieux** : aucune modification sur une colonne d'eau ou à 2 blocs
   d'une eau ; atténuation jusqu'à 6 blocs. Atténuation forte sur les routes et dans le rayon des
   lieux (terrain constructible). Une terre (altitude ≥ 1) reste ≥ 1.
3. **Surfaces variées** : `variantes` par région (taches de bruit), `falaise` sur les pentes ≥
   `penteFalaise`, roche au-dessus de `rocheAuDessus` en montagne, teinte de l'herbe modulée (±8 %).
4. **Végétation** : prefabs du jeu (`Server/Prefabs/...`), désignés par dossier dans `palette.json`
   (`vegetation.densite`, `vegetation.prefabs[{dossier, poids}]`). Grille de 5 blocs, positions
   et choix déterministes, densité modulée par un bruit lent (bosquets, clairières). Pas d'arbre
   dans l'eau, sur une route, dans un lieu, sur une pente ≥ 3 ni au-dessus de `limiteArbres`.
   Chaque chunk pose aussi la partie qui lui revient des arbres ancrés dans les 8 chunks voisins ;
   un prefab ne remplace jamais un bloc déjà posé.
5. **Corrections** : `ServerVersion` dans le manifeste ; création automatique de `donnees/` ;
   `palette.json` versionnée (`version: 2`) — une ancienne palette est renommée
   `palette-v1.json` et remplacée.

## Non-objectifs
Grottes, rotation des prefabs, monuments uniques (tours de mages placées aux ruines : plus tard),
ambiances/ciel par région.

## Approches considérées
- **Relief calculé dans l'atelier (Python)** : il faudrait régénérer et renvoyer les cartes à
  chaque réglage. Écarté.
- **Relief calculé dans le plugin, colonne par colonne (retenu)** : fonction pure de (x, z), donc
  identique d'un chunk à l'autre ; réglable dans `palette.json` sans toucher aux cartes.
- **Végétation par le système de décor natif de Hytale** : exige le format de génération natif, que
  notre générateur n'utilise pas. Écarté au profit de prefabs posés par le plugin.

## Faits d'API vérifiés (HytaleServer.jar 0.7.7, javap)
- `PrefabStore.get().getAssetPrefabDir(String)` → `Map<Path, BlockSelection>`.
- `BlockSelection.forEachBlock((x, y, z, BlockHolder) -> …)` : coordonnées locales ; position monde
  = cible + local + (getX() − getAnchorX()) (idem y, z), d'après `computePlacementBounds`.
- `BlockHolder` : `blockId()`, `rotation()`, `filler()`.

## Laissé au constructeur
Valeurs exactes des bruits (longueurs d'onde, octaves), choix des graines.
