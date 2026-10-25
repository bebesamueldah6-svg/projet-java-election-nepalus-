package ci.inphb.election.modele;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Élection législative de Nepalus : départements et partis enrôlés, résultats
 * du vote et réponses aux questions d'analyse.
 */
public class Election {

    private final Map<Integer, Departement> departements = new TreeMap<>();
    private final Map<String, PartiPolitique> partis = new TreeMap<>();
    private final RegleScrutin regle;

    public Election() {
        this(new ScrutinMajoritaireUnTour());
    }

    public Election(RegleScrutin regle) {
        this.regle = regle;
    }

    public RegleScrutin getRegle() {
        return regle;
    }

    // ------------------------------------------------------------------ enrôlement

    public void enrolerDepartement(Departement departement) throws ElectionException {
        if (departements.containsKey(departement.getNumero())) {
            throw new ElectionException("Le département n° " + departement.getNumero() + " est déjà enrôlé");
        }
        partis.values().forEach(departement::ajouterCandidat);
        departements.put(departement.getNumero(), departement);
    }

    /**
     * Enrôle un parti : il présente un candidat dans chaque département.
     * Les résultats déjà enregistrés deviennent caducs et sont effacés.
     */
    public void enrolerParti(PartiPolitique parti) throws ElectionException {
        if (partis.containsKey(parti.getSigle())) {
            throw new ElectionException("Le parti " + parti.getSigle() + " est déjà enrôlé");
        }
        partis.put(parti.getSigle(), parti);
        departements.values().forEach(d -> {
            d.ajouterCandidat(parti);
            d.reinitialiserResultats();
        });
    }

    public void retirerDepartement(Departement departement) {
        departements.remove(departement.getNumero());
    }

    public void retirerParti(PartiPolitique parti) {
        if (partis.remove(parti.getSigle()) != null) {
            departements.values().forEach(d -> {
                d.retirerCandidat(parti);
                d.reinitialiserResultats();
            });
        }
    }

    public List<Departement> getDepartements() {
        return new ArrayList<>(departements.values());
    }

    public List<PartiPolitique> getPartis() {
        return new ArrayList<>(partis.values());
    }

    public Optional<Departement> trouverDepartement(int numero) {
        return Optional.ofNullable(departements.get(numero));
    }

    public Optional<PartiPolitique> trouverParti(String sigle) {
        return Optional.ofNullable(partis.get(sigle == null ? null : sigle.toUpperCase()));
    }

    public boolean aDesResultats() {
        return departements.values().stream().anyMatch(Departement::isDepouille);
    }

    // ------------------------------------------------------------------ résultats

    /**
     * Enregistre les résultats d'un département. Si la règle de scrutin est violée,
     * les anciens résultats sont restaurés et une exception est levée.
     */
    public void enregistrerResultats(Departement d, int inscrits, int votants,
                                     Map<PartiPolitique, Integer> voixParParti) throws ElectionException {
        int anciensInscrits = d.getInscrits();
        int anciensVotants = d.getVotants();
        Map<Candidat, Integer> anciennesVoix = new LinkedHashMap<>();
        d.getCandidats().forEach(c -> anciennesVoix.put(c, c.getVoix()));
        try {
            d.setParticipation(inscrits, votants);
            for (Candidat c : d.getCandidats()) {
                c.setVoix(voixParParti.getOrDefault(c.getParti(), 0));
            }
            regle.valider(d);
        } catch (ElectionException | IllegalArgumentException e) {
            d.setParticipation(anciensInscrits, anciensVotants);
            anciennesVoix.forEach(Candidat::setVoix);
            throw e instanceof ElectionException ee ? ee : new ElectionException(e.getMessage());
        }
    }

    public void effacerResultats() {
        departements.values().forEach(Departement::reinitialiserResultats);
    }

    // ------------------------------------------------------------------ questions d'analyse

    /** Départements où au moins {@code seuil} (entre 0 et 1) des électeurs inscrits ont voté. */
    public List<Departement> departementsAvecParticipationAuMoins(double seuil) {
        return departements.values().stream()
                .filter(d -> d.getInscrits() > 0 && d.getTauxParticipation() >= seuil)
                .collect(Collectors.toList());
    }

    /** Parti gagnant d'un département (vide tant que le département n'est pas dépouillé). */
    public Optional<PartiPolitique> partiGagnant(Departement d) {
        return regle.designerElu(d).map(Candidat::getParti);
    }

    /** Parti ayant obtenu le plus mauvais score dans un département. */
    public Optional<PartiPolitique> partiDernier(Departement d) {
        return regle.designerDernier(d).map(Candidat::getParti);
    }

    /** Départements où tous les inscrits ont voté, et tous pour le même parti. */
    public List<Departement> departementsUnanimes() {
        return departements.values().stream()
                .filter(Departement::isDepouille)
                .filter(d -> d.getVotants() == d.getInscrits())
                .filter(d -> d.getCandidats().stream().anyMatch(c -> c.getVoix() == d.getVotants()))
                .collect(Collectors.toList());
    }

    /** Taux de participation de l'ensemble du pays : total des votants / total des inscrits. */
    public double tauxParticipationGeneral() {
        long inscrits = departements.values().stream().mapToLong(Departement::getInscrits).sum();
        long votants = departements.values().stream().mapToLong(Departement::getVotants).sum();
        return inscrits == 0 ? 0 : (double) votants / inscrits;
    }

    /** Nombre de sièges (départements gagnés) par parti, tous partis compris. */
    public Map<PartiPolitique, Long> siegesParParti() {
        Map<PartiPolitique, Long> sieges = new LinkedHashMap<>();
        partis.values().forEach(p -> sieges.put(p, 0L));
        departements.values().forEach(d -> partiGagnant(d).ifPresent(p -> sieges.merge(p, 1L, Long::sum)));
        return sieges;
    }

    /** Nombre de sièges à pourvoir : un député par département. */
    public int nombreSieges() {
        return departements.size();
    }

    /** Sièges nécessaires pour la majorité absolue : plus de la moitié des sièges. */
    public int siegesPourMajoriteAbsolue() {
        return departements.size() / 2 + 1;
    }

    /** Parti détenant à lui seul la majorité absolue des sièges, s'il existe. */
    public Optional<PartiPolitique> partiMajoritaire() {
        int seuil = siegesPourMajoriteAbsolue();
        return siegesParParti().entrySet().stream()
                .filter(e -> e.getValue() >= seuil)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public long nombreDepartementsDepouilles() {
        return departements.values().stream().filter(Departement::isDepouille).count();
    }

    public StatistiquesParticipation statistiquesParticipation() {
        return StatistiquesParticipation.calculer(departements.values());
    }

    /** Total national des voix obtenues par chaque parti. */
    public Map<PartiPolitique, Integer> voixParParti() {
        Map<PartiPolitique, Integer> voix = new LinkedHashMap<>();
        partis.values().forEach(p -> voix.put(p, 0));
        departements.values().forEach(d ->
                d.getCandidats().forEach(c -> voix.merge(c.getParti(), c.getVoix(), Integer::sum)));
        return voix;
    }
}
