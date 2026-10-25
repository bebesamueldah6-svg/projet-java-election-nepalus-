package ci.inphb.election.simulation;

import ci.inphb.election.modele.Candidat;
import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.ElectionException;
import ci.inphb.election.modele.PartiPolitique;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Génère aléatoirement les données du scrutin :
 * inscrits et votants de chaque département (avec une borne supérieure),
 * puis la répartition des votants entre les candidats.
 * Le total des voix est toujours égal au nombre de votants et deux candidats
 * d'un même département n'ont jamais le même nombre de voix.
 */
public class GenerateurResultats {

    private static final int ESSAIS_MAX = 1000;

    private final Random aleatoire;

    public GenerateurResultats() {
        this(new Random());
    }

    /** Constructeur à graine fixe, pour des tirages reproductibles (tests). */
    public GenerateurResultats(Random aleatoire) {
        this.aleatoire = aleatoire;
    }

    /** Génère les résultats de tous les départements de l'élection. */
    public void generer(Election election, int borneMaxInscrits) throws ElectionException {
        if (election.getPartis().isEmpty()) {
            throw new ElectionException("Aucun parti enrôlé : impossible de générer des résultats");
        }
        for (Departement d : election.getDepartements()) {
            genererDepartement(election, d, borneMaxInscrits);
        }
    }

    public void genererDepartement(Election election, Departement d, int borneMaxInscrits) throws ElectionException {
        appliquer(election, tirer(d, borneMaxInscrits));
    }

    /**
     * Résultat tiré au sort pour un département, pas encore appliqué à l'élection.
     * Le tirage ne modifie pas le modèle : il peut être calculé dans un thread d'arrière-plan.
     */
    public static final class Tirage {
        private final Departement departement;
        private final int inscrits;
        private final int votants;
        private final Map<PartiPolitique, Integer> voix;

        Tirage(Departement departement, int inscrits, int votants, Map<PartiPolitique, Integer> voix) {
            this.departement = departement;
            this.inscrits = inscrits;
            this.votants = votants;
            this.voix = voix;
        }

        public Departement getDepartement() {
            return departement;
        }
    }

    /** Tire au sort participation et voix d'un département, sans modifier l'élection. */
    public Tirage tirer(Departement d, int borneMaxInscrits) throws ElectionException {
        List<Candidat> candidats = d.getCandidats();
        if (candidats.isEmpty()) {
            throw new ElectionException("Aucun candidat dans le département " + d.getLibelle());
        }
        int minimumVotants = Math.max(1, minimumVotants(candidats.size()));
        if (borneMaxInscrits < minimumVotants) {
            throw new ElectionException("La borne des inscrits (" + borneMaxInscrits + ") est trop faible : avec "
                    + candidats.size() + " candidats il faut au moins " + minimumVotants
                    + " votants pour éviter les égalités de voix");
        }
        int inscrits = entierEntre(minimumVotants, borneMaxInscrits);
        int votants = entierEntre(minimumVotants, inscrits);
        int[] voix = repartirVoix(votants, candidats.size());

        Map<PartiPolitique, Integer> voixParParti = new HashMap<>();
        for (int i = 0; i < candidats.size(); i++) {
            voixParParti.put(candidats.get(i).getParti(), voix[i]);
        }
        return new Tirage(d, inscrits, votants, voixParParti);
    }

    /** Enregistre un tirage dans l'élection (validé par la règle de scrutin). */
    public void appliquer(Election election, Tirage t) throws ElectionException {
        election.enregistrerResultats(t.departement, t.inscrits, t.votants, t.voix);
    }

    /**
     * Nombre minimal de votants permettant à n candidats d'avoir des scores tous différents :
     * 0 + 1 + ... + (n-1).
     */
    public static int minimumVotants(int nombreCandidats) {
        return nombreCandidats * (nombreCandidats - 1) / 2;
    }

    /**
     * Répartit {@code total} voix entre {@code n} candidats, sans égalité.
     * On tire n-1 points de coupure dans [0, total] ; les écarts successifs donnent les voix.
     * Si aucun tirage sans égalité n'est trouvé, on part de 0, 1, ..., n-1 (mélangés)
     * et on attribue le reste au plus fort, ce qui conserve des scores distincts.
     */
    int[] repartirVoix(int total, int n) {
        if (n == 1) {
            return new int[]{total};
        }
        for (int essai = 0; essai < ESSAIS_MAX; essai++) {
            int[] coupures = new int[n + 1];
            coupures[n] = total;
            for (int i = 1; i < n; i++) {
                coupures[i] = aleatoire.nextInt(total + 1);
            }
            Arrays.sort(coupures);
            int[] voix = new int[n];
            for (int i = 0; i < n; i++) {
                voix[i] = coupures[i + 1] - coupures[i];
            }
            if (sontDistinctes(voix)) {
                return voix;
            }
        }
        int[] voix = new int[n];
        for (int i = 0; i < n; i++) {
            voix[i] = i;
        }
        voix[n - 1] += total - minimumVotants(n);
        melanger(voix);
        return voix;
    }

    private static boolean sontDistinctes(int[] valeurs) {
        Set<Integer> vues = new HashSet<>();
        for (int v : valeurs) {
            if (!vues.add(v)) {
                return false;
            }
        }
        return true;
    }

    private void melanger(int[] t) {
        for (int i = t.length - 1; i > 0; i--) {
            int j = aleatoire.nextInt(i + 1);
            int tmp = t[i];
            t[i] = t[j];
            t[j] = tmp;
        }
    }

    /** Entier uniforme dans [min, max]. */
    private int entierEntre(int min, int max) {
        return min + aleatoire.nextInt(max - min + 1);
    }
}
