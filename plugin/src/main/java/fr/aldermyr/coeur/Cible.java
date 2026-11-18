package fr.aldermyr.coeur;

/** Destination d'un chunk en cours de génération, en coordonnées locales (0 à 31 en x et z). */
public interface Cible {
    void bloc(int lx, int y, int lz, String nom);

    void eau(int lx, int y, int lz);

    void teinte(int lx, int lz, int argb);
}
