package ci.inphb.election.persistance;

import ci.inphb.election.modele.Departement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Table departement(numero, nom, inscrits, votants). */
public class DepotJdbcDepartements extends DepotJdbc<Departement> {

    public DepotJdbcDepartements(Connection connexion) {
        super(connexion);
    }

    @Override
    protected String getNomTable() {
        return "departement";
    }

    @Override
    protected String requeteSelection() {
        return "SELECT numero, nom, inscrits, votants FROM departement ORDER BY numero";
    }

    @Override
    protected String requeteSuppression() {
        return "DELETE FROM departement";
    }

    @Override
    protected String requeteInsertion() {
        return "INSERT INTO departement (numero, nom, inscrits, votants) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected Departement depuisLigne(ResultSet ligne) throws SQLException {
        Departement d = new Departement(ligne.getInt("numero"), ligne.getString("nom"));
        d.setParticipation(ligne.getInt("inscrits"), ligne.getInt("votants"));
        return d;
    }

    @Override
    protected void renseigner(PreparedStatement requete, Departement d) throws SQLException {
        requete.setInt(1, d.getNumero());
        requete.setString(2, d.getNom());
        requete.setInt(3, d.getInscrits());
        requete.setInt(4, d.getVotants());
    }
}
