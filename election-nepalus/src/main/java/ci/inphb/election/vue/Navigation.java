package ci.inphb.election.vue;

import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.PartiPolitique;

/**
 * Déplacements d'un onglet à l'autre déclenchés par l'utilisateur (clic sur un indicateur,
 * double-clic sur une ligne...). Implémentée par le contrôleur principal.
 */
public interface Navigation {

    int ONGLET_DEPARTEMENTS = 0;
    int ONGLET_PARTIS = 1;
    int ONGLET_RESULTATS = 2;
    int ONGLET_ANALYSES = 3;

    void afficherOnglet(int index);

    /** Ouvre l'onglet des résultats sur ce département. */
    void afficherDepartement(Departement departement);

    /** Ouvre les analyses filtrées sur les départements gagnés par ce parti. */
    void afficherVictoires(PartiPolitique parti);
}
