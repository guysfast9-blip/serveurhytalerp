# Plugin générateur d'Aldermyr — plan de réalisation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**But :** un plugin serveur Hytale qui enregistre le générateur de monde `Aldermyr` et construit, chunk par chunk, le monde décrit par les cartes de données de l'atelier.

**Architecture :** deux couches. Le **cœur** (`fr.aldermyr.coeur`) est du Java pur, sans aucune classe Hytale : il lit les cartes, la palette, et décrit chaque colonne de blocs. Il est entièrement testé avec JUnit. L'**adaptateur** (`fr.aldermyr.hytale`) est mince : il enregistre le générateur, traduit les noms de blocs en identifiants Hytale et recopie les colonnes dans les chunks. Il est vérifié par la compilation contre l'API réelle, puis en jeu par le porteur du projet.

**Technologies :** Java (bytecode 21, le serveur tourne en Java 25), `javac`/`jar` en scripts shell (pas de Gradle : pas de réseau pour ses dépendances), JUnit 4.13.2 + hamcrest-core 1.3 (présents dans `/opt/gradle-8.14.3/lib/`), Gson (fourni par le serveur Hytale).

**Spec :** `docs/specs/2026-11-18-generateur-terrain-design.md`, sections 3.1 et 5.2. Données d'entrée : celles de l'atelier (plan `2026-11-18-atelier-de-carte.md`, contraintes globales).

## Contraintes globales

- API vérifiée sur **HytaleServer.jar 0.7.7** (bytecode Java 25). Le JDK disponible est en version 21. On compile contre une **copie de compilation** du JAR, dont les numéros de version des classes sont ramenés à 65 (`outils/abaisser_version.py`, déjà écrit). Cette copie n'est jamais exécutée ni livrée. `lib/` est ignoré par git : le JAR du serveur est propriétaire et ne doit jamais être versionné.
- Compilation : `javac --release 21`.
- Paquets : `fr.aldermyr.coeur` (pur Java : le seul import tiers autorisé est `com.google.gson`) et `fr.aldermyr.hytale`.
- Monde : chunks de **32 × 32**, hauteur **320** (Y de 0 à 319), **niveau de la mer Y = 100**. Bloc `(x, z)` du monde ↔ pixel `(x + 2400, z + 1800)` des cartes de 4 800 × 3 600.
- Cartes lues (format de l'atelier) : `altitude.png` (16 bits, alt + 1000), `eau.png` (16 bits, 0 = sec, sinon niveau + 1000), `regions.png` (8 bits, indices 0 mer … 8 Cratère), `routes.png` (8 bits, 255 = route), `lieux.json`.
- Hors de la carte ou hors de l'extrait configuré : mer (fond à −40, eau jusqu'à Y = 100).
- API Hytale à utiliser (lue dans le JAR) :
  - `IWorldGenProvider` (`getGenerator()`, `CODEC`) ; enregistrement par `getCodecRegistry(IWorldGenProvider.CODEC).register("Aldermyr", Classe, CODEC)` dans `setup()`.
  - `IWorldGen.generate(int graine, long index, int cx, int cz, LongPredicate, Long2FloatFunction)` → `CompletableFuture<GeneratedChunk>`, plus `getTimings()` (renvoyer `null` est accepté par `FlatWorldGen`, à vérifier) et `getSpawnPoints(int)` → `Transform[]` (`new Transform(x, y, z)` en doubles).
  - `new GeneratedBlockChunk(index, cx, cz)`, `setBlock(lx, y, lz, id, 0, 0)`, `setTint(lx, lz, argb)`, `setEnvironment(lx, y, lz, envId)`. Puis `new GeneratedChunk(blocs, new GeneratedEntityChunk(), GeneratedChunk.makeSections())`.
  - Eau : `sections[y >> 5].ensureAndGetComponent(FluidSection.getComponentType())`, puis `.setFluid(lx, y, lz, fluidId, (byte) Fluid.getAssetMap().getAsset(fluidId).getMaxFluidLevel())`.
  - Identifiants : `BlockType.getAssetMap().getIndex(nom)` et `Fluid.getAssetMap().getIndex(nom)`. Une valeur `Integer.MIN_VALUE` signifie « inconnu ». Environnement par défaut : `Environment.UNKNOWN_ID` (0).
  - Codec : `BuilderCodec.builder(Classe.class, Classe::new).append(new KeyedCodec<>("Cle", codec), setter, getter).add().build()`, comme `FlatWorldGenProvider`.
