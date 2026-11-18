package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RemplisseurTest {

    /** Cible factice qui enregistre les appels. */
    static final class Enregistreur implements Cible {
        final Set<String> blocs = new HashSet<>();
        final Set<String> eaux = new HashSet<>();
        final List<Integer> teintes = new ArrayList<>();
        final List<String> prefabs = new ArrayList<>();

        @Override public void bloc(int lx, int y, int lz, String nom) {
            assertTrue(lx >= 0 && lx < 32 && lz >= 0 && lz < 32 && y >= 0 && y < 320);
            blocs.add(lx + "," + y + "," + lz + "," + nom);
        }
        @Override public void eau(int lx, int y, int lz) { eaux.add(lx + "," + y + "," + lz); }
        @Override public void teinte(int lx, int lz, int argb) { teintes.add(argb); }
        @Override public void prefab(String dossier, long tirage, int lx, int y, int lz) {
            prefabs.add(lx + "," + y + "," + lz + "," + dossier);
        }
    }

    static Enregistreur remplir(CartesMonde c, Extrait e, int cx, int cz) {
        Enregistreur r = new Enregistreur();
        new Remplisseur(c, Palette.defaut(), e).remplir(cx, cz, r);
        return r;
    }

    @Test
    public void chunkOrigine() {
        CartesMonde c = CartesMonde.vide();
        c.poserZone(-1, -1, 3, 3, 10, 1);
        assertTrue(remplir(c, Extrait.tout(), 0, 0).blocs.contains("0,110,0,Soil_Grass"));
    }

    @Test
    public void chunkNegatif() {
        CartesMonde c = CartesMonde.vide();
        c.poserZone(-2, -2, 3, 3, 10, 1);
        assertTrue(remplir(c, Extrait.tout(), -1, -1).blocs.contains("31,110,31,Soil_Grass"));
    }

    @Test
    public void eauEmpilee() {
        CartesMonde c = CartesMonde.vide();
        c.poser(0, 0, 5, 1, 8, false);
        Enregistreur r = remplir(c, Extrait.tout(), 0, 0);
        assertTrue(r.eaux.contains("0,106,0") && r.eaux.contains("0,107,0") && r.eaux.contains("0,108,0"));
        assertFalse(r.eaux.contains("0,109,0"));
        assertFalse(r.eaux.contains("0,105,0"));
    }

    @Test
    public void horsExtraitCestLaMer() {
        CartesMonde c = CartesMonde.vide();
        c.poser(20, 0, 10, 1, null, false);
        Enregistreur r = remplir(c, new Extrait(0, 0, 10, 10), 0, 0);
        assertTrue(r.blocs.contains("20,60,0,Soil_Sand"));
        assertTrue(r.eaux.contains("20,100,0"));
        assertFalse(r.eaux.contains("20,101,0"));
    }

    @Test
    public void teinteParColonne() {
        assertEquals(1024, remplir(CartesMonde.vide(), Extrait.tout(), 3, -7).teintes.size());
    }

    @Test
    public void extraitContient() {
        Extrait e = new Extrait(-256, -256, 512, 512);
        assertTrue(e.contient(-256, -256) && e.contient(255, 255));
        assertFalse(e.contient(256, 0) || e.contient(0, -257));
        assertTrue(Extrait.tout().contient(-2400, 1799));
    }

    @Test
    public void prefabsTransmisAvecCoordonneesLocales() {
        CartesMonde c = CartesMonde.vide();
        c.poserZone(-64, -64, 192, 192, 10, 4);
        Enregistreur r = remplir(c, Extrait.tout(), 0, 0);
        assertFalse(r.prefabs.isEmpty());
        boolean dedans = false, dehors = false;
        for (String p : r.prefabs) {
            int lx = Integer.parseInt(p.split(",")[0]);
            if (lx >= 0 && lx < 32) dedans = true; else dehors = true;
        }
        assertTrue(dedans && dehors);
        assertTrue(remplir(c, new Extrait(1000, 1000, 10, 10), 0, 0).prefabs.isEmpty());
    }
}
