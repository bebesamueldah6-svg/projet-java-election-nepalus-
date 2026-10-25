package ci.inphb.election.vue;

import ci.inphb.election.persistance.ParametresConnexion;
import ci.inphb.election.persistance.PersistanceException;
import ci.inphb.election.persistance.StockageBaseDeDonnees;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

/** Boîte de dialogue de saisie et de test des paramètres de connexion à la base MySQL. */
public class DialogueConnexion extends Dialog<ParametresConnexion> {

    private final TextField champHote = new TextField();
    private final TextField champPort = new TextField();
    private final TextField champBase = new TextField();
    private final TextField champUtilisateur = new TextField();
    private final PasswordField champMotDePasse = new PasswordField();
    private final Label resultatTest = new Label();

    public DialogueConnexion(Window proprietaire, ParametresConnexion actuels) {
        initOwner(proprietaire);
        setTitle("Base de données MySQL");
        setHeaderText("Paramètres de connexion\n(XAMPP / WAMP : utilisateur « root », mot de passe vide)");

        champHote.setText(actuels.getHote());
        champPort.setText(String.valueOf(actuels.getPort()));
        champBase.setText(actuels.getBase());
        champUtilisateur.setText(actuels.getUtilisateur());
        champMotDePasse.setText(actuels.getMotDePasse());
        resultatTest.setWrapText(true);
        resultatTest.setMaxWidth(380);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(8);
        grille.addRow(0, new Label("Hôte :"), champHote);
        grille.addRow(1, new Label("Port :"), champPort);
        grille.addRow(2, new Label("Base :"), champBase);
        grille.addRow(3, new Label("Utilisateur :"), champUtilisateur);
        grille.addRow(4, new Label("Mot de passe :"), champMotDePasse);
        grille.add(resultatTest, 0, 5, 2, 1);
        getDialogPane().setContent(grille);

        ButtonType boutonTester = new ButtonType("Tester la connexion", ButtonBar.ButtonData.OTHER);
        getDialogPane().getButtonTypes().addAll(boutonTester, ButtonType.OK, ButtonType.CANCEL);

        // Le bouton « Tester » ne ferme pas la fenêtre
        Node tester = getDialogPane().lookupButton(boutonTester);
        tester.addEventFilter(ActionEvent.ACTION, evenement -> {
            evenement.consume();
            tester();
        });
        // OK refusé tant que les champs sont invalides
        Node ok = getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, evenement -> {
            if (lireParametres().isEmpty()) {
                evenement.consume();
            }
        });

        setResultConverter(bouton -> bouton == ButtonType.OK ? lireParametres().orElse(null) : null);
    }

    private Optional<ParametresConnexion> lireParametres() {
        try {
            return Optional.of(new ParametresConnexion(champHote.getText(), Integer.parseInt(champPort.getText().trim()),
                    champBase.getText(), champUtilisateur.getText(), champMotDePasse.getText()));
        } catch (NumberFormatException e) {
            afficher("Le port doit être un nombre (3306 par défaut).", false);
        } catch (IllegalArgumentException e) {
            afficher(e.getMessage(), false);
        }
        return Optional.empty();
    }

    private void tester() {
        lireParametres().ifPresent(p -> {
            try {
                afficher(new StockageBaseDeDonnees(p).testerConnexion(), true);
            } catch (PersistanceException e) {
                afficher(e.getMessage(), false);
            }
        });
    }

    private void afficher(String message, boolean succes) {
        resultatTest.setText(message);
        resultatTest.setStyle(succes ? "-fx-text-fill: #00796b;" : "-fx-text-fill: #c62828;");
    }
}