- Création du monde en jeu : `/world add aldermyr --gen=Aldermyr`.
- Noms de blocs connus dans le JAR : `Rock_Stone`, `Rock_Shale`, `Rock_Basalt`, `Rock_Granite`, `Rock_Marble`, `Rock_Quartzite`, `Rock_Volcanic`, `Soil_Grass`, `Soil_Dirt`, `Soil_Gravel`, `Soil_Clay`, `Soil_Mud`, `Soil_Needles`. Fluide : `Water_Source`. Les autres noms (sable, neige, obsidienne, plantes) sont **inconnus** : un nom inconnu n'arrête pas la génération. Il est remplacé par le repli de sa palette et signalé une seule fois dans le journal.

## Points d'attention pour la relecture

1. **Colonne au bord d'un chunk** : un bloc en `x = −1` appartient au chunk `cx = −1`, en `lx = 31`, pas en `lx = −1`. Les coordonnées négatives se convertissent avec `Math.floorDiv` / `Math.floorMod`. Test dans la tâche 4.
2. **Altitude extrême** (sommet à +180, fond de mer à −40) : Y reste dans [1, 318], sans exception de `setBlock`. Test dans la tâche 2.
3. **Fichier de carte manquant ou de mauvaise taille** : un message clair qui nomme le fichier et la taille attendue, pas une `NullPointerException`. Test dans la tâche 1.
4. **Nom de bloc inconnu dans la palette** : un repli et un seul avertissement, et le monde se génère quand même. Test dans la tâche 2 (côté cœur) et vérification en jeu.
5. **Eau posée dans l'air ou dans un bloc** : l'eau ne va que de la surface + 1 jusqu'au niveau de l'eau, jamais au-dessus. Test dans la tâche 2.

---

### Task 1: outillage et lecture des cartes

**Fichiers :**
- Créer : `plugin/outils/preparer_api.sh` (argument : chemin vers `HytaleServer.jar`, produit `plugin/lib/HytaleServer-api21.jar` via `outils/abaisser_version.py`, à déplacer dans le dépôt depuis `/home/claude/outils/`), `plugin/outils/tester.sh`, `plugin/outils/construire.sh`, `plugin/.gitignore` (`lib/`, `build/`)
- Créer : `plugin/src/main/java/fr/aldermyr/coeur/CartesMonde.java`
- Tests : `plugin/src/test/java/fr/aldermyr/coeur/CartesMondeTest.java`

**Interfaces :**
- `tester.sh` : compile `src/main/java/fr/aldermyr/coeur/**` et `src/test/java/**` dans `build/test/` avec le classpath `lib/HytaleServer-api21.jar:/opt/gradle-8.14.3/lib/junit-4.13.2.jar:/opt/gradle-8.14.3/lib/hamcrest-core-1.3.jar`, puis lance `org.junit.runner.JUnitCore` sur toutes les classes `*Test`. Code de sortie non nul en cas d'échec.
- `construire.sh` : compile tout `src/main/java` en `--release 21`, copie `src/main/resources/*` et produit `build/AldermyrGenerateur.jar` (utilisé à partir de la tâche 4).
- Produit, dans `CartesMonde` :
  - `public static final int LARGEUR = 4800, HAUTEUR = 3600, DECALAGE = 1000;`
  - `public static CartesMonde charger(Path dossier) throws IOException` : lit les quatre PNG avec `javax.imageio.ImageIO` (le 16 bits passe par `getRaster().getSample(x, z, 0)`) et `lieux.json` avec Gson.
  - `public static CartesMonde mer()` : un monde entièrement marin (utile pour les tests et le repli)
  - `public int altitude(int x, int z)` (coordonnées de **bloc**, centrées), `public int region(int x, int z)`, `public Integer niveauEau(int x, int z)` (`null` = sec), `public boolean route(int x, int z)`. Hors carte : altitude −40, région 0, niveau 0, pas de route.
  - `public List<Lieu> lieux()` avec `public record Lieu(String nom, String type, int x, int z, int rayon)`
  - Stockage : tableaux `short[]`, `byte[]`, `short[]` et `BitSet` de taille 4 800 × 3 600.
