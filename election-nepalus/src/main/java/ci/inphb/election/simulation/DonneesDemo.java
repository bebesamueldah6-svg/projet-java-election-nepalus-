package ci.inphb.election.simulation;

import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.ElectionException;
import ci.inphb.election.modele.PartiPolitique;

/** Jeu de données fictif de Nepalus, pour démarrer rapidement une démonstration. */
public final class DonneesDemo {

    private static final String[] DEPARTEMENTS = {
            "Kalinor", "Mirevalle", "Ostrava-Sud", "Pélorie", "Banzéa", "Torvane",
            "Lunastre", "Kossé", "Varnheim", "Doréal", "Sambaro", "Ivrelune"
    };

    private static final String[][] PARTIS = {
            {"PUN", "Parti de l'Unité Nepalaise"},
            {"RDP", "Rassemblement des Démocrates Progressistes"},
            {"MVN", "Mouvement Vert de Nepalus"},
            {"ALR", "Alliance Libérale Républicaine"},
            {"UPS", "Union pour le Progrès Social"}
    };

    private DonneesDemo() {
    }

    public static Election creer() {
        Election election = new Election();
        try {
            for (String[] p : PARTIS) {
                election.enrolerParti(new PartiPolitique(p[0], p[1]));
            }
            for (int i = 0; i < DEPARTEMENTS.length; i++) {
                election.enrolerDepartement(new Departement(i + 1, DEPARTEMENTS[i]));
            }
        } catch (ElectionException e) {
            throw new IllegalStateException("Données de démonstration incohérentes", e);
        }
        return election;
    }
}
