package fr.aldermyr.coeur;

/** Rectangle de blocs (coordonnées centrées) à générer ; ailleurs, c'est la mer. */
public record Extrait(int x, int z, int largeur, int hauteur) {

    public static Extrait tout() {
        return new Extrait(-CartesMonde.LARGEUR / 2, -CartesMonde.HAUTEUR / 2, CartesMonde.LARGEUR, CartesMonde.HAUTEUR);
    }

    public boolean contient(int bx, int bz) {
        return bx >= x && bz >= z && bx < x + largeur && bz < z + hauteur;
    }

    /** Lit "x,z,largeur,hauteur". */
    public static Extrait lire(String texte) {
        String[] p = texte.split(",");
        if (p.length != 4) throw new IllegalArgumentException("Extrait attendu sous la forme x,z,largeur,hauteur : " + texte);
        return new Extrait(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()),
            Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()));
    }
}
