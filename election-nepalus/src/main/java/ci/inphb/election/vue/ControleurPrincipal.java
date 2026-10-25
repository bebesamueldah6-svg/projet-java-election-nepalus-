package ci.inphb.election.vue;

import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.ElectionException;
import ci.inphb.election.modele.PartiPolitique;
import ci.inphb.election.persistance.ConfigurationStockage;
import ci.inphb.election.persistance.ConfigurationStockage.Mode;
import ci.inphb.election.persistance.PersistanceException;
import ci.inphb.election.persistance.StockageBaseDeDonnees;
import ci.inphb.election.persistance.StockageElection;
import ci.inphb.election.persistance.StockageFichiers;
import ci.inphb.election.simulation.DonneesDemo;
import ci.inphb.election.simulation.GenerateurResultats;
import ci.inphb.election.simulation.GenerateurResultats.Tirage;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Contrôleur de la fenêtre principale : menus, choix du stockage, barre d'état,
 * exécution des traitements longs dans un thread, et transmission du contexte
 * aux contrôleurs des onglets (inclus avec fx:include).
 */
public class ControleurPrincipal {

    static final int BORNE_INSCRITS_PAR_DEFAUT = 10_000;

    /** Membres du groupe, affichés dans « À propos ». */
    static final String MEMBRES_GROUPE = "DAH BEBE SAMUEL, BROU KOUAKOU CEPHAS, DJE KOFFI EUDES";

    @FXML
    private MenuBar menus;
    @FXML
    private TabPane onglets;
    @FXML
    private RadioMenuItem choixFichiers;
    @FXML
    private RadioMenuItem choixBase;
    @FXML
    private Label etat;
    @FXML
    private ProgressBar progression;
    @FXML
    private Label stockageActif;
    @FXML
    private HBox indicateurs;
    @FXML
    private Pane zoneTitres;
    @FXML
    private VBox titres;

    private final IndicateurAnime indicateurDepartements =
            new IndicateurAnime("Départements", v -> String.valueOf(Math.round(v)));
    private final IndicateurAnime indicateurPartis =
            new IndicateurAnime("Partis", v -> String.valueOf(Math.round(v)));
    private final IndicateurAnime indicateurParticipation =
            new IndicateurAnime("Participation", v -> ControleurOnglet.FORMAT_POURCENTAGE.format(v));
    private final IndicateurAnime indicateurTete =
            new IndicateurAnime("En tête (sièges)", v -> "—");

    // Contrôleurs des vues incluses : nom = fx:id de l'inclusion + "Controller"
    @FXML
    private ControleurDepartements departementsController;
    @FXML
    private ControleurPartis partisController;
    @FXML
    private ControleurResultats resultatsController;
    @FXML
    private ControleurAnalyses analysesController;

    private ContexteApplication contexte;
    private Stage stage;

