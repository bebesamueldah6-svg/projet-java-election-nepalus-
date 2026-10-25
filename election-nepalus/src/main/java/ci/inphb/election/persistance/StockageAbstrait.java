package ci.inphb.election.persistance;

import ci.inphb.election.modele.Candidat;
import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.ElectionException;
import ci.inphb.election.modele.PartiPolitique;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lecture et écriture d'une élection à partir de trois dépôts (partis, départements, résultats),
 * quel que soit leur support. L'ordre compte : les résultats référencent départements et partis.
 */
public abstract class StockageAbstrait implements StockageElection {

    /**
     * Reconstruit l'élection. Les résultats chargés sont validés par la règle de scrutin.
     *
     * @param resultats fabrique du dépôt des résultats, qui a besoin de l'élection en cours de construction
     */
    protected static Election lire(Depot<PartiPolitique> partis, Depot<Departement> departements,
                                   Function<Election, Depot<Candidat>> resultats) throws PersistanceException {
        Election election = new Election();
        try {
            for (PartiPolitique p : partis.chargerTout()) {
                election.enrolerParti(p);
            }
            for (Departement d : departements.chargerTout()) {
                election.enrolerDepartement(d);
            }
            resultats.apply(election).chargerTout();
            for (Departement d : election.getDepartements()) {
                if (d.getVotants() > 0 || d.getTotalVoix() > 0) {
                    election.getRegle().valider(d);
                }
            }
        } catch (ElectionException e) {
            throw new PersistanceException("Données enregistrées incohérentes : " + e.getMessage(), e);
        }
        return election;
    }

    protected static void ecrire(Election election, Depot<PartiPolitique> partis, Depot<Departement> departements,
                                 Depot<Candidat> resultats) throws PersistanceException {
        partis.sauvegarderTout(election.getPartis());
        departements.sauvegarderTout(election.getDepartements());
        List<Candidat> candidats = election.getDepartements().stream()
                .flatMap(d -> d.getCandidats().stream())
                .collect(Collectors.toList());
        resultats.sauvegarderTout(candidats);
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
