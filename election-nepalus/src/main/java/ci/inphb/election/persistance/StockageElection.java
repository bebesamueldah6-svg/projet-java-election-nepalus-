package ci.inphb.election.persistance;

import ci.inphb.election.modele.Election;

/**
 * Lieu où l'élection complète est enregistrée et rechargée.
 * Implémentations : {@link StockageFichiers} (fichiers CSV) et {@link StockageBaseDeDonnees} (MySQL).
 */
public interface StockageElection {

    /** Description lisible, affichée dans la barre d'état. */
    String getDescription();

    /** Vrai si des départements ou des partis ont déjà été enregistrés. */
    boolean contientDesDonnees() throws PersistanceException;

    Election charger() throws PersistanceException;

    void sauvegarder(Election election) throws PersistanceException;
}
