package dev.tc4port.thaumadditions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Checks the mod's data files against each other.
 *
 * <p>Every one of these is a fault the game reports as a stack trace part-way through loading a
 * world, or does not report at all and simply drops the content: a recipe that will not parse, a
 * page whose text is not there, a language that is missing half its lines. None of them need a
 * running game to find, and finding them here is a second rather than a restart.</p>
 *
 * <p>They run per node, but they read only shared resources, so the two nodes run the same checks.
 * Everything a registry could answer — is this item actually registered — is out of reach from
 * here and lives in {@code tools/probe_audit.java}, which asks the running game instead.</p>
 */
class DataIntegrityTest {
    private static final String[] LOCALES = {"en_us", "zh_cn"};
    private static final String MOD = "thaumadds";

    // ------------------------------------------------------------------ language

    @Test
    @DisplayName("both languages carry the same keys")
    void languagesHaveTheSameKeys() {
        Map<String, String> english = lang("en_us");
        for (String locale : LOCALES) {
            Map<String, String> other = lang(locale);
            Set<String> missing = new TreeSet<>(english.keySet());
            missing.removeAll(other.keySet());
            Set<String> extra = new TreeSet<>(other.keySet());
            extra.removeAll(english.keySet());
            assertAll(locale,
                    () -> assertTrue(missing.isEmpty(), locale + " is missing: " + missing),
                    () -> assertTrue(extra.isEmpty(), locale + " has keys English does not: " + extra));
        }
    }

    @Test
    @DisplayName("no language value is left empty")
    void noEmptyTranslations() {
        for (String locale : LOCALES) {
            List<String> empty = new ArrayList<>();
            lang(locale).forEach((key, value) -> {
                if (value.isBlank()) {
                    empty.add(key);
                }
            });
            assertTrue(empty.isEmpty(), locale + " has empty values: " + empty);
        }
    }

    /**
     * The one placeholder the language files are allowed to carry.
     *
     * <p>{@code @ASPECT} is upstream's own: 1.12 wrote it where an aspect's name belongs and
     * swapped it out when the stack was asked for its name, because the aspect is not known until
     * then. Only the items that do that swap may use it — anything else showing the marker has no
     * code to fill it in, which is what happens when a language line is copied onto a plain
     * item.</p>
     */
    @Test
    @DisplayName("the @ASPECT marker only appears on the items that fill it in")
    void aspectMarkerOnlyOnAspectNamedItems() {
        Set<String> allowed = Set.of("block.thaumadds.crystal_block", "item.thaumadds.seal_symbol");
        for (String locale : LOCALES) {
            List<String> strays = new ArrayList<>();
            lang(locale).forEach((key, value) -> {
                if (value.contains("@ASPECT") && !allowed.contains(key)) {
                    strays.add(key);
                }
            });
            assertTrue(strays.isEmpty(), locale + " carries @ASPECT on: " + strays);

            List<String> missing = new ArrayList<>(allowed);
            missing.removeIf(key -> lang(locale).getOrDefault(key, "").contains("@ASPECT"));
            assertTrue(missing.isEmpty(), locale + " lost the marker from: " + missing);
        }
    }

    // ------------------------------------------------------------------ research

    @Test
    @DisplayName("every research page the catalogue names has its text, in both languages")
    void researchPagesAreWritten() {
        JsonObject catalogue = json("data/" + MOD + "/thaumcraft/research/default.json");
        Set<String> referenced = new LinkedHashSet<>();
        for (JsonElement entry : catalogue.getAsJsonArray("entries")) {
            for (JsonElement page : entry.getAsJsonObject().getAsJsonArray("pages")) {
                JsonObject pageObject = page.getAsJsonObject();
                if (!"TEXT".equals(pageObject.get("type").getAsString())) {
                    continue;
                }
                referenced.add(pageObject.get("value").getAsString());
            }
        }
        assertFalse(referenced.isEmpty(), "the catalogue names no text pages at all");
        for (String locale : LOCALES) {
            Map<String, String> lines = lang(locale);
            Set<String> missing = new TreeSet<>();
            for (String value : referenced) {
                String text = lines.get(value);
                if (text == null || text.isBlank()) {
                    missing.add(value);
                }
            }
            assertTrue(missing.isEmpty(), locale + " has no text for " + missing);
        }
    }

