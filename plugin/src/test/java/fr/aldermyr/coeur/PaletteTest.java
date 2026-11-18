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

    @Test
    public void candidatsEau() {
        assertEquals(java.util.List.of("Water_Source", "Fluid_Water", "Water"), Palette.defaut().candidatsEau());
        Palette p = Palette.lire(new StringReader("{\"eau\": \"Eau_Perso\"}"));
        assertEquals(java.util.List.of("Eau_Perso", "Water_Source", "Fluid_Water", "Water"), p.candidatsEau());
    }

    @Test
    public void reglagesV2() {
        Palette p = Palette.defaut();
        assertEquals(2, p.version());
        assertEquals(3, p.penteFalaise());
        assertEquals(0, p.decalagePrefabs());
        assertEquals(-1, Palette.lire(new StringReader("{\"decalagePrefabs\": -1}")).decalagePrefabs());
        Palette.Region montagnes = p.region(2);
        assertTrue(montagnes.crete() > montagnes.relief());
        assertEquals("Rock_Stone", montagnes.falaise());
        assertEquals(0, p.region(0).vegetation().densite(), 0);
        Palette.Region foret = p.region(4);
        assertTrue(foret.vegetation().densite() > p.region(1).vegetation().densite());
        assertFalse(foret.vegetation().prefabs().isEmpty());
        assertTrue(foret.vegetation().prefabs().get(0).dossier().startsWith("Trees/"));
        for (int r = 0; r <= 8; r++) {
            for (Palette.Variante v : p.region(r).variantes()) assertTrue(v.part() > 0 && v.part() < 1);
        }
    }

    @Test
    public void ancienneVersionDetectee() {
        assertEquals(1, Palette.lire(new StringReader("{\"neige\": \"Soil_Snow\"}")).version());
    }
}
