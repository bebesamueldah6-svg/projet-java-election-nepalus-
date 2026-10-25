package ci.inphb.election.persistance;

/** Échec de lecture ou d'écriture des données (fichier illisible, base inaccessible, données incohérentes...). */
public class PersistanceException extends Exception {

    public PersistanceException(String message) {
        super(message);
    }

    public PersistanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
