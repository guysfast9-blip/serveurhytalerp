package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import org.junit.Test;

public class PlanColonneTest {

    static final Palette P = Palette.defaut();

    static PlanColonne.Plan plan(int alt, int region, Integer niveau, boolean route) {
        CartesMonde c = CartesMonde.vide();
        c.poser(0, 0, alt, region, niveau, route);
        return PlanColonne.planifier(c, P, 0, 0);
    }

    @Test
    public void couchesDesPlaines() {
        PlanColonne.Plan p = plan(10, 1, null, false);
        assertEquals(110, p.surfaceY());
        assertEquals("Soil_Grass", p.blocs()[110]);
        for (int y = 107; y <= 109; y++) assertEquals("Soil_Dirt", p.blocs()[y]);
        assertEquals("Rock_Stone", p.blocs()[0]);
        assertEquals("Rock_Stone", p.blocs()[106]);
        assertNull(p.eauJusquA());
        assertEquals(0xFF7DB24A, p.teinte());
    }

    @Test
    public void riviere() {
        PlanColonne.Plan p = plan(5, 1, 8, false);
        assertEquals("Soil_Gravel", p.blocs()[105]);
        assertEquals(Integer.valueOf(108), p.eauJusquA());
    }

    @Test
    public void merProfonde() {
        PlanColonne.Plan p = plan(-40, 0, 0, false);
        assertEquals(60, p.surfaceY());
        assertEquals("Soil_Sand", p.blocs()[60]);
        assertEquals(Integer.valueOf(100), p.eauJusquA());
    }

    @Test
    public void eauJamaisSousLaSurface() {
        assertNull(plan(10, 1, 3, false).eauJusquA());
    }

    @Test
    public void sommetBorne() {
        assertEquals(318, plan(400, 2, null, false).surfaceY());
        assertEquals(1, plan(-200, 0, 0, false).surfaceY());
    }

    @Test
    public void neigeEnAltitude() {
        assertEquals("Soil_Snow", plan(160, 2, null, false).blocs()[260]);
        assertEquals("Rock_Stone", plan(120, 2, null, false).blocs()[220]);
    }

    @Test
    public void routePrioritaire() {
        assertEquals("Soil_Dirt", plan(10, 1, null, true).blocs()[110]);
        assertEquals("Soil_Gravel", plan(5, 1, 8, true).blocs()[105]);
    }

    @Test
    public void obsidienneEtVolcan() {
        CartesMonde c = CartesMonde.vide();
        c.ajouterLieu(new CartesMonde.Lieu("Champs d'Obsidienne", "lieu", 0, 0, 40));
        c.ajouterLieu(new CartesMonde.Lieu("Mont Braise", "lieu", 500, 0, 0));
        c.poser(10, 0, 20, 3, null, false);
        c.poser(560, 0, 60, 1, null, false);
        c.poser(700, 0, 20, 1, null, false);
        assertEquals("Rock_Obsidian", PlanColonne.planifier(c, P, 10, 0).blocs()[120]);
        assertEquals("Rock_Volcanic", PlanColonne.planifier(c, P, 560, 0).blocs()[160]);
        assertEquals("Soil_Grass", PlanColonne.planifier(c, P, 700, 0).blocs()[120]);
    }
}
