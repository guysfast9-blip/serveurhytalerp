package fr.aldermyr.hytale;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenLoadException;
import fr.aldermyr.coeur.Palette;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Traduit les noms de blocs de la palette en identifiants Hytale, avec replis et avertissements. */
final class Identifiants {

    static final String REPLI_FINAL = "Rock_Stone";
    private static final int INCONNU = Integer.MIN_VALUE;

    private final Palette palette;
    private final Consumer<String> avertir;
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();
    final int eau;
    final byte niveauEauMax;

    Identifiants(Palette palette, Consumer<String> avertir) throws WorldGenLoadException {
        this.palette = palette;
        this.avertir = avertir;
        int idEau = Fluid.getAssetMap().getIndex(palette.eau());
        if (idEau == INCONNU) {
            throw new WorldGenLoadException("Fluide d'eau inconnu dans la palette : " + palette.eau());
        }
        this.eau = idEau;
        this.niveauEauMax = (byte) Fluid.getAssetMap().getAsset(idEau).getMaxFluidLevel();
    }

    int bloc(String nom) {
        return cache.computeIfAbsent(nom, this::resoudre);
    }

    private int resoudre(String nom) {
        int id = BlockType.getAssetMap().getIndex(nom);
        if (id != INCONNU) return id;
        String repli = palette.repli(nom);
        int idRepli = repli == null ? INCONNU : BlockType.getAssetMap().getIndex(repli);
        if (idRepli == INCONNU) {
            repli = REPLI_FINAL;
            idRepli = BlockType.getAssetMap().getIndex(REPLI_FINAL);
        }
        avertir.accept("Bloc inconnu « " + nom + " » : remplacé par « " + repli
            + " ». Utilisez /aldermyr blocs <filtre> pour trouver le bon nom, puis corrigez palette.json.");
        return idRepli;
    }
}
