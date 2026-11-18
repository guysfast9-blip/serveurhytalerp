# Aldermyr — Règles du jeu (pouvoir, économie, guerre, rôles)

Document de conception validé au fil de la discussion du 18/11/2026 avec le fondateur.
Les points marqués **[choix de Claude]** sont des décisions prises par défaut, à confirmer à la relecture.

## 1. Intention

Aldermyr est un serveur Hytale de roleplay médiéval-fantastique francophone où **ce sont les joueurs
qui font l'histoire et l'économie**. Cinq nations existent à l'ouverture ; n'importe quel joueur peut
devenir roi, général, marchand, brigand, mercenaire ou chasseur de monstres.

Les règles doivent :
- donner à un dirigeant **la sensation d'être un vrai dirigeant** (pouvoirs réels, appliqués par le jeu) ;
- donner à chaque nation **sa propre façon de prendre le pouvoir**, qui fait partie de son identité ;
- garder le **fair-play** au centre : l'illégal est permis, jamais au prix de la bonne ambiance ;
- empêcher les joueurs de **s'enrichir trop vite** (l'argent doit circuler et disparaître) ;
- rester **accessibles aux nouveaux** (gains de départ simples, protection des débutants).

**Anti-objectifs** (échec même si « ça marche ») : un serveur où tout se gagne au nombre de joueurs en
combat ; un trône bloqué par un joueur absent ; une économie où l'argent ne vaut plus rien au bout d'un mois ;
des joueurs qui perdent des semaines de jeu sur une seule erreur.

## 2. Mise en place

**Tout est programmé avant l'ouverture** (choix du fondateur), sous forme de plugins Hytale, dans la
lignée du générateur de monde existant. Écarté : mods communautaires (dépendance), ouverture par étapes
avec gestion manuelle par l'équipe. L'équipe garde la main sur ce qui demande un jugement humain
(validation des casus belli, épreuve elfique, épreuves de rang, procès), avec des outils dédiés.

## 3. Peuples et nations

- **La race décide de la nation** : humains → Empire d'Aurelion, nains → Forges de Durmarak,
  elfes → Sylvaë, orcs → Clans de Gorrath.
- **Port-Écume** (cité de toutes les races) : **[choix de Claude]** à la création du personnage, un joueur
  choisit sa race, puis soit la nation de sa race, soit Port-Écume. On ne change plus ensuite.
- La race fixe les capacités (magie des mages humains, runes naines, lien élémentaire elfique, aucune
  magie chez les orcs) — voir l'univers.

## 4. Le pouvoir

### 4.1 Prendre le pouvoir, nation par nation

**Empire d'Aurelion — la course aux Insignes**
- À l'ouverture, **le trône est vide** : l'empereur est mort sans héritier.
  **[choix de Claude]** Il est mort juste avant de révéler un secret sur la trahison de la Fracture ;
  chaque maison se prétend héritière.
- **3 ou 4 maisons nobles** sont fondées par des joueurs. **[choix de Claude]** Les fondateurs sont
  choisis par l'équipe sur candidature (histoire de famille, blason, revendication).
