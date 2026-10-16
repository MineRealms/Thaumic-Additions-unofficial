// Walks everything the mod registered, and everything its data files point at, looking for the
// faults a unit test cannot see: an id named by a recipe, a loot table, a research icon or a tag
// that resolves to nothing, an item with no model, a block with no loot table.
//
// These are the failures that are silent in game — a recipe that quietly does not exist, a loot
// table that drops air — and the ones the game reports as a stack trace only when the world loads.
StringBuilder out = new StringBuilder();
int[] problems = {0};

// The integrated server, reached the way both loaders reach it: the client owns it.
var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
var level = server.overworld();
String MOD = "thaumadds";

// Two pack sets, and neither one carries what the other does: the client holds assets/, the
// integrated server holds data/. Asking the wrong one reports every model in the mod as missing.
var assets = net.minecraft.client.Minecraft.getInstance().getResourceManager();
var data = server.getResourceManager();

java.util.function.Function<String, Boolean> hasAsset = path -> {
    try {
        return assets.getResource(net.minecraft.resources.ResourceLocation.tryParse(MOD + ":" + path)).isPresent();
    } catch (RuntimeException bad) {
        return false;
    }
};
java.util.function.Function<String, Boolean> hasData = path -> {
    try {
        return data.getResource(net.minecraft.resources.ResourceLocation.tryParse(MOD + ":" + path)).isPresent();
    } catch (RuntimeException bad) {
        return false;
    }
};

// ---------------------------------------------------------------- items have a model
int items = 0;
java.util.List<String> itemless = new java.util.ArrayList<>();
for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
    var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
    if (!MOD.equals(id.getNamespace())) {
        continue;
    }
    items++;
    if (!hasAsset.apply("models/item/" + id.getPath() + ".json")) {
        itemless.add(id.toString());
    }
}
out.append("items registered: ").append(items).append("\n");
if (!itemless.isEmpty()) {
    problems[0] += itemless.size();
    out.append("ITEMS WITH NO MODEL: ").append(itemless).append("\n");
}

// ---------------------------------------------------------------- blocks have a state and a loot table
int blocks = 0;
java.util.List<String> stateless = new java.util.ArrayList<>();
java.util.List<String> lootless = new java.util.ArrayList<>();
for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
    var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
    if (!MOD.equals(id.getNamespace())) {
        continue;
    }
    blocks++;
    if (!hasAsset.apply("blockstates/" + id.getPath() + ".json")) {
        stateless.add(id.toString());
    }
    // A block with no loot table drops nothing; one that says so on purpose still has the file,
    // because the port writes an empty pool for the few that should drop nothing.
    if (!hasData.apply("loot_tables/blocks/" + id.getPath() + ".json")
            && !hasData.apply("loot_table/blocks/" + id.getPath() + ".json")) {
        lootless.add(id.toString());
    }
}
out.append("blocks registered: ").append(blocks).append("\n");
if (!stateless.isEmpty()) {
    problems[0] += stateless.size();
    out.append("BLOCKS WITH NO BLOCKSTATE: ").append(stateless).append("\n");
}
if (!lootless.isEmpty()) {
    out.append("blocks with no loot table (fine if deliberate): ").append(lootless).append("\n");
}

// ---------------------------------------------------------------- every id the data names resolves
java.util.function.BiFunction<com.google.gson.JsonElement, String, java.util.List<String>> idsIn =
        (node, field) -> {
            java.util.List<String> found = new java.util.ArrayList<>();
            java.util.function.Consumer<com.google.gson.JsonElement> walk = new java.util.function.Consumer<>() {
                @Override
                public void accept(com.google.gson.JsonElement element) {
                    if (element.isJsonObject()) {
                        for (var entry : element.getAsJsonObject().entrySet()) {
                            if (entry.getKey().equals(field) && entry.getValue().isJsonPrimitive()) {
                                found.add(entry.getValue().getAsString());
                            } else {
                                accept(entry.getValue());
                            }
                        }
                    } else if (element.isJsonArray()) {
                        for (var child : element.getAsJsonArray()) {
                            accept(child);
                        }
                    }
                }
            };
            walk.accept(node);
            return found;
        };

