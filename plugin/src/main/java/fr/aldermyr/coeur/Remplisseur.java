package fr.aldermyr.coeur;

/** Remplit un chunk de 32 × 32 colonnes à partir des cartes. */
public final class Remplisseur {

    public static final int TAILLE_CHUNK = 32;
    private static final CartesMonde HAUTE_MER = CartesMonde.mer();

    private final CartesMonde cartes;
    private final Palette palette;
    private final Extrait extrait;
    private final Relief relief;
    private final Relief reliefMer;
    private final Vegetation vegetation;

    public Remplisseur(CartesMonde cartes, Palette palette, Extrait extrait) {
        this.cartes = cartes;
        this.palette = palette;
        this.extrait = extrait;
        this.relief = new Relief(cartes, palette);
        this.reliefMer = new Relief(HAUTE_MER, palette);
        this.vegetation = new Vegetation(cartes, palette, relief, extrait);
    }

    private int altitude(int x, int z) {
        return extrait.contient(x, z) ? relief.altitude(x, z) : reliefMer.altitude(x, z);
    }

    public void remplir(int cx, int cz, Cible cible) {
        int n = TAILLE_CHUNK + 2;
        int[] alt = new int[n * n];
        for (int gz = 0; gz < n; gz++) {
            for (int gx = 0; gx < n; gx++) {
                alt[gz * n + gx] = altitude(cx * TAILLE_CHUNK + gx - 1, cz * TAILLE_CHUNK + gz - 1);
            }
        }
        for (int lz = 0; lz < TAILLE_CHUNK; lz++) {
            for (int lx = 0; lx < TAILLE_CHUNK; lx++) {
                int x = cx * TAILLE_CHUNK + lx, z = cz * TAILLE_CHUNK + lz;
                CartesMonde source = extrait.contient(x, z) ? cartes : HAUTE_MER;
                int i = (lz + 1) * n + lx + 1, h = alt[i];
                int pente = Math.max(Math.max(Math.abs(alt[i - 1] - h), Math.abs(alt[i + 1] - h)),
                    Math.max(Math.abs(alt[i - n] - h), Math.abs(alt[i + n] - h)));
                PlanColonne.Plan plan = PlanColonne.planifier(source, palette, x, z, h, pente);
                String[] blocs = plan.blocs();
                for (int y = 0; y < blocs.length; y++) {
                    if (blocs[y] != null) cible.bloc(lx, y, lz, blocs[y]);
                }
                if (plan.eauJusquA() != null) {
                    for (int y = plan.surfaceY() + 1; y <= plan.eauJusquA(); y++) cible.eau(lx, y, lz);
                }
                cible.teinte(lx, lz, plan.teinte());
                if (plan.plante() != null) cible.plante(lx, plan.surfaceY() + 1, lz, plan.plante());
            }
        }
        int ox = cx * TAILLE_CHUNK, oz = cz * TAILLE_CHUNK;
        for (Vegetation.Placement p : vegetation.placements(cx, cz)) {
            cible.prefab(p.dossier(), p.tirage(), p.x() - ox, p.y(), p.z() - oz);
        }
    }
}
