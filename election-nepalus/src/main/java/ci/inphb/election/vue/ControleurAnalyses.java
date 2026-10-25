package ci.inphb.election.vue;

import ci.inphb.election.modele.Candidat;
import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.PartiPolitique;
import ci.inphb.election.modele.StatistiquesParticipation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Onglet « Analyses » : tableau de bord interactif répondant aux questions posées.
 * Sections repliables, graphiques avec infobulles et clics qui filtrent le tableau
 * des départements, double-clic sur un département pour ouvrir ses résultats.
 */
public class ControleurAnalyses extends ControleurOnglet {

    /** Sections vers lesquelles on peut faire défiler l'onglet. */
    public enum Section { NATIONAL, STATISTIQUES }

    private static final int CLASSES_HISTOGRAMME = 10;

    @FXML
    private ScrollPane defilement;
    @FXML
    private VBox contenu;
    @FXML
    private TitledPane panneauNational;
    @FXML
    private TitledPane panneauStatistiques;
    @FXML
    private TitledPane panneauDepartements;
    @FXML
    private TitledPane panneauSeuil;
    @FXML
    private TitledPane panneauUnanimes;

    // Résultat national
    @FXML
    private Label conclusionNationale;
    @FXML
    private TableView<PartiPolitique> tableNationale;
    @FXML
    private TableColumn<PartiPolitique, PartiPolitique> colNatParti;
    @FXML
    private TableColumn<PartiPolitique, Long> colNatSieges;
    @FXML
    private TableColumn<PartiPolitique, Double> colNatPartSieges;
    @FXML
    private TableColumn<PartiPolitique, Integer> colNatVoix;
    @FXML
    private TableColumn<PartiPolitique, Double> colNatPartVoix;
    @FXML
    private BarChart<String, Number> graphiqueSieges;

    // Participation au-dessus d'un seuil (50 % par défaut)
    @FXML
    private Slider seuil;
    @FXML
    private Label libelleSeuil;
    @FXML
    private Label nombreAuDessusSeuil;
    @FXML
    private TableView<Departement> tableSeuil;
    @FXML
    private TableColumn<Departement, Integer> colSeuilNumero;
    @FXML
    private TableColumn<Departement, String> colSeuilNom;
    @FXML
    private TableColumn<Departement, Double> colSeuilTaux;

    // Gagnant, dernier et participation par département (avec filtre)
    @FXML
    private Label filtreActif;
    @FXML
    private Button boutonToutAfficher;
    @FXML
    private TableView<Departement> tableDepartements;
    @FXML
    private TableColumn<Departement, Integer> colNumero;
    @FXML
    private TableColumn<Departement, String> colNom;
    @FXML
    private TableColumn<Departement, Double> colParticipation;
    @FXML
    private TableColumn<Departement, PartiPolitique> colGagnant;
    @FXML
    private TableColumn<Departement, Integer> colVoixGagnant;
    @FXML
    private TableColumn<Departement, PartiPolitique> colDernier;
    @FXML
    private TableColumn<Departement, Integer> colVoixDernier;

    // Unanimité
    @FXML
    private ListView<Departement> listeUnanimes;
    @FXML
    private Label remarqueUnanimes;

    // Participation générale et statistiques
    @FXML
    private Label tauxGeneral;
    @FXML
    private Label statEffectif;
    @FXML
    private Label statMoyenne;
    @FXML
    private Label statMediane;
    @FXML
    private Label statEcartType;
    @FXML
    private Label statMinimum;
    @FXML
    private Label statMaximum;
    @FXML
    private BarChart<String, Number> histogramme;

    private Map<PartiPolitique, Long> sieges = Map.of();
    private Map<PartiPolitique, Integer> voix = Map.of();
    private long totalSieges;
    private long totalVoix;

    private final ObservableList<Departement> departements = FXCollections.observableArrayList();
    private final FilteredList<Departement> departementsFiltres = new FilteredList<>(departements);
    /** Filtre courant (parti gagnant ou tranche de participation), réappliqué à chaque rafraîchissement. */
    private Runnable filtreCourant;

