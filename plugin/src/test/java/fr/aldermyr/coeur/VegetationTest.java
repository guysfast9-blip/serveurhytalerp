package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class VegetationTest {

    static CartesMonde c;
    static Relief relief;
    static Vegetation v;
    static final Palette P = Palette.defaut();

    /** Forêt (x < 320) et plaine (x ≥ 320) sur 640 × 640, avec une rivière, une route et un village. */
    @BeforeClass
    public static void preparer() {
        c = CartesMonde.vide();
        c.poserZone(0, 0, 320, 640, 10, 4);
        c.poserZone(320, 0, 320, 640, 10, 1);
        for (int x = 0; x < 640; x++) {
            for (int z = 100; z < 105; z++) c.poser(x, z, 6, x < 320 ? 4 : 1, 9, false);
            c.poser(x, 200, 10, x < 320 ? 4 : 1, null, true);
        }
        c.ajouterLieu(new CartesMonde.Lieu("Village", "village", 160, 450, 40));
        relief = new Relief(c, P);
        v = new Vegetation(c, P, relief, Extrait.tout());
    }

    static java.util.List<Vegetation.Placement> zone(int cx0, int cx1) {
        java.util.List<Vegetation.Placement> tout = new java.util.ArrayList<>();
        for (int cz = 1; cz < 19; cz++) {
            for (int cx = cx0; cx < cx1; cx++) {
                for (Vegetation.Placement p : v.placements(cx, cz)) {
                    if (Math.floorDiv(p.x(), 32) == cx && Math.floorDiv(p.z(), 32) == cz) tout.add(p);
                }
            }
        }
        return tout;
    }

    @Test
    public void foretPlusDenseQuePlaine() {
        int foret = zone(1, 9).size(), plaine = zone(11, 19).size();
        assertTrue("forêt " + foret + " / plaine " + plaine, foret > 4 * plaine && plaine > 0);
    }

    @Test
    public void jamaisDansLEauSurUneRouteNiDansUnLieu() {
        for (Vegetation.Placement p : zone(0, 20)) {
            assertNull(c.niveauEau(p.x(), p.z()));
            assertNotEquals(200, p.z());
            assertTrue(Math.hypot(p.x() - 160, p.z() - 450) > 40);
            assertEquals(PlanColonne.NIVEAU_MER_Y + relief.altitude(p.x(), p.z()) + 1, p.y());
        }
    }

    @Test
    public void prefabsDeLaRegion() {
        for (Vegetation.Placement p : zone(1, 3)) {
            assertTrue(P.region(4).vegetation().prefabs().stream().anyMatch(f -> f.dossier().equals(p.dossier())));
        }
    }

    @Test
    public void deterministeEtPartageEntreVoisins() {
        List<Vegetation.Placement> a = v.placements(4, 4);
        assertEquals(a, v.placements(4, 4));
        List<Vegetation.Placement> voisin = v.placements(5, 4);
        int communs = 0;
        for (Vegetation.Placement p : a) {
            if (p.x() >= 5 * 32 - 32 && p.x() < 4 * 32 + 64) {
                assertTrue(voisin.contains(p));
                communs++;
            }
        }
        assertTrue(communs > 0);
    }

    @Test
    public void merSansVegetation() {
        assertTrue(new Vegetation(CartesMonde.mer(), P, new Relief(CartesMonde.mer(), P), Extrait.tout())
            .placements(10, 10).isEmpty());
    }
}
