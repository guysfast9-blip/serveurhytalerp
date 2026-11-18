package fr.aldermyr.coeur;

import java.util.Arrays;
import java.util.List;

/** Décrit une colonne de blocs du monde à partir des cartes, du relief et de la palette. */
public final class PlanColonne {

    public static final int NIVEAU_MER_Y = 100, Y_MIN = 1, Y_MAX = 318;
    static final String CHAMPS_OBSIDIENNE = "Champs d'Obsidienne", MONT_BRAISE = "Mont Braise";
    static final int RAYON_VOLCAN = 90;
    private static final int REGION_MER = 0, REGION_MONTAGNES = 2;
    static final int GRAINE_VARIANTE = 41, GRAINE_TEINTE = 53;
    static final double ONDE_VARIANTE = 14, ONDE_TEINTE = 30, ECART_TEINTE = 0.08;

    /** Quantiles du bruit des variantes : une variante de part p couvre environ p de la surface. */
    private static final double[] QUANTILES = new double[1001];

    static {
        double[] ech = new double[20000];
        for (int i = 0; i < ech.length; i++) {
            ech[i] = Bruit.fbm(97, (i % 200) * 37.3, (i / 200) * 41.7, ONDE_VARIANTE, 2);
        }
        Arrays.sort(ech);
        for (int q = 0; q <= 1000; q++) QUANTILES[q] = ech[Math.min(ech.length - 1, q * (ech.length - 1) / 1000)];
    }

    /**
     * @param surfaceY  Y du bloc de surface
     * @param blocs     nom du bloc pour chaque Y de 0 à surfaceY (null = air)
     * @param eauJusquA Y le plus haut rempli d'eau (inclus), ou null
     * @param teinte    couleur ARGB de l'herbe pour cette colonne
     */
    public record Plan(int surfaceY, String[] blocs, Integer eauJusquA, int teinte) {}

    private PlanColonne() {}

    public static Plan planifier(CartesMonde c, Palette p, Relief r, int x, int z) {
        return planifier(c, p, x, z, r.altitude(x, z), r.pente(x, z));
    }

    /** Plan d'une colonne dont l'altitude finale et la pente sont déjà connues. */
    public static Plan planifier(CartesMonde c, Palette p, int x, int z, int alt, int pente) {
        int region = c.region(x, z);
        Integer niveau = c.niveauEau(x, z);
        Palette.Couches couches = p.couches(region);

        int surfaceY = Math.max(Y_MIN, Math.min(Y_MAX, NIVEAU_MER_Y + alt));
        String[] blocs = new String[surfaceY + 1];
        for (int y = 0; y < surfaceY; y++) {
            blocs[y] = y >= surfaceY - 3 ? couches.sousCouche() : couches.profondeur();
        }
        blocs[surfaceY] = surface(c, p, x, z, alt, pente, region, niveau, couches);

        Integer eauJusquA = null;
        if (niveau != null && NIVEAU_MER_Y + niveau > surfaceY) {
            eauJusquA = Math.min(NIVEAU_MER_Y + niveau, Y_MAX);
        }
        return new Plan(surfaceY, blocs, eauJusquA, teinte(Palette.argb(couches.teinte()), x, z));
    }

    private static String surface(CartesMonde c, Palette p, int x, int z, int alt, int pente, int region,
                                  Integer niveau, Palette.Couches couches) {
        if (niveau != null) return region == REGION_MER ? p.fondMer() : p.litEau();
        if (c.route(x, z)) return p.route();
        for (CartesMonde.Lieu l : c.lieux()) {
            long dx = x - l.x(), dz = z - l.z(), d2 = dx * dx + dz * dz;
            if (CHAMPS_OBSIDIENNE.equals(l.nom()) && d2 <= (long) l.rayon() * l.rayon()) return p.obsidienne();
            if (MONT_BRAISE.equals(l.nom()) && d2 <= (long) RAYON_VOLCAN * RAYON_VOLCAN) return p.volcan();
        }
        if (region == REGION_MONTAGNES && alt >= p.altitudeNeige()) return p.neige();
        Palette.Region reglages = p.region(region);
        if (alt >= reglages.rocheAuDessus() || pente >= p.penteFalaise()) return reglages.falaise();
        List<Palette.Variante> variantes = reglages.variantes();
        for (int i = 0; i < variantes.size(); i++) {
            Palette.Variante v = variantes.get(i);
            double seuil = QUANTILES[(int) Math.round((1 - v.part()) * 1000)];
            if (Bruit.fbm(GRAINE_VARIANTE + i * 7, x, z, ONDE_VARIANTE, 2) > seuil) return v.bloc();
        }
        return couches.surface();
    }

    /** Fait varier légèrement la teinte de l'herbe pour casser l'uniformité. */
    static int teinte(int argb, int x, int z) {
        double f = 1 + ECART_TEINTE * Bruit.fbm(GRAINE_TEINTE, x, z, ONDE_TEINTE, 2);
        int r = Math.min(255, (int) Math.round(((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) Math.round(((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) Math.round((argb & 0xFF) * f));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
