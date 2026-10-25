package ci.inphb.election.persistance;

import ci.inphb.election.modele.Election;

import java.nio.file.Files;
import java.nio.file.Path;

/** Élection enregistrée dans trois fichiers CSV : partis.csv, departements.csv, resultats.csv. */
public class StockageFichiers extends StockageAbstrait {

    private final Path dossier;

    public StockageFichiers(Path dossier) {
        this.dossier = dossier;
    }

    public Path getDossier() {
        return dossier;
    }

    @Override
    public String getDescription() {
        return "fichiers CSV (" + dossier.toAbsolutePath().normalize() + ")";
    }

    @Override
    public boolean contientDesDonnees() {
        return Files.exists(dossier.resolve("partis.csv")) || Files.exists(dossier.resolve("departements.csv"));
    }

    @Override
    public Election charger() throws PersistanceException {
        return lire(new DepotPartis(dossier.resolve("partis.csv")),
                new DepotDepartements(dossier.resolve("departements.csv")),
                election -> new DepotResultats(dossier.resolve("resultats.csv"), election));
    }

    @Override
    public void sauvegarder(Election election) throws PersistanceException {
        ecrire(election,
                new DepotPartis(dossier.resolve("partis.csv")),
                new DepotDepartements(dossier.resolve("departements.csv")),
                new DepotResultats(dossier.resolve("resultats.csv"), election));
    }
}
