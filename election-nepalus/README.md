# Élection législative de Nepalus

Projet Java SE – Master 2 Data Sciences – INP-HB Yamoussoukro (Prof. Bi Tra GOORE).

Groupe : **DAH BEBE SAMUEL**, **BROU KOUAKOU CEPHAS**, **DJE KOFFI EUDES**.
Simulation du traitement informatique d'une élection législative, avec une interface JavaFX
et un stockage au choix dans des fichiers CSV ou dans une base de données MySQL.

## Prérequis

- Un JDK 17 ou plus récent (testé avec Temurin 21).
- Facultatif : un serveur MySQL / MariaDB (XAMPP ou WAMP) pour le stockage en base de données.
- Rien d'autre : le wrapper Maven (`mvnw` / `mvnw.cmd`) télécharge Maven, JavaFX et le pilote JDBC
  au premier lancement (connexion Internet nécessaire la première fois).

## Commandes

Depuis le dossier `election-nepalus` (sous Windows : `.\mvnw.cmd`) :

| Action | Commande |
|---|---|
| Lancer l'application depuis les sources | `.\mvnw.cmd javafx:run` |
| Lancer les tests | `.\mvnw.cmd test` |
| Produire le `.jar` à rendre | `.\mvnw.cmd clean package` → `target/election-nepalus.jar` |
| Lancer le `.jar` | `java -jar target\election-nepalus.jar` |

Le `.jar` contient JavaFX pour Windows, Linux et macOS (Apple Silicon) ainsi que le pilote MySQL :
il s'exécute avec un simple Java 17+.

Toujours utiliser `clean package` pour le `.jar` : VS Code compile aussi le projet dans `target/classes`,
et une classe compilée par l'éditeur au milieu d'une modification peut sinon se retrouver dans le `.jar`.

## Utilisation

1. **Fichier > Données de démonstration** charge 12 départements et 5 partis fictifs
   (ou bien enrôlez-les dans les onglets *Départements* et *Partis politiques*).
2. **Simulation > Générer les résultats aléatoirement** tire, pour chaque département, inscrits et votants
   (avec la borne supérieure demandée) puis la répartition des voix. Le calcul s'exécute dans un thread
   d'arrière-plan avec barre de progression. Les résultats d'un département peuvent aussi être saisis à la main.
3. L'onglet **Analyses** répond aux questions de l'énoncé et présente le résultat national
   (sièges, majorité absolue) et les statistiques de participation (moyenne, médiane, écart-type, histogramme).
4. **Fichier > Enregistrer** (Ctrl+S) enregistre dans le stockage actif, indiqué dans la barre d'état.

Les listes de départements et de partis disposent d'un champ de recherche (insensible aux accents).

## Stockage : fichiers CSV ou base MySQL

Menu **Stockage** :

- **Fichiers CSV** : dossier `donnees/` du répertoire de lancement (`partis.csv`, `departements.csv`, `resultats.csv`).
- **Base de données MySQL** : base `projets_java` sur `localhost:3306`, utilisateur `root` sans mot de passe
  (valeurs par défaut de XAMPP), modifiables dans *Stockage > Paramètres de connexion MySQL...*
  avec un bouton *Tester la connexion*.

La base doit exister (la créer dans phpMyAdmin si besoin) ; **les tables sont créées automatiquement** :

| Table | Colonnes |
|---|---|
| `parti` | `sigle` (clé primaire), `nom` |
| `departement` | `numero` (clé primaire), `nom`, `inscrits`, `votants` — contrôle `votants <= inscrits` |
| `resultat` | `numero_departement`, `sigle_parti` (clé primaire composée, clés étrangères), `voix` |

En changeant de stockage, l'application propose de **charger** les données de la nouvelle source
ou d'y **copier** l'élection affichée (migration CSV ⇄ MySQL). L'enregistrement en base se fait dans une
transaction : en cas d'erreur, la base reste inchangée.

