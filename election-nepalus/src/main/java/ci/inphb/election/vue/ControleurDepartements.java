package ci.inphb.election.vue;

import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.ElectionException;
import ci.inphb.election.modele.PartiPolitique;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Onglet « Départements » : enrôlement, retrait et liste des départements. */
public class ControleurDepartements extends ControleurOnglet {

    @FXML
    private TableView<Departement> table;
    @FXML
    private TableColumn<Departement, Integer> colNumero;
    @FXML
    private TableColumn<Departement, String> colNom;
    @FXML
    private TableColumn<Departement, Integer> colInscrits;
    @FXML
    private TableColumn<Departement, Integer> colVotants;
    @FXML
    private TableColumn<Departement, Double> colParticipation;
    @FXML
    private TableColumn<Departement, String> colEtat;
    @FXML
    private TableColumn<Departement, PartiPolitique> colElu;
    @FXML
    private TextField champNumero;
    @FXML
    private TextField champNom;
    @FXML
    private Button boutonRetirer;
    @FXML
    private TextField recherche;

    private final ObservableList<Departement> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        lierTableFiltree(table, donnees, recherche, d -> d.getNumero() + " " + d.getNom());
        colonne(colNumero, Departement::getNumero);
        colonne(colNom, Departement::getNom);
        colonneEntier(colInscrits, Departement::getInscrits);
        colonneEntier(colVotants, Departement::getVotants);
        colonnePourcentage(colParticipation, Departement::getTauxParticipation);
        colonne(colEtat, d -> d.isDepouille() ? "Dépouillé" : "En attente");
        colEtat.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String etat, boolean vide) {
                super.updateItem(etat, vide);
                setText(vide ? null : etat);
                getStyleClass().removeAll("etat-depouille", "etat-attente");
                if (!vide && etat != null) {
                    getStyleClass().add(etat.equals("Dépouillé") ? "etat-depouille" : "etat-attente");
                }
            }
        });
        colonneParti(colElu, d -> election().partiGagnant(d).orElse(null), false);
        // Double-clic sur un département : ouverture de ses résultats
        doubleClicSurLigne(table, d -> contexte.getNavigation().afficherDepartement(d));
        boutonRetirer.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        champNom.setOnAction(e -> enroler());
    }

    @Override
    protected void rafraichir() {
        donnees.setAll(election().getDepartements());
        // Proposer automatiquement le numéro suivant
        int suivant = election().getDepartements().stream().mapToInt(Departement::getNumero).max().orElse(0) + 1;
        champNumero.setText(String.valueOf(suivant));
    }

    @FXML
    private void enroler() {
        try {
            int numero = Integer.parseInt(champNumero.getText().trim());
            election().enrolerDepartement(new Departement(numero, champNom.getText()));
            champNom.clear();
            contexte.notifierModification();
            champNom.requestFocus();
        } catch (NumberFormatException e) {
            afficherErreur("Le numéro du département doit être un entier.");
        } catch (ElectionException | IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void retirer() {
        Departement d = table.getSelectionModel().getSelectedItem();
        if (d != null) {
            election().retirerDepartement(d);
            contexte.notifierModification();
        }
    }
}
