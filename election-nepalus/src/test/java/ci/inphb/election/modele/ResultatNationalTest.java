package ci.inphb.election.modele;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultatNationalTest {

    private static Election electionAvecTroisDepartements(PartiPolitique a, PartiPolitique b) throws ElectionException {
        Election e = new Election();
        e.enrolerParti(a);
        e.enrolerParti(b);
        for (int i = 1; i <= 3; i++) {
            e.enrolerDepartement(new Departement(i, "D" + i));
        }
        return e;
    }

    @Test
    void majoriteAbsolue() throws ElectionException {
        PartiPolitique a = new PartiPolitique("AAA", "A");
        PartiPolitique b = new PartiPolitique("BBB", "B");
        Election e = electionAvecTroisDepartements(a, b);
        assertEquals(3, e.nombreSieges());
        assertEquals(2, e.siegesPourMajoriteAbsolue());

        e.enregistrerResultats(e.trouverDepartement(1).orElseThrow(), 100, 80, Map.of(a, 50, b, 30));
        assertEquals(Optional.empty(), e.partiMajoritaire(), "1 siège sur 3 ne suffit pas");
        e.enregistrerResultats(e.trouverDepartement(2).orElseThrow(), 100, 80, Map.of(a, 41, b, 39));
        assertEquals(Optional.of(a), e.partiMajoritaire());
        assertEquals(2, e.nombreDepartementsDepouilles());
    }

    @Test
    void pasDeMajoriteSiSiegesPartages() throws ElectionException {
        PartiPolitique a = new PartiPolitique("AAA", "A");
        PartiPolitique b = new PartiPolitique("BBB", "B");
        Election e = electionAvecTroisDepartements(a, b);
        e.enrolerDepartement(new Departement(4, "D4"));
        e.enregistrerResultats(e.trouverDepartement(1).orElseThrow(), 10, 10, Map.of(a, 6, b, 4));
        e.enregistrerResultats(e.trouverDepartement(2).orElseThrow(), 10, 10, Map.of(a, 6, b, 4));
        e.enregistrerResultats(e.trouverDepartement(3).orElseThrow(), 10, 10, Map.of(a, 4, b, 6));
        e.enregistrerResultats(e.trouverDepartement(4).orElseThrow(), 10, 10, Map.of(a, 4, b, 6));
        assertEquals(3, e.siegesPourMajoriteAbsolue());
        assertEquals(Optional.empty(), e.partiMajoritaire());
    }

    @Test
    void statistiquesDeParticipation() {
        Departement d1 = new Departement(1, "Un");
        Departement d2 = new Departement(2, "Deux");
        Departement d3 = new Departement(3, "Trois");
        Departement d4 = new Departement(4, "Sans inscrits");
        d1.setParticipation(100, 20);
        d2.setParticipation(100, 50);
        d3.setParticipation(100, 100);

        StatistiquesParticipation s = StatistiquesParticipation.calculer(List.of(d1, d2, d3, d4));
        assertEquals(3, s.getEffectif(), "un département sans inscrits est ignoré");
        assertEquals(0.5666666, s.getMoyenne(), 1e-6);
        assertEquals(0.5, s.getMediane(), 1e-12);
        assertEquals(0.2, s.getMinimum(), 1e-12);
        assertEquals(1.0, s.getMaximum(), 1e-12);
        assertEquals(Math.sqrt((0.1344444 + 0.0044444 + 0.1877778) / 3), s.getEcartType(), 1e-6);
        // 20 % dans la classe [20;30[, 50 % dans [50;60[, 100 % dans la dernière classe
        assertArrayEquals(new int[]{0, 0, 1, 0, 0, 1, 0, 0, 0, 1}, s.histogramme(10));
    }

    @Test
    void statistiquesSansDonnees() {
        StatistiquesParticipation s = StatistiquesParticipation.calculer(List.of());
        assertEquals(0, s.getEffectif());
        assertEquals(0, s.getMediane());
        assertEquals(0, s.getEcartType());
    }
}