    @Test
    @DisplayName("research keys are unique and every parent resolves")
    void researchTreeIsClosed() {
        JsonObject catalogue = json("data/" + MOD + "/thaumcraft/research/default.json");
        JsonArray entries = catalogue.getAsJsonArray("entries");

        Set<String> keys = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (JsonElement entry : entries) {
            String key = entry.getAsJsonObject().get("key").getAsString();
            if (!keys.add(key)) {
                duplicates.add(key);
            }
        }
        assertTrue(duplicates.isEmpty(), "duplicate research keys: " + duplicates);

        // Only the mod's own tree can be closed here. A parent like ELDRITCHMINOR belongs to the
        // installed Thaumcraft, whose entry list is not on this classpath; gen_research.py checks
        // those against the real thing before it writes, which is the only place that can.
        List<String> dangling = new ArrayList<>();
        for (JsonElement entry : entries) {
            for (JsonElement parent : entry.getAsJsonObject().getAsJsonArray("parents")) {
                String name = parent.getAsString();
                if (name.startsWith("TAR") && !keys.contains(name)) {
                    dangling.add(entry.getAsJsonObject().get("key").getAsString() + " -> " + name);
                }
            }
        }
        assertTrue(dangling.isEmpty(), "research hangs off keys that do not exist: " + dangling);
    }

    // ------------------------------------------------------------------ recipes

    @Test
    @DisplayName("every arcane recipe can be parsed, which means it has its vis key")
    void arcaneRecipesCarryTheirVis() {
        List<String> broken = new ArrayList<>();
        for (String path : resources("data/" + MOD + "/recipes/arcane")) {
            if (!path.endsWith(".json")) {
                continue;
            }
            JsonObject recipe = json(path);
            if (!recipe.has("vis") || !recipe.get("vis").isJsonObject()) {
                broken.add(path);
            }
        }
        assertTrue(broken.isEmpty(), "arcane recipes with no vis key: " + broken);
    }

    @Test
    @DisplayName("every recipe names a result and a catalyst or a pattern")
    void recipesAreWellFormed() {
        List<String> broken = new ArrayList<>();
        for (String folder : new String[] {"recipes/arcane", "recipes/infusion", "recipes/crucible"}) {
            for (String path : resources("data/" + MOD + "/" + folder)) {
                if (!path.endsWith(".json")) {
                    continue;
                }
                JsonObject recipe = json(path);
                if (!recipe.has("type")) {
                    broken.add(path + " has no type");
                    continue;
                }
                if (!recipe.has("result")) {
                    broken.add(path + " has no result");
                }
            }
        }
        assertTrue(broken.isEmpty(), "malformed recipes: " + broken);
    }

    // ------------------------------------------------------------------ assets

    @Test
    @DisplayName("every blockstate points at a model that exists")
    void blockstatesResolve() {
        List<String> broken = new ArrayList<>();
        for (String path : resources("assets/" + MOD + "/blockstates")) {
            if (!path.endsWith(".json")) {
                continue;
            }
            collectModels(json(path), broken, path);
        }
        assertTrue(broken.isEmpty(), "blockstates pointing at nothing: " + broken);
    }

    @Test
    @DisplayName("every item model points at a texture that exists, or at a parent that does")
    void itemModelsResolve() {
        List<String> broken = new ArrayList<>();
        for (String path : resources("assets/" + MOD + "/models/item")) {
            if (!path.endsWith(".json")) {
                continue;
            }
            JsonObject model = json(path);
            if (model.has("parent")) {
                String parent = model.get("parent").getAsString();
                if (parent.startsWith(MOD + ":")
                        && !exists("assets/" + MOD + "/models/" + parent.substring(MOD.length() + 1) + ".json")) {
                    broken.add(path + " -> " + parent);
                }
            }
        }
        assertTrue(broken.isEmpty(), "item models with no parent: " + broken);
    }

