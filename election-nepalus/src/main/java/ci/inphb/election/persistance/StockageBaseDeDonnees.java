package ci.inphb.election.persistance;

import ci.inphb.election.modele.Election;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Élection enregistrée dans une base MySQL / MariaDB via JDBC.
 * Les tables sont créées au premier accès. L'enregistrement se fait dans une
 * transaction : en cas d'erreur, la base reste dans son état précédent.
 */
public class StockageBaseDeDonnees extends StockageAbstrait {

    static final String PILOTE = "com.mysql.cj.jdbc.Driver";

    private static final String[] CREATION_TABLES = {
            "CREATE TABLE IF NOT EXISTS parti ("
                    + " sigle VARCHAR(20) NOT NULL PRIMARY KEY,"
                    + " nom VARCHAR(150) NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS departement ("
                    + " numero INT NOT NULL PRIMARY KEY,"
                    + " nom VARCHAR(150) NOT NULL,"
                    + " inscrits INT NOT NULL DEFAULT 0,"
                    + " votants INT NOT NULL DEFAULT 0,"
                    + " CONSTRAINT chk_votants CHECK (votants >= 0 AND votants <= inscrits)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS resultat ("
                    + " numero_departement INT NOT NULL,"
                    + " sigle_parti VARCHAR(20) NOT NULL,"
                    + " voix INT NOT NULL DEFAULT 0,"
                    + " PRIMARY KEY (numero_departement, sigle_parti),"
                    + " CONSTRAINT fk_resultat_departement FOREIGN KEY (numero_departement)"
                    + "   REFERENCES departement(numero) ON DELETE CASCADE,"
                    + " CONSTRAINT fk_resultat_parti FOREIGN KEY (sigle_parti)"
                    + "   REFERENCES parti(sigle) ON DELETE CASCADE,"
                    + " CONSTRAINT chk_voix CHECK (voix >= 0)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
    };

    private final ParametresConnexion parametres;

    public StockageBaseDeDonnees(ParametresConnexion parametres) {
        this.parametres = parametres;
    }

    public ParametresConnexion getParametres() {
        return parametres;
    }

    @Override
    public String getDescription() {
        return "base MySQL « " + parametres.getBase() + " » (" + parametres.getHote() + ":" + parametres.getPort() + ")";
    }

    /** Ouvre une connexion : chargement du pilote puis DriverManager.getConnection(url, utilisateur, mot de passe). */
    Connection ouvrirConnexion() throws PersistanceException {
        try {
            Class.forName(PILOTE);
            return DriverManager.getConnection(parametres.getUrl(), parametres.getUtilisateur(), parametres.getMotDePasse());
        } catch (ClassNotFoundException e) {
            throw new PersistanceException("Pilote JDBC introuvable : " + PILOTE, e);
        } catch (SQLException e) {
            throw new PersistanceException("Connexion impossible à " + parametres + " : " + e.getMessage(), e);
        }
    }

    private static void creerTablesSiAbsentes(Connection connexion) throws SQLException {
        try (Statement instruction = connexion.createStatement()) {
            for (String requete : CREATION_TABLES) {
                instruction.executeUpdate(requete);
            }
        }
    }

    /** Vérifie l'accès à la base et décrit le serveur (méta-données de la base). */
    public String testerConnexion() throws PersistanceException {
        try (Connection connexion = ouvrirConnexion()) {
            DatabaseMetaData meta = connexion.getMetaData();
            return "Connexion réussie à « " + parametres.getBase() + " »\n"
                    + "Serveur : " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion() + "\n"
                    + "Pilote : " + meta.getDriverName() + " " + meta.getDriverVersion() + "\n"
                    + "Utilisateur : " + meta.getUserName();
        } catch (SQLException e) {
            throw new PersistanceException("Lecture des informations du serveur impossible : " + e.getMessage(), e);
        }
    }

    @Override
    public boolean contientDesDonnees() throws PersistanceException {
        try (Connection connexion = ouvrirConnexion()) {
            creerTablesSiAbsentes(connexion);
            try (Statement instruction = connexion.createStatement();
                 ResultSet resultat = instruction.executeQuery(
                         "SELECT (SELECT COUNT(*) FROM parti) + (SELECT COUNT(*) FROM departement)")) {
                return resultat.next() && resultat.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new PersistanceException("Interrogation de la base impossible : " + e.getMessage(), e);
        }
    }

    @Override
    public Election charger() throws PersistanceException {
        try (Connection connexion = ouvrirConnexion()) {
            creerTablesSiAbsentes(connexion);
            return lire(new DepotJdbcPartis(connexion),
                    new DepotJdbcDepartements(connexion),
                    election -> new DepotJdbcResultats(connexion, election));
        } catch (SQLException e) {
            throw new PersistanceException("Chargement depuis la base impossible : " + e.getMessage(), e);
        }
    }

    @Override
    public void sauvegarder(Election election) throws PersistanceException {
        try (Connection connexion = ouvrirConnexion()) {
            creerTablesSiAbsentes(connexion);
            connexion.setAutoCommit(false);
            try {
                // Vider « parti » et « departement » supprime aussi les résultats (ON DELETE CASCADE)
                ecrire(election,
                        new DepotJdbcPartis(connexion),
                        new DepotJdbcDepartements(connexion),
                        new DepotJdbcResultats(connexion, election));
                connexion.commit();
            } catch (PersistanceException | RuntimeException e) {
                connexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistanceException("Enregistrement dans la base impossible : " + e.getMessage(), e);
        }
    }
}
