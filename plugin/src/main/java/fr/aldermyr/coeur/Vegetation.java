package fr.aldermyr.coeur;

import java.util.ArrayList;
import java.util.List;

/**
 * Choisit où poser les prefabs de végétation (arbres, buissons, rochers). Une grille de GRILLE blocs
 * porte au plus un prefab par case ; la position et le choix ne dépendent que de la case, si bien
 * que chaque chunk retrouve aussi les arbres ancrés chez ses voisins.
 */
public final class Vegetation {

    public record Placement(int x, int y, int z, String dossier, long tirage) {}

    static final int GRILLE = 5, MARGE = Remplisseur.TAILLE_CHUNK;
    static final int GRAINE_POSITION = 61, GRAINE_CHOIX = 67, GRAINE_BOSQUETS = 71;
    static final double ONDE_BOSQUETS = 160, PENTE_MAX = 3, ECART_LIEU = 5;

    private final CartesMonde cartes;
    private final Palette palette;
    private final Relief relief;
    private final Extrait extrait;

    public Vegetation(CartesMonde cartes, Palette palette, Relief relief, Extrait extrait) {
        this.cartes = cartes;
        this.palette = palette;
        this.relief = relief;
        this.extrait = extrait;
    }

    /** Prefabs ancrés dans le chunk (cx, cz) et dans ses huit voisins. */
    public List<Placement> placements(int cx, int cz) {
        int x0 = cx * Remplisseur.TAILLE_CHUNK - MARGE, x1 = (cx + 1) * Remplisseur.TAILLE_CHUNK + MARGE;
        int z0 = cz * Remplisseur.TAILLE_CHUNK - MARGE, z1 = (cz + 1) * Remplisseur.TAILLE_CHUNK + MARGE;
        List<Placement> liste = new ArrayList<>();
        for (int gz = Math.floorDiv(z0, GRILLE); gz <= Math.floorDiv(z1 - 1, GRILLE); gz++) {
            for (int gx = Math.floorDiv(x0, GRILLE); gx <= Math.floorDiv(x1 - 1, GRILLE); gx++) {
                long h = Bruit.hachage(GRAINE_POSITION, gx, gz);
                int x = gx * GRILLE + (int) Math.floorMod(h, (long) GRILLE);
                int z = gz * GRILLE + (int) Math.floorMod(h >>> 8, (long) GRILLE);
                if (x < x0 || x >= x1 || z < z0 || z >= z1) continue;
                Placement p = placement(x, z, ((h >>> 16) & 0xFFFFFF) / (double) (1 << 24));
                if (p != null) liste.add(p);
            }
        }
        return liste;
    }

    private Placement placement(int x, int z, double tirage) {
        if (!extrait.contient(x, z)) return null;
        Palette.Vegetation veg = palette.region(cartes.region(x, z)).vegetation();
        if (veg.densite() <= 0 || veg.prefabs().isEmpty()) return null;
        double bosquets = 1 + 0.8 * Bruit.fbm(GRAINE_BOSQUETS, x, z, ONDE_BOSQUETS, 2);
        if (tirage >= veg.densite() * bosquets) return null;
        if (cartes.niveauEau(x, z) != null) return null;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (cartes.route(x + dx, z + dz)) return null;
            }
        }
        for (CartesMonde.Lieu l : cartes.lieux()) {
            if (Math.hypot(x - l.x(), z - l.z()) <= l.rayon() + ECART_LIEU) return null;
        }
        int alt = relief.altitude(x, z);
        if (alt >= palette.limiteArbres() || alt >= palette.altitudeNeige()) return null;
        if (relief.pente(x, z) >= PENTE_MAX) return null;
        return new Placement(x, PlanColonne.NIVEAU_MER_Y + alt + 1 + palette.decalagePrefabs(), z, choisir(veg, x, z),
            Bruit.hachage(GRAINE_CHOIX + 1, x, z));
    }

    private static String choisir(Palette.Vegetation veg, int x, int z) {
        double total = 0;
        for (Palette.Prefab p : veg.prefabs()) total += p.poids();
        double t = Bruit.valeur(GRAINE_CHOIX, x, z) * total;
        for (Palette.Prefab p : veg.prefabs()) {
            t -= p.poids();
            if (t < 0) return p.dossier();
        }
        return veg.prefabs().get(veg.prefabs().size() - 1).dossier();
    }
}
