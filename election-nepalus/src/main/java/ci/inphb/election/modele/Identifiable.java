package ci.inphb.election.modele;

/**
 * Tout objet persistant de l'application est repéré par un code unique
 * (numéro de département, sigle de parti, ...).
 */
public interface Identifiable {

    String getCode();
}