    public void demarrer(Stage stage) {
        this.stage = stage;
        ConfigurationStockage configuration = ConfigurationStockage.charger(Path.of("election.properties"));
        contexte = new ContexteApplication(new Election(), configuration, configuration.creerStockage());
        // Navigation entre onglets (classe anonyme implémentant l'interface Navigation)
        contexte.setNavigation(new Navigation() {
            @Override
            public void afficherOnglet(int index) {
                onglets.getSelectionModel().select(index);
            }

            @Override
            public void afficherDepartement(Departement departement) {
                onglets.getSelectionModel().select(ONGLET_RESULTATS);
                resultatsController.selectionner(departement);
            }

            @Override
            public void afficherVictoires(PartiPolitique parti) {
                onglets.getSelectionModel().select(ONGLET_ANALYSES);
                analysesController.filtrerParParti(parti);
            }
        });

        departementsController.initialiserContexte(contexte);
        partisController.initialiserContexte(contexte);
        resultatsController.initialiserContexte(contexte);
        analysesController.initialiserContexte(contexte);

        stockageActif.textProperty().bind(Bindings.concat("Stockage : ", contexte.descriptionStockageProperty()));
        indicateurs.getChildren().addAll(indicateurDepartements, indicateurPartis, indicateurParticipation, indicateurTete);
        Animations.defilement(zoneTitres, titres);
        // Les indicateurs du bandeau sont des raccourcis vers le détail correspondant
        Navigation navigation = contexte.getNavigation();
        indicateurDepartements.auClic("Voir la liste des départements",
                () -> navigation.afficherOnglet(Navigation.ONGLET_DEPARTEMENTS));
        indicateurPartis.auClic("Voir la liste des partis",
                () -> navigation.afficherOnglet(Navigation.ONGLET_PARTIS));
        indicateurParticipation.auClic("Voir les statistiques de participation", () -> {
            navigation.afficherOnglet(Navigation.ONGLET_ANALYSES);
            analysesController.afficherSection(ControleurAnalyses.Section.STATISTIQUES);
        });
        indicateurTete.auClic("Voir le résultat national", () -> {
            navigation.afficherOnglet(Navigation.ONGLET_ANALYSES);
            analysesController.afficherSection(ControleurAnalyses.Section.NATIONAL);
        });
        contexte.revisionProperty().addListener((obs, a, n) -> majIndicateurs());
        majIndicateurs();
        // Transition à chaque changement d'onglet
        onglets.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null && nouveau.getContent() != null) {
                Animations.apparition(nouveau.getContent());
            }
        });
        majChoixStockage();
        stage.titleProperty().bind(Bindings.when(contexte.modifieProperty())
                .then("Élection législative de Nepalus *")
                .otherwise("Élection législative de Nepalus"));
        stage.setOnCloseRequest(evenement -> {
            if (!confirmerAbandonModifications()) {
                evenement.consume();
            }
        });

        chargementInitial();
    }

    /**
     * Premier chargement. Si la base MySQL est inaccessible, on bascule sur les fichiers :
     * sans message au tout premier lancement (poste sans MySQL), avec un avertissement si
     * l'utilisateur avait explicitement choisi la base.
     */
    private void chargementInitial() {
        StockageElection stockage = contexte.getStockage();
        executer("Chargement depuis " + stockage.getDescription(), stockage::charger,
                election -> {
                    contexte.remplacerElection(election, false);
                    etat.setText(election.getDepartements().isEmpty() && election.getPartis().isEmpty()
                            ? "Aucune donnée : enrôlez départements et partis, ou Fichier > Données de démonstration"
                            : "Données chargées");
                },
                erreur -> {
                    if (stockage instanceof StockageBaseDeDonnees) {
                        StockageFichiers repli = contexte.getConfiguration().creerStockageFichiers();
                        contexte.setStockage(repli);
                        majChoixStockage();
                        if (contexte.getConfiguration().existait()) {
                            ControleurOnglet.afficherErreur(erreur.getMessage()
                                    + "\n\nL'application utilise les fichiers CSV en attendant. "
                                    + "Vérifiez que MySQL est démarré (XAMPP) puis choisissez Stockage > Base de données MySQL.");
                        }
                        chargementInitial();
                    } else {
                        ControleurOnglet.afficherErreur("Les données enregistrées n'ont pas pu être chargées :\n" + erreur.getMessage());
                        etat.setText("Échec du chargement, élection vide");
                    }
                });
    }

    /** Met à jour les indicateurs du bandeau (les chiffres défilent jusqu'à leur nouvelle valeur). */
    private void majIndicateurs() {
        Election e = contexte.getElection();
        indicateurDepartements.animerVers(e.getDepartements().size());
        indicateurPartis.animerVers(e.getPartis().size());
        indicateurParticipation.animerVers(e.tauxParticipationGeneral());
        // Parti en tête : le plus de sièges, puis le plus de voix
        Map<PartiPolitique, Long> sieges = e.siegesParParti();
        Map<PartiPolitique, Integer> voix = e.voixParParti();
        String tete = sieges.keySet().stream()
                .filter(p -> sieges.get(p) > 0)
                .max(Comparator.comparing((PartiPolitique p) -> sieges.get(p)).thenComparing(voix::get))
                .map(p -> p.getSigle() + " (" + sieges.get(p) + ")")
                .orElse("—");
        indicateurTete.afficherTexte(tete);
    }

    private void majChoixStockage() {
        boolean base = contexte.getStockage() instanceof StockageBaseDeDonnees;
        choixBase.setSelected(base);
        choixFichiers.setSelected(!base);
    }

    // ------------------------------------------------------------------ traitements en arrière-plan

    /**
     * Exécute une tâche dans un thread séparé pour ne pas figer la fenêtre.
     * Pendant ce temps les menus et onglets sont désactivés et la barre de progression est affichée.
     * Les traitements de fin (succès ou échec) s'exécutent dans le thread JavaFX.
     */
    private <T> void executer(String message, Task<T> tache, Consumer<T> succes, Consumer<Throwable> echec) {
        etat.setText(message + "...");
        progression.progressProperty().bind(tache.progressProperty());
        progression.visibleProperty().bind(tache.runningProperty());
        menus.disableProperty().bind(tache.runningProperty());
        onglets.disableProperty().bind(tache.runningProperty());
        tache.setOnSucceeded(e -> succes.accept(tache.getValue()));
        tache.setOnFailed(e -> echec.accept(tache.getException()));
        Thread fil = new Thread(tache, "election-arriere-plan");
        fil.setDaemon(true);
        fil.start();
    }

    private <T> void executer(String message, Callable<T> travail, Consumer<T> succes, Consumer<Throwable> echec) {
        executer(message, new Task<T>() {
            @Override
            protected T call() throws Exception {
                return travail.call();
            }
        }, succes, echec);
    }

    private <T> void executer(String message, Callable<T> travail, Consumer<T> succes) {
        executer(message, travail, succes, erreur -> {
            etat.setText("Échec");
            ControleurOnglet.afficherErreur(erreur.getMessage());
        });
    }

    // ------------------------------------------------------------------ menu Fichier

    @FXML
    private void nouvelleElection() {
        if (confirmerAbandonModifications()) {
            contexte.remplacerElection(new Election(), true);
            etat.setText("Nouvelle élection vide");
        }
    }

    @FXML
    private void chargerDemo() {
        if (confirmerAbandonModifications()) {
            contexte.remplacerElection(DonneesDemo.creer(), true);
            etat.setText("Données de démonstration chargées : utilisez Simulation > Générer les résultats");
        }
    }

    @FXML
    private void enregistrerMenu() {
        Election election = contexte.getElection();
        StockageElection stockage = contexte.getStockage();
        executer("Enregistrement dans " + stockage.getDescription(), () -> {
            stockage.sauvegarder(election);
            return null;
        }, rien -> {
            contexte.modifieProperty().set(false);
            etat.setText("Enregistré dans " + stockage.getDescription());
        });
    }

    /** Enregistrement immédiat (avant de quitter) : bloquant mais rapide. */
    private boolean enregistrerMaintenant() {
        try {
            contexte.getStockage().sauvegarder(contexte.getElection());
            contexte.modifieProperty().set(false);
            return true;
        } catch (PersistanceException e) {
            ControleurOnglet.afficherErreur("Enregistrement impossible :\n" + e.getMessage());
            return false;
        }
    }

    @FXML
    private void recharger() {
        if (!confirmerAbandonModifications()) {
            return;
        }
        StockageElection stockage = contexte.getStockage();
        executer("Chargement depuis " + stockage.getDescription(), stockage::charger, election -> {
            contexte.remplacerElection(election, false);
            etat.setText("Données rechargées depuis " + stockage.getDescription());
        });
    }

    @FXML
    private void quitter() {
        if (confirmerAbandonModifications()) {
            Platform.exit();
        }
    }

    // ------------------------------------------------------------------ menu Stockage

    @FXML
    private void choisirFichiers() {
        changerStockage(Mode.FICHIERS);
    }

    @FXML
    private void choisirBase() {
        changerStockage(Mode.BASE_DE_DONNEES);
    }

    /**
     * Change le stockage actif. L'utilisateur choisit de charger les données de la
     * nouvelle source ou d'y copier l'élection affichée (migration CSV ⇄ MySQL).
     */
    private void changerStockage(Mode mode) {
        ConfigurationStockage config = contexte.getConfiguration();
        StockageElection cible = mode == Mode.FICHIERS ? config.creerStockageFichiers() : config.creerStockageBase();
        if (cible.getDescription().equals(contexte.getStockage().getDescription())) {
            return;
        }
        ButtonType charger = new ButtonType("Charger ses données", ButtonBar.ButtonData.YES);
        ButtonType copier = new ButtonType("Y enregistrer l'élection affichée", ButtonBar.ButtonData.NO);
        Alert choix = new Alert(Alert.AlertType.CONFIRMATION,
                "Nouveau stockage : " + cible.getDescription() + "\n\nQue faire de l'élection affichée ?",
                charger, copier, ButtonType.CANCEL);
        choix.initOwner(stage);
        choix.setHeaderText("Changer de stockage");
        ButtonType reponse = choix.showAndWait().orElse(ButtonType.CANCEL);
        if (reponse == ButtonType.CANCEL || (reponse == charger && !confirmerAbandonModifications())) {
            majChoixStockage();
            return;
        }

        Election election = contexte.getElection();
        Callable<Election> travail = reponse == charger
                ? cible::charger
                : () -> {
                    cible.sauvegarder(election);
                    return election;
                };
        executer("Accès à " + cible.getDescription(), travail, resultat -> {
            contexte.setStockage(cible);
            config.setMode(mode);
            enregistrerConfiguration();
            if (reponse == charger) {
                contexte.remplacerElection(resultat, false);
                etat.setText("Données chargées depuis " + cible.getDescription());
            } else {
                contexte.modifieProperty().set(false);
                etat.setText("Élection copiée dans " + cible.getDescription());
            }
            majChoixStockage();
        }, erreur -> {
            majChoixStockage();
            etat.setText("Stockage inchangé");
            ControleurOnglet.afficherErreur(erreur.getMessage());
        });
    }

    @FXML
    private void parametresConnexion() {
        ConfigurationStockage config = contexte.getConfiguration();
        new DialogueConnexion(stage, config.getParametres()).showAndWait().ifPresent(parametres -> {
            config.setParametres(parametres);
            enregistrerConfiguration();
            if (contexte.getStockage() instanceof StockageBaseDeDonnees) {
                contexte.setStockage(config.creerStockageBase());
                etat.setText("Paramètres enregistrés : utilisez Fichier > Recharger pour lire cette base");
            } else {
                etat.setText("Paramètres enregistrés : choisissez Stockage > Base de données MySQL pour les utiliser");
            }
        });
    }

    private void enregistrerConfiguration() {
        try {
            contexte.getConfiguration().enregistrer();
        } catch (PersistanceException e) {
            ControleurOnglet.afficherErreur(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ menu Simulation

    @FXML
    private void genererResultats() {
        Election election = contexte.getElection();
        if (election.getDepartements().isEmpty() || election.getPartis().isEmpty()) {
            ControleurOnglet.afficherErreur("Enrôlez au moins un département et un parti avant de générer des résultats.");
            return;
        }
        demanderBorneInscrits().ifPresent(borne -> {
            GenerateurResultats generateur = new GenerateurResultats();
            List<Departement> departements = election.getDepartements();
            // Les tirages sont calculés dans le thread d'arrière-plan sans modifier le modèle...
            Task<List<Tirage>> tache = new Task<>() {
                @Override
                protected List<Tirage> call() throws ElectionException {
                    List<Tirage> tirages = new ArrayList<>();
                    for (int i = 0; i < departements.size(); i++) {
                        tirages.add(generateur.tirer(departements.get(i), borne));
                        updateProgress(i + 1, departements.size());
                    }
                    return tirages;
                }
            };
            // ... puis appliqués dans le thread JavaFX
            executer("Génération des résultats", tache, tirages -> {
                try {
                    for (Tirage t : tirages) {
                        generateur.appliquer(election, t);
                    }
                    etat.setText("Résultats générés aléatoirement (au plus " + borne + " inscrits par département)");
                } catch (ElectionException e) {
                    ControleurOnglet.afficherErreur(e.getMessage());
                }
                contexte.notifierModification();
            }, erreur -> ControleurOnglet.afficherErreur(erreur.getMessage()));
        });
    }

    @FXML
    private void effacerResultats() {
        contexte.getElection().effacerResultats();
        contexte.notifierModification();
        etat.setText("Résultats effacés");
    }

    @FXML
    private void aPropos() {
        Alert alerte = new Alert(Alert.AlertType.INFORMATION);
        alerte.initOwner(stage);
        alerte.setTitle("À propos");
        alerte.setHeaderText("Simulation du traitement d'une élection législative");
        alerte.setContentText("Projet Java SE - Master 2 Data Sciences - INP-HB Yamoussoukro\n"
                + "Cours du Prof. Bi Tra GOORE\n\n"
                + "Réalisé par : " + MEMBRES_GROUPE + "\n\n"
                + "Scrutin majoritaire à un tour : pas de vote blanc, pas d'égalité de voix "
                + "dans un département, le candidat qui a le plus de voix est élu.");
        alerte.showAndWait();
    }

    /** Demande la borne supérieure du nombre d'inscrits par département. */
    static Optional<Integer> demanderBorneInscrits() {
        TextInputDialog dialogue = new TextInputDialog(String.valueOf(BORNE_INSCRITS_PAR_DEFAUT));
        dialogue.setTitle("Génération aléatoire");
        dialogue.setHeaderText("Nombre maximal d'électeurs inscrits par département");
        dialogue.setContentText("Borne supérieure :");
        Optional<String> saisie = dialogue.showAndWait();
        if (saisie.isEmpty()) {
            return Optional.empty();
        }
        try {
            int borne = Integer.parseInt(saisie.get().trim().replace(" ", "").replace(" ", ""));
            if (borne <= 0) {
                throw new NumberFormatException();
            }
            return Optional.of(borne);
        } catch (NumberFormatException e) {
            ControleurOnglet.afficherErreur("La borne doit être un entier positif.");
            return Optional.empty();
        }
    }

    /** Vrai si l'on peut continuer : rien à enregistrer, enregistrement réussi, ou abandon accepté. */
    private boolean confirmerAbandonModifications() {
        if (!contexte.isModifie()) {
            return true;
        }
        ButtonType boutonEnregistrer = new ButtonType("Enregistrer", ButtonBar.ButtonData.YES);
        ButtonType boutonIgnorer = new ButtonType("Ne pas enregistrer", ButtonBar.ButtonData.NO);
        Alert alerte = new Alert(Alert.AlertType.CONFIRMATION,
                "Des modifications n'ont pas été enregistrées dans " + contexte.getStockage().getDescription() + ".",
                boutonEnregistrer, boutonIgnorer, ButtonType.CANCEL);
        alerte.initOwner(stage);
        alerte.setHeaderText("Enregistrer avant de continuer ?");
        ButtonType choix = alerte.showAndWait().orElse(ButtonType.CANCEL);
        if (choix == boutonEnregistrer) {
            return enregistrerMaintenant();
        }
        return choix == boutonIgnorer;
    }
}
