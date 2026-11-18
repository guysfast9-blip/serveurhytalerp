package fr.aldermyr.coeur;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CartesMondeTest {

    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder();

    static Path dossier;
    static CartesMonde cartes;

    /** Écrit un jeu de cartes complet : mer partout, sauf quelques pixels de test. */
    static Path ecrireCartes(Path dir, int largeurRegions, int hauteurRegions) throws IOException {
        Files.createDirectories(dir);
        BufferedImage alt = new BufferedImage(4800, 3600, BufferedImage.TYPE_USHORT_GRAY);
        BufferedImage eau = new BufferedImage(4800, 3600, BufferedImage.TYPE_USHORT_GRAY);
        BufferedImage reg = new BufferedImage(largeurRegions, hauteurRegions, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage rou = new BufferedImage(4800, 3600, BufferedImage.TYPE_BYTE_GRAY);
        alt.getRaster().setSample(2400, 1800, 0, 1050);
        alt.getRaster().setSample(0, 0, 0, 960);
        eau.getRaster().setSample(2401, 1800, 0, 1003);
        if (largeurRegions > 2400) reg.getRaster().setSample(2400, 1800, 0, 1);
        rou.getRaster().setSample(2400, 1800, 0, 255);
        ImageIO.write(alt, "png", dir.resolve("altitude.png").toFile());
        ImageIO.write(eau, "png", dir.resolve("eau.png").toFile());
        ImageIO.write(reg, "png", dir.resolve("regions.png").toFile());
        ImageIO.write(rou, "png", dir.resolve("routes.png").toFile());
        Files.writeString(dir.resolve("lieux.json"),
            "[{\"nom\":\"Aurelia\",\"type\":\"capitale\",\"x\":0,\"z\":0,\"rayon\":60},"
                + "{\"nom\":\"Cœur-Sylve\",\"type\":\"capitale\",\"x\":1728,\"z\":-360,\"rayon\":60}]",
            StandardCharsets.UTF_8);
        return dir;
    }

    @BeforeClass
    public static void preparer() throws IOException {
        dossier = ecrireCartes(tmp.getRoot().toPath().resolve("ok"), 4800, 3600);
        cartes = CartesMonde.charger(dossier);
    }

    @Test
    public void chargeAltitudeEtConvertitLesCoordonnees() {
        assertEquals(50, cartes.altitude(0, 0));
        assertEquals(-40, cartes.altitude(-2400, -1800));
        assertEquals(1, cartes.region(0, 0));
        assertTrue(cartes.route(0, 0));
        assertFalse(cartes.route(1, 0));
    }

    @Test
    public void eauNullSurLeSec() {
        assertNull(cartes.niveauEau(0, 0));
        assertEquals(Integer.valueOf(3), cartes.niveauEau(1, 0));
    }

    @Test
    public void horsCarteCestLaMer() {
        assertEquals(-40, cartes.altitude(5000, 0));
        assertEquals(0, cartes.region(5000, 0));
        assertEquals(Integer.valueOf(0), cartes.niveauEau(5000, 0));
        assertFalse(cartes.route(5000, 0));
    }

    @Test
    public void lieuxLus() {
        assertEquals(2, cartes.lieux().size());
        CartesMonde.Lieu l = cartes.lieux().get(1);
        assertEquals("Cœur-Sylve", l.nom());
        assertEquals(1728, l.x());
        assertEquals(60, l.rayon());
    }

    @Test
    public void fichierManquantMessageClair() throws IOException {
        Path d = tmp.getRoot().toPath().resolve("incomplet");
        Files.createDirectories(d);
        for (String f : new String[] {"altitude.png", "regions.png", "routes.png", "lieux.json"}) {
            Files.copy(dossier.resolve(f), d.resolve(f));
        }
        try {
            CartesMonde.charger(d);
            fail("une IOException était attendue");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("eau.png"));
        }
    }

    @Test
    public void mauvaiseTailleRefusee() throws IOException {
        Path d = ecrireCartes(tmp.getRoot().toPath().resolve("petit"), 100, 100);
        try {
            CartesMonde.charger(d);
            fail("une IOException était attendue");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("4800×3600"));
        }
    }
}
