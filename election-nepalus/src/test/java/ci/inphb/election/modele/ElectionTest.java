package ci.inphb.election.modele;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectionTest {

    private Election election;
    private PartiPolitique a;
    private PartiPolitique b;
    private PartiPolitique c;
    private Departement d1;
    private Departement d2;

    @BeforeEach
    void preparer() throws ElectionException {
        election = new Election();
        a = new PartiPolitique("AAA", "Parti A");
        b = new PartiPolitique("BBB", "Parti B");
        c = new PartiPolitique("CCC", "Parti C");
        election.enrolerParti(a);
        election.enrolerParti(b);
        election.enrolerParti(c);
        d1 = new Departement(1, "Un");
        d2 = new Departement(2, "Deux");
        election.enrolerDepartement(d1);
        election.enrolerDepartement(d2);
    }

    @Test
    void chaquePartiPresenteUnCandidatParDepartement() throws ElectionException {
        assertEquals(3, d1.getCandidats().size());
        Departement d3 = new Departement(3, "Trois");
        election.enrolerDepartement(d3);
        assertEquals(3, d3.getCandidats().size());
    }

    @Test
    void doublonsRefuses() {
        assertThrows(ElectionException.class, () -> election.enrolerParti(new PartiPolitique("aaa", "Autre")));
        assertThrows(ElectionException.class, () -> election.enrolerDepartement(new Departement(1, "Copie")));
    }

    @Test
    void resultatsValidesEnregistres() throws ElectionException {
        election.enregistrerResultats(d1, 100, 60, Map.of(a, 30, b, 20, c, 10));
        assertTrue(d1.isDepouille());
        assertEquals(a, election.partiGagnant(d1).orElseThrow());
        assertEquals(c, election.partiDernier(d1).orElseThrow());
    }

    @Test
    void voteBlancInterdit() {
        assertThrows(ElectionException.class,
                () -> election.enregistrerResultats(d1, 100, 60, Map.of(a, 30, b, 20, c, 5)));
        assertFalse(d1.isDepouille(), "les anciens résultats doivent être restaurés");
        assertEquals(0, d1.getVotants());
    }

    @Test
    void egaliteInterdite() {
        assertThrows(ElectionException.class,
                () -> election.enregistrerResultats(d1, 100, 60, Map.of(a, 25, b, 25, c, 10)));
    }

    @Test
    void plusDeVotantsQueDInscritsInterdit() {
        assertThrows(ElectionException.class,
                () -> election.enregistrerResultats(d1, 50, 60, Map.of(a, 30, b, 20, c, 10)));
    }

    @Test
    void participationAuMoinsCinquantePourCent() throws ElectionException {
        election.enregistrerResultats(d1, 100, 50, Map.of(a, 30, b, 15, c, 5));
        election.enregistrerResultats(d2, 100, 49, Map.of(a, 30, b, 15, c, 4));
        assertEquals(List.of(d1), election.departementsAvecParticipationAuMoins(0.5));
        assertEquals(0.495, election.tauxParticipationGeneral(), 1e-9);
    }

    @Test
    void unanimiteImpossibleAvecTroisPartis() {
        // Tous les votes pour A laisse B et C à égalité (0 voix) : rejeté par la règle.
        assertThrows(ElectionException.class,
                () -> election.enregistrerResultats(d1, 40, 40, Map.of(a, 40, b, 0, c, 0)));
    }

    @Test
    void unanimiteDetecteeAvecDeuxPartis() throws ElectionException {
        election.retirerParti(c);
        election.enregistrerResultats(d1, 40, 40, Map.of(a, 40, b, 0));
        election.enregistrerResultats(d2, 40, 39, Map.of(a, 39, b, 0));
        assertEquals(List.of(d1), election.departementsUnanimes());
    }

    @Test
    void siegesParParti() throws ElectionException {
        election.enregistrerResultats(d1, 100, 60, Map.of(a, 30, b, 20, c, 10));
        election.enregistrerResultats(d2, 100, 60, Map.of(a, 10, b, 20, c, 30));
        Map<PartiPolitique, Long> sieges = election.siegesParParti();
        assertEquals(1L, sieges.get(a));
        assertEquals(0L, sieges.get(b));
        assertEquals(1L, sieges.get(c));
    }

    @Test
    void enrolerUnPartiEffaceLesResultats() throws ElectionException {
        election.enregistrerResultats(d1, 100, 60, Map.of(a, 30, b, 20, c, 10));
        election.enrolerParti(new PartiPolitique("DDD", "Parti D"));
        assertFalse(d1.isDepouille());
        assertEquals(4, d1.getCandidats().size());
    }
}