    @FXML
    private void initialize() {
        colonneParti(colNatParti, p -> p, true);
        colonne(colNatSieges, p -> sieges.getOrDefault(p, 0L));
        colonnePourcentage(colNatPartSieges, p -> totalSieges == 0 ? 0.0 : (double) sieges.getOrDefault(p, 0L) / totalSieges);
        colonneEntier(colNatVoix, p -> voix.getOrDefault(p, 0));
        colonnePourcentage(colNatPartVoix, p -> totalVoix == 0 ? 0.0 : (double) voix.getOrDefault(p, 0) / totalVoix);
        // Un clic sur un parti filtre les départements qu'il a gagnés
        tableNationale.getSelectionModel().selectedItemProperty().addListener((obs, ancien, parti) -> {
            if (parti != null) {
                filtrerParParti(parti);
            }
        });

        libelleSeuil.textProperty().bind(seuil.valueProperty().asString("Seuil de participation : %.0f %%"));
        seuil.valueProperty().addListener((obs, a, n) -> majSeuil());
        colonne(colSeuilNumero, Departement::getNumero);
        colonne(colSeuilNom, Departement::getNom);
        colonnePourcentage(colSeuilTaux, Departement::getTauxParticipation);

        SortedList<Departement> tries = new SortedList<>(departementsFiltres);
        tries.comparatorProperty().bind(tableDepartements.comparatorProperty());
        tableDepartements.setItems(tries);
        colonne(colNumero, Departement::getNumero);
        colonne(colNom, Departement::getNom);
        colonnePourcentage(colParticipation, Departement::getTauxParticipation);
        colonneParti(colGagnant, d -> election().partiGagnant(d).orElse(null), true);
        colonneEntier(colVoixGagnant, d -> voixDe(election().getRegle().designerElu(d)));
        colonneParti(colDernier, d -> election().partiDernier(d).orElse(null), true);
        colonneEntier(colVoixDernier, d -> voixDe(election().getRegle().designerDernier(d)));

        // Le bouton « Afficher tout » n'apparaît que lorsqu'un filtre est actif
        boutonToutAfficher.visibleProperty().bind(filtreActif.visibleProperty());
        boutonToutAfficher.managedProperty().bind(filtreActif.visibleProperty());
        filtreActif.managedProperty().bind(filtreActif.visibleProperty());
        filtreActif.setVisible(false);

        // Double-clic sur un département : ouverture de ses résultats
        doubleClicSurLigne(tableDepartements, d -> contexte.getNavigation().afficherDepartement(d));
        doubleClicSurLigne(tableSeuil, d -> contexte.getNavigation().afficherDepartement(d));
        listeUnanimes.setOnMouseClicked(e -> {
            Departement d = listeUnanimes.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && d != null) {
                contexte.getNavigation().afficherDepartement(d);
            }
        });
    }

    private static Integer voixDe(Optional<Candidat> c) {
        return c.map(Candidat::getVoix).orElse(null);
    }

    @Override
    protected void rafraichir() {
        Election e = election();
        majResultatNational(e);
        majSeuil();
        departements.setAll(e.getDepartements());
        if (filtreCourant != null) {
            filtreCourant.run();
        }
        tableDepartements.refresh();
        majUnanimite(e);
        majStatistiques(e);
    }

    // ------------------------------------------------------------------ résultat national

    private void majResultatNational(Election e) {
        sieges = e.siegesParParti();
        voix = e.voixParParti();
        totalSieges = sieges.values().stream().mapToLong(Long::longValue).sum();
        totalVoix = voix.values().stream().mapToLong(Integer::longValue).sum();

        // Partis classés par sièges, puis par voix
        List<PartiPolitique> classement = e.getPartis().stream()
                .sorted(Comparator.comparing((PartiPolitique p) -> sieges.getOrDefault(p, 0L))
                        .thenComparing(p -> voix.getOrDefault(p, 0))
                        .reversed())
                .collect(Collectors.toList());
        tableNationale.setItems(FXCollections.observableArrayList(classement));

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        classement.forEach(p -> serie.getData().add(new XYChart.Data<>(p.getSigle(), sieges.getOrDefault(p, 0L))));
        graphiqueSieges.getData().setAll(List.of(serie));
        // Chaque barre : couleur du parti, infobulle, clic = départements gagnés
        for (int i = 0; i < classement.size(); i++) {
            PartiPolitique p = classement.get(i);
            String infobulle = p.getLibelle() + "\n" + nombre(sieges.getOrDefault(p, 0L), "siège", "sièges") + " et "
                    + FORMAT_ENTIER.format(voix.getOrDefault(p, 0)) + " voix\nCliquez pour voir les départements gagnés";
            rendreInteractif(serie.getData().get(i).nodeProperty(), "-fx-bar-fill: " + couleur(p) + ";",
                    infobulle, () -> filtrerParParti(p));
        }

        long depouilles = e.nombreDepartementsDepouilles();
        String entete = nombre(e.nombreSieges(), "siège", "sièges") + " à pourvoir, majorité absolue à "
                + nombre(e.siegesPourMajoriteAbsolue(), "siège", "sièges") + ". Dépouillement : "
                + depouilles + " sur " + nombre(e.nombreSieges(), "département", "départements") + ".\n";
        if (depouilles == 0) {
            conclusionNationale.setText(entete + "Aucun résultat enregistré pour le moment.");
        } else if (depouilles < e.nombreSieges()) {
            conclusionNationale.setText(entete + "Résultat provisoire : tous les départements ne sont pas dépouillés.");
        } else {
            conclusionNationale.setText(entete + e.partiMajoritaire()
                    .map(p -> "Le " + p.getLibelle() + " obtient la majorité absolue avec "
                            + sieges.get(p) + " sièges.")
                    .orElse("Aucun parti n'obtient la majorité absolue : une coalition est nécessaire pour gouverner."));
        }
    }

    // ------------------------------------------------------------------ filtres du tableau des départements

    /** Affiche uniquement les départements gagnés par ce parti (appelé aussi depuis les autres onglets). */
    public void filtrerParParti(PartiPolitique parti) {
        appliquerFiltre(() -> {
            departementsFiltres.setPredicate(d -> election().partiGagnant(d).filter(parti::equals).isPresent());
            return "Départements gagnés par " + parti.getSigle();
        });
    }

    /** Affiche uniquement les départements dont la participation est dans la tranche k (de 10 %). */
    private void filtrerParTranche(int k) {
        appliquerFiltre(() -> {
            departementsFiltres.setPredicate(dansTranche(k));
            return "Participation entre " + (k * 10) + " et " + ((k + 1) * 10) + " %";
        });
    }

    private static Predicate<Departement> dansTranche(int k) {
        return d -> d.getInscrits() > 0
                && Math.min((int) (d.getTauxParticipation() * CLASSES_HISTOGRAMME), CLASSES_HISTOGRAMME - 1) == k;
    }

    /** Applique un filtre, affiche son libellé et amène la section des départements à l'écran. */
    private void appliquerFiltre(java.util.function.Supplier<String> filtre) {
        filtreCourant = () -> filtreActif.setText("Filtre : " + filtre.get() + " (" + departementsFiltres.size() + ")");
        filtreCourant.run();
        filtreActif.setVisible(true);
        panneauDepartements.setExpanded(true);
        defilerVers(panneauDepartements);
        Animations.apparition(tableDepartements);
    }

    @FXML
    private void toutAfficher() {
        filtreCourant = null;
        departementsFiltres.setPredicate(null);
        filtreActif.setVisible(false);
        tableNationale.getSelectionModel().clearSelection();
        Animations.apparition(tableDepartements);
    }

    // ------------------------------------------------------------------ sections repliables et défilement

    private List<TitledPane> panneaux() {
        return List.of(panneauNational, panneauStatistiques, panneauDepartements, panneauSeuil, panneauUnanimes);
    }

    @FXML
    private void toutDeplier() {
        panneaux().forEach(p -> p.setExpanded(true));
    }

    @FXML
    private void toutReplier() {
        panneaux().forEach(p -> p.setExpanded(false));
    }

    /** Déplie une section et la fait défiler jusqu'en haut de l'écran (appelé depuis le bandeau). */
    public void afficherSection(Section section) {
        TitledPane panneau = section == Section.NATIONAL ? panneauNational : panneauStatistiques;
        panneau.setExpanded(true);
        defilerVers(panneau);
        Animations.pulsation(panneau);
    }

    /** Défilement animé jusqu'au panneau, une fois la mise en page recalculée. */
    private void defilerVers(TitledPane panneau) {
        Platform.runLater(() -> {
            contenu.applyCss();
            contenu.layout();
            double hauteurVisible = defilement.getViewportBounds().getHeight();
            double debattement = contenu.getHeight() - hauteurVisible;
            double cible = debattement <= 0 ? 0 : Math.min(1, panneau.getBoundsInParent().getMinY() / debattement);
            new Timeline(new KeyFrame(Duration.millis(450),
                    new KeyValue(defilement.vvalueProperty(), cible, Interpolator.EASE_BOTH))).play();
        });
    }

    // ------------------------------------------------------------------ autres sections

    private void majSeuil() {
        if (contexte == null) {
            return;
        }
        List<Departement> resultat = election().departementsAvecParticipationAuMoins(seuil.getValue() / 100.0);
        tableSeuil.setItems(FXCollections.observableArrayList(resultat));
        nombreAuDessusSeuil.setText(resultat.size() + " sur "
                + nombre(election().getDepartements().size(), "département", "départements"));
    }

    private void majUnanimite(Election e) {
        listeUnanimes.setItems(FXCollections.observableArrayList(e.departementsUnanimes()));
        remarqueUnanimes.setText(e.getPartis().size() >= 3
                ? "Avec trois partis ou plus, ce cas est impossible : les partis sans voix seraient à égalité "
                + "(0 voix chacun), ce que la règle du scrutin interdit."
                : "Département où tous les inscrits ont voté pour le même parti.");
    }

    private void majStatistiques(Election e) {
        StatistiquesParticipation stats = e.statistiquesParticipation();
        tauxGeneral.setText("Taux de participation général : " + FORMAT_POURCENTAGE.format(e.tauxParticipationGeneral()));
        statEffectif.setText(String.valueOf(stats.getEffectif()));
        statMoyenne.setText(FORMAT_POURCENTAGE.format(stats.getMoyenne()));
        statMediane.setText(FORMAT_POURCENTAGE.format(stats.getMediane()));
        statEcartType.setText(String.format("%.2f points", stats.getEcartType() * 100));
        statMinimum.setText(FORMAT_POURCENTAGE.format(stats.getMinimum()));
        statMaximum.setText(FORMAT_POURCENTAGE.format(stats.getMaximum()));

        int[] effectifs = stats.histogramme(CLASSES_HISTOGRAMME);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        int largeur = 100 / CLASSES_HISTOGRAMME;
        for (int k = 0; k < effectifs.length; k++) {
            serie.getData().add(new XYChart.Data<>((k * largeur) + "-" + ((k + 1) * largeur) + " %", effectifs[k]));
        }
        histogramme.getData().setAll(List.of(serie));
        // Chaque barre : infobulle listant les départements de la tranche, clic = filtre
        for (int k = 0; k < effectifs.length; k++) {
            final int tranche = k;
            String noms = e.getDepartements().stream().filter(dansTranche(k))
                    .map(Departement::getNom).collect(Collectors.joining(", "));
            String infobulle = "Participation de " + (k * largeur) + " à " + ((k + 1) * largeur) + " % : "
                    + nombre(effectifs[k], "département", "départements")
                    + (noms.isEmpty() ? "" : "\n" + noms + "\nCliquez pour les afficher");
            rendreInteractif(serie.getData().get(k).nodeProperty(), "", infobulle,
                    effectifs[k] == 0 ? null : () -> filtrerParTranche(tranche));
        }
    }
}
