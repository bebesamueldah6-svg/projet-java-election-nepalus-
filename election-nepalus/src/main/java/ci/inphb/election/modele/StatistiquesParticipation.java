package ci.inphb.election.modele;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Statistiques descriptives des taux de participation des départements
 * ayant des inscrits : effectif, moyenne, médiane, écart-type, extrêmes, histogramme.
 */
public final class StatistiquesParticipation {

    private final List<Double> taux;   // triés par ordre croissant
    private final double moyenne;
    private final double ecartType;

    private StatistiquesParticipation(List<Double> taux) {
        this.taux = taux;
        this.moyenne = taux.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = taux.stream().mapToDouble(t -> (t - moyenne) * (t - moyenne)).average().orElse(0);
        this.ecartType = Math.sqrt(variance);
    }

    public static StatistiquesParticipation calculer(Collection<Departement> departements) {
        List<Double> taux = departements.stream()
                .filter(d -> d.getInscrits() > 0)
                .map(Departement::getTauxParticipation)
                .sorted()
                .collect(Collectors.toList());
        return new StatistiquesParticipation(taux);
    }

    /** Nombre de départements pris en compte (ceux qui ont des inscrits). */
    public int getEffectif() {
        return taux.size();
    }

    public double getMoyenne() {
        return moyenne;
    }

    public double getMediane() {
        int n = taux.size();
        if (n == 0) {
            return 0;
        }
        return n % 2 == 1 ? taux.get(n / 2) : (taux.get(n / 2 - 1) + taux.get(n / 2)) / 2;
    }

    /** Écart-type de la population des taux. */
    public double getEcartType() {
        return ecartType;
    }

    public double getMinimum() {
        return taux.isEmpty() ? 0 : taux.get(0);
    }

    public double getMaximum() {
        return taux.isEmpty() ? 0 : taux.get(taux.size() - 1);
    }

    /**
     * Répartition des départements en classes de participation de même largeur sur [0 ; 1].
     * La classe k contient les taux de [k/n ; (k+1)/n[, la dernière inclut 100 %.
     */
    public int[] histogramme(int nombreClasses) {
        int[] effectifs = new int[nombreClasses];
        for (double t : taux) {
            int classe = Math.min((int) (t * nombreClasses), nombreClasses - 1);
            effectifs[classe]++;
        }
        return effectifs;
    }
}
