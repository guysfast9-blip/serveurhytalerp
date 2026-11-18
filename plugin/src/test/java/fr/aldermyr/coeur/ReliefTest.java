package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class ReliefTest {

    static CartesMonde c;
    static Relief r;

    /** Plaine (alt 10) de 400 × 400 avec une rivière en x = 200 et un village en (100, 300). */
    @BeforeClass
    public static void preparer() {
        c = CartesMonde.vide();
        c.poserZone(0, 0, 400, 400, 10, 1);
        for (int z = 0; z < 400; z++) {
            for (int x = 198; x <= 202; x++) c.poser(x, z, 6, 1, 9, false);
        }
        c.poserZone(0, -20, 400, 20, 2, 6);
        c.ajouterLieu(new CartesMonde.Lieu("Village", "village", 100, 300, 30));
        r = new Relief(c, Palette.defaut());
    }

    @Test
    public void eauInchangee() {
        for (int z = 0; z < 400; z += 13) assertEquals(6, r.altitude(200, z));
    }

    @Test
    public void bergesJamaisSousLEau() {
        for (int z = 0; z < 400; z++) {
            for (int x = 192; x <= 208; x++) {
                if (x >= 198 && x <= 202) continue;
                assertTrue(r.altitude(x, z) >= 10);
                if (x >= 196 && x <= 204) assertEquals(10, r.altitude(x, z));
            }
        }
    }

    @Test
    public void plaineVarieeMaisBornee() {
        int min = 99, max = -99;
        for (int z = 20; z < 180; z += 3) {
            for (int x = 20; x < 180; x += 3) {
                int a = r.altitude(x, z);
                min = Math.min(min, a);
                max = Math.max(max, a);
            }
        }
        assertTrue("relief absent : " + min + ".." + max, max - min >= 3);
        assertTrue(min >= 6 && max <= 14);
    }

    @Test
    public void terreResteHorsDeLEau() {
        for (int x = 0; x < 400; x += 2) {
            for (int z = -18; z < -2; z += 2) assertTrue(r.altitude(x, z) >= 1);
        }
    }

    @Test
    public void lieuPresquePlat() {
        for (int z = 280; z <= 320; z += 2) {
            for (int x = 80; x <= 120; x += 2) assertTrue(Math.abs(r.altitude(x, z) - 10) <= 1);
        }
    }

    @Test
    public void pente() {
        assertEquals(0, r.pente(200, 50));
        int maxPente = 0;
        for (int x = 20; x < 180; x += 5) maxPente = Math.max(maxPente, r.pente(x, 60));
        assertTrue(maxPente <= 3);
    }
}
