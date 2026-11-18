package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import org.junit.Test;

public class BruitTest {

    @Test
    public void deterministe() {
        assertEquals(Bruit.valeur(7, 123, -456), Bruit.valeur(7, 123, -456), 0);
        assertEquals(Bruit.fbm(3, 10.5, -3.25, 40, 4), Bruit.fbm(3, 10.5, -3.25, 40, 4), 0);
    }

    @Test
    public void bornes() {
        for (int i = -500; i < 500; i += 7) {
            double v = Bruit.valeur(1, i, i * 3);
            assertTrue(v >= 0 && v < 1);
            double f = Bruit.fbm(2, i * 1.3, -i, 30, 4);
            assertTrue(f >= -1 && f <= 1);
        }
    }

    @Test
    public void varieEtDependDeLaGraine() {
        double min = 1, max = -1;
        for (int i = 0; i < 100; i++) {
            double f = Bruit.fbm(5, i * 13, i * 7, 40, 3);
            min = Math.min(min, f);
            max = Math.max(max, f);
        }
        assertTrue(max - min > 0.5);
        assertNotEquals(Bruit.fbm(1, 50, 50, 40, 3), Bruit.fbm(2, 50, 50, 40, 3), 1e-9);
    }

    @Test
    public void continu() {
        assertEquals(Bruit.fbm(4, 100, 100, 50, 3), Bruit.fbm(4, 100.5, 100, 50, 3), 0.1);
    }
}
