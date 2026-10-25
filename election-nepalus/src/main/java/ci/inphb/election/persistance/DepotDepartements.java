package ci.inphb.election.persistance;

import ci.inphb.election.modele.Departement;

import java.nio.file.Path;

/** Fichier departements.csv : numero;nom;inscrits;votants */
public class DepotDepartements extends DepotFichierCsv<Departement> {

    public DepotDepartements(Path fichier) {
        super(fichier);
    }

    @Override
    protected String getEntete() {
        return "numero;nom;inscrits;votants";
    }

    @Override
    protected String[] versChamps(Departement d) {
        return new String[]{
                String.valueOf(d.getNumero()), d.getNom(),
                String.valueOf(d.getInscrits()), String.valueOf(d.getVotants())
        };
    }

    @Override
    protected Departement depuisChamps(String[] champs) {
        verifierNombreChamps(champs, 4);
        Departement d = new Departement(Integer.parseInt(champs[0].trim()), champs[1]);
        d.setParticipation(Integer.parseInt(champs[2].trim()), Integer.parseInt(champs[3].trim()));
        return d;
    }
}
