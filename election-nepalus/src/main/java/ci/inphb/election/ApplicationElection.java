package ci.inphb.election;

import ci.inphb.election.vue.ControleurPrincipal;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;

/**
 * Application JavaFX : écran d'accueil animé, puis fenêtre principale décrite en FXML.
 */
public class ApplicationElection extends Application {

    private static final Duration DUREE_ACCUEIL = Duration.seconds(1.8);

    @Override
    public void start(Stage stage) throws Exception {
        Stage accueil = creerEcranAccueil();
        accueil.show();

        // Préparation de la fenêtre principale pendant l'affichage de l'accueil
        URL vue = getClass().getResource("vue/principale.fxml");
        FXMLLoader chargeur = new FXMLLoader(vue);
        Parent racine = chargeur.load();
        // Taille adaptée à l'écran (zone utile, sans la barre des tâches) : sur un petit écran
        // ou avec une mise à l'échelle Windows de 125-150 %, la fenêtre s'ouvre agrandie
        Rectangle2D ecran = Screen.getPrimary().getVisualBounds();
        double largeur = Math.min(1200, ecran.getWidth());
        double hauteur = Math.min(760, ecran.getHeight());
        Scene scene = new Scene(racine, largeur, hauteur);
        scene.getStylesheets().add(feuilleDeStyle());
        stage.setScene(scene);
        stage.setMinWidth(Math.min(950, ecran.getWidth()));
        stage.setMinHeight(Math.min(600, ecran.getHeight()));
        if (ecran.getWidth() < 1200 || ecran.getHeight() < 800) {
            stage.setMaximized(true);
        }
        ajouterIcones(stage);
        ControleurPrincipal controleur = chargeur.getController();
        controleur.demarrer(stage);

        // Après l'accueil : fondu sortant de l'accueil, fondu entrant de la fenêtre principale
        PauseTransition attente = new PauseTransition(DUREE_ACCUEIL);
        attente.setOnFinished(e -> {
            FadeTransition sortie = new FadeTransition(Duration.millis(350), accueil.getScene().getRoot());
            sortie.setToValue(0);
            sortie.setOnFinished(f -> accueil.close());
            sortie.play();
            stage.setOpacity(0);
            stage.show();
            new Timeline(new KeyFrame(Duration.millis(450), new KeyValue(stage.opacityProperty(), 1))).play();
        });
        attente.play();
    }

    private Stage creerEcranAccueil() {
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("vue/images/logo-256.png")));
        logo.setFitWidth(150);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        Label titre = new Label("Élection législative de Nepalus");
        titre.getStyleClass().add("accueil-titre");
        Label sousTitre = new Label("Simulation du traitement d'un scrutin majoritaire à un tour");
        sousTitre.getStyleClass().add("accueil-sous-titre");
        ProgressBar barre = new ProgressBar();
        barre.setPrefWidth(260);
        Label auteurs = new Label("Projet Java SE, M2 Data Sciences, INP-HB\n"
                + "DAH BEBE SAMUEL, BROU KOUAKOU CEPHAS et DJE KOFFI EUDES");
        auteurs.getStyleClass().add("accueil-auteurs");

        VBox contenu = new VBox(14, logo, titre, sousTitre, barre, auteurs);
        contenu.setAlignment(Pos.CENTER);
        contenu.getStyleClass().add("ecran-accueil");

        Scene scene = new Scene(contenu, 560, 420);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(feuilleDeStyle());
        Stage accueil = new Stage(StageStyle.TRANSPARENT);
        accueil.setScene(scene);
        ajouterIcones(accueil);

        // Apparition : fondu + logo qui grossit légèrement
        contenu.setOpacity(0);
        FadeTransition apparition = new FadeTransition(Duration.millis(500), contenu);
        apparition.setToValue(1);
        apparition.play();
        ScaleTransition zoom = new ScaleTransition(Duration.millis(700), logo);
        zoom.setFromX(0.7);
        zoom.setFromY(0.7);
        zoom.setToX(1);
        zoom.setToY(1);
        zoom.play();
        return accueil;
    }

    private String feuilleDeStyle() {
        return getClass().getResource("vue/election.css").toExternalForm();
    }

    private void ajouterIcones(Stage fenetre) {
        for (int taille : new int[]{16, 32, 48, 64, 256}) {
            fenetre.getIcons().add(new Image(getClass().getResourceAsStream("vue/images/logo-" + taille + ".png")));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
