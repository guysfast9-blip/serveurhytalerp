package fr.aldermyr.coeur;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Blocs utilisés pour chaque région et pour les cas particuliers (eau, routes, neige…). */
public final class Palette {

    public record Couches(String surface, String sousCouche, String profondeur, String teinte) {}

    public record Variante(String bloc, double part) {}

    public record Prefab(String dossier, double poids) {}

    public record Vegetation(double densite, java.util.List<Prefab> prefabs) {}

    public record Plante(String bloc, double poids) {}

    /** Herbes, fougères et fleurs posées d'un bloc au-dessus du sol. */
    public record Sol(double densite, java.util.List<Plante> blocs) {}

    /** Réglages du relief, des surfaces et de la végétation d'une région. */
    public record Region(double relief, double crete, String falaise, int rocheAuDessus,
                         java.util.List<Variante> variantes, Vegetation vegetation, Sol sol) {}

    private final JsonObject json;
    private final int versionFichier;
    private final Region[] regions = new Region[256];
    private volatile java.util.Set<String> fertiles;

    private Palette(JsonObject json, int versionFichier) {
        this.json = json;
        this.versionFichier = versionFichier;
    }

    public static Palette defaut() {
        JsonObject j = lireDefaut();
        return new Palette(j, j.get("version").getAsInt());
    }

    /** Lit une palette ; toute clé absente reprend la valeur par défaut. */
    public static Palette lire(Reader r) {
        JsonObject base = lireDefaut();
        JsonObject perso = new Gson().fromJson(r, JsonObject.class);
        int version = perso != null && perso.has("version") ? perso.get("version").getAsInt() : 1;
        if (perso != null) fusionner(base, perso);
        return new Palette(base, version);
    }

    /** Le JSON complet (valeurs par défaut comprises), pour l'écrire sur disque. */
    public String enJson() {
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
    }