- Erreurs : un fichier absent provoque `IOException("Fichier de carte introuvable : <chemin>")`. Une image de mauvaise taille provoque `IOException("<nom> : 4800×3600 attendu, <l>×<h> trouvé")`.

- [ ] **Step 1 : déplacer `abaisser_version.py` dans `plugin/outils/`, écrire `preparer_api.sh` et le lancer** sur `/home/claude/hytale-jar/HytaleServer.jar`. Attendu : `plugin/lib/HytaleServer-api21.jar` existe.
- [ ] **Step 2 : écrire les tests qui échouent** dans `CartesMondeTest`. Un utilitaire de test `ecrireCartes(Path, Consumer<…>)` produit des PNG de 4 800 × 3 600 remplis de valeurs choisies (mer partout, sauf un carré de test).
  - `chargeAltitudeEtConvertitLesCoordonnees` : altitude encodée 1050 au pixel (2400, 1800) → `altitude(0, 0) == 50` ; encodée 960 au pixel (0, 0) → `altitude(-2400, -1800) == -40`.
  - `eauNullSurLeSec` : pixel d'eau 0 → `niveauEau == null` ; pixel 1003 → `niveauEau == 3`.
  - `horsCarteCestLaMer` : `altitude(5000, 0) == -40`, `region(5000, 0) == 0`, `niveauEau(5000, 0) == 0`.
  - `lieuxLus` : un `lieux.json` à deux lieux → `lieux().get(1).nom()` correct, accents compris (« Cœur-Sylve »).
  - `fichierManquantMessageClair` : sans `eau.png` → `IOException` dont le message contient `eau.png`.
  - `mauvaiseTailleRefusee` : `regions.png` en 100 × 100 → message contenant `4800×3600`.
- [ ] **Step 3 : lancer `plugin/outils/tester.sh`.** Attendu : échec de compilation (`CartesMonde` absent).
- [ ] **Step 4 : écrire `CartesMonde`.**
- [ ] **Step 5 : relancer `tester.sh`.** Attendu : `OK (6 tests)`.
- [ ] **Step 6 : commit** : `feat(plugin): outillage et lecture des cartes`.

---

### Task 2: palette et plan de colonne

**Fichiers :**
- Créer : `plugin/src/main/java/fr/aldermyr/coeur/Palette.java`, `plugin/src/main/java/fr/aldermyr/coeur/PlanColonne.java`, `plugin/src/main/resources/palette-defaut.json`
- Tests : `plugin/src/test/java/fr/aldermyr/coeur/PaletteTest.java`, `PlanColonneTest.java`

**Interfaces :**
- Consomme : `CartesMonde` (tâche 1)
- `Palette` :
  - `public static Palette defaut()` (lit `palette-defaut.json` dans les ressources), `public static Palette lire(Reader)`
  - `public record Couches(String surface, String sousCouche, String profondeur, String teinte)`. `teinte` est au format `#RRGGBB`.
  - `public Couches couches(int region)`, `public String neige()`, `public int altitudeNeige()` (en blocs au-dessus de la mer), `public String litEau()` (fond des rivières et lacs), `public String fondMer()`, `public String route()`, `public String obsidienne()`, `public String volcan()`, `public String eau()`
  - `public static int argb(String teinte)` : `#RRGGBB` → `0xFFRRGGBB`
- Valeurs de `palette-defaut.json` :

