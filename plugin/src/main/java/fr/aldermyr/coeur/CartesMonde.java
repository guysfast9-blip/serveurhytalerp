package fr.aldermyr.coeur;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Cartes de données produites par l'atelier (altitude, régions, eau, routes, lieux).
 * Toutes les méthodes d'accès prennent des coordonnées de bloc centrées : le bloc (0, 0) est le
 * centre de l'image. Hors de la carte, c'est la haute mer.
 */
public final class CartesMonde {

    public static final int LARGEUR = 4800, HAUTEUR = 3600, DECALAGE = 1000;
    static final int ALT_HAUTE_MER = -40;

    public record Lieu(String nom, String type, int x, int z, int rayon) {}

    private final short[] altitude = new short[LARGEUR * HAUTEUR];
    private final byte[] region = new byte[LARGEUR * HAUTEUR];
    private final short[] eau = new short[LARGEUR * HAUTEUR];   // 0 = sec, sinon niveau + DECALAGE
    private final BitSet routes = new BitSet(LARGEUR * HAUTEUR);
    private final List<Lieu> lieux = new ArrayList<>();

    private CartesMonde() {}

    /** Un monde entièrement marin. */
    public static CartesMonde mer() {
        CartesMonde c = new CartesMonde();
        Arrays.fill(c.altitude, (short) ALT_HAUTE_MER);
        Arrays.fill(c.eau, (short) DECALAGE);
        return c;
    }

    /** Monde marin modifiable, pour les tests. */
    static CartesMonde vide() {
        return mer();
    }

    /** Pose une colonne (tests uniquement). */
    void poser(int x, int z, int alt, int reg, Integer niveau, boolean route) {
        int i = indice(x, z);
        altitude[i] = (short) alt;
        region[i] = (byte) reg;
        eau[i] = niveau == null ? 0 : (short) (niveau + DECALAGE);
        routes.set(i, route);
    }

    /** Pose un rectangle de terre sèche, sans route (tests uniquement). */
    void poserZone(int x0, int z0, int largeur, int hauteur, int alt, int reg) {
        for (int z = z0; z < z0 + hauteur; z++) {
            for (int x = x0; x < x0 + largeur; x++) poser(x, z, alt, reg, null, false);
        }
    }

    /** Ajoute un lieu (tests uniquement). */
    void ajouterLieu(Lieu l) {
        lieux.add(l);
    }

    public static CartesMonde charger(Path dossier) throws IOException {
        CartesMonde c = new CartesMonde();
        Raster alt = lireImage(dossier, "altitude.png");
        Raster eau = lireImage(dossier, "eau.png");
        Raster reg = lireImage(dossier, "regions.png");
        Raster rou = lireImage(dossier, "routes.png");
        for (int z = 0; z < HAUTEUR; z++) {
            for (int x = 0; x < LARGEUR; x++) {
                int i = z * LARGEUR + x;
                c.altitude[i] = (short) (alt.getSample(x, z, 0) - DECALAGE);
                c.eau[i] = (short) eau.getSample(x, z, 0);
                c.region[i] = (byte) reg.getSample(x, z, 0);
                if (rou.getSample(x, z, 0) > 0) c.routes.set(i);
            }
        }
        Path json = dossier.resolve("lieux.json");
        if (!Files.exists(json)) throw new IOException("Fichier de carte introuvable : " + json);
        try (Reader r = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            List<Lieu> l = new Gson().fromJson(r, new TypeToken<List<Lieu>>() {}.getType());
            if (l != null) c.lieux.addAll(l);
        }
        return c;
    }

    private static Raster lireImage(Path dossier, String nom) throws IOException {
        Path f = dossier.resolve(nom);
        if (!Files.exists(f)) throw new IOException("Fichier de carte introuvable : " + f);
        BufferedImage im = ImageIO.read(f.toFile());
        if (im == null) throw new IOException(nom + " : image illisible");
        if (im.getWidth() != LARGEUR || im.getHeight() != HAUTEUR) {
            throw new IOException(nom + " : " + LARGEUR + "×" + HAUTEUR + " attendu, "
                + im.getWidth() + "×" + im.getHeight() + " trouvé");
        }
        return im.getRaster();
    }

    private static int indice(int x, int z) {
        int px = x + LARGEUR / 2, pz = z + HAUTEUR / 2;
        if (px < 0 || pz < 0 || px >= LARGEUR || pz >= HAUTEUR) return -1;
        return pz * LARGEUR + px;
    }

    public int altitude(int x, int z) {
        int i = indice(x, z);
        return i < 0 ? ALT_HAUTE_MER : altitude[i];
    }

    public int region(int x, int z) {
        int i = indice(x, z);
        return i < 0 ? 0 : region[i] & 0xFF;
    }

    /** Niveau de la surface de l'eau (blocs au-dessus de la mer), ou null s'il n'y a pas d'eau. */
    public Integer niveauEau(int x, int z) {
        int i = indice(x, z);
        if (i < 0) return 0;
        int v = eau[i] & 0xFFFF;
        return v == 0 ? null : v - DECALAGE;
    }

    public boolean route(int x, int z) {
        int i = indice(x, z);
        return i >= 0 && routes.get(i);
    }

    public List<Lieu> lieux() {
        return lieux;
    }
}
