package fr.aldermyr.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.aldermyr.coeur.CartesMonde;
import fr.aldermyr.coeur.Description;
import fr.aldermyr.coeur.PlanColonne;
import java.util.List;
import java.util.Optional;
import org.joml.Vector3d;

/** /aldermyr ici | aller <lieu> | blocs <filtre> — outils du staff. */
final class CommandeAldermyr extends AbstractCommandCollection {

    static final String PAS_DE_MONDE = "Aucun monde Aldermyr n'est chargé. Créez-le avec /world add aldermyr --gen=Aldermyr.";

    CommandeAldermyr() {
        super("aldermyr", "Outils du staff pour le monde d'Aldermyr");
        addSubCommand(new Ici());
        addSubCommand(new Aller());
        addSubCommand(new Blocs());
    }

    private static void dire(CommandContext ctx, String texte) {
        ctx.sendMessage(Message.raw(texte));
    }

    /** /aldermyr ici : décrit la colonne sous le joueur. */
    static final class Ici extends AbstractPlayerCommand {
        Ici() {
            super("ici", "Décrit la région, l'altitude et l'eau prévues sous vos pieds");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef joueur, World monde) {
            CartesMonde cartes = AldermyrWorldGenProvider.cartesChargees();
            if (cartes == null) {
                dire(ctx, PAS_DE_MONDE);
                return;
            }
            Vector3d pos = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
            dire(ctx, Description.ici(cartes, (int) Math.floor(pos.x()), (int) Math.floor(pos.z())));
        }
    }

    /** /aldermyr aller <lieu> : téléporte le joueur au-dessus d'un lieu nommé. */
    static final class Aller extends AbstractPlayerCommand {
        private final RequiredArg<String> lieu =
            withRequiredArg("lieu", "Nom du lieu (accents facultatifs)", ArgTypes.GREEDY_STRING);

        Aller() {
            super("aller", "Téléporte au-dessus d'un lieu nommé d'Aldermyr");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef joueur, World monde) {
            CartesMonde cartes = AldermyrWorldGenProvider.cartesChargees();
            if (cartes == null) {
                dire(ctx, PAS_DE_MONDE);
                return;
            }
            Optional<CartesMonde.Lieu> l = Description.lieu(cartes, ctx.get(lieu));
            if (l.isEmpty()) {
                dire(ctx, "Lieu inconnu : " + ctx.get(lieu));
                return;
            }
            CartesMonde.Lieu cible = l.get();
            double y = PlanColonne.NIVEAU_MER_Y + Math.max(0, cartes.altitude(cible.x(), cible.z())) + 3;
            store.addComponent(ref, Teleport.getComponentType(),
                Teleport.createForPlayer(new Transform(cible.x() + 0.5, y, cible.z() + 0.5)));
            dire(ctx, "Direction : " + cible.nom());
        }
    }

    /** /aldermyr blocs <filtre> : liste les noms de blocs du jeu (pour compléter palette.json). */
    static final class Blocs extends CommandBase {
        private final RequiredArg<String> filtre =
            withRequiredArg("filtre", "Partie du nom recherché, par exemple sand", ArgTypes.STRING);

        Blocs() {
            super("blocs", "Liste les noms de blocs contenant un texte");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            List<String> noms = Description.filtrer(BlockType.getAssetMap().getAssetMap().keySet(), ctx.get(filtre));
            dire(ctx, noms.isEmpty() ? "Aucun bloc ne contient « " + ctx.get(filtre) + " »."
                : noms.size() + " résultat(s) : " + String.join(", ", noms));
        }
    }
}
