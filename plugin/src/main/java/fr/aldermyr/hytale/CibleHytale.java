package fr.aldermyr.hytale;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import fr.aldermyr.coeur.Cible;
import java.util.List;

/** Écrit les blocs, l'eau et les teintes dans un chunk Hytale en cours de génération. */
final class CibleHytale implements Cible {

    private final GeneratedBlockChunk blocs;
    private final Holder<ChunkStore>[] sections;
    private final Identifiants ids;
    private final Prefabs prefabs;

    CibleHytale(GeneratedBlockChunk blocs, Holder<ChunkStore>[] sections, Identifiants ids, Prefabs prefabs) {
        this.blocs = blocs;
        this.sections = sections;
        this.ids = ids;
        this.prefabs = prefabs;
    }

    @Override
    public void bloc(int lx, int y, int lz, String nom) {
        blocs.setBlock(lx, y, lz, ids.bloc(nom), 0, 0);
    }

    @Override
    public void eau(int lx, int y, int lz) {
        FluidSection fluides = sections[y >> 5].ensureAndGetComponent(FluidSection.getComponentType());
        fluides.setFluid(lx, y, lz, ids.eau, ids.niveauEauMax);
    }

    @Override
    public void teinte(int lx, int lz, int argb) {
        blocs.setTint(lx, lz, argb);
    }

    @Override
    public void prefab(String dossier, long tirage, int lx, int y, int lz) {
        List<BlockSelection> variantes = prefabs.dossier(dossier);
        if (variantes.isEmpty()) return;
        BlockSelection s = variantes.get((int) Math.floorMod(tirage, (long) variantes.size()));
        int ox = lx + s.getX() - s.getAnchorX();
        int oy = y + s.getY() - s.getAnchorY();
        int oz = lz + s.getZ() - s.getAnchorZ();
        s.forEachBlock((bx, by, bz, b) -> {
            int x = ox + bx, yy = oy + by, z = oz + bz;
            if (x < 0 || x >= 32 || z < 0 || z >= 32 || yy < 0 || yy >= 320) return;
            if (b.blockId() == 0 || blocs.getBlock(x, yy, z) != 0) return;
            blocs.setBlock(x, yy, z, b.blockId(), b.rotation(), b.filler());
        });
    }
}
