package ci.inphb.election.modele;

import java.util.Objects;

/**
 * Entité enrôlée dans l'élection : elle possède un code unique et un nom.
 * Départements et partis politiques en héritent.
 */
public abstract class Entite implements Identifiable, Comparable<Entite> {

    private final String nom;

    protected Entite(String nom) {
        this.nom = verifierTexte(nom, "nom");
    }

    public String getNom() {
        return nom;
    }

    /** Libellé lisible de l'entité, affiché dans les listes de l'interface. */
    public abstract String getLibelle();

    @Override
    public int compareTo(Entite autre) {
        return getCode().compareTo(autre.getCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return getCode().equals(((Entite) o).getCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getCode());
    }

    @Override
    public String toString() {
        return getLibelle();
    }

    /** Refuse les textes vides et le caractère ';' utilisé comme séparateur dans les fichiers. */
    protected static String verifierTexte(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ « " + champ + " » est obligatoire");
        }
        if (valeur.contains(";")) {
            throw new IllegalArgumentException("Le champ « " + champ + " » ne doit pas contenir de ';'");
        }
        return valeur.trim();
    }
}
