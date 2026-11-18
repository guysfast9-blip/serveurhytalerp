package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import java.io.StringReader;
import org.junit.Test;

public class PaletteTest {

    @Test
    public void defautComplet() {
        Palette p = Palette.defaut();
        for (int r = 0; r <= 8; r++) {
            Palette.Couches c = p.couches(r);
            assertFalse(c.surface().isEmpty());
            assertFalse(c.sousCouche().isEmpty());
            assertFalse(c.profondeur().isEmpty());
        }
        assertEquals(0xFF7DB24A, Palette.argb("#7DB24A"));
        assertEquals("Water_Source", p.eau());
        assertEquals("Soil_Clay", p.repli("Soil_Sand"));
    }

    @Test
    public void lectureJson() {
        Palette p = Palette.lire(new StringReader(
            "{\"regions\": {\"1\": {\"surface\": \"Soil_Needles\"}}}"));
        assertEquals("Soil_Needles", p.couches(1).surface());
        assertEquals("Soil_Dirt", p.couches(1).sousCouche());
        assertEquals("Rock_Granite", p.couches(2).profondeur());
        assertEquals(150, p.altitudeNeige());
    }
}
