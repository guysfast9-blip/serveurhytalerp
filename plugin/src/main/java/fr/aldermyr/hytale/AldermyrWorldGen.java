package fr.aldermyr.hytale;

// API lue dans HytaleServer.jar 0.7.7 (javap -c) :
//  - FlatWorldGen.getTimings() renvoie null ;
//  - GeneratedEntityChunk a un constructeur public sans argument ;
//  - getCodecRegistry(StringCodecMapCodec).register(String, Class<? extends T>, C).

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedEntityChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenTimingsCollector;
import fr.aldermyr.coeur.CartesMonde;
import fr.aldermyr.coeur.PlanColonne;
import fr.aldermyr.coeur.Remplisseur;
import it.unimi.dsi.fastutil.longs.Long2FloatFunction;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongPredicate;

/** Génère chaque chunk du monde d'Aldermyr à partir des cartes de données. */
final class AldermyrWorldGen implements IWorldGen {

    static final String LIEU_APPARITION = "Port-Écume";

    private final CartesMonde cartes;
    private final Remplisseur remplisseur;
    private final Identifiants ids;
    private final Prefabs prefabs;

    AldermyrWorldGen(CartesMonde cartes, Remplisseur remplisseur, Identifiants ids, Prefabs prefabs) {
        this.cartes = cartes;
        this.remplisseur = remplisseur;
        this.ids = ids;
        this.prefabs = prefabs;
    }

    @Override
    public WorldGenTimingsCollector getTimings() {
        return null;
    }

    @Override
    public CompletableFuture<GeneratedChunk> generate(int graine, long index, int cx, int cz,
                                                      LongPredicate encoreUtile, Long2FloatFunction inutilise) {
        GeneratedBlockChunk blocs = new GeneratedBlockChunk(index, cx, cz);
        Holder<ChunkStore>[] sections = GeneratedChunk.makeSections();
        remplisseur.remplir(cx, cz, new CibleHytale(blocs, sections, ids, prefabs));
        return CompletableFuture.completedFuture(new GeneratedChunk(blocs, new GeneratedEntityChunk(), sections));
    }

    @Override
    public Transform[] getSpawnPoints(int graine) {
        for (CartesMonde.Lieu l : cartes.lieux()) {
            if (LIEU_APPARITION.equals(l.nom())) {
                int y = PlanColonne.NIVEAU_MER_Y + cartes.altitude(l.x(), l.z()) + 2;
                return new Transform[] {new Transform(l.x() + 0.5, y, l.z() + 0.5)};
            }
        }
        return new Transform[] {new Transform(0.5, 120, 0.5)};
    }
}
