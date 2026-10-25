package ci.inphb.election.modele;

import java.util.Optional;

/**
 * Règle électorale appliquée dans chaque département : elle valide des résultats
 * et désigne l'élu. Une autre règle (scrutin à deux tours, ...) pourrait être
 * substituée sans modifier le reste de l'application.
 */
public interface RegleScrutin {

    /** Lève une exception si les résultats du département violent la règle. */
    void valider(Departement departement) throws ElectionException;

    /** Candidat élu, vide si le département n'est pas dépouillé. */
    Optional<Candidat> designerElu(Departement departement);

    /** Candidat ayant obtenu le plus mauvais score, vide si le département n'est pas dépouillé. */
    Optional<Candidat> designerDernier(Departement departement);
}
