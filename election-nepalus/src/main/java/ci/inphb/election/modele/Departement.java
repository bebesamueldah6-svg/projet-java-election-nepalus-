package ci.inphb.election.modele;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Département de Nepalus : circonscription électorale qui élit un député.
 * Il porte ses chiffres de participation et la liste des candidats (un par parti).
 */
public class Departement extends Entite {

    private final int numero;
    private int inscrits;
    private int votants;
    private final List<Candidat> candidats = new ArrayList<>();

    public Departement(int numero, String nom) {
        super(nom);
        if (numero <= 0) {
            throw new IllegalArgumentException("Le numéro de département doit être positif");
        }
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    /** Code sur 3 chiffres pour que l'ordre alphabétique suive l'ordre numérique. */
    @Override
    public String getCode() {
        return String.format("%03d", numero);
    }

    @Override
    public String getLibelle() {
        return numero + " - " + getNom();
    }

    public int getInscrits() {
        return inscrits;
    }

    public int getVotants() {
        return votants;
    }

    public void setParticipation(int inscrits, int votants) {
        if (inscrits < 0 || votants < 0) {
            throw new IllegalArgumentException("Les effectifs ne peuvent pas être négatifs");
        }
        if (votants > inscrits) {
            throw new IllegalArgumentException("Le nombre de votants dépasse le nombre d'inscrits");
        }
        this.inscrits = inscrits;
        this.votants = votants;
    }

    /** Taux de participation entre 0 et 1. */
    public double getTauxParticipation() {
        return inscrits == 0 ? 0 : (double) votants / inscrits;
    }

    public List<Candidat> getCandidats() {
        return Collections.unmodifiableList(candidats);
    }

    public Optional<Candidat> getCandidat(PartiPolitique parti) {
        return candidats.stream().filter(c -> c.getParti().equals(parti)).findFirst();
    }

    void ajouterCandidat(PartiPolitique parti) {
        if (getCandidat(parti).isEmpty()) {
            candidats.add(new Candidat(this, parti));
        }
    }

    void retirerCandidat(PartiPolitique parti) {
        candidats.removeIf(c -> c.getParti().equals(parti));
    }

    public int getTotalVoix() {
        return candidats.stream().mapToInt(Candidat::getVoix).sum();
    }

    /** Vrai lorsque des résultats ont été enregistrés pour ce département. */
    public boolean isDepouille() {
        return votants > 0 && getTotalVoix() == votants;
    }

    /** Efface participation et voix (utilisé quand la liste des partis change). */
    public void reinitialiserResultats() {
        inscrits = 0;
        votants = 0;
        candidats.forEach(c -> c.setVoix(0));
    }
}
