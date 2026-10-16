// Which of the mod's items can be made at all?
//
// Asked of the recipe manager rather than of the data files: a recipe that is present but whose
// type does not parse, or whose result names nothing, is a recipe that does not exist as far as
// the game is concerned, and that is the difference between a file and a recipe.
StringBuilder out = new StringBuilder();
var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
String MOD = "thaumadds";

var produced = new java.util.HashSet<String>();
var byType = new java.util.TreeMap<String, Integer>();
for (Object holder : server.getRecipeManager().getRecipes()) {
    Object value = holder;
    try {
        value = holder.getClass().getMethod("value").invoke(holder);
    } catch (ReflectiveOperationException notAHolder) {
        // 1.20.1 hands out the recipe itself.
    }
    var recipe = (net.minecraft.world.item.crafting.Recipe<?>) value;
    String type = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString();
    if (type.startsWith(MOD)) {
        byType.merge(type, 1, Integer::sum);
    }
    // Anything the recipe makes, whatever the recipe is.
    var result = recipe.getResultItem(server.registryAccess());
    if (!result.isEmpty()) {
        produced.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
    }
}

out.append("recipes by type: ").append(byType).append("\n");

var makeless = new java.util.ArrayList<String>();
int items = 0;
for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
    var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
    if (!MOD.equals(id.getNamespace())) {
        continue;
    }
    items++;
    // Every block that has an item is also made by placing its block; only count what is made.
    if (!produced.contains(id.toString())) {
        makeless.add(id.getPath());
    }
}
out.append("items registered: ").append(items).append("\n");
out.append("items no recipe produces: ").append(makeless.size()).append("\n");
java.util.Collections.sort(makeless);
out.append("  ").append(makeless).append("\n");

// The jars specifically, since that is what was asked about.
for (String jar : java.util.List.of("jar_brass", "jar_thaumium", "jar_eldritch", "jar_mithrillium",
        "jar_adaminite", "jar_mithminite")) {
    out.append("  ").append(jar).append(" produced=").append(produced.contains(MOD + ":" + jar)).append("\n");
}
return out.toString();
