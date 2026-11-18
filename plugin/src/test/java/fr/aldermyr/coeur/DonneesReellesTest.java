package fr.aldermyr.coeur;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

/** Vérifie que les fichiers réellement produits par l'atelier (Pillow) se lisent correctement. */
public class DonneesReellesTest {

    static final Path DONNEES = Path.of("../donnees");

    @Test
    public void lectureDesCartesDeLAtelier() throws Exception {
        assumeTrue("données de l'atelier absentes", Files.exists(DONNEES.resolve("altitude.png")));
        CartesMonde c = CartesMonde.charger(DONNEES);
        assertEquals(23, c.lieux().size());
        CartesMonde.Lieu aurelia = c.lieux().stream().filter(l -> l.nom().equals("Aurelia")).findFirst().orElseThrow();
        assertEquals(0, aurelia.x());
        assertEquals(1, c.region(0, 0));                       // plaines
        int alt = c.altitude(0, 0);
        assertTrue("altitude d'Aurelia : " + alt, alt >= 1 && alt <= 20);
        assertNull(c.niveauEau(0, 0));                         // au sec
        assertEquals(Integer.valueOf(0), c.niveauEau(-2390, -1790)); // coin de la carte : la mer
        assertTrue(c.altitude(-2390, -1790) <= -5);
        // le grand fleuve se jette en mer près de Port-Écume : il existe de l'eau douce au-dessus du niveau de la mer
        boolean eauDouce = false;
        for (int x = -200; x <= 200 && !eauDouce; x++) {
            Integer n = c.niveauEau(x, 300);
            eauDouce = n != null && n > 0;
        }
        assertTrue(eauDouce);
        // la région Montagnes contient des sommets enneigés
        int max = Integer.MIN_VALUE;
        for (int x = -2000; x < 2000; x += 7) max = Math.max(max, c.altitude(x, -1400));
        assertTrue("sommet le plus haut : " + max, max >= 100);
    }
}
