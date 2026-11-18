package fr.aldermyr.coeur;

/** Remplit un chunk de 32 × 32 colonnes à partir des cartes. */
public final class Remplisseur {

    public static final int TAILLE_CHUNK = 32;
    private static final CartesMonde HAUTE_MER = CartesMonde.mer();

    private final CartesMonde cartes;
    private final Palette palette;
    private final Extrait extrait;

    public Remplisseur(CartesMonde cartes, Palette palette, Extrait extrait) {
        this.cartes = cartes;
        this.palette = palette;
        this.extrait = extrait;
    }

    public void remplir(int cx, int cz, Cible cible) {
        for (int lz = 0; lz < TAILLE_CHUNK; lz++) {
            for (int lx = 0; lx < TAILLE_CHUNK; lx++) {
                int x = cx * TAILLE_CHUNK + lx, z = cz * TAILLE_CHUNK + lz;
                CartesMonde source = extrait.contient(x, z) ? cartes : HAUTE_MER;
                PlanColonne.Plan plan = PlanColonne.planifier(source, palette, x, z);
                String[] blocs = plan.blocs();
                for (int y = 0; y < blocs.length; y++) {
                    if (blocs[y] != null) cible.bloc(lx, y, lz, blocs[y]);
                }
                if (plan.eauJusquA() != null) {
                    for (int y = plan.surfaceY() + 1; y <= plan.eauJusquA(); y++) cible.eau(lx, y, lz);
                }
                cible.teinte(lx, lz, plan.teinte());
            }
        }
    }
}
