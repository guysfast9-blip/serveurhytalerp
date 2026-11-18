package fr.aldermyr.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import java.util.logging.Level;

/** Point d'entrée : enregistre le générateur de monde « Aldermyr ». */
public final class AldermyrPlugin extends JavaPlugin {

    public static final String TYPE_GENERATEUR = "Aldermyr";
    private static AldermyrPlugin instance;

    public AldermyrPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static AldermyrPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        getCodecRegistry(IWorldGenProvider.CODEC)
            .register(TYPE_GENERATEUR, AldermyrWorldGenProvider.class, AldermyrWorldGenProvider.CODEC);
        getCommandRegistry().registerCommand(new CommandeAldermyr());
        try {
            java.nio.file.Files.createDirectories(getDataDirectory().resolve("donnees"));
        } catch (java.io.IOException e) {
            avertir("Impossible de créer le dossier donnees : " + e.getMessage());
        }
        getLogger().at(Level.INFO).log("Générateur Aldermyr enregistré. Données attendues dans : %s",
            getDataDirectory().resolve("donnees").toAbsolutePath());
    }

    void avertir(String message) {
        getLogger().at(Level.WARNING).log("%s", message);
    }
}
