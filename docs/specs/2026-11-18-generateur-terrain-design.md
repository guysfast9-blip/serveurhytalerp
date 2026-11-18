# Générateur de terrain d'Aldermyr — document de conception

*Version 1 — 18 novembre 2026*

## 1. Intention

Aldermyr est un serveur Hytale francophone de RP médiéval-fantasy. Cinq nations jouables existent dès le lancement (Empire d'Aurelion, Forges de Durmarak, Sylvaë, Port-Écume, Clans de Gorrath). Au centre se trouve le Cratère, vestige de la chute de l'ancien empire d'Arcanthis, entouré des Marches brisées, des terres sans loi.

L'univers repose sur une **géographie précise** :
- le Cratère au centre, que toutes les nations convoitent ;
- des frontières disputées (le fer et l'obsidienne entre nains, orcs et Empire, la forêt elfique menacée par l'Empire) ;
- des routes commerciales qui traversent toutes les Marches pour rejoindre Port-Écume, ce qui fait vivre les marchands, les brigands et les escortes.

Un monde généré au hasard ne respecterait pas cette géographie. Le but de ce projet est donc un **générateur qui produit dans Hytale un monde fidèle à la carte dessinée** (ébauche v0.5, `carte/aldermyr_carte_v05.png`).

## 2. Objectifs, non-objectifs, anti-objectifs

**Objectifs (version 1)**
- Un monde d'environ **4 800 × 3 600 blocs** (taille « moyenne » choisie par le porteur du projet), qui est une île : la mer forme une frontière naturelle.
- Le contour des côtes, les régions, le relief, les rivières, les lacs et la mer correspondent à la carte.
- Chaque région a une identité visuelle nette : montagnes enneigées au nord, steppes de cendre volcaniques, grande forêt, plaines cultivables, Marches boueuses, côte sableuse, Cratère sombre et magique.
- Les routes de la carte apparaissent au sol sous forme de chemins.
- Les emplacements des capitales, de la ville et des villages sont préparés (terrain aplani) pour être construits à la main ensuite.
- Le même résultat à chaque génération (déterministe), et la possibilité de **pré-générer** tout le monde avant l'ouverture.
- La possibilité de ne générer qu'un **extrait** de la carte, pour tester sur le petit serveur gratuit.

**Non-objectifs (pas dans la version 1)**
- Construire les bâtiments : les capitales, villages et ruines seront bâtis à la main ou avec des modèles de structures (prefabs) dans un second temps.
- Répartir les ressources propres à chaque nation (fer nain, obsidienne orque, bois rare elfique…). Ça dépend des règles d'économie, qui seront conçues à l'étape suivante. La version 1 se contente de couches souterraines simples.
- Des réseaux de grottes élaborés.
- Le générateur de monde V2 de Hytale (voir les approches).

**Anti-objectifs (ce qui serait un échec, même si ça « marche »)**
- Un monde qui paraît artificiel : frontières de régions en lignes droites, falaises en escalier, relief plat et répétitif.
- Des nations mal placées par rapport à la carte, ou des routes qui ne mènent nulle part.
- Une génération qui fait planter ou ramer le serveur.
- Un générateur qui s'appuie sur des bidouilles internes et casse à chaque mise à jour de Hytale.

## 3. Contraintes

- **Hytale est en accès anticipé** : l'API serveur change souvent. Il faut épingler la version du serveur visée, et isoler le code qui touche l'API dans une petite couche facile à adapter.
- Plugin serveur en **Java 25**, distribué en JAR, chargé depuis le dossier `mods/`.
- Le code source du serveur n'est pas publié. L'API se lit à partir de `HytaleServer.jar` (non obfusqué) et de la documentation communautaire (hytalemodding.dev, références API).
- **Pas de Hytale dans l'environnement de développement.** Les tests en jeu se font avec le porteur du projet, sur son serveur MineStrator (offre gratuite, 4 Go de mémoire) : d'abord des extraits de la carte, puis le monde entier sur un serveur plus puissant.
- D'après la documentation communautaire, l'API expose une interface de générateur (`IWorldGen`, avec une méthode qui génère des tronçons de chunks de façon asynchrone) et un point d'entrée pour enregistrer un générateur personnalisé (`IWorldGenProvider`). Des commandes permettent de créer un monde avec un générateur donné (`/world add <nom> --gen=<générateur>`). **Ces signatures sont à vérifier sur la version réelle du serveur avant d'écrire le code.**

### 3.1 Vérifié dans `HytaleServer.jar` (version 0.7.7, Java 25)

- `IWorldGen.generate(int, long, int, int, LongPredicate, Long2FloatFunction)` renvoie un `CompletableFuture<GeneratedChunk>`. Les chunks font **32 × 32 blocs** de côté.
- `IWorldGenProvider` expose un `CODEC` (`BuilderCodecMapCodec`), et `getGenerator()` fournit le générateur.
- Le plugin intégré `WorldGenPlugin` enregistre son générateur exactement comme nous le ferons : `IWorldGenProvider.CODEC.register(priorité, "Hytale", HytaleWorldGenProvider.class, CODEC)`. Un plugin dispose aussi de `getCodecRegistry(StringCodecMapCodec)`, qui gère l'enregistrement et le nettoyage à sa place.
- `FlatWorldGenProvider` est un exemple complet et simple (couches de blocs, `GeneratedBlockChunk.setBlock`, `setEnvironment`, `setTint`) qui servira de modèle.
- **Hauteur du monde : 320 blocs** (Y de 0 à 319, 10 sections). Choix du concepteur : **niveau de la mer à Y = 100**. Les montagnes culminent alors à Y ≈ 280, le fond de la mer descend à Y ≈ 60 et le fond du Cratère à Y ≈ 70.
- À vérifier pendant le plan du plugin : la façon de poser l'eau (les fluides ne passent pas par `setBlock`), la résolution des identifiants de blocs, et la syntaxe exacte de création d'un monde avec notre générateur.

## 4. Approches envisagées

1. **A — Un plugin qui génère le terrain à partir de la carte dessinée.** *Retenue par le porteur du projet.* C'est la plus fidèle à la carte, mais c'est la plus de travail, et elle demande des allers-retours de test.
2. **B — Des biomes personnalisés avec le générateur V2 de Hytale (HytaleGenerator, graphes de nœuds).** Beaux paysages et outil officiel, mais la répartition reste procédurale : on ne contrôle pas où tombe chaque région. Rejetée comme base, à reconsidérer pour embellir le terrain si V2 permet un jour de lire des masques venant d'images.
3. **C — Le monde de base de Hytale, limité en taille, avec des emplacements choisis à la main.** Rapide, mais pas fidèle à l'univers. Rejetée.

**Comment réaliser A** (choix du concepteur, à confirmer pendant la planification) :
- **A1 — un générateur personnalisé enregistré par le plugin** (`IWorldGenProvider` → `IWorldGen`), qui calcule chaque chunk à partir de cartes de données. *Recommandée* : propre, autonome, et le générateur n'appartient qu'à nous.
- A2 — modifier par réflexion le générateur existant de Hytale (comme le font certains mods). Rejetée : fragile à chaque mise à jour, ce qui est un anti-objectif.
- A3 — un générateur V2 piloté par des masques d'image. À garder en réserve si A1 se révèle impossible sur la version visée.

## 5. Conception

Le projet a deux parties : un **atelier de carte**, hors du jeu, qui produit des cartes de données, et un **plugin de génération** dans Hytale qui les lit.

### 5.1 L'atelier de carte (hors jeu)

Un outil en Python (dans le prolongement du script qui dessine l'ébauche) produit, à partir de la même description du monde, des **cartes de données** qui sont la vraie référence. La jolie carte n'est qu'une illustration.

| Fichier | Contenu |
|---|---|
| `altitude.png` | Altitude en niveaux de gris sur 16 bits, en hauteur de blocs (relativement au niveau de la mer) |
| `regions.png` | Une couleur indexée par région : mer, plaines, montagnes, steppes, forêt, Marches, côte, ruines, Cratère |
| `eau.png` | Masque des rivières et des lacs, avec le niveau d'eau de chaque lac |
| `routes.png` | Masque des routes |
| `lieux.json` | Les lieux nommés (capitales, ville, villages, lieux des steppes), avec leurs coordonnées en blocs, leur type et le rayon de la zone à aplanir |

- **Résolution** : une carte de données par bloc horizontal, soit 4 800 × 3 600 pixels (choix du concepteur : le plugin n'a alors pas d'interpolation à faire). Le plugin ajoute un **bruit de détail** fin pour que le terrain ne soit jamais lisse.
- **Frontières entre régions** : elles sont adoucies sur une bande de 20 à 60 blocs (mélange progressif de l'altitude et des matériaux), pour éviter les lignes droites.
- **Rivières** : elles creusent un lit dont la profondeur et la largeur augmentent vers l'aval, avec des berges en pente douce. Leur niveau descend de façon continue des montagnes jusqu'à la mer.
- **Vérification** : l'atelier produit aussi un aperçu visuel des cartes de données, à comparer avec l'ébauche avant tout test en jeu.

### 5.2 Le plugin de génération (dans Hytale)

- Il enregistre un générateur nommé `aldermyr`. Le monde se crée avec `/world add aldermyr --gen=aldermyr` (syntaxe à vérifier). Le monde par défaut du serveur reste intact.
- Il lit les cartes de données depuis son dossier de configuration au démarrage, et les garde en mémoire sous une forme compacte.
- Pour chaque colonne de blocs :
  1. il lit l'altitude, la région et l'eau ;
  2. il ajoute le bruit de détail propre à la région ;
  3. il remplit les couches avec la palette de la région (surface, sous-couche, pierre) ;
  4. il remplit d'eau la mer, les lacs et les rivières jusqu'à leur niveau ;
  5. il pose la végétation selon la densité de la région ;
  6. il trace les routes et aplanit les zones des lieux nommés.
- **Réglages par région** (valeurs de départ, à ajuster pendant les tests) :

| Région | Altitude au-dessus de la mer | Relief | Surface | Végétation |
|---|---|---|---|---|
| Plaines (Empire) | +6 à +20 | doux, collines légères | herbe, terre | arbres épars, champs |
| Montagnes (Durmarak) | +40 à +180 | fort, crêtes, neige en altitude | pierre, gravier, neige | conifères bas, rien au sommet |
| Steppes (Gorrath) | +10 à +40 | accidenté, plateaux, coulées | cendre, roche sombre, basalte | buissons secs, presque rien |
| Forêt (Sylvaë) | +8 à +30 | vallonné | herbe, mousse | forêt très dense, grands arbres |
| Marches brisées | +2 à +15 | irrégulier, cuvettes | boue, herbe sèche, gravier | arbres morts, roseaux |
| Côte (Port-Écume) | 0 à +8 | plat | sable, herbe rase | palmiers ou arbres côtiers rares |
| Ruines (anneau du Cratère) | +10 à +35 | rebord abrupt | pierre fissurée | rien |
| Cratère | −30 à +5 | cuvette | roche sombre, cristaux violets | rien |

- **Mer** : le fond descend d'environ −5 blocs près des côtes jusqu'à −40 au large.
- **Cratère** : son fond est sous le niveau de la mer mais reste **à sec**. Seule la carte `eau.png` décide où il y a de l'eau, pas l'altitude.
- **Point d'apparition** : réglable dans la configuration. Par défaut, Port-Écume, la cité neutre ouverte à tous les peuples.
- **Lieux des steppes** : le Mont Braise est un volcan (cône et coulées de roche volcanique). Les Champs d'Obsidienne sont une plaque de blocs d'obsidienne en surface. L'Arène des Duels et la Fosse aux Chaînes sont de simples zones aplanies, construites à la main ensuite.
- **Routes** : des chemins de terre battue de 3 blocs de large qui suivent le relief, avec un gué ou un emplacement de pont là où ils croisent une rivière.
- **Zones des lieux nommés** : un aplanissement doux, avec un rayon d'environ 60 blocs pour une capitale, 35 pour la ville et 15 pour un village, raccordé au terrain alentour sans falaise.
- **Commandes pour le staff** : `/aldermyr ici` affiche la région et l'altitude prévues sous les pieds, `/aldermyr aller <lieu>` téléporte vers un lieu nommé.
- **Pré-génération** : avec un plugin existant (Chunker ou Chunk Generator), à un rythme réglable pour ne pas saturer le serveur.
- **Extrait de test** : une option de configuration limite la génération à un rectangle de la carte (par exemple 512 × 512 blocs autour d'Aurelia). En dehors, le générateur ne produit que de la mer.

### 5.3 Ordre de réalisation proposé

1. L'atelier de carte produit les cartes de données et leur aperçu.
2. Un plugin minimal génère l'extrait d'Aurelia avec l'altitude et des blocs simples, testé sur le serveur gratuit. Ça valide la chaîne complète et l'API.
3. Palettes et végétation par région, eau, rivières.
4. Routes, zones des lieux nommés, lieux des steppes, Cratère.
5. Génération du monde entier sur un serveur plus puissant, et pré-génération.

## 6. Ce qui a été décidé avec le porteur du projet

- La solution A (un monde fidèle à la carte dessinée).
- La carte ébauche v0.5 comme référence : régions, rivières, lacs, villages, la ville de Valmont et les lieux des steppes. Les noms sont provisoires.
- La taille moyenne, environ 4 800 × 3 600 blocs.
- Les capitales, villages et ruines sont construits à la main. Le générateur ne fait que préparer le terrain.
- Les premiers tests se font sur le serveur gratuit MineStrator, avec un extrait de la carte.

## 7. Choix faits par le concepteur (à vérifier par le porteur du projet)

- Une carte de données par bloc (4 800 × 3 600) plutôt qu'une image agrandie.
- Les altitudes et les réglages par région du tableau 5.2.
- Les ressources par nation sont laissées à l'étape « règles d'économie ». La version 1 n'a que des couches simples.
- Pas de grottes élaborées dans la version 1.
- Des routes de terre de 3 blocs, avec des gués aux rivières.
- La syntaxe des commandes du staff.
- La pré-génération avec un plugin existant plutôt qu'un outil maison.
- Le point d'apparition par défaut à Port-Écume.
- La profondeur de la mer, et le Cratère à sec malgré son fond sous le niveau de la mer.

## 8. Laissé au constructeur

- Les identifiants exacts des blocs et des éléments de végétation de Hytale à utiliser pour chaque palette.
- La hauteur maximale du monde et le niveau de la mer (à lire dans la version du serveur), puis l'ajustement de l'échelle verticale en conséquence.
- Les algorithmes de bruit, la gestion de la mémoire et les performances.
- Les signatures réelles de l'API de génération sur la version épinglée, et le plan de repli (approche A3) si l'enregistrement d'un générateur personnalisé n'est pas possible.
