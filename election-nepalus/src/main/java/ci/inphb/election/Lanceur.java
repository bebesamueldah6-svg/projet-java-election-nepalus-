package ci.inphb.election;

import javafx.application.Application;

/**
 * Point d'entrée du .jar exécutable. Une classe qui n'hérite pas de
 * {@link Application} est nécessaire pour lancer JavaFX depuis un jar
 * contenant ses dépendances (java -jar election-nepalus.jar).
 */
public final class Lanceur {

    private Lanceur() {
    }

    public static void main(String[] args) {
        Application.launch(ApplicationElection.class, args);
    }
}
