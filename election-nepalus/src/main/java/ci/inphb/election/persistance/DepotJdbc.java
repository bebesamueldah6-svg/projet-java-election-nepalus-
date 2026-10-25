package ci.inphb.election.persistance;

import ci.inphb.election.modele.Identifiable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dépôt stocké dans une table de base de données relationnelle, via JDBC.
 * Les sous-classes fournissent les requêtes SQL et la conversion entre un objet
 * et une ligne de table. La connexion (et donc la transaction) est gérée par l'appelant.
 */
public abstract class DepotJdbc<T extends Identifiable> implements Depot<T> {

    private final Connection connexion;

    protected DepotJdbc(Connection connexion) {
        this.connexion = connexion;
    }

    /** Requête SELECT renvoyant toutes les lignes de la table. */
    protected abstract String requeteSelection();

    /** Requête vidant la table. */
    protected abstract String requeteSuppression();

    /** Requête INSERT paramétrée (avec des « ? »). */
    protected abstract String requeteInsertion();

    /** Construit un objet à partir de la ligne courante du résultat. */
    protected abstract T depuisLigne(ResultSet ligne) throws SQLException;

    /** Renseigne les paramètres de la requête d'insertion pour un objet. */
    protected abstract void renseigner(PreparedStatement requete, T element) throws SQLException;

    protected abstract String getNomTable();

    @Override
    public List<T> chargerTout() throws PersistanceException {
        List<T> elements = new ArrayList<>();
        try (Statement instruction = connexion.createStatement();
             ResultSet resultat = instruction.executeQuery(requeteSelection())) {
            while (resultat.next()) {
                elements.add(depuisLigne(resultat));
            }
        } catch (SQLException e) {
            throw new PersistanceException("Lecture de la table « " + getNomTable() + " » impossible : " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new PersistanceException("Table « " + getNomTable() + " » : donnée invalide : " + e.getMessage(), e);
        }
        return elements;
    }

    @Override
    public void sauvegarderTout(Collection<? extends T> elements) throws PersistanceException {
        try (Statement instruction = connexion.createStatement();
             PreparedStatement insertion = connexion.prepareStatement(requeteInsertion())) {
            instruction.executeUpdate(requeteSuppression());
            for (T element : elements) {
                renseigner(insertion, element);
                insertion.addBatch();
            }
            insertion.executeBatch();
        } catch (SQLException e) {
            throw new PersistanceException("Écriture dans la table « " + getNomTable() + " » impossible : " + e.getMessage(), e);
        }
    }
}
