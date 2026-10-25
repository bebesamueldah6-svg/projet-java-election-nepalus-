package ci.inphb.election.persistance;

import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.PartiPolitique;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Stockage MySQL, sur une base de test dédiée (« projets_java_test ») pour ne jamais
 * toucher aux données réelles. Tests ignorés si aucun serveur MySQL local n'est démarré.
 */
class StockageBaseDeDonneesTest {

    private static final String BASE_TEST = "projets_java_test";
    private static boolean serveurDisponible;

    private final StockageBaseDeDonnees stockage = new StockageBaseDeDonnees(
            new ParametresConnexion("localhost", 3306, BASE_TEST, "root", ""));

    @BeforeAll
    static void creerBaseDeTest() {
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/?connectTimeout=2000", "root", "");
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS " + BASE_TEST + " CHARACTER SET utf8mb4");
            serveurDisponible = true;
        } catch (SQLException e) {
            serveurDisponible = false;
        }
    }

    @BeforeEach
    void viderBase() throws SQLException {
        assumeTrue(serveurDisponible, "Serveur MySQL local indisponible : test ignoré");
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + BASE_TEST, "root", "");
             Statement s = c.createStatement()) {
            s.executeUpdate("DROP TABLE IF EXISTS resultat");
            s.executeUpdate("DROP TABLE IF EXISTS departement");
            s.executeUpdate("DROP TABLE IF EXISTS parti");
        }
    }

    @Test
    void baseNeuveVide() throws PersistanceException {
        assertFalse(stockage.contientDesDonnees());
        assertTrue(stockage.charger().getDepartements().isEmpty());
    }

    @Test
    void sauvegardePuisRechargementRestitueLesDonnees() throws Exception {
        StockageElectionTest.verifierAllerRetour(stockage);
    }

    @Test
    void secondeSauvegardeRemplaceLaPremiere() throws Exception {
        StockageElectionTest.verifierAllerRetour(stockage);
        Election petite = new Election();
        petite.enrolerParti(new PartiPolitique("XYZ", "Parti unique"));
        stockage.sauvegarder(petite);

        Election relue = stockage.charger();
        assertEquals(1, relue.getPartis().size());
        assertTrue(relue.getDepartements().isEmpty());
    }

    @Test
    void testDeConnexionDecritLeServeur() throws PersistanceException {
        String description = stockage.testerConnexion();
        assertTrue(description.contains("Connexion réussie"));
        assertTrue(description.contains("MySQL Connector/J"));
    }

    @Test
    void mauvaisMotDePasseSignale() {
        StockageBaseDeDonnees invalide = new StockageBaseDeDonnees(
                new ParametresConnexion("localhost", 3306, BASE_TEST, "utilisateur_inexistant", "faux"));
        PersistanceException e = assertThrows(PersistanceException.class, invalide::charger);
        assertTrue(e.getMessage().startsWith("Connexion impossible"));
    }
}
