# Installer et tester le générateur d'Aldermyr

Version 0.1.0, pour un serveur Hytale **0.7.7**.

## Ce qu'il te faut

- `AldermyrGenerateur.jar` : le plugin.
- Le dossier `donnees/`, qui contient `altitude.png`, `eau.png`, `regions.png`, `routes.png` et `lieux.json`. Il est fourni avec le plugin. Pour le régénérer : `python3 -m atelier donnees/` à la racine du projet.

## Installation sur MineStrator

1. Arrête le serveur.
2. Dans le gestionnaire de fichiers, dépose `AldermyrGenerateur.jar` dans le dossier `mods/`.
3. Démarre le serveur une première fois. Dans la console, cherche la ligne :
   `Générateur Aldermyr enregistré. Données attendues dans : …`
   Elle indique le dossier exact où mettre les données (normalement `mods/fr.aldermyr_AldermyrGenerateur/donnees/`).
4. Arrête le serveur et dépose les 5 fichiers de `donnees/` dans ce dossier.
5. Redémarre le serveur.

## Créer le monde

Dans le jeu, en étant opérateur :

```
/world add aldermyr --gen=Aldermyr
/tp world aldermyr
```

Tu apparais au-dessus de Port-Écume.

Au premier chargement, le plugin crée `palette.json` à côté du dossier `donnees/`. C'est la liste des blocs utilisés pour chaque région, et tu peux la modifier.

## Sur le serveur gratuit (4 Go)

**Ne pré-génère pas tout le monde.** Le terrain ne se génère que là où les joueurs se déplacent, et c'est suffisant pour tester. Commence par Aurelia avec `/aldermyr aller Aurelia`.

Pour un monde de test limité à une zone, ajoute `"Extrait": "-256,-256,512,512"` (x, z, largeur, hauteur en blocs) dans la section `WorldGen` du fichier de configuration du monde, puis recrée le monde. Hors de cette zone, il n'y a que la mer.

Sur un serveur plus puissant, pré-génère le monde avec un plugin comme Chunker ou Chunk Generator, à un rythme modéré.

## Commandes du staff

- `/aldermyr ici` : la région, l'altitude et l'eau prévues sous tes pieds.
- `/aldermyr aller <lieu>` : téléportation au-dessus d'un lieu (par exemple `aller coeur-sylve` ; les accents sont facultatifs).
- `/aldermyr blocs <texte>` : les noms de blocs du jeu qui contiennent ce texte (par exemple `blocs sand`). Sers-t'en pour corriger `palette.json`.

## Si un bloc est inconnu

Certains noms de la palette par défaut sont des suppositions (`Soil_Sand`, `Soil_Snow`, `Rock_Obsidian`). S'ils n'existent pas, la console affiche :
`Bloc inconnu « Soil_Sand » : remplacé par « Soil_Clay »…`

Le monde se génère quand même avec le bloc de remplacement. Pour corriger :
1. Cherche le vrai nom avec `/aldermyr blocs sand`.
2. Remplace-le dans `palette.json`.
3. Redémarre et recrée le monde.

## Liste de vérification en jeu

Note ce que tu vois. Une capture d'écran par point aide beaucoup.

- [ ] Le monde se crée sans erreur dans la console. Note les éventuels avertissements « Bloc inconnu ».
- [ ] Tu apparais au-dessus de Port-Écume, sur la terre ferme, au bord de la mer.
- [ ] La mer entoure l'île, et le fond descend progressivement.
- [ ] Le grand fleuve coule de Port-Écume vers le nord, l'eau reste dans son lit, et il n'y a pas de mur d'eau.
- [ ] Un lac est bien plat, par exemple au Miroir d'Onde : `/aldermyr aller cœur-sylve`, puis descends vers le sud.
- [ ] Aurelia est sur un terrain aplani (`/aldermyr aller aurelia`).
- [ ] Une route en terre battue part d'Aurelia.
- [ ] Le Cratère est sec, avec des parois et un fond sombre.
- [ ] Il y a de la neige (ou le bloc qui la remplace) sur les sommets du nord.
- [ ] `/aldermyr ici` donne une description qui correspond à ce que tu vois.

Envoie-moi tes notes, les avertissements de la console et tes captures : je corrige à partir de ça.