| Région | surface | sous-couche | profondeur | teinte |
|---|---|---|---|---|
| 1 plaines | Soil_Grass | Soil_Dirt | Rock_Stone | #7DB24A |
| 2 montagnes | Rock_Stone | Rock_Stone | Rock_Granite | #6E8A5A |
| 3 steppes | Rock_Volcanic | Rock_Basalt | Rock_Stone | #9A8456 |
| 4 forêt | Soil_Grass | Soil_Dirt | Rock_Stone | #4E8F3A |
| 5 Marches | Soil_Mud | Soil_Dirt | Rock_Stone | #8C8A4E |
| 6 côte | Soil_Sand | Soil_Clay | Rock_Stone | #A8B060 |
| 7 ruines | Rock_Shale | Rock_Stone | Rock_Stone | #77706A |
| 8 Cratère | Rock_Basalt | Rock_Shale | Rock_Stone | #5A4A66 |
| 0 mer | Soil_Sand | Soil_Clay | Rock_Stone | #6A9A5A |

  Autres clés : `neige` = `Soil_Snow`, `altitudeNeige` = 150, `litEau` = `Soil_Gravel`, `fondMer` = `Soil_Sand`, `route` = `Soil_Dirt`, `obsidienne` = `Rock_Obsidian`, `volcan` = `Rock_Volcanic`, `eau` = `Water_Source`. Chaque nom a un repli dans `"replis"` : `Soil_Sand` → `Soil_Clay`, `Soil_Snow` → `Rock_Marble`, `Rock_Obsidian` → `Rock_Basalt`. Les noms vraiment inconnus sont traités par l'adaptateur (tâche 4).
- `PlanColonne` :
  - `public static final int NIVEAU_MER_Y = 100, Y_MIN = 1, Y_MAX = 318;`
  - `public record Plan(int surfaceY, String[] blocs, Integer eauJusquA, int teinte)`. `blocs[y]` est le nom du bloc en Y, ou `null` pour l'air, pour `y ∈ [0, surfaceY]`. `eauJusquA` est le Y le plus haut rempli d'eau (inclus), ou `null`.
  - `public static Plan planifier(CartesMonde c, Palette p, int x, int z)`
  - Règles :
    - `surfaceY = clamp(100 + altitude, Y_MIN, Y_MAX)`. De `0` à `surfaceY − 4` : `profondeur`. Les 3 blocs du dessous : `sousCouche`. Bloc `surfaceY` : la surface, choisie par priorité :
      1. sous l'eau (niveau non nul) : `fondMer` en mer (région 0), sinon `litEau` ;
      2. route : `route` ;
      3. à moins de `rayon` blocs du centre des « Champs d'Obsidienne » : `obsidienne` ;
      4. à moins de 90 blocs du « Mont Braise » : `volcan` ;
      5. région montagnes et altitude ≥ `altitudeNeige` : `neige` ;
      6. sinon la surface de la région.
    - Eau : si `niveauEau` n'est pas nul et `100 + niveau > surfaceY`, alors `eauJusquA = min(100 + niveau, Y_MAX)`. Sinon `null`.
    - `teinte = Palette.argb(couches(region).teinte())`.

- [ ] **Step 1 : écrire les tests qui échouent.**
  - `PaletteTest.defautComplet` : les 9 régions ont 3 noms non vides, et `argb("#7DB24A") == 0xFF7DB24A`.
  - `PaletteTest.lectureJson` : un JSON minimal qui redéfinit la surface des plaines → `couches(1).surface()` vaut la nouvelle valeur, et le reste vient des valeurs par défaut.
  - `PlanColonneTest`. Utiliser un `CartesMonde` construit en mémoire : ajouter à `CartesMonde` un constructeur `CartesMonde.vide()` et des mutateurs package-private `poser(x, z, alt, region, niveauOuNull, route)` pour les tests.
    - `couchesDesPlaines` : alt 10, région 1 → `surfaceY == 110`, `blocs[110] == "Soil_Grass"`, `blocs[107..109] == "Soil_Dirt"`, `blocs[0] == "Rock_Stone"`, `eauJusquA == null`.
    - `riviere` : alt 5, niveau 8, région 1 → `blocs[105] == "Soil_Gravel"`, `eauJusquA == 108`.
    - `merProfonde` : alt −40, région 0, niveau 0 → `surfaceY == 60`, `blocs[60] == "Soil_Sand"`, `eauJusquA == 100`.
    - `eauJamaisSousLaSurface` : alt 10, niveau 3 (incohérent) → `eauJusquA == null`.
    - `sommetBorne` : alt 400 → `surfaceY == 318`. Et alt −200 → `surfaceY == 1`.
    - `neigeEnAltitude` : alt 160, région 2 → `"Soil_Snow"` ; alt 120 → `"Rock_Stone"`.
    - `routePrioritaire` : route sur les plaines → `"Soil_Dirt"`. Route sous l'eau (gué) → `"Soil_Gravel"`.
    - `obsidienneEtVolcan` : `lieux` contenant « Champs d'Obsidienne » (rayon 40) en (0, 0) et « Mont Braise » (rayon 0) en (500, 0). La colonne (10, 0) a la surface `"Rock_Obsidian"`, la colonne (560, 0) a `"Rock_Volcanic"`.
