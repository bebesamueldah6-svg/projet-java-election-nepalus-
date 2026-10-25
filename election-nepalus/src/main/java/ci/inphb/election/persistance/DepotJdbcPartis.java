package ci.inphb.election.persistance;

import ci.inphb.election.modele.PartiPolitique;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Table parti(sigle, nom). */
public class DepotJdbcPartis extends DepotJdbc<PartiPolitique> {

    public DepotJdbcPartis(Connection connexion) {
        super(connexion);
    }

    @Override
    protected String getNomTable() {
        return "parti";
    }

    @Override
    protected String requeteSelection() {
        return "SELECT sigle, nom FROM parti ORDER BY sigle";
    }

    @Override
    protected String requeteSuppression() {
        return "DELETE FROM parti";
    }

    @Override
    protected String requeteInsertion() {
        return "INSERT INTO parti (sigle, nom) VALUES (?, ?)";
    }

    @Override
    protected PartiPolitique depuisLigne(ResultSet ligne) throws SQLException {
        return new PartiPolitique(ligne.getString("sigle"), ligne.getString("nom"));
    }

    @Override
    protected void renseigner(PreparedStatement requete, PartiPolitique p) throws SQLException {
        requete.setString(1, p.getSigle());
        requete.setString(2, p.getNom());
    }
}
