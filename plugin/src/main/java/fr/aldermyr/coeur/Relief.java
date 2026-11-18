package fr.aldermyr.coeur;

/**
 * Altitude finale d'une colonne : celle de la carte, plus un relief de détail qui dépend de la
 * région. Fonction pure de (x, z) : deux chunks voisins calculent la même chose sur leur bord.
 */
public final class Relief {

    static final int GRAINE_DETAIL = 11, GRAINE_CRETE = 23;
    static final double ONDE_DETAIL = 48, ONDE_CRETE = 110;
    /** Écart entre les échantillons de région (lissage des transitions). */
    static final int ECART_REGIONS = 12;
    /** Distances (en blocs) à l'eau : figé jusqu'à BERGE, atténué jusqu'à ABORDS. */
    static final int BERGE = 2, ABORDS = 6;

    private final CartesMonde cartes;
    private final Palette palette;

    public Relief(CartesMonde cartes, Palette palette) {
        this.cartes = cartes;
        this.palette = palette;
    }

    /** Altitude finale, en blocs au-dessus du niveau de la mer. */
    public int altitude(int x, int z) {
        int alt = cartes.altitude(x, z);
        if (cartes.niveauEau(x, z) != null) return alt;
        double amp = 0, crete = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Palette.Region r = palette.region(cartes.region(x + dx * ECART_REGIONS, z + dz * ECART_REGIONS));
                amp += r.relief();
                crete += r.crete();
            }
        }
        amp /= 9;
        crete /= 9;
        if (amp == 0 && crete == 0) return alt;

        int eau = distanceEau(x, z);
        if (eau <= BERGE) return alt;
        double att = eau <= ABORDS ? 0.25 + 0.75 * (eau - BERGE) / (ABORDS - BERGE + 1.0) : 1;
        att = Math.min(att, attenuationAmenagements(x, z));

        double delta = amp * Bruit.fbm(GRAINE_DETAIL, x, z, ONDE_DETAIL, 4);
        if (crete > 0) {
            double r = 1 - Math.abs(Bruit.fbm(GRAINE_CRETE, x, z, ONDE_CRETE, 4));
            double hauteur = Math.max(0, Math.min(1.3, (alt - 30) / 80.0));
            delta += crete * hauteur * (r * r - 0.3);
        }
        if (eau <= ABORDS) delta = Math.max(0, delta);
        int resultat = (int) Math.round(alt + delta * att);
        return alt >= 1 ? Math.max(1, resultat) : resultat;
    }

    /** Plus grand écart d'altitude avec les quatre voisines. */
    public int pente(int x, int z) {
        int h = altitude(x, z);
        int p = Math.abs(altitude(x + 1, z) - h);
        p = Math.max(p, Math.abs(altitude(x - 1, z) - h));
        p = Math.max(p, Math.abs(altitude(x, z + 1) - h));
        return Math.max(p, Math.abs(altitude(x, z - 1) - h));
    }

    /** Distance (de Tchebychev) à l'eau la plus proche, plafonnée à ABORDS + 1. */
    int distanceEau(int x, int z) {
        int meilleure = ABORDS + 1;
        for (int dz = -ABORDS; dz <= ABORDS; dz++) {
            for (int dx = -ABORDS; dx <= ABORDS; dx++) {
                int d = Math.max(Math.abs(dx), Math.abs(dz));
                if (d < meilleure && cartes.niveauEau(x + dx, z + dz) != null) meilleure = d;
            }
        }
        return meilleure;
    }

    /** Terrain presque plat sur les routes et dans les lieux, pour qu'on puisse y construire. */
    private double attenuationAmenagements(int x, int z) {
        double att = 1;
        for (CartesMonde.Lieu l : cartes.lieux()) {
            double d = Math.hypot(x - l.x(), z - l.z());
            if (d <= l.rayon()) return 0.1;
            if (d <= l.rayon() + 20) att = Math.min(att, 0.1 + 0.9 * (d - l.rayon()) / 20);
        }
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (cartes.route(x + dx, z + dz)) return Math.min(att, 0.2);
            }
        }
        return att;
    }
}
