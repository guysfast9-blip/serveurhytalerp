package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class DescriptionTest {

    @Test
    public void iciPlaines() {
        CartesMonde c = CartesMonde.vide();
        c.poser(0, 0, 12, 1, null, false);
        assertEquals("Région : Plaines — altitude : +12 — eau : non", Description.ici(c, 0, 0));
    }

    @Test
    public void iciEau() {
        CartesMonde c = CartesMonde.vide();
        c.poser(0, 0, -2, 4, 3, false);
        assertEquals("Région : Forêt — altitude : -2 — eau : niveau +3", Description.ici(c, 0, 0));
    }

    @Test
    public void lieuSansAccents() {
        CartesMonde c = CartesMonde.vide();
        c.ajouterLieu(new CartesMonde.Lieu("Cœur-Sylve", "capitale", 10, 20, 60));
        assertEquals("Cœur-Sylve", Description.lieu(c, "coeur-sylve").orElseThrow().nom());
        assertEquals("Cœur-Sylve", Description.lieu(c, "CŒUR-SYLVE").orElseThrow().nom());
        assertTrue(Description.lieu(c, "atlantis").isEmpty());
    }

    @Test
    public void filtreLimite() {
        List<String> noms = new ArrayList<>();
        for (int i = 0; i < 60; i++) noms.add(String.format("Soil_Sand_%02d", 59 - i));
        noms.add("Rock_Stone");
        List<String> r = Description.filtrer(noms, "sand");
        assertEquals(50, r.size());
        assertEquals("Soil_Sand_00", r.get(0));
    }
}