    private static JsonObject lireDefaut() {
        try (InputStream in = Palette.class.getResourceAsStream("/palette-defaut.json")) {
            if (in == null) throw new IllegalStateException("palette-defaut.json absente du plugin");
            return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void fusionner(JsonObject base, JsonObject perso) {
        for (Map.Entry<String, JsonElement> e : perso.entrySet()) {
            JsonElement b = base.get(e.getKey());
            if (b != null && b.isJsonObject() && e.getValue().isJsonObject()) {
                fusionner(b.getAsJsonObject(), e.getValue().getAsJsonObject());
            } else {
                base.add(e.getKey(), e.getValue());
            }
        }
    }

    public Couches couches(int region) {
        JsonObject regions = json.getAsJsonObject("regions");
        JsonObject r = regions.getAsJsonObject(Integer.toString(region));
        if (r == null) r = regions.getAsJsonObject("0");
        return new Couches(r.get("surface").getAsString(), r.get("sousCouche").getAsString(),
            r.get("profondeur").getAsString(), r.get("teinte").getAsString());
    }

    /** Version du fichier lu (1 si la clé "version" est absente). */
    public int version() { return versionFichier; }
    public int penteFalaise() { return json.get("penteFalaise").getAsInt(); }
    public int limiteArbres() { return json.get("limiteArbres").getAsInt(); }
    /** Décalage vertical des prefabs (si les arbres flottent ou s'enfoncent d'un bloc). */
    public int decalagePrefabs() { return json.has("decalagePrefabs") ? json.get("decalagePrefabs").getAsInt() : 0; }

    /** Surfaces sur lesquelles poussent les plantes du sol. */
    public java.util.Set<String> solsFertiles() {
        java.util.Set<String> s = fertiles;
        if (s == null) {
            java.util.Set<String> noms = new java.util.HashSet<>();
            if (json.has("solsFertiles")) json.getAsJsonArray("solsFertiles").forEach(e -> noms.add(e.getAsString()));
            fertiles = s = java.util.Set.copyOf(noms);
        }
        return s;
    }

    /** Tous les noms de plantes du sol, toutes régions confondues. */
    public java.util.Set<String> toutesLesPlantes() {
        java.util.Set<String> noms = new java.util.HashSet<>();
        for (String cle : json.getAsJsonObject("regions").keySet()) {
            for (Plante p : region(Integer.parseInt(cle)).sol().blocs()) noms.add(p.bloc());
        }
        return noms;
    }

    public Region region(int region) {
        Region r = regions[region & 0xFF];
        if (r == null) {
            r = lireRegion(region);
            regions[region & 0xFF] = r;
        }
        return r;
    }

    private Region lireRegion(int region) {
        JsonObject toutes = json.getAsJsonObject("regions");
        JsonObject o = toutes.getAsJsonObject(Integer.toString(region));
        if (o == null) o = toutes.getAsJsonObject("0");
        java.util.List<Variante> variantes = new java.util.ArrayList<>();
        if (o.has("variantes")) {
            for (JsonElement e : o.getAsJsonArray("variantes")) {
                JsonObject v = e.getAsJsonObject();
                variantes.add(new Variante(v.get("bloc").getAsString(), v.get("part").getAsDouble()));
            }
        }
        double densite = 0;
        java.util.List<Prefab> prefabs = new java.util.ArrayList<>();
        if (o.has("vegetation")) {
            JsonObject veg = o.getAsJsonObject("vegetation");
            densite = veg.has("densite") ? veg.get("densite").getAsDouble() : 0;
            if (veg.has("prefabs")) {
                for (JsonElement e : veg.getAsJsonArray("prefabs")) {
                    JsonObject pf = e.getAsJsonObject();
                    prefabs.add(new Prefab(pf.get("dossier").getAsString(), pf.get("poids").getAsDouble()));
                }
            }
        }
        double densiteSol = 0;
        java.util.List<Plante> plantes = new java.util.ArrayList<>();
        if (o.has("sol")) {
            JsonObject sol = o.getAsJsonObject("sol");
            densiteSol = sol.has("densite") ? sol.get("densite").getAsDouble() : 0;
            if (sol.has("blocs")) {
                for (JsonElement e : sol.getAsJsonArray("blocs")) {
                    JsonObject pl = e.getAsJsonObject();
                    plantes.add(new Plante(pl.get("bloc").getAsString(), pl.get("poids").getAsDouble()));
                }
            }
        }
        String surface = o.get("surface").getAsString();
        return new Region(
            o.has("relief") ? o.get("relief").getAsDouble() : 0,
            o.has("crete") ? o.get("crete").getAsDouble() : 0,
            o.has("falaise") ? o.get("falaise").getAsString() : surface,
            o.has("rocheAuDessus") ? o.get("rocheAuDessus").getAsInt() : Integer.MAX_VALUE,
            java.util.List.copyOf(variantes), new Vegetation(densite, java.util.List.copyOf(prefabs)),
            new Sol(plantes.isEmpty() ? 0 : densiteSol, java.util.List.copyOf(plantes)));
    }

    private String texte(String cle) {
        return json.get(cle).getAsString();
    }

    public String neige() { return texte("neige"); }
    public int altitudeNeige() { return json.get("altitudeNeige").getAsInt(); }
    public String litEau() { return texte("litEau"); }
    public String fondMer() { return texte("fondMer"); }
    public String route() { return texte("route"); }
    public String obsidienne() { return texte("obsidienne"); }
    public String volcan() { return texte("volcan"); }
    public String eau() { return texte("eau"); }

    /** Noms de fluide à essayer pour l'eau, dans l'ordre : celui de la palette, puis les noms connus du jeu. */
    public java.util.List<String> candidatsEau() {
        java.util.LinkedHashSet<String> noms = new java.util.LinkedHashSet<>();
        noms.add(eau());
        noms.addAll(java.util.List.of("Water_Source", "Fluid_Water", "Water"));
        return java.util.List.copyOf(noms);
    }

    /** Bloc de remplacement prévu pour un nom, ou null s'il n'y en a pas. */
    public String repli(String nom) {
        JsonObject replis = json.getAsJsonObject("replis");
        JsonElement e = replis == null ? null : replis.get(nom);
        return e == null ? null : e.getAsString();
    }

    public static int argb(String teinte) {
        return 0xFF000000 | Integer.parseInt(teinte.substring(1), 16);
    }
}
