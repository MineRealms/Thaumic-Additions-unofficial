// Which of the mod's recipes have a JEI page?
//
// A category is not a page. Thaumcraft registers one category per recipe kind and then decides
// what goes in it, so a recipe can load, be craftable, sit in the Thaumonomicon and still have no
// JEI page to look up. The id is read off the recipe rather than off its toString, because a
// recipe's toString is only useful when it happens to be a record.
StringBuilder out = new StringBuilder();
var internal = Class.forName("mezz.jei.common.Internal");
var runtime = internal.getMethod("getJeiRuntime").invoke(null);
var manager = runtime.getClass().getMethod("getRecipeManager").invoke(runtime);

var recipeTypeClass = Class.forName("mezz.jei.api.recipe.RecipeType");
var lookupType = Class.forName("mezz.jei.api.recipe.IRecipeLookup");
var streamType = Class.forName("java.util.stream.Stream");

String[] names = {"thaumcraft:arcane", "thaumcraft:infusion", "thaumcraft:crucible",
        "thaumadds:flux_concentrator", "minecraft:crafting"};
for (String name : names) {
    var found = (java.util.Optional<?>) manager.getClass()
            .getMethod("getRecipeType", net.minecraft.resources.ResourceLocation.class)
            .invoke(manager, net.minecraft.resources.ResourceLocation.tryParse(name));
    if (found.isEmpty()) {
        out.append(name).append(": category not registered").append("\n");
        continue;
    }
    var lookup = manager.getClass().getMethod("createRecipeLookup", recipeTypeClass)
            .invoke(manager, found.get());
    var stream = lookupType.getMethod("get").invoke(lookup);
    java.util.List<?> all = (java.util.List<?>) streamType.getMethod("toList").invoke(stream);

    int ours = 0;
    java.util.List<String> samples = new java.util.ArrayList<>();
    for (Object entry : all) {
        // JEI holds whatever it was given: a recipe, or a holder carrying one.
        Object recipe = entry;
        try {
            recipe = entry.getClass().getMethod("value").invoke(entry);
        } catch (ReflectiveOperationException direct) {
            // A recipe then.
        }
        if (!(recipe instanceof net.minecraft.world.item.crafting.Recipe<?> typed)) {
            continue;
        }
        String id = typed.getId().toString();
        if (id.startsWith("thaumadds:")) {
            ours++;
            if (samples.size() < 4) {
                samples.add(id);
            }
        }
    }
    out.append(name).append(": ").append(all.size()).append(" pages, ").append(ours)
       .append(" ours ").append(samples).append("\n");
}
return out.toString();
