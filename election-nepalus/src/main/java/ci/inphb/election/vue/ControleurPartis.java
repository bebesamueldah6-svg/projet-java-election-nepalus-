package ci.inphb.election.vue;

import ci.inphb.election.modele.ElectionException;
import ci.inphb.election.modele.PartiPolitique;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Map;

/** Onglet « Partis politiques » : enrôlement, retrait, voix et sièges nationaux. */
public class ControleurPartis extends ControleurOnglet {

    @FXML
    private TableView<PartiPolitique> table;
    @FXML
    private TableColumn<PartiPolitique, PartiPolitique> colSigle;
    @FXML
    private TableColumn<PartiPolitique, String> colNom;
    @FXML
    private TableColumn<PartiPolitique, Integer> colVoix;
    @FXML
    private TableColumn<PartiPolitique, Long> colSieges;
    @FXML
    private TextField champSigle;
    @FXML
    private TextField champNom;
    @FXML
    private Button boutonRetirer;
    @FXML
    private TextField recherche;

    private final ObservableList<PartiPolitique> donnees = FXCollections.observableArrayList();
    private Map<PartiPolitique, Integer> voix = Map.of();
    private Map<PartiPolitique, Long> sieges = Map.of();

    @FXML
    private void initialize() {
        lierTableFiltree(table, donnees, recherche, p -> p.getSigle() + " " + p.getNom());
        colonneParti(colSigle, p -> p, false);
        // Double-clic sur un parti : départements qu'il a gagnés (onglet Analyses)
        doubleClicSurLigne(table, p -> contexte.getNavigation().afficherVictoires(p));
        colonne(colNom, PartiPolitique::getNom);
        colonneEntier(colVoix, p -> voix.getOrDefault(p, 0));
        colonne(colSieges, p -> sieges.getOrDefault(p, 0L));
        boutonRetirer.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        champNom.setOnAction(e -> enroler());
    }

    @Override
    protected void rafraichir() {
        voix = election().voixParParti();
        sieges = election().siegesParParti();
        donnees.setAll(election().getPartis());
        table.refresh();
    }

    @FXML
    private void enroler() {
        if (!confirmerEffacementResultats()) {
            return;
        }
        try {
            election().enrolerParti(new PartiPolitique(champSigle.getText(), champNom.getText()));
            champSigle.clear();
            champNom.clear();
            contexte.notifierModification();
            champSigle.requestFocus();
        } catch (ElectionException | IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void retirer() {
        PartiPolitique p = table.getSelectionModel().getSelectedItem();
        if (p != null && confirmerEffacementResultats()) {
            election().retirerParti(p);
            contexte.notifierModification();
        }
    }

    /** Ajouter ou retirer un parti change les candidats : les résultats existants sont effacés. */
    private boolean confirmerEffacementResultats() {
        if (!election().aDesResultats()) {
            return true;
        }
        Alert alerte = new Alert(Alert.AlertType.CONFIRMATION,
                "Modifier la liste des partis change les candidats de chaque département : "
                        + "tous les résultats enregistrés seront effacés. Continuer ?",
                ButtonType.OK, ButtonType.CANCEL);
        alerte.setHeaderText("Effacement des résultats");
        return alerte.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
