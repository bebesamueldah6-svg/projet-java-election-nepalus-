package ci.inphb.election.modele;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Scrutin majoritaire à un tour de Nepalus :
 * pas de bulletin blanc (toutes les voix vont à un candidat),
 * pas d'égalité de voix entre deux candidats d'un même département,
 * le candidat ayant le plus de voix est élu.
 */
public class ScrutinMajoritaireUnTour implements RegleScrutin {

    private static final Comparator<Candidat> PAR_VOIX = Comparator.comparingInt(Candidat::getVoix);

    @Override
    public void valider(Departement d) throws ElectionException {
        if (d.getCandidats().isEmpty()) {
            throw new ElectionException("Aucun candidat dans le département " + d.getLibelle());
        }
        if (d.getVotants() > d.getInscrits()) {
            throw new ElectionException("Département " + d.getLibelle() + " : plus de votants que d'inscrits");
        }
        if (d.getTotalVoix() != d.getVotants()) {
            throw new ElectionException("Département " + d.getLibelle() + " : le total des voix ("
                    + d.getTotalVoix() + ") doit être égal au nombre de votants (" + d.getVotants()
                    + "), le vote blanc n'étant pas autorisé");
        }
        Set<Integer> voixDejaVues = new HashSet<>();
        for (Candidat c : d.getCandidats()) {
            if (!voixDejaVues.add(c.getVoix())) {
                throw new ElectionException("Département " + d.getLibelle()
                        + " : deux candidats ne peuvent pas obtenir le même nombre de voix (" + c.getVoix() + ")");
            }
        }
    }

    @Override
    public Optional<Candidat> designerElu(Departement d) {
        return d.isDepouille() ? d.getCandidats().stream().max(PAR_VOIX) : Optional.empty();
    }

    @Override
    public Optional<Candidat> designerDernier(Departement d) {
        return d.isDepouille() ? d.getCandidats().stream().min(PAR_VOIX) : Optional.empty();
    }
}
