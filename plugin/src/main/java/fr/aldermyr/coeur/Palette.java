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

    private final JsonObject json;

    private Palette(JsonObject json) {
        this.json = json;
    }

    public static Palette defaut() {
        return new Palette(lireDefaut());
    }

    /** Lit une palette ; toute clé absente reprend la valeur par défaut. */
    public static Palette lire(Reader r) {
        JsonObject base = lireDefaut();
        JsonObject perso = new Gson().fromJson(r, JsonObject.class);
        if (perso != null) fusionner(base, perso);
        return new Palette(base);
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
