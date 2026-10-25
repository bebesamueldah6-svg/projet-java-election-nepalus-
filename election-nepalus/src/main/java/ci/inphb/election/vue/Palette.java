package ci.inphb.election.vue;

/**
 * Couleurs des partis : chaque parti reçoit une couleur selon sa position dans la liste
 * des partis enrôlés, utilisée partout (pastilles, camembert, barres) pour rester cohérent.
 */
public final class Palette {

    private static final String[] COULEURS = {
            "#1E88E5", // bleu
            "#E53935", // rouge
            "#43A047", // vert
            "#FB8C00", // orange
            "#8E24AA", // violet
            "#00ACC1", // cyan
            "#C0CA33", // olive
            "#6D4C41", // brun
            "#D81B60", // rose
            "#3949AB"  // indigo
    };

    /** Couleur neutre (parti inconnu, département non dépouillé). */
    public static final String NEUTRE = "#B0BEC5";

    private Palette() {
    }

    public static String couleur(int index) {
        return index < 0 ? NEUTRE : COULEURS[index % COULEURS.length];
    }
}
