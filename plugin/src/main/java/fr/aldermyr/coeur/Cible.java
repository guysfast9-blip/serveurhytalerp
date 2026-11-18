package fr.aldermyr.coeur;

/** Destination d'un chunk en cours de génération, en coordonnées locales (0 à 31 en x et z). */
public interface Cible {
    void bloc(int lx, int y, int lz, String nom);

    void eau(int lx, int y, int lz);

    void teinte(int lx, int lz, int argb);

    /** Pose une plante (herbe, fougère, fleur) ; un nom inconnu ne doit rien poser. */
    default void plante(int lx, int y, int lz, String nom) {
        bloc(lx, y, lz, nom);
    }

    /**
     * Pose la partie d'un prefab qui tombe dans ce chunk. (lx, lz) est la position de l'ancre, qui
     * peut se trouver dans un chunk voisin ; tirage choisit la variante dans le dossier.
     */
    default void prefab(String dossier, long tirage, int lx, int y, int lz) {}
}
