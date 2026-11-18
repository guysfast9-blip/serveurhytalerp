package fr.aldermyr.hytale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenLoadException;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import fr.aldermyr.coeur.CartesMonde;
import fr.aldermyr.coeur.Extrait;
import fr.aldermyr.coeur.Palette;
import fr.aldermyr.coeur.Remplisseur;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fournisseur du générateur « Aldermyr ». Réglages facultatifs du monde :
 * "Dossier" (dossier des cartes de données) et "Extrait" ("x,z,largeur,hauteur").
 */
public final class AldermyrWorldGenProvider implements IWorldGenProvider {

    public static final BuilderCodec<AldermyrWorldGenProvider> CODEC = BuilderCodec
        .builder(AldermyrWorldGenProvider.class, AldermyrWorldGenProvider::new)
        .documentation("Génère le monde d'Aldermyr à partir des cartes de données de l'atelier.")
        .append(new KeyedCodec<>("Dossier", Codec.STRING), (p, v) -> p.dossier = v, p -> p.dossier)
        .documentation("Dossier des cartes (par défaut : <dossier du plugin>/donnees).")
        .add()
        .append(new KeyedCodec<>("Extrait", Codec.STRING), (p, v) -> p.extrait = v, p -> p.extrait)
        .documentation("Rectangle à générer, \"x,z,largeur,hauteur\" en blocs ; ailleurs, la mer.")
        .add()
        .build();

    private static final Map<Path, CartesMonde> CARTES = new ConcurrentHashMap<>();
    private static volatile CartesMonde dernieresCartes;

    private String dossier;
    private String extrait;

    /** Les cartes du dernier monde Aldermyr chargé (null si aucun), pour les commandes du staff. */
    public static CartesMonde cartesChargees() {
        return dernieresCartes;
    }

    @Override
    public IWorldGen getGenerator() throws WorldGenLoadException {
        AldermyrPlugin plugin = AldermyrPlugin.get();
        Path base = plugin.getDataDirectory();
        Path dir = dossier == null || dossier.isBlank() ? base.resolve("donnees") : Path.of(dossier);
        try {
            CartesMonde cartes = CARTES.computeIfAbsent(dir.toAbsolutePath(), d -> {
                try {
                    return CartesMonde.charger(d);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
            dernieresCartes = cartes;
            Palette palette = chargerPalette(base.resolve("palette.json"));
            Extrait zone = extrait == null || extrait.isBlank() ? Extrait.tout() : Extrait.lire(extrait);
            Identifiants ids = new Identifiants(palette, plugin::avertir);
            return new AldermyrWorldGen(cartes, new Remplisseur(cartes, palette, zone), ids);
        } catch (java.io.UncheckedIOException e) {
            throw new WorldGenLoadException(e.getCause().getMessage());
        } catch (IOException | IllegalArgumentException e) {
            throw new WorldGenLoadException(e.getMessage());
        }
    }

    private static Palette chargerPalette(Path fichier) throws IOException {
        if (Files.exists(fichier)) {
            try (Reader r = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
                return Palette.lire(r);
            }
        }
        Palette defaut = Palette.defaut();
        Files.createDirectories(fichier.getParent());
        Files.writeString(fichier, defaut.enJson(), StandardCharsets.UTF_8);
        return defaut;
    }
}
