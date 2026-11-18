package fr.aldermyr.coeur;

/** Bruit de valeur déterministe (sans état), utilisé pour le relief, les surfaces et la végétation. */
public final class Bruit {

    private Bruit() {}

    /** Mélange (x, z, graine) en un entier 64 bits bien réparti. */
    public static long hachage(int graine, int x, int z) {
        long h = graine * 0x9E3779B97F4A7C15L ^ x * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    /** Valeur pseudo-aléatoire dans [0, 1) attachée au point entier (x, z). */
    public static double valeur(int graine, int x, int z) {
        return (hachage(graine, x, z) >>> 11) * 0x1.0p-53;
    }

    private static double lisse(double t) {
        return t * t * (3 - 2 * t);
    }

    /** Bruit de valeur interpolé dans [-1, 1], pour une longueur d'onde de 1. */
    static double interpole(int graine, double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double tx = lisse(x - x0), tz = lisse(z - z0);
        double a = valeur(graine, x0, z0), b = valeur(graine, x0 + 1, z0);
        double c = valeur(graine, x0, z0 + 1), d = valeur(graine, x0 + 1, z0 + 1);
        double haut = a + (b - a) * tx, bas = c + (d - c) * tx;
        return (haut + (bas - haut) * tz) * 2 - 1;
    }

    /** Somme de plusieurs octaves, normalisée dans [-1, 1]. */
    public static double fbm(int graine, double x, double z, double onde, int octaves) {
        double somme = 0, amplitude = 1, total = 0, f = 1 / onde;
        for (int o = 0; o < octaves; o++) {
            somme += amplitude * interpole(graine + o * 101, x * f, z * f);
            total += amplitude;
            amplitude *= 0.5;
            f *= 2;
        }
        return somme / total;
    }
}