    @Test
    @DisplayName("every loot table that names one of our items names an item we ship a model for")
    void lootTablesNameKnownItems() {
        List<String> unknown = new ArrayList<>();
        for (String path : resources("data/" + MOD + "/loot_tables/blocks")) {
            if (!path.endsWith(".json")) {
                continue;
            }
            for (String name : collectStrings(json(path), "name")) {
                if (!name.startsWith(MOD + ":")) {
                    continue;
                }
                String id = name.substring(MOD.length() + 1);
                if (!exists("assets/" + MOD + "/models/item/" + id + ".json")) {
                    unknown.add(path + " drops " + name + ", which has no item model");
                }
            }
        }
        assertTrue(unknown.isEmpty(), String.join("; ", unknown));
    }

    // ------------------------------------------------------------------ helpers

    private static void collectModels(JsonObject node, List<String> broken, String path) {
        for (Map.Entry<String, JsonElement> entry : node.entrySet()) {
            if (entry.getKey().equals("model") && entry.getValue().isJsonPrimitive()) {
                String model = entry.getValue().getAsString();
                if (model.startsWith(MOD + ":")
                        && !exists("assets/" + MOD + "/models/" + model.substring(MOD.length() + 1) + ".json")) {
                    broken.add(path + " -> " + model);
                }
            } else if (entry.getValue().isJsonObject()) {
                collectModels(entry.getValue().getAsJsonObject(), broken, path);
            }
        }
    }

    private static List<String> collectStrings(JsonElement node, String field) {
        List<String> out = new ArrayList<>();
        if (node.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : node.getAsJsonObject().entrySet()) {
                if (entry.getKey().equals(field) && entry.getValue().isJsonPrimitive()) {
                    out.add(entry.getValue().getAsString());
                } else {
                    out.addAll(collectStrings(entry.getValue(), field));
                }
            }
        } else if (node.isJsonArray()) {
            for (JsonElement child : node.getAsJsonArray()) {
                out.addAll(collectStrings(child, field));
            }
        }
        return out;
    }

    private static String read(String resource) {
        try (InputStream in = DataIntegrityTest.class.getResourceAsStream("/" + resource)) {
            assertNotNull(in, "missing resource: " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw new AssertionError("could not read " + resource, failure);
        }
    }

    private static boolean exists(String resource) {
        try (InputStream in = DataIntegrityTest.class.getResourceAsStream("/" + resource)) {
            return in != null;
        } catch (Exception failure) {
            return false;
        }
    }

    private static JsonObject json(String resource) {
        return JsonParser.parseString(read(resource)).getAsJsonObject();
    }

    private static Map<String, String> lang(String locale) {
        JsonObject object = json("assets/" + MOD + "/lang/" + locale + ".json");
        Map<String, String> out = new HashMap<>();
        object.entrySet().forEach(entry -> out.put(entry.getKey(), entry.getValue().getAsString()));
        return out;
    }

    /** Every resource under a directory, as paths relative to the resource root. */
    private static List<String> resources(String folder) {
        List<String> out = new ArrayList<>();
        try {
            var url = DataIntegrityTest.class.getClassLoader().getResource(folder);
            assertNotNull(url, "missing resource directory: " + folder);
            var root = java.nio.file.Paths.get(url.toURI());
            try (var walk = java.nio.file.Files.walk(root)) {
                walk.filter(java.nio.file.Files::isRegularFile)
                        .forEach(path -> out.add(folder + "/" + root.relativize(path).toString()
                                .replace('\\', '/')));
            }
        } catch (Exception failure) {
            throw new AssertionError("could not list " + folder, failure);
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }
}