java.util.function.BiFunction<String, String, com.google.gson.JsonElement> readJson = (path, label) -> {
    try {
        var resource = data.getResource(net.minecraft.resources.ResourceLocation.tryParse(MOD + ":" + path));
        if (resource.isEmpty()) {
            return null;
        }
        try (var in = resource.get().open()) {
            return com.google.gson.JsonParser.parseString(
                    new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
    } catch (Exception failure) {
        out.append("could not read ").append(label).append(": ").append(failure).append("\n");
        problems[0]++;
        return null;
    }
};

// Recipes: every item id they name is registered.
int recipes = 0;
java.util.List<String> unresolved = new java.util.ArrayList<>();
for (Object recipe : server.getRecipeManager().getRecipes()) {
    // 1.20.1 hands out Recipe, which carries its own id; 1.21.1 hands out RecipeHolder, which
    // carries the id beside it. Neither type fits the other target, so it is read reflectively.
    net.minecraft.resources.ResourceLocation id = null;
    for (String accessor : java.util.List.of("id", "getId")) {
        try {
            Object value = recipe.getClass().getMethod(accessor).invoke(recipe);
            if (value instanceof net.minecraft.resources.ResourceLocation found) {
                id = found;
                break;
            }
        } catch (ReflectiveOperationException notHere) {
            // try the next name
        }
    }
    if (id == null || !MOD.equals(id.getNamespace())) {
        continue;
    }
    recipes++;
    // 1.20.1 keeps recipes in recipes/, 1.21 in recipe/; the id's path is the rest either way.
    var path = "recipes/" + id.getPath() + ".json";
    if (!hasData.apply(path)) {
        path = "recipe/" + id.getPath() + ".json";
    }
    if (!hasData.apply(path)) {
        unresolved.add("recipe " + id + " has no data file");
        continue;
    }
    var json = readJson.apply(path, path);
    if (json == null) {
        continue;
    }
    for (String named : idsIn.apply(json, "item")) {
        if (named.startsWith(MOD + ":")
                && net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getOptional(net.minecraft.resources.ResourceLocation.tryParse(named)).isEmpty()) {
            unresolved.add(path + " names " + named);
        }
    }
}
out.append("recipes loaded: ").append(recipes).append("\n");
if (!unresolved.isEmpty()) {
    problems[0] += unresolved.size();
    out.append("RECIPES NAMING NOTHING: ").append(unresolved).append("\n");
}

// Research: every icon is a registered item, and every entry key is unique.
var catalogue = readJson.apply("thaumcraft/research/default.json", "research");
if (catalogue != null) {
    java.util.Set<String> keys = new java.util.LinkedHashSet<>();
    java.util.List<String> badIcons = new java.util.ArrayList<>();
    java.util.List<String> repeats = new java.util.ArrayList<>();
    for (var entry : catalogue.getAsJsonObject().getAsJsonArray("entries")) {
        var object = entry.getAsJsonObject();
        var key = object.get("key").getAsString();
        if (!keys.add(key)) {
            repeats.add(key);
        }
        var icon = object.getAsJsonObject("icon").getAsJsonObject("stack").get("id").getAsString();
        if (icon.startsWith(MOD + ":")
                && net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getOptional(net.minecraft.resources.ResourceLocation.tryParse(icon)).isEmpty()) {
            badIcons.add(key + " -> " + icon);
        }
    }
    out.append("research entries: ").append(keys.size()).append("\n");
    if (!repeats.isEmpty()) {
        problems[0] += repeats.size();
        out.append("REPEATED RESEARCH KEYS: ").append(repeats).append("\n");
    }
    if (!badIcons.isEmpty()) {
        problems[0] += badIcons.size();
        out.append("RESEARCH ICONS THAT ARE NOT ITEMS: ").append(badIcons).append("\n");
    }
}

// Loot tables: everything they drop is registered.
java.util.List<String> badDrops = new java.util.ArrayList<>();
for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
    var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
    if (!MOD.equals(id.getNamespace())) {
        continue;
    }
    var json = readJson.apply("loot_tables/blocks/" + id.getPath() + ".json", "loot " + id);
    if (json == null) {
        continue;
    }
    for (String named : idsIn.apply(json, "name")) {
        if (named.startsWith(MOD + ":")
                && net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getOptional(net.minecraft.resources.ResourceLocation.tryParse(named)).isEmpty()) {
            badDrops.add(id + " drops " + named);
        }
    }
}
if (!badDrops.isEmpty()) {
    problems[0] += badDrops.size();
    out.append("LOOT DROPPING NOTHING: ").append(badDrops).append("\n");
}

// ------------------------------------------------------------------ the two new pieces
var water = net.minecraft.core.registries.BuiltInRegistries.FLUID
        .get(net.minecraft.resources.ResourceLocation.tryParse(MOD + ":crystal_water"));
out.append("crystal water: ").append(water)
   .append(" bucket=").append(water.getBucket()).append("\n");

out.append(problems[0] == 0 ? "AUDIT PASS" : "AUDIT FAIL (" + problems[0] + " problems)").append("\n");
return out.toString();
