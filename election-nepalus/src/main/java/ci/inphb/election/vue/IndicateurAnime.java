package ci.inphb.election.vue;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.DoubleFunction;

/**
 * Carte d'indicateur du bandeau : un titre et une valeur. Quand la valeur change,
 * le chiffre défile de l'ancienne à la nouvelle valeur (animation par Timeline
 * sur une propriété observable).
 */
public class IndicateurAnime extends VBox {

    private final Label valeur = new Label();
    private final DoubleProperty valeurAffichee = new SimpleDoubleProperty(0);
    private Timeline animation;

    public IndicateurAnime(String titre, DoubleFunction<String> format) {
        Label libelle = new Label(titre.toUpperCase());
        libelle.getStyleClass().add("indicateur-titre");
        valeur.getStyleClass().add("indicateur-valeur");
        getStyleClass().add("indicateur");
        getChildren().addAll(valeur, libelle);
        valeurAffichee.addListener((obs, ancienne, nouvelle) -> valeur.setText(format.apply(nouvelle.doubleValue())));
        valeur.setText(format.apply(0));
    }

    /** Fait défiler la valeur affichée jusqu'à la cible. */
    public void animerVers(double cible) {
        if (animation != null) {
            animation.stop();
        }
        animation = new Timeline(new KeyFrame(Duration.millis(700),
                new KeyValue(valeurAffichee, cible, Interpolator.EASE_OUT)));
        animation.play();
    }

    /** Rend la carte cliquable : infobulle, curseur main, surbrillance au survol (CSS) et action. */
    public void auClic(String infobulle, Runnable action) {
        Tooltip bulle = new Tooltip(infobulle);
        bulle.setShowDelay(Duration.millis(150));
        Tooltip.install(this, bulle);
        getStyleClass().add("indicateur-cliquable");
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> {
            Animations.pulsation(this);
            action.run();
        });
    }

    /** Affiche un texte (valeur non numérique), avec un fondu s'il change. */
    public void afficherTexte(String texte) {
        if (!texte.equals(valeur.getText())) {
            valeur.setText(texte);
            Animations.fondu(valeur);
        }
    }
}
