package fr.aldermyr.coeur;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Textes et recherches utilisés par les commandes du staff. */
public final class Description {

    static final String[] REGIONS = {
        "Mer", "Plaines", "Montagnes", "Steppes", "Forêt", "Marches brisées", "Côte", "Ruines", "Cratère"};
    static final int MAX_RESULTATS = 50;

    private Description() {}

    public static String ici(CartesMonde c, int x, int z) {
        int r = c.region(x, z);
        String region = r < REGIONS.length ? REGIONS[r] : "Inconnue (" + r + ")";
        Integer niveau = c.niveauEau(x, z);
        String eau = niveau == null ? "non" : String.format("niveau %+d", niveau);
        return String.format("Région : %s — altitude : %+d — eau : %s", region, c.altitude(x, z), eau);
    }

    public static Optional<CartesMonde.Lieu> lieu(CartesMonde c, String nom) {
        String cherche = simplifier(nom);
        return c.lieux().stream().filter(l -> simplifier(l.nom()).equals(cherche)).findFirst();
    }

    public static List<String> filtrer(Collection<String> noms, String filtre) {
        String f = filtre.toLowerCase(Locale.ROOT);
        return noms.stream().filter(n -> n.toLowerCase(Locale.ROOT).contains(f)).sorted().limit(MAX_RESULTATS).toList();
    }

    /** Minuscules, sans accents ; « œ » et « æ » deviennent « oe » et « ae ». */
    static String simplifier(String s) {
        String t = s.toLowerCase(Locale.ROOT).replace("œ", "oe").replace("æ", "ae");
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
