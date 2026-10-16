// Are the mod's crucible recipes loaded, and as what?
StringBuilder out = new StringBuilder();
var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
String[] ids = {"thaumadds:crucible/crystal_water", "thaumadds:crucible/odour_powder",
        "thaumadds:crucible/phantom_ink_phial", "thaumcraft:crucible/minecraft/clay_ball"};
for (String id : ids) {
    var key = net.minecraft.resources.ResourceLocation.tryParse(id);
    var found = server.getRecipeManager().byKey(key);
    if (found.isEmpty()) {
        out.append(id).append(": NOT LOADED").append("\n");
        continue;
    }
    Object holder = found.get();
    Object value = holder;
    try {
        value = holder.getClass().getMethod("value").invoke(holder);
    } catch (ReflectiveOperationException direct) {
        // 1.20.1 hands the recipe itself.
    }
    var recipe = (net.minecraft.world.item.crafting.Recipe<?>) value;
    var type = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
    out.append(id).append(": type=").append(type)
       .append(" result=").append(recipe.getResultItem(server.registryAccess()))
       .append("\n");
}
return out.toString();
