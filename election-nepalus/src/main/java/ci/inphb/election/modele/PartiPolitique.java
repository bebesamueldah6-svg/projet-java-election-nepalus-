package ci.inphb.election.modele;

/** Parti politique, identifié par son sigle. */
public class PartiPolitique extends Entite {

    private final String sigle;

    public PartiPolitique(String sigle, String nom) {
        super(nom);
        this.sigle = verifierTexte(sigle, "sigle").toUpperCase();
    }

    public String getSigle() {
        return sigle;
    }

    @Override
    public String getCode() {
        return sigle;
    }

    @Override
    public String getLibelle() {
        return sigle + " - " + getNom();
    }
}
