// What does JEI actually have?
//
// JEI shows a recipe only if a plugin registered a *category* for that recipe's type and then
// handed the recipes to it. The mod's own plugin registers one category; everything Thaumcraft's
// recipe types carry is Thaumcraft's plugin's business, so a recipe with no category behind it is
// a recipe with no JEI page however many times it loaded.
StringBuilder out = new StringBuilder();
var internal = Class.forName("mezz.jei.common.Internal");
var runtime = internal.getMethod("getJeiRuntime").invoke(null);
var manager = runtime.getClass().getMethod("getRecipeManager").invoke(runtime);

// Ask through the exported interfaces: getMethod on the concrete class hands back a Method whose
// declaring class is a JDK-internal implementation, which cannot be invoked from here.
var streamType = Class.forName("java.util.stream.Stream");
var lookup = manager.getClass().getMethod("createRecipeCategoryLookup").invoke(manager);
var stream = lookup.getClass().getMethod("get").invoke(lookup);
java.util.List<?> categories = (java.util.List<?>) streamType.getMethod("toList").invoke(stream);

java.util.List<String> ids = new java.util.ArrayList<>();
for (Object category : categories) {
    var recipeType = category.getClass().getMethod("getRecipeType").invoke(category);
    ids.add(String.valueOf(recipeType.getClass().getMethod("getUid").invoke(recipeType)));
}
java.util.Collections.sort(ids);
out.append("JEI categories (").append(ids.size()).append("):\n");
for (String id : ids) {
    out.append("  ").append(id).append("\n");
}
return out.toString();
