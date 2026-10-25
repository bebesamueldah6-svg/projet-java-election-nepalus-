package ci.inphb.election.persistance;

/** Paramètres d'accès à la base MySQL / MariaDB (valeurs par défaut : XAMPP ou WAMP en local). */
public class ParametresConnexion {

    public static final String HOTE_PAR_DEFAUT = "localhost";
    public static final int PORT_PAR_DEFAUT = 3306;
    public static final String BASE_PAR_DEFAUT = "projets_java";
    public static final String UTILISATEUR_PAR_DEFAUT = "root";

    private final String hote;
    private final int port;
    private final String base;
    private final String utilisateur;
    private final String motDePasse;

    public ParametresConnexion(String hote, int port, String base, String utilisateur, String motDePasse) {
        if (hote == null || hote.isBlank() || base == null || base.isBlank()) {
            throw new IllegalArgumentException("L'hôte et le nom de la base sont obligatoires");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port invalide : " + port);
        }
        this.hote = hote.trim();
        this.port = port;
        this.base = base.trim();
        this.utilisateur = utilisateur == null ? "" : utilisateur.trim();
        this.motDePasse = motDePasse == null ? "" : motDePasse;
    }

    public static ParametresConnexion parDefaut() {
        return new ParametresConnexion(HOTE_PAR_DEFAUT, PORT_PAR_DEFAUT, BASE_PAR_DEFAUT, UTILISATEUR_PAR_DEFAUT, "");
    }

    /** URL JDBC de la forme jdbc:mysql://machine:port/base?propriétés */
    public String getUrl() {
        return "jdbc:mysql://" + hote + ":" + port + "/" + base
                + "?useUnicode=true&characterEncoding=UTF-8&connectTimeout=5000";
    }

    public String getHote() {
        return hote;
    }

    public int getPort() {
        return port;
    }

    public String getBase() {
        return base;
    }

    public String getUtilisateur() {
        return utilisateur;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    @Override
    public String toString() {
        return base + " sur " + hote + ":" + port;
    }
}
