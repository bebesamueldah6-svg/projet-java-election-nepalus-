package ci.inphb.election.vue;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/** Animations de l'interface (transitions JavaFX). */
public final class Animations {

    /** Vitesse des textes défilants, en pixels par seconde. */
    private static final double VITESSE_DEFILEMENT = 70;

    private Animations() {
    }

    /** Apparition en fondu avec un léger glissement vers le haut. */
    public static void apparition(Node noeud) {
        FadeTransition fondu = new FadeTransition(Duration.millis(320), noeud);
        fondu.setFromValue(0);
        fondu.setToValue(1);
        TranslateTransition glissement = new TranslateTransition(Duration.millis(320), noeud);
        glissement.setFromY(14);
        glissement.setToY(0);
        glissement.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fondu, glissement).play();
    }

    /** Fondu simple, pour un texte qui change. */
    public static void fondu(Node noeud) {
        FadeTransition fondu = new FadeTransition(Duration.millis(400), noeud);
        fondu.setFromValue(0.2);
        fondu.setToValue(1);
        fondu.play();
    }

    /** Pulsation brève (agrandissement puis retour), pour attirer l'attention. */
    public static void pulsation(Node noeud) {
        ScaleTransition echelle = new ScaleTransition(Duration.millis(180), noeud);
        echelle.setFromX(1);
        echelle.setFromY(1);
        echelle.setToX(1.08);
        echelle.setToY(1.08);
        echelle.setAutoReverse(true);
        echelle.setCycleCount(2);
        echelle.play();
    }

    /**
     * Fait défiler {@code contenu} de droite à gauche, en boucle, dans {@code zone} :
     * le texte sort par la gauche puis réapparaît par la droite. Le défilement s'arrête
     * tant que la souris survole la zone et se recalcule quand la fenêtre change de taille.
     */
    public static void defilement(Pane zone, Region contenu) {
        // Le texte n'est visible qu'à l'intérieur de la zone
        Rectangle cadre = new Rectangle();
        cadre.widthProperty().bind(zone.widthProperty());
        cadre.heightProperty().bind(zone.heightProperty());
        zone.setClip(cadre);
        contenu.layoutYProperty().bind(zone.heightProperty().subtract(contenu.heightProperty()).divide(2));

        TranslateTransition boucle = new TranslateTransition(Duration.ONE, contenu);
        boucle.setInterpolator(Interpolator.LINEAR);
        boucle.setCycleCount(Animation.INDEFINITE);
        Runnable relancer = () -> {
            double largeurZone = zone.getWidth();
            double largeurContenu = contenu.getWidth();
            if (largeurZone <= 0 || largeurContenu <= 0) {
                return;
            }
            boucle.stop();
            boucle.setDuration(Duration.seconds((largeurZone + largeurContenu) / VITESSE_DEFILEMENT));
            boucle.setFromX(largeurZone);
            boucle.setToX(-largeurContenu);
            // Départ texte aligné à gauche, pour qu'il soit lisible dès l'ouverture
            boucle.playFrom(Duration.seconds(largeurZone / VITESSE_DEFILEMENT));
        };
        zone.widthProperty().addListener((observable, ancienne, nouvelle) -> relancer.run());
        contenu.widthProperty().addListener((observable, ancienne, nouvelle) -> relancer.run());
        zone.setOnMouseEntered(e -> boucle.pause());
        zone.setOnMouseExited(e -> boucle.play());
    }
}
