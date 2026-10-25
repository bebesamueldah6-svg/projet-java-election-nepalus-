package ci.inphb.election.persistance;

import ci.inphb.election.modele.Identifiable;

import java.util.Collection;
import java.util.List;

/**
 * Stockage persistant d'objets identifiables. Deux familles d'implémentations :
 * fichiers CSV ({@link DepotFichierCsv}) et base de données ({@link DepotJdbc}).
 *
 * @param <T> type des objets stockés
 */
public interface Depot<T extends Identifiable> {

    List<T> chargerTout() throws PersistanceException;

    /** Remplace tout le contenu stocké par les éléments fournis. */
    void sauvegarderTout(Collection<? extends T> elements) throws PersistanceException;
}