- Les **Insignes impériaux** sont brisés en **fragments** (4 à 5) dispersés dans le monde, de difficultés
  différentes : ruines d'Arcanthis, détenu par une autre nation, gardé par un monstre, etc.
  **Les fragments sont révélés au fil du temps** (par ex. un par semaine, lors d'un événement).
- **Seules les maisons nobles de l'Empire** peuvent revendiquer et posséder des fragments. Elles
  **engagent d'autres joueurs** (contre argent ou autre) pour les récupérer.
- **Un mercenaire a le droit de trahir** et de livrer le fragment à une autre maison.
  Un joueur non noble qui détient un fragment a **48 heures** pour le remettre à une maison ; sinon
  le fragment lui échappe et réapparaît ailleurs (événement).
- La maison qui **réunit tous les fragments** choisit : se couronner, céder les Insignes contre une
  alliance, ou fonder son propre royaume (sur le modèle de la Bannière du Dragon de Bannerlord).
- Ensuite, **le titre se transmet dans la dynastie** gagnante. Un empereur peut être renversé plus
  tard si l'on vole les Insignes et que l'on reforme l'ensemble **[choix de Claude]**.

**Forges de Durmarak** — le **Conseil des Clans** (un chef par famille naine) élit un
Roi-sous-la-Montagne **à vie**. À l'ouverture : les joueurs fondent des clans, puis premier vote.

**Sylvaë** — le Gardien ou la Gardienne de Cœur-Sylve est désigné par une **épreuve du lien élémentaire**
(« la forêt choisit ») organisée et jugée par l'équipe. Première épreuve à l'ouverture.

**Port-Écume** — le **Conseil marchand** réunit les compagnies les plus riches et élit un **Doge** pour un
mandat limité (**deux mois**). Première élection après quelques semaines de commerce.

**Clans de Gorrath** — le **Chef de Guerre** se prend **en duel** à l'Arène des Duels. Tout orc reconnu peut
défier le chef, **au plus un défi par mois**. À l'ouverture : grand tournoi à l'Arène.

### 4.2 Pouvoirs d'un dirigeant
- **Déclarer la guerre** (avec un casus belli validé) et **signer la paix** et des traités.
- **Gérer l'économie** : fixer les **impôts**, appliqués automatiquement par le jeu ; disposer de la
  **caisse de la nation**.
- **Créer les postes de sa cour, leur donner le nom qu'il veut** et y nommer des joueurs.
  Le système impose seulement : créer, nommer, révoquer, afficher le titre.

### 4.3 Dirigeant absent
- Après **14 jours** sans connexion (et sans absence prévenue), **un membre de l'équipe devient régent**.
- Le régent gère les **affaires courantes** seulement : ni guerre, ni changement de loi.
- La nation désigne un nouveau dirigeant **par sa propre règle** (héritier, vote, épreuve, duel).

## 5. L'économie

### 5.1 Monnaies
- **Chaque nation a sa monnaie.** Les **taux de change sont libres** entre joueurs (les changeurs de
  Port-Écume en sont le centre naturel).
- **Les pièces sont des objets dans l'inventaire** : on paie de la main à la main, on peut se faire voler.
- **Banques** dans les capitales : on y dépose ses pièces en sécurité.

### 5.2 D'où vient l'argent
- **Capital de départ** versé par l'équipe dans la caisse de chaque nation à l'ouverture.
- **Revenu quotidien** versé par le plugin à la caisse de chaque nation, **selon ses territoires**
  (villages, mines, ports…). Le dirigeant paie ensuite soldes, salaires et contrats.
- **PNJ** : tâches (livrer, récolter, chasser) et achat de ressources, pour les débutants.
  Gains **modestes et plafonnés par jour**, prix d'achat **bas** (un artisan joueur paie toujours mieux),
  paiement **dans la monnaie de la nation du PNJ**.

### 5.3 Où l'argent disparaît
- **Terrains** en ville : achat + **taxe de propriété**.
- **Entretien des armées** (soldats, gardes) payé chaque semaine par la nation.
- **Services** : gravure de runes, services des mages, réparations.
- **Impôts** (vers la caisse de la nation, puis redistribués en salaires).

### 5.4 S'installer
- **Dans toutes les villes**, on **achète son terrain**.
- **Hors des villes, sur les terres d'Aurelion, de Durmarak et de Sylvaë** : il faut la **permission d'un seigneur**.
- **Partout ailleurs** (steppes de Gorrath, terres de Port-Écume, Marches, ruines, terres sauvages) :
  installation **libre**, à **50 blocs minimum** de la construction d'un autre joueur, sauf accord des deux.
- **[choix de Claude]** Les constructions en terres libres ne sont pas protégées contre le vol de coffres
  (mais jamais contre la destruction, voir § 7).

## 6. La guerre

- **Pas de guerre sans casus belli**, déposé auprès de l'équipe avec sa justification et validé par elle.
  Types : **revendication** (on gagne le territoire), **vengeance** (or ou excuses publiques),
  **indépendance** (la région devient libre), **libération** (reprendre une terre perdue).
- **La carte est découpée en territoires**, chacun avec un **cœur** (donjon ou place, marqué d'une bannière).
  Prendre un territoire = **tenir le cœur pendant un siège**.
- **Sièges à créneaux annoncés** : au moins **24 heures** à l'avance, sur une **plage horaire fixe**
  (par ex. 18 h – 23 h).
- **Durée maximale d'une guerre : trois semaines** ; sans but atteint, paix blanche.
- Seule une guerre déclarée, **pendant un siège**, permet de détruire des constructions.

## 7. La mort

- Une mort ordinaire laisse le personnage **blessé** : il réapparaît affaibli pendant un temps et doit se soigner.
- **Mort définitive** seulement : **exécution après un procès**, **duel à mort accepté** par les deux
  joueurs, ou **choix du joueur**.

## 8. Rôles hors du pouvoir

### 8.1 Brigands (fair-play d'abord)
Permis :
- embuscade **annoncée en roleplay** avant d'attaquer (« La bourse ou la vie ! ») ;
- prendre les **pièces et marchandises transportées** (jamais armes ni armure de la victime) ;
- **voler dans les coffres** des constructions en terres libres.

Interdit :
- **détruire ou brûler** des constructions (hors siège de guerre) ;
- attaquer dans les **zones sûres** (capitales, banques, point d'apparition) ;
- **s'acharner** : une attaque par jour au plus sur la même victime ; pas d'attente au point de réapparition ;
- attaquer un **débutant** (protégé pendant ses **5 premières heures** de jeu) ;
- imposer la **mort définitive** (la victime est seulement blessée).

Conséquences en jeu : primes, avis de recherche, gardes, procès (et exécution possible après procès).

### 8.2 Mercenaires
Engagés par contrat (maisons nobles, nations, particuliers). **La trahison est permise.**

### 8.3 Chasseurs de monstres
- **Guildes de chasseurs**, bâtiments **uniquement dans l'Empire**, **ouvertes à toutes les races**.
- **Dirigées par l'équipe, avec des PNJ** (rôle administratif, peu de gameplay pour un joueur) ;
  institution neutre que l'Empereur ne contrôle pas.
- **Rangs F, E, D, C, B, A, S.** Points gagnés par quête ; **épreuves** (organisées par l'équipe) pour passer
  certains paliers (vers C et vers A).
- **Le rang ouvre les quêtes** : du loup près des villages (F) aux créatures de la Fracture près des
  ruines d'Arcanthis (S).
- **Deux sources de quêtes** : tableau des PNJ (gains de départ) et **primes posées par les joueurs et les
  nations**.

## 9. Systèmes à programmer (avant l'ouverture)

| Système | Couvre |
|---|---|
| Personnage | race, nation, statut (blessé, mort définitive), protection débutant |
| Nations et titres | dirigeant, régence (14 jours), postes libres, affichage des titres, règles de succession |
| Monnaies et banques | pièces-objets par nation, dépôts, caisse de nation, impôts automatiques |
| Revenus | versement quotidien selon les territoires, capital de départ |
| Terrains | parcelles en ville (achat, taxe), permissions seigneuriales, distance de 50 blocs, zones sûres |
| Guerre | casus belli (dépôt / validation), territoires et cœurs, sièges planifiés, durée max, destruction en siège |
| Insignes | fragments-objets, révélations planifiées, règle des 48 heures, couronnement |
| PNJ et quêtes | tâches de départ plafonnées, achat de ressources |
| Guildes de chasseurs | rangs, points, épreuves, tableau de quêtes, primes |
| Outils de l'équipe | régence, validations, épreuves, procès, événements |

## 10. Laissé au constructeur
Montants (capital, revenus, plafonds PNJ, taxes, coûts d'entretien), durée et effets de la blessure,
découpage précis des territoires (à partir des lieux et régions de la carte), nombre exact de fragments,
plage horaire des sièges, ordre de développement des systèmes.
