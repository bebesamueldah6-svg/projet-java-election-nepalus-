package ci.inphb.election.persistance;

import ci.inphb.election.modele.PartiPolitique;

import java.nio.file.Path;

/** Fichier partis.csv : sigle;nom */
public class DepotPartis extends DepotFichierCsv<PartiPolitique> {

    public DepotPartis(Path fichier) {
        super(fichier);
    }

    @Override
    protected String getEntete() {
        return "sigle;nom";
    }

    @Override
    protected String[] versChamps(PartiPolitique p) {
        return new String[]{p.getSigle(), p.getNom()};
    }

    @Override
    protected PartiPolitique depuisChamps(String[] champs) {
        verifierNombreChamps(champs, 2);
        return new PartiPolitique(champs[0], champs[1]);
    }
}