- [ ] **Step 2 : lancer `tester.sh`.** Attendu : échec de compilation.
- [ ] **Step 3 : écrire `palette-defaut.json`, `Palette`, `PlanColonne`, et les ajouts de test à `CartesMonde`.**
- [ ] **Step 4 : relancer `tester.sh`.** Attendu : tous les tests passent (6 + 10).
- [ ] **Step 5 : commit** : `feat(plugin): palette et plan de colonne`.

---

### Task 3: remplissage d'un chunk (cœur, sans Hytale)

**Fichiers :**
- Créer : `plugin/src/main/java/fr/aldermyr/coeur/Cible.java`, `plugin/src/main/java/fr/aldermyr/coeur/Remplisseur.java`
- Tests : `plugin/src/test/java/fr/aldermyr/coeur/RemplisseurTest.java`

**Interfaces :**
- Consomme : `PlanColonne.planifier`, `Palette`, `CartesMonde`
- Produit :
  - `public interface Cible { void bloc(int lx, int y, int lz, String nom); void eau(int lx, int y, int lz); void teinte(int lx, int lz, int argb); }`
  - `public final class Remplisseur { public Remplisseur(CartesMonde c, Palette p, Extrait extrait); public void remplir(int cx, int cz, Cible cible); }`
  - `public record Extrait(int x, int z, int largeur, int hauteur) { public boolean contient(int x, int z); public static Extrait tout(); }` (dans `Remplisseur.java` ou son propre fichier). Hors extrait : la colonne est planifiée comme de la mer à −40.
  - Colonne du monde : `x = cx * 32 + lx`, `z = cz * 32 + lz`, pour `lx, lz ∈ [0, 31]`. Les blocs `null` (air) ne sont pas envoyés.

- [ ] **Step 1 : écrire les tests qui échouent**, avec une `Cible` factice qui enregistre les appels.
  - `chunkOrigine` : chunk (0, 0), carte vide sauf le bloc (0, 0) à alt 10 en plaines → `bloc(0, 110, 0, "Soil_Grass")` est reçu.
  - `chunkNegatif` : le bloc (−1, −1) à alt 10 en plaines → dans `remplir(-1, -1, …)`, l'appel `bloc(31, 110, 31, "Soil_Grass")` est reçu.
  - `eauEmpilee` : bloc à alt 5 avec un niveau 8 → `eau` reçu pour Y = 106, 107 et 108, et pas pour 109.
  - `horsExtraitCestLaMer` : `Extrait(0, 0, 10, 10)` et le bloc (20, 0) en plaines alt 10 → sa colonne reçoit `bloc(…, 60, …, "Soil_Sand")` et de l'eau jusqu'à Y = 100.
  - `teinteParColonne` : 1 024 appels à `teinte` par chunk.
- [ ] **Step 2 : lancer `tester.sh`.** Attendu : échec de compilation.
- [ ] **Step 3 : écrire `Cible`, `Extrait` et `Remplisseur`.**
- [ ] **Step 4 : relancer `tester.sh`.** Attendu : tous les tests passent.
- [ ] **Step 5 : commit** : `feat(plugin): remplissage de chunk indépendant de Hytale`.

---

### Task 4: adaptateur Hytale (plugin, fournisseur, générateur)

