package ci.inphb.election.modele;

/**
 * Candidat présenté par un parti dans un département, avec le nombre de voix obtenues.
 * Chaque parti présente exactement un candidat par département.
 */
public class Candidat implements Identifiable {

    private final Departement departement;
    private final PartiPolitique parti;
    private int voix;

    public Candidat(Departement departement, PartiPolitique parti) {
        this.departement = departement;
        this.parti = parti;
    }

    public Departement getDepartement() {
        return departement;
    }

    public PartiPolitique getParti() {
        return parti;
    }

    public int getVoix() {
        return voix;
    }

    public void setVoix(int voix) {
        if (voix < 0) {
            throw new IllegalArgumentException("Le nombre de voix ne peut pas être négatif");
        }
        this.voix = voix;
    }

    /** Part des suffrages du département obtenue par ce candidat, entre 0 et 1. */
    public double getPourcentage() {
        int votants = departement.getVotants();
        return votants == 0 ? 0 : (double) voix / votants;
    }

    @Override
    public String getCode() {
        return departement.getCode() + "-" + parti.getCode();
    }

    @Override
    public String toString() {
        return "Candidat " + parti.getSigle() + " (" + departement.getNom() + ") : " + voix + " voix";
    }
}
