package ci.inphb.election.persistance;

import ci.inphb.election.modele.Candidat;
import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.PartiPolitique;

import java.nio.file.Path;

/**
 * Fichier resultats.csv : departement;parti;voix (une ligne par candidat).
 * Les départements et partis référencés doivent déjà être enrôlés dans l'élection.
 */
public class DepotResultats extends DepotFichierCsv<Candidat> {

    private final Election election;

    public DepotResultats(Path fichier, Election election) {
        super(fichier);
        this.election = election;
    }

    @Override
    protected String getEntete() {
        return "departement;parti;voix";
    }

    @Override
    protected String[] versChamps(Candidat c) {
        return new String[]{
                String.valueOf(c.getDepartement().getNumero()), c.getParti().getSigle(), String.valueOf(c.getVoix())
        };
    }

    /** Renvoie le candidat de l'élection concerné, avec ses voix mises à jour. */
    @Override
    protected Candidat depuisChamps(String[] champs) {
        verifierNombreChamps(champs, 3);
        int numero = Integer.parseInt(champs[0].trim());
        Departement d = election.trouverDepartement(numero)
                .orElseThrow(() -> new IllegalArgumentException("département inconnu : " + numero));
        PartiPolitique p = election.trouverParti(champs[1].trim())
                .orElseThrow(() -> new IllegalArgumentException("parti inconnu : " + champs[1]));
        Candidat c = d.getCandidat(p)
                .orElseThrow(() -> new IllegalArgumentException("aucun candidat " + p.getSigle() + " dans " + d.getNom()));
        c.setVoix(Integer.parseInt(champs[2].trim()));
        return c;
    }
}