**Fichiers :**
- Créer : `plugin/src/main/java/fr/aldermyr/hytale/AldermyrPlugin.java`, `AldermyrWorldGenProvider.java`, `AldermyrWorldGen.java`, `CibleHytale.java`, `Identifiants.java`, `plugin/src/main/resources/manifest.json`
- Tests : `plugin/src/test/java/fr/aldermyr/coeur/CoordonneesTest.java` (le seul calcul pur introduit ici, s'il y en a) et la compilation complète par `construire.sh`

**Interfaces :**
- Consomme : `Remplisseur`, `Cible`, `Palette`, `CartesMonde`, `Extrait`
- `manifest.json` : `{"Group": "fr.aldermyr", "Name": "AldermyrGenerateur", "Version": "0.1.0", "Main": "fr.aldermyr.hytale.AldermyrPlugin", "Description": "Générateur de terrain d'Aldermyr"}`
- `AldermyrPlugin extends JavaPlugin` : constructeur `(JavaPluginInit)`. Dans `setup()` : `getCodecRegistry(IWorldGenProvider.CODEC).register("Aldermyr", AldermyrWorldGenProvider.class, AldermyrWorldGenProvider.CODEC)`. Garde une instance statique accessible par `AldermyrPlugin.get()` (même modèle que `WorldGenPlugin.get()`), pour exposer `getDataDirectory()` et `getLogger()`.
- `AldermyrWorldGenProvider implements IWorldGenProvider` : `CODEC` avec deux clés facultatives, `"Dossier"` (texte, par défaut `<dossier du plugin>/donnees`) et `"Extrait"` (texte `"x,z,largeur,hauteur"`, par défaut tout). `getGenerator()` charge `CartesMonde` (une seule fois par dossier, en cache statique), puis `Palette` (`<dossier du plugin>/palette.json` s'il existe, sinon les valeurs par défaut, en écrivant ce fichier par défaut pour qu'on puisse le modifier). Une `IOException` est convertie en `WorldGenLoadException` avec le même message.
- `Identifiants` : traduit et met en cache nom → identifiant (`BlockType.getAssetMap().getIndex`). En cas d'inconnu : repli de la palette, sinon `Rock_Stone`, avec **un seul** avertissement par nom dans le journal du plugin. Identifiant d'eau : `Fluid.getAssetMap().getIndex(palette.eau())`. Si l'eau est inconnue, c'est une `WorldGenLoadException` (sans eau, le monde est inutilisable).
- `CibleHytale implements Cible` : enveloppe un `GeneratedBlockChunk` et le tableau `Holder<ChunkStore>[] sections`. `bloc` → `setBlock(lx, y, lz, id, 0, 0)` ; `eau` → `FluidSection` de `sections[y >> 5]`, puis `setFluid(lx, y, lz, idEau, niveauMax)` ; `teinte` → `setTint(lx, lz, argb)`.
- `AldermyrWorldGen implements IWorldGen` : `generate(graine, index, cx, cz, …)` crée les sections avec `GeneratedChunk.makeSections()` et le `GeneratedBlockChunk(index, cx, cz)`, appelle `remplisseur.remplir(cx, cz, cible)` et renvoie `CompletableFuture.completedFuture(new GeneratedChunk(blocs, new GeneratedEntityChunk(), sections))`. `getSpawnPoints` → un `Transform` au-dessus de Port-Écume : `(x, 100 + altitude + 2, z)` du lieu « Port-Écume » de `lieux.json`, ou `(0, 120, 0)` à défaut. `getTimings()` : ce que fait `FlatWorldGen` (à lire avec `javap -c` avant d'écrire).

- [ ] **Step 1 : lire avec `javap -c` `FlatWorldGen.getTimings`, le constructeur de `GeneratedEntityChunk`, et la signature exacte de `getCodecRegistry(…).register`.** Les reporter en commentaire en tête de `AldermyrWorldGen`.
- [ ] **Step 2 : écrire les classes de `fr.aldermyr.hytale` et `manifest.json`.**
- [ ] **Step 3 : lancer `plugin/outils/construire.sh`.** Attendu : `build/AldermyrGenerateur.jar` produit, sans erreur de compilation. Vérifier que le JAR contient `manifest.json` à la racine et les deux paquets.
- [ ] **Step 4 : lancer `tester.sh`.** Attendu : toute la suite du cœur passe encore.
- [ ] **Step 5 : commit** : `feat(plugin): enregistrement du générateur Aldermyr dans Hytale`.

---

### Task 5: commandes du staff, installation et protocole de test en jeu

**Fichiers :**
- Créer : `plugin/src/main/java/fr/aldermyr/hytale/CommandeAldermyr.java`, `plugin/INSTALLATION.md`
- Modifier : `AldermyrPlugin.setup()` (enregistrement de la commande)
- Tests : `plugin/src/test/java/fr/aldermyr/coeur/DescriptionTest.java`

**Interfaces :**
- Cœur : `public final class Description { public static String ici(CartesMonde c, int x, int z); public static Optional<Lieu> lieu(CartesMonde c, String nom); public static List<String> filtrer(Collection<String> noms, String filtre); }`
  - `ici` → `"Région : Plaines — altitude : +12 — eau : non"` (ou `"eau : niveau +3"`). Noms de régions : Mer, Plaines, Montagnes, Steppes, Forêt, Marches brisées, Côte, Ruines, Cratère.
  - `lieu` : correspondance insensible à la casse et aux accents (`java.text.Normalizer`, forme NFD, sans les marques).
  - `filtrer` : les noms qui contiennent le filtre (insensible à la casse), triés, 50 au maximum.
- Adaptateur : la commande `/aldermyr` a trois sous-commandes. Avant d'écrire, lire avec `javap` les classes de base (`CommandBase`, `AbstractPlayerCommand`, `AbstractCommandCollection`) et la façon de lire un argument et la position du joueur, sur le modèle d'une commande intégrée simple.
  - `ici` : décrit la colonne sous le joueur.
  - `aller <lieu>` : téléporte le joueur au-dessus du lieu.
  - `blocs <filtre>` : liste les noms de `BlockType.getAssetMap()` qui contiennent le filtre. Elle sert à compléter `palette.json` avec les vrais noms de sable, neige, obsidienne et plantes.
- `INSTALLATION.md` (en français, pour le porteur du projet) :
  1. générer les données (`python3 -m atelier donnees/`) ;
  2. déposer `AldermyrGenerateur.jar` dans `mods/` sur MineStrator ;
  3. déposer les 5 fichiers de données dans `mods/AldermyrGenerateur/donnees/` (le chemin exact est à confirmer au premier démarrage : le plugin l'écrit dans le journal) ;
  4. redémarrer, puis `/world add aldermyr --gen=Aldermyr` ;
  5. pour le serveur gratuit, régler un extrait de 512 × 512 autour d'Aurelia ;
  6. pré-générer le terrain avec un plugin existant (Chunker ou Chunk Generator), à un rythme modéré ;
  7. **liste de vérification en jeu** : la mer autour de l'île, une rivière qui coule vers la mer, un lac plat, Aurelia aplanie, une route visible, le Cratère sec, de la neige sur les sommets, les avertissements de noms de blocs dans le journal.

- [ ] **Step 1 : écrire les tests qui échouent** dans `DescriptionTest` : `iciPlaines` (texte exact ci-dessus pour alt 12, région 1, sec), `iciEau` (niveau 3), `lieuSansAccents` (`"coeur-sylve"` trouve « Cœur-Sylve »), `filtreLimite` (60 noms correspondants → 50 renvoyés, triés).
- [ ] **Step 2 : lancer `tester.sh`.** Attendu : échec de compilation.
- [ ] **Step 3 : écrire `Description`.** Relancer `tester.sh` : ça passe.
- [ ] **Step 4 : lire l'API des commandes (`javap`), écrire `CommandeAldermyr`, l'enregistrer dans `setup()`.** Lancer `construire.sh` : le JAR est produit sans erreur.
- [ ] **Step 5 : écrire `INSTALLATION.md`.**
- [ ] **Step 6 : commit** : `feat(plugin): commandes du staff et guide d'installation`.

---

## Hors de ce plan (étapes suivantes, après le premier test en jeu)

- **Arbres et végétation en volume.** Il faut l'API des prefabs et les vrais noms des modèles d'arbres, qu'on ne peut lire qu'en jeu ou dans `Assets.zip`. La version 1 donne l'identité des régions par les blocs de surface et les teintes d'herbe.
- **Bruit de détail ajouté par le plugin** (spec 5.1). Les cartes de l'atelier sont déjà détaillées bloc par bloc. On jugera en jeu si un bruit supplémentaire est utile.
- **Plantes isolées** (fleurs, herbes hautes). Elles se feront par la palette, dès que `/aldermyr blocs` aura donné les vrais noms.
