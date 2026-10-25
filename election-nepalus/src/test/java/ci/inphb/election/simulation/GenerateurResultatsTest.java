package ci.inphb.election.simulation;

import ci.inphb.election.modele.Departement;
import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.ElectionException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateurResultatsTest {

    @Test
    void generationRespecteLesReglesSurDeNombreuxTirages() throws ElectionException {
        for (int graine = 0; graine < 200; graine++) {
            Election election = DonneesDemo.creer();
            new GenerateurResultats(new Random(graine)).generer(election, 5000);
            for (Departement d : election.getDepartements()) {
                assertTrue(d.isDepouille());
                assertTrue(d.getVotants() <= d.getInscrits());
                assertTrue(d.getInscrits() <= 5000);
                election.getRegle().valider(d);
            }
        }
    }

    @Test
    void repartitionAvecPeuDeVoixResteDistincte() {
        GenerateurResultats g = new GenerateurResultats(new Random(1));
        for (int i = 0; i < 100; i++) {
            int[] voix = g.repartirVoix(10, 5); // minimum exact : 0+1+2+3+4
            assertEquals(10, Arrays.stream(voix).sum());
            assertEquals(5, Arrays.stream(voix).distinct().count());
        }
    }

    @Test
    void borneTropFaibleRefusee() {
        Election election = DonneesDemo.creer(); // 5 partis : il faut au moins 10 votants
        assertThrows(ElectionException.class, () -> new GenerateurResultats().generer(election, 9));
    }
}
