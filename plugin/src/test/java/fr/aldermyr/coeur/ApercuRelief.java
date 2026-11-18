package fr.aldermyr.coeur;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Outil (pas un test) : rend un ombrage du relief et des prefabs autour d'un point. */
public final class ApercuRelief {
    public static void main(String[] a) throws Exception {
        CartesMonde c = CartesMonde.charger(Path.of(a[0]));
        Palette p = Palette.defaut();
        Relief r = new Relief(c, p);
        Vegetation v = new Vegetation(c, p, r, Extrait.tout());
        int cx = Integer.parseInt(a[1]), cz = Integer.parseInt(a[2]), n = Integer.parseInt(a[3]);
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        int[][] h = new int[n + 1][n + 1];
        for (int z = 0; z <= n; z++) for (int x = 0; x <= n; x++) h[z][x] = r.altitude(cx - n / 2 + x, cz - n / 2 + z);
        for (int z = 0; z < n; z++) {
            for (int x = 0; x < n; x++) {
                int wx = cx - n / 2 + x, wz = cz - n / 2 + z;
                int pente = Math.max(Math.abs(h[z][x + 1] - h[z][x]), Math.abs(h[z + 1][x] - h[z][x]));
                PlanColonne.Plan pl = PlanColonne.planifier(c, p, wx, wz, h[z][x], pente);
                int coul = couleur(pl.blocs()[pl.surfaceY()], pl.teinte(), pl.eauJusquA() != null);
                double ombre = 1 + 0.12 * (h[z][x] - h[z + 1][x + 1]);
                ombre = Math.max(0.45, Math.min(1.5, ombre));
                int rr = (int) Math.min(255, ((coul >> 16) & 255) * ombre);
                int gg = (int) Math.min(255, ((coul >> 8) & 255) * ombre);
                int bb = (int) Math.min(255, (coul & 255) * ombre);
                img.setRGB(x, z, rr << 16 | gg << 8 | bb);
            }
        }
        for (int ky = cz - n / 2; ky < cz + n / 2; ky += 32) {
            for (int kx = cx - n / 2; kx < cx + n / 2; kx += 32) {
                for (Vegetation.Placement pl : v.placements(Math.floorDiv(kx, 32), Math.floorDiv(ky, 32))) {
                    int x = pl.x() - (cx - n / 2), z = pl.z() - (cz - n / 2);
                    int coul = pl.dossier().startsWith("Trees") ? 0x1E4A14 : pl.dossier().startsWith("Plants") ? 0x6A8A2A : 0x777777;
                    for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                        if (x + dx >= 0 && x + dx < n && z + dz >= 0 && z + dz < n) img.setRGB(x + dx, z + dz, coul);
                    }
                }
            }
        }
        ImageIO.write(img, "png", Path.of(a[4]).toFile());
    }

    static int couleur(String bloc, int teinte, boolean eau) {
        if (eau) return 0x3A6EA8;
        return switch (bloc) {
            case "Soil_Grass" -> teinte & 0xFFFFFF;
            case "Soil_Dirt" -> 0x7A5A3A;
            case "Soil_Mud" -> 0x5E5038;
            case "Soil_Gravel" -> 0x8A8580;
            case "Soil_Sand" -> 0xD8C890;
            case "Soil_Snow" -> 0xF2F4F8;
            case "Rock_Stone", "Rock_Granite" -> 0x8C8C8C;
            case "Rock_Shale" -> 0x6A6460;
            case "Rock_Basalt" -> 0x3A3640;
            case "Rock_Volcanic" -> 0x4A3A34;
            case "Rock_Obsidian" -> 0x201828;
            default -> 0xFF00FF;
        };
    }
}
