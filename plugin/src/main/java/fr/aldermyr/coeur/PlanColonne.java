package fr.aldermyr.coeur;

/** Décrit une colonne de blocs du monde à partir des cartes et de la palette. */
public final class PlanColonne {

    public static final int NIVEAU_MER_Y = 100, Y_MIN = 1, Y_MAX = 318;
    static final String CHAMPS_OBSIDIENNE = "Champs d'Obsidienne", MONT_BRAISE = "Mont Braise";
    static final int RAYON_VOLCAN = 90;
    private static final int REGION_MER = 0, REGION_MONTAGNES = 2;

    /**
     * @param surfaceY  Y du bloc de surface
     * @param blocs     nom du bloc pour chaque Y de 0 à surfaceY (null = air)
     * @param eauJusquA Y le plus haut rempli d'eau (inclus), ou null
     * @param teinte    couleur ARGB de l'herbe pour cette colonne
     */
    public record Plan(int surfaceY, String[] blocs, Integer eauJusquA, int teinte) {}

    private PlanColonne() {}

    public static Plan planifier(CartesMonde c, Palette p, int x, int z) {
        int alt = c.altitude(x, z);
        int region = c.region(x, z);
        Integer niveau = c.niveauEau(x, z);
        Palette.Couches couches = p.couches(region);

        int surfaceY = Math.max(Y_MIN, Math.min(Y_MAX, NIVEAU_MER_Y + alt));
        String[] blocs = new String[surfaceY + 1];
        for (int y = 0; y < surfaceY; y++) {
            blocs[y] = y >= surfaceY - 3 ? couches.sousCouche() : couches.profondeur();
        }
        blocs[surfaceY] = surface(c, p, x, z, alt, region, niveau, couches);

        Integer eauJusquA = null;
        if (niveau != null && NIVEAU_MER_Y + niveau > surfaceY) {
            eauJusquA = Math.min(NIVEAU_MER_Y + niveau, Y_MAX);
        }
        return new Plan(surfaceY, blocs, eauJusquA, Palette.argb(couches.teinte()));
    }

    private static String surface(CartesMonde c, Palette p, int x, int z, int alt, int region,
                                  Integer niveau, Palette.Couches couches) {
        if (niveau != null) return region == REGION_MER ? p.fondMer() : p.litEau();
        if (c.route(x, z)) return p.route();
        for (CartesMonde.Lieu l : c.lieux()) {
            long dx = x - l.x(), dz = z - l.z(), d2 = dx * dx + dz * dz;
            if (CHAMPS_OBSIDIENNE.equals(l.nom()) && d2 <= (long) l.rayon() * l.rayon()) return p.obsidienne();
            if (MONT_BRAISE.equals(l.nom()) && d2 <= (long) RAYON_VOLCAN * RAYON_VOLCAN) return p.volcan();
        }
        if (region == REGION_MONTAGNES && alt >= p.altitudeNeige()) return p.neige();
        return couches.surface();
    }
}