Au premier lancement, l'application essaie la base MySQL ; si aucun serveur ne répond, elle utilise
les fichiers CSV sans message d'erreur. Le choix est mémorisé dans `election.properties`
(qui contient aussi le mot de passe MySQL : ne pas le diffuser).

## Règles du scrutin appliquées

- Pas de vote blanc : le total des voix d'un département est égal à son nombre de votants.
- Pas d'égalité de voix entre deux candidats d'un même département.
- Scrutin majoritaire à un tour : le candidat qui a le plus de voix est élu (un siège par département).
- Le nombre de votants ne dépasse pas le nombre d'inscrits.

Conséquence à mentionner dans le rapport : avec 3 partis ou plus, un département « où tous les inscrits ont voté
pour le même parti » est impossible, car les autres partis auraient tous 0 voix, donc seraient à égalité.
Ce cas n'apparaît qu'avec 2 partis.

Autre conséquence : pour que n candidats aient des scores tous différents, il faut au moins
0 + 1 + … + (n−1) = n(n−1)/2 votants ; le générateur refuse une borne d'inscrits inférieure.

## Tests

27 tests JUnit : règles du scrutin, générateur (200 tirages), résultat national, statistiques,
fichiers CSV, configuration, et base MySQL. Les tests MySQL utilisent une base séparée
`projets_java_test` (créée automatiquement) pour ne jamais toucher aux données réelles ;
ils sont ignorés si aucun serveur MySQL local n'est démarré.

## Organisation du code

| Package | Rôle |
|---|---|
| `ci.inphb.election.modele` | Modèle métier : `Identifiable` (interface), `Entite` (classe abstraite), `Departement`, `PartiPolitique`, `Candidat`, `RegleScrutin` (interface) / `ScrutinMajoritaireUnTour`, `Election`, `StatistiquesParticipation` |
| `ci.inphb.election.simulation` | `GenerateurResultats` (tirages aléatoires, classe interne `Tirage`), `DonneesDemo` |
| `ci.inphb.election.persistance` | `StockageElection` (interface) / `StockageAbstrait` / `StockageFichiers`, `StockageBaseDeDonnees` ; `Depot<T extends Identifiable>` (interface générique) avec deux familles : `DepotFichierCsv<T>` et `DepotJdbc<T>` ; `ConfigurationStockage`, `ParametresConnexion` |
| `ci.inphb.election.vue` | Contrôleurs JavaFX (MVC + FXML) : `ControleurPrincipal`, `ControleurOnglet` (classe abstraite) et un contrôleur par onglet, `ContexteApplication` (propriétés observables), `DialogueConnexion` |
| `src/main/resources/.../vue` | Vues FXML et feuille de style CSS |

Les diagrammes de classes sont dans [`docs/`](docs/) (`modele-metier.png`, `architecture.png`,
source PlantUML `diagramme-classes.puml`).

## Notions du cours mises en œuvre

| Notion | Où |
|---|---|
| Interfaces, classes abstraites, héritage, polymorphisme | `Identifiable`, `Entite`, `RegleScrutin`, `StockageElection`, `Depot`, `ControleurOnglet` |
| Classes internes et anonymes | `GenerateurResultats.Tirage`, `Task` anonyme, cellules de `TableView` |
| Expressions lambda, références de méthode, streams | `Election` (questions d'analyse), contrôleurs |
| Généricité bornée et jokers | `Depot<T extends Identifiable>`, `Collection<? extends T>`, méthodes génériques de `ControleurOnglet` |
| Collections | `List`, `Map`, `TreeMap`, `Set`, `FilteredList`, `SortedList` |
| Annotations | `@FXML`, `@Override`, tests JUnit |
| JDBC | `DriverManager`, `PreparedStatement` (requêtes paramétrées, lots), `ResultSet`, transactions, `DatabaseMetaData` |
| Threads | `Task` exécutée par `new Thread(...)` pour la génération et les accès au stockage |
| JavaFX | FXML/MVC avec `fx:include`, `TableView`, `ListView`, graphiques, CSS, propriétés observables, *binding* |
