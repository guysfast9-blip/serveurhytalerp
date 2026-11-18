package fr.aldermyr.hytale;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import fr.aldermyr.coeur.Cible;

/** Écrit les blocs, l'eau et les teintes dans un chunk Hytale en cours de génération. */
final class CibleHytale implements Cible {

    private final GeneratedBlockChunk blocs;
    private final Holder<ChunkStore>[] sections;
    private final Identifiants ids;

    CibleHytale(GeneratedBlockChunk blocs, Holder<ChunkStore>[] sections, Identifiants ids) {
        this.blocs = blocs;
        this.sections = sections;
        this.ids = ids;
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
}
