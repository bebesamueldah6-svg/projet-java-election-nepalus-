package ci.inphb.election.persistance;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Préférences de stockage enregistrées dans un fichier « election.properties » :
 * mode choisi (fichiers CSV ou base MySQL), dossier des fichiers, paramètres de connexion.
 */
public class ConfigurationStockage {

    public enum Mode { FICHIERS, BASE_DE_DONNEES }

    private static final String MODE = "stockage.mode";
    private static final String DOSSIER = "fichiers.dossier";
    private static final String HOTE = "mysql.hote";
    private static final String PORT = "mysql.port";
    private static final String BASE = "mysql.base";
    private static final String UTILISATEUR = "mysql.utilisateur";
    private static final String MOT_DE_PASSE = "mysql.motdepasse";

    private final Path fichier;
    private final boolean existait;
    private Mode mode = Mode.BASE_DE_DONNEES;
    private Path dossierFichiers = Path.of("donnees");
    private ParametresConnexion parametres = ParametresConnexion.parDefaut();

    private ConfigurationStockage(Path fichier, boolean existait) {
        this.fichier = fichier;
        this.existait = existait;
    }

    /** Lit la configuration ; un fichier absent ou illisible donne les valeurs par défaut. */
    public static ConfigurationStockage charger(Path fichier) {
        ConfigurationStockage config = new ConfigurationStockage(fichier, Files.exists(fichier));
        if (!config.existait) {
            return config;
        }
        Properties p = new Properties();
        try (Reader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            p.load(lecteur);
            config.mode = Mode.valueOf(p.getProperty(MODE, Mode.BASE_DE_DONNEES.name()));
            config.dossierFichiers = Path.of(p.getProperty(DOSSIER, "donnees"));
            config.parametres = new ParametresConnexion(
                    p.getProperty(HOTE, ParametresConnexion.HOTE_PAR_DEFAUT),
                    Integer.parseInt(p.getProperty(PORT, String.valueOf(ParametresConnexion.PORT_PAR_DEFAUT))),
                    p.getProperty(BASE, ParametresConnexion.BASE_PAR_DEFAUT),
                    p.getProperty(UTILISATEUR, ParametresConnexion.UTILISATEUR_PAR_DEFAUT),
                    p.getProperty(MOT_DE_PASSE, ""));
        } catch (IOException | IllegalArgumentException e) {
            // Configuration illisible : on garde les valeurs par défaut
        }
        return config;
    }

    public void enregistrer() throws PersistanceException {
        Properties p = new Properties();
        p.setProperty(MODE, mode.name());
        p.setProperty(DOSSIER, dossierFichiers.toString());
        p.setProperty(HOTE, parametres.getHote());
        p.setProperty(PORT, String.valueOf(parametres.getPort()));
        p.setProperty(BASE, parametres.getBase());
        p.setProperty(UTILISATEUR, parametres.getUtilisateur());
        p.setProperty(MOT_DE_PASSE, parametres.getMotDePasse());
        try (Writer redacteur = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            p.store(redacteur, "Configuration du stockage - Election Nepalus");
        } catch (IOException e) {
            throw new PersistanceException("Enregistrement de la configuration impossible : " + e.getMessage(), e);
        }
    }

    /** Vrai si la configuration provient d'un fichier (choix explicite de l'utilisateur). */
    public boolean existait() {
        return existait;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public ParametresConnexion getParametres() {
        return parametres;
    }

    public void setParametres(ParametresConnexion parametres) {
        this.parametres = parametres;
    }

    public StockageFichiers creerStockageFichiers() {
        return new StockageFichiers(dossierFichiers);
    }

    public StockageBaseDeDonnees creerStockageBase() {
        return new StockageBaseDeDonnees(parametres);
    }

    public StockageElection creerStockage() {
        return mode == Mode.FICHIERS ? creerStockageFichiers() : creerStockageBase();
    }
}
