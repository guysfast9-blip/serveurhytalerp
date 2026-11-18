package fr.aldermyr.hytale;

// API lue dans HytaleServer.jar 0.7.7 (javap) :
//  - PrefabStore.get().getAssetPrefabDir(String) → Map<Path, BlockSelection> ;
//  - position monde d'un bloc = ancre + local + (getX() − getAnchorX()), d'après computePlacementBounds ;
//  - GeneratedBlockChunk.setBlock(x, y, z, id, rotation, filler) ; BlockType.EMPTY_ID = 0.

import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Charge (une seule fois) les prefabs d'un dossier du jeu, triés pour un choix déterministe. */
final class Prefabs {

    private final Map<String, List<BlockSelection>> cache = new ConcurrentHashMap<>();
    private final Consumer<String> avertir;

    Prefabs(Consumer<String> avertir) {
        this.avertir = avertir;
    }

    List<BlockSelection> dossier(String dossier) {
        return cache.computeIfAbsent(dossier, this::charger);
    }

    private List<BlockSelection> charger(String dossier) {
        try {
            Map<Path, BlockSelection> trouves = PrefabStore.get().getAssetPrefabDir(dossier);
            if (trouves == null || trouves.isEmpty()) {
                avertir.accept("Aucun prefab dans « " + dossier + " » : vérifiez ce dossier dans palette.json.");
                return List.of();
            }
            return trouves.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        } catch (RuntimeException e) {
            avertir.accept("Prefabs « " + dossier + " » illisibles (" + e.getMessage()
                + ") : vérifiez ce dossier dans palette.json.");
            return List.of();
        }
    }
}
