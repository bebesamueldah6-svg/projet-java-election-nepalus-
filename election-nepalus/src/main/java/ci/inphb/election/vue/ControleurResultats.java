package ci.inphb.election.vue;

import ci.inphb.election.modele.Candidat;
import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.ElectionException;
import ci.inphb.election.modele.PartiPolitique;
import ci.inphb.election.simulation.GenerateurResultats;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Onglet « Résultats par département » : consultation, saisie et génération des résultats. */
public class ControleurResultats extends ControleurOnglet {

    @FXML
    private ListView<Departement> listeDepartements;
    @FXML
    private Label titre;
    @FXML
    private Label participation;
    @FXML
    private Label elu;
    @FXML
    private TableView<Candidat> tableCandidats;
    @FXML
    private TableColumn<Candidat, Integer> colRang;
    @FXML
    private TableColumn<Candidat, PartiPolitique> colParti;
    @FXML
    private TableColumn<Candidat, Integer> colVoix;
    @FXML
    private TableColumn<Candidat, Double> colPourcentage;
    @FXML
    private PieChart graphique;
    @FXML
    private Button boutonSaisir;
    @FXML
    private Button boutonGenerer;
    @FXML
    private TextField recherche;

    private final ObservableList<Departement> departements = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        listeDepartements.setItems(filtrer(departements, recherche, d -> d.getNumero() + " " + d.getNom()));
        // Chaque département est précédé d'un point de la couleur du parti élu (gris si non dépouillé)
        listeDepartements.setCellFactory(liste -> new ListCell<>() {
            @Override
            protected void updateItem(Departement d, boolean vide) {
                super.updateItem(d, vide);
                if (vide || d == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(d.getLibelle());
                    Circle point = new Circle(6, Color.web(couleur(election().partiGagnant(d).orElse(null))));
                    setGraphic(point);
                }
            }
        });
        colonne(colRang, this::rang);
        colonneParti(colParti, Candidat::getParti, true);
        colonneEntier(colVoix, Candidat::getVoix);
        colonnePourcentage(colPourcentage, Candidat::getPourcentage);
        // Ligne du candidat élu mise en évidence (classe CSS « ligne-elu »)
        tableCandidats.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Candidat c, boolean vide) {
                super.updateItem(c, vide);
                boolean estElu = !vide && c != null
                        && election().getRegle().designerElu(c.getDepartement()).filter(c::equals).isPresent();
                getStyleClass().remove("ligne-elu");
                if (estElu) {
                    getStyleClass().add("ligne-elu");
                }
            }
        });
        listeDepartements.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            afficherDetail(nouveau);
            if (nouveau != null && !nouveau.equals(ancien)) {
                Animations.apparition(graphique);
                Animations.pulsation(elu);
            }
        });
        boutonSaisir.disableProperty().bind(listeDepartements.getSelectionModel().selectedItemProperty().isNull());
        boutonGenerer.disableProperty().bind(listeDepartements.getSelectionModel().selectedItemProperty().isNull());
    }

    @Override
    protected void rafraichir() {
        Departement selection = listeDepartements.getSelectionModel().getSelectedItem();
        departements.setAll(election().getDepartements());
        Optional<Departement> aReselectionner = selection == null
                ? election().getDepartements().stream().findFirst()
                : election().trouverDepartement(selection.getNumero());
        aReselectionner.ifPresentOrElse(d -> listeDepartements.getSelectionModel().select(d),
                () -> afficherDetail(null));
        afficherDetail(listeDepartements.getSelectionModel().getSelectedItem());
    }

    /** Rang du candidat dans son département (1 = élu), null si non dépouillé. */
    private Integer rang(Candidat c) {
        Departement d = c.getDepartement();
        if (!d.isDepouille()) {
            return null;
        }
        return (int) d.getCandidats().stream().filter(autre -> autre.getVoix() > c.getVoix()).count() + 1;
    }

    private void afficherDetail(Departement d) {
        if (d == null) {
            titre.setText("Sélectionnez un département");
            participation.setText("");
            elu.setText("");
            tableCandidats.getItems().clear();
            graphique.getData().clear();
            return;
        }
        titre.setText("Département " + d.getLibelle());
        participation.setText(nombre(d.getInscrits(), "inscrit", "inscrits") + ", "
                + nombre(d.getVotants(), "votant", "votants") + ", soit une participation de "
                + FORMAT_POURCENTAGE.format(d.getTauxParticipation()));
        Optional<PartiPolitique> gagnant = election().partiGagnant(d);
        elu.setText(gagnant.map(p -> "Élu : " + p.getNom()).orElse("Résultats non encore enregistrés"));
        elu.setGraphic(gagnant.map(this::pastille).orElse(null));

        List<Candidat> candidats = new ArrayList<>(d.getCandidats());
        candidats.sort(Comparator.comparingInt(Candidat::getVoix).reversed());
        tableCandidats.setItems(FXCollections.observableArrayList(candidats));

        graphique.getData().clear();
        if (d.isDepouille()) {
            for (Candidat c : candidats) {
                PieChart.Data part = new PieChart.Data(c.getParti().getSigle() + " "
                        + FORMAT_POURCENTAGE.format(c.getPourcentage()), c.getVoix());
                graphique.getData().add(part);
                // Couleur du parti, infobulle, clic = sélection du candidat dans le tableau
                rendreInteractif(part.nodeProperty(), "-fx-pie-color: " + couleur(c.getParti()) + ";",
                        c.getParti().getLibelle() + "\n" + FORMAT_ENTIER.format(c.getVoix()) + " voix ("
                                + FORMAT_POURCENTAGE.format(c.getPourcentage()) + ")",
                        () -> {
                            tableCandidats.getSelectionModel().select(c);
                            tableCandidats.scrollTo(c);
                        });
            }
        }
    }

    /** Affiche les résultats d'un département (appelé depuis les autres onglets). */
    public void selectionner(Departement d) {
        recherche.clear();
        listeDepartements.getSelectionModel().select(d);
        listeDepartements.scrollTo(d);
    }

    // ------------------------------------------------------------------ génération

    @FXML
    private void genererDepartement() {
        Departement d = listeDepartements.getSelectionModel().getSelectedItem();
        if (d == null) {
            return;
        }
        ControleurPrincipal.demanderBorneInscrits().ifPresent(borne -> {
            try {
                new GenerateurResultats().genererDepartement(election(), d, borne);
                contexte.notifierModification();
            } catch (ElectionException e) {
                afficherErreur(e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ saisie manuelle

    @FXML
    private void saisirResultats() {
        Departement d = listeDepartements.getSelectionModel().getSelectedItem();
        if (d == null) {
            return;
        }
        if (d.getCandidats().isEmpty()) {
            afficherErreur("Aucun parti n'est enrôlé : il n'y a pas de candidat dans ce département.");
            return;
        }

        TextField champInscrits = champEntier(d.getInscrits());
        TextField champVotants = champEntier(d.getVotants());
        Map<PartiPolitique, TextField> champsVoix = new LinkedHashMap<>();

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(8);
        grille.addRow(0, new Label("Électeurs inscrits :"), champInscrits);
        grille.addRow(1, new Label("Votants :"), champVotants);
        int ligne = 2;
        for (Candidat c : d.getCandidats()) {
            TextField champ = champEntier(c.getVoix());
            champsVoix.put(c.getParti(), champ);
            grille.addRow(ligne++, new Label("Voix " + c.getParti().getLibelle() + " :"), champ);
        }

        // Total des voix recalculé à chaque frappe (binding sur les propriétés text des champs)
        List<ObservableValue<String>> dependances = new ArrayList<>();
        champsVoix.values().forEach(champ -> dependances.add(champ.textProperty()));
        dependances.add(champVotants.textProperty());
        StringBinding total = Bindings.createStringBinding(() -> {
            int somme = champsVoix.values().stream().mapToInt(champ -> lireEntier(champ, 0)).sum();
            return "Total des voix : " + somme + " / " + lireEntier(champVotants, 0) + " votants";
        }, dependances.toArray(new ObservableValue[0]));
        Label etiquetteTotal = new Label();
        etiquetteTotal.textProperty().bind(total);
        etiquetteTotal.getStyleClass().add("total-saisie");
        grille.add(etiquetteTotal, 0, ligne, 2, 1);

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setTitle("Saisie des résultats");
        dialogue.setHeaderText("Résultats du département " + d.getLibelle()
                + "\nPas de vote blanc, pas d'égalité de voix entre candidats.");
        dialogue.getDialogPane().setContent(grille);
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validation avant fermeture : en cas d'erreur la fenêtre reste ouverte
        Node boutonOk = dialogue.getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.addEventFilter(ActionEvent.ACTION, evenement -> {
            try {
                Map<PartiPolitique, Integer> voix = new LinkedHashMap<>();
                for (Map.Entry<PartiPolitique, TextField> entree : champsVoix.entrySet()) {
                    voix.put(entree.getKey(), lireEntierObligatoire(entree.getValue(), "voix " + entree.getKey().getSigle()));
                }
                election().enregistrerResultats(d,
                        lireEntierObligatoire(champInscrits, "inscrits"),
                        lireEntierObligatoire(champVotants, "votants"), voix);
            } catch (ElectionException e) {
                afficherErreur(e.getMessage());
                evenement.consume();
            }
        });

        dialogue.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(b -> contexte.notifierModification());
    }

    private static TextField champEntier(int valeur) {
        TextField champ = new TextField(String.valueOf(valeur));
        champ.setPrefColumnCount(8);
        // N'accepte que des chiffres
        champ.textProperty().addListener((obs, ancien, nouveau) -> {
            if (!nouveau.matches("\\d*")) {
                champ.setText(ancien);
            }
        });
        return champ;
    }

    private static int lireEntier(TextField champ, int parDefaut) {
        try {
            return Integer.parseInt(champ.getText().trim());
        } catch (NumberFormatException e) {
            return parDefaut;
        }
    }

    private static int lireEntierObligatoire(TextField champ, String nom) throws ElectionException {
        try {
            return Integer.parseInt(champ.getText().trim());
        } catch (NumberFormatException e) {
            throw new ElectionException("Valeur manquante ou trop grande pour « " + nom + " »");
        }
    }
}
