package ci.inphb.election.modele;

/** Violation d'une règle de l'élection (doublon, résultats incohérents, ...). */
public class ElectionException extends Exception {

    public ElectionException(String message) {
        super(message);
    }
}
