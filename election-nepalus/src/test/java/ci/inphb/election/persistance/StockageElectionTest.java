package ci.inphb.election.persistance;

import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.simulation.DonneesDemo;
import ci.inphb.election.simulation.GenerateurResultats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stockage dans des fichiers CSV. */
class StockageElectionTest {

    @TempDir
    Path dossier;

    /** Vérifie qu'une élection relue est identique à l'originale (utilisé aussi pour la base de données). */
    static void verifierAllerRetour(StockageElection stockage) throws Exception {
        Election originale = DonneesDemo.creer();
        new GenerateurResultats(new Random(42)).generer(originale, 10_000);
        stockage.sauvegarder(originale);
        assertTrue(stockage.contientDesDonnees());

        Election relue = stockage.charger();
        assertEquals(originale.getPartis(), relue.getPartis());
        assertEquals(originale.getDepartements(), relue.getDepartements());
        for (Departement d : originale.getDepartements()) {
            Departement r = relue.trouverDepartement(d.getNumero()).orElseThrow();
            assertEquals(d.getInscrits(), r.getInscrits());
            assertEquals(d.getVotants(), r.getVotants());
            assertTrue(r.isDepouille());
            assertEquals(originale.partiGagnant(d), relue.partiGagnant(r));
        }
        assertEquals(originale.tauxParticipationGeneral(), relue.tauxParticipationGeneral(), 1e-12);
    }

    @Test
    void sauvegardePuisRechargementRestitueLesDonnees() throws Exception {
        verifierAllerRetour(new StockageFichiers(dossier));
    }

    @Test
    void dossierVideDonneUneElectionVide() throws PersistanceException {
        StockageFichiers stockage = new StockageFichiers(dossier);
        assertFalse(stockage.contientDesDonnees());
        Election e = stockage.charger();
        assertTrue(e.getDepartements().isEmpty());
        assertTrue(e.getPartis().isEmpty());
    }

    @Test
    void fichierCorrompuSignale() throws IOException {
        Files.writeString(dossier.resolve("partis.csv"), "sigle;nom\nPUN\n", StandardCharsets.UTF_8);
        PersistanceException e = assertThrows(PersistanceException.class, () -> new StockageFichiers(dossier).charger());
        assertTrue(e.getMessage().contains("ligne 2"));
    }

    @Test
    void configurationEnregistreeEtRelue() throws PersistanceException {
        Path fichier = dossier.resolve("election.properties");
        ConfigurationStockage config = ConfigurationStockage.charger(fichier);
        assertFalse(config.existait());
        config.setMode(ConfigurationStockage.Mode.FICHIERS);
        config.setParametres(new ParametresConnexion("serveur", 3307, "ma_base", "admin", "secret"));
        config.enregistrer();

        ConfigurationStockage relue = ConfigurationStockage.charger(fichier);
        assertTrue(relue.existait());
        assertEquals(ConfigurationStockage.Mode.FICHIERS, relue.getMode());
        assertEquals("jdbc:mysql://serveur:3307/ma_base?useUnicode=true&characterEncoding=UTF-8&connectTimeout=5000",
                relue.getParametres().getUrl());
        assertEquals("secret", relue.getParametres().getMotDePasse());
    }
}
