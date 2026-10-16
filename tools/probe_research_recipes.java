// Does the Thaumonomicon have a recipe to show on its pages?
//
// TC4R prints "This recipe is not available in the current world" when a page's recipe id does not
// resolve, which is what every page of this mod's book said: the ids came from upstream's own
// names and did not match the files the port wrote.
StringBuilder out = new StringBuilder();
var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
var resources = server.getResourceManager();
String MOD = "thaumadds";

var resource = resources.getResource(
        net.minecraft.resources.ResourceLocation.tryParse(MOD + ":thaumcraft/research/default.json"));
var catalogue = com.google.gson.JsonParser.parseString(
        new String(resource.get().open().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
        .getAsJsonObject();

int pages = 0;
int broken = 0;
java.util.List<String> samples = new java.util.ArrayList<>();
for (var entry : catalogue.getAsJsonArray("entries")) {
    var object = entry.getAsJsonObject();
    for (var page : object.getAsJsonArray("pages")) {
        var pageObject = page.getAsJsonObject();
        String type = pageObject.get("type").getAsString();
        if ("TEXT".equals(type) || "TEXT_CONCEALED".equals(type) || "LORE".equals(type)) {
            continue;
        }
        pages++;
        var policy = pageObject.get("recipe_link_policy").getAsString();
        if ("DISABLED".equals(policy)) {
            broken++;
            if (samples.size() < 6) {
                samples.add(object.get("key").getAsString() + " is DISABLED");
            }
            continue;
        }
        for (var id : pageObject.getAsJsonArray("recipe_ids")) {
            var key = net.minecraft.resources.ResourceLocation.tryParse(id.getAsString());
            if (server.getRecipeManager().byKey(key).isEmpty()) {
                broken++;
                if (samples.size() < 6) {
                    samples.add(object.get("key").getAsString() + " -> " + key);
                }
            }
        }
    }
}
out.append("crafting pages: ").append(pages).append("\n");
out.append(broken == 0 ? "every page has a recipe to show" : broken + " pages show nothing")
   .append("\n");
if (!samples.isEmpty()) {
    out.append("  ").append(samples).append("\n");
}
return out.toString();
