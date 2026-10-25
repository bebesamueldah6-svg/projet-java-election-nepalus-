package ci.inphb.election.persistance;

import ci.inphb.election.modele.Candidat;
import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.PartiPolitique;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Table resultat(numero_departement, sigle_parti, voix) : une ligne par candidat.
 * Les départements et partis référencés doivent déjà être enrôlés dans l'élection.
 */
public class DepotJdbcResultats extends DepotJdbc<Candidat> {

    private final Election election;

    public DepotJdbcResultats(Connection connexion, Election election) {
        super(connexion);
        this.election = election;
    }

    @Override
    protected String getNomTable() {
        return "resultat";
    }

    @Override
    protected String requeteSelection() {
        return "SELECT numero_departement, sigle_parti, voix FROM resultat";
    }

    @Override
    protected String requeteSuppression() {
        return "DELETE FROM resultat";
    }

    @Override
    protected String requeteInsertion() {
        return "INSERT INTO resultat (numero_departement, sigle_parti, voix) VALUES (?, ?, ?)";
    }

    /** Renvoie le candidat de l'élection concerné, avec ses voix mises à jour. */
    @Override
    protected Candidat depuisLigne(ResultSet ligne) throws SQLException {
        int numero = ligne.getInt("numero_departement");
        String sigle = ligne.getString("sigle_parti");
        Departement d = election.trouverDepartement(numero)
                .orElseThrow(() -> new IllegalArgumentException("département inconnu : " + numero));
        PartiPolitique p = election.trouverParti(sigle)
                .orElseThrow(() -> new IllegalArgumentException("parti inconnu : " + sigle));
        Candidat c = d.getCandidat(p)
                .orElseThrow(() -> new IllegalArgumentException("aucun candidat " + sigle + " dans " + d.getNom()));
        c.setVoix(ligne.getInt("voix"));
        return c;
    }

    @Override
    protected void renseigner(PreparedStatement requete, Candidat c) throws SQLException {
        requete.setInt(1, c.getDepartement().getNumero());
        requete.setString(2, c.getParti().getSigle());
        requete.setInt(3, c.getVoix());
    }
}
