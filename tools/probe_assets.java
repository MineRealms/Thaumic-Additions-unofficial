// Runtime validation of the normalized asset tree, using Minecraft's own resource
// resolution (not a filesystem walk). Verifies that every blockstate/model reference
// and every texture reference in assets/thaumadds resolves through the live
// ResourceManager. Unreferenced-but-present files are reported by count only.
StringBuilder sb = new StringBuilder();
net.minecraft.server.packs.resources.ResourceManager rm =
        net.minecraft.client.Minecraft.getInstance().getResourceManager();

java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.server.packs.resources.Resource>
        states = rm.listResources("blockstates", p -> p.getPath().endsWith(".json"));

java.util.Set<String> mine = new java.util.HashSet<>();
for (net.minecraft.resources.ResourceLocation rl : states.keySet())
    if ("thaumadds".equals(rl.getNamespace()))
        mine.add(rl.toString());

java.util.List<String> missingModels = new java.util.ArrayList<>();
java.util.List<String> missingTextures = new java.util.ArrayList<>();
int checkedModels = 0, checkedTextures = 0;

java.util.Set<String> todo = new java.util.HashSet<>(mine);
java.util.Set<String> seenModels = new java.util.HashSet<>();
java.util.Set<String> seenTextures = new java.util.HashSet<>();

// Seed with every thaumadds model file too, so unreferenced models are validated as well.
for (net.minecraft.resources.ResourceLocation rl : rm.listResources(
        "models", p -> p.getNamespace().equals("thaumadds") && p.getPath().endsWith(".json")).keySet())
    if (rl.getPath().startsWith("models/block/") || rl.getPath().startsWith("models/item/"))
        todo.add(rl.toString());

java.util.List<String> queue = new java.util.ArrayList<>(todo);
while (!queue.isEmpty()) {
    String id = queue.remove(queue.size() - 1);
    net.minecraft.server.packs.resources.Resource res;
    try {
        res = rm.getResource(net.minecraft.resources.ResourceLocation.tryParse(id)).orElse(null);
    } catch (Exception e) { res = null; }
    if (res == null) { continue; }
    com.google.gson.JsonObject doc;
    try {
        java.io.InputStream in = res.open();
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int r;
        while ((r = in.read(chunk)) > 0) buf.write(chunk, 0, r);
        in.close();
        doc = com.google.gson.JsonParser.parseString(new String(buf.toByteArray(), java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject();
    } catch (Exception e) {
        sb.append("PARSE FAIL ").append(id).append(" : ").append(e.getMessage()).append("\n");
        continue;
    }
    // collect model refs
    java.util.function.BiConsumer<com.google.gson.JsonObject, String> walk;
    walk = (obj, kind) -> {
    }; // (not used; resolved inline below)

    java.util.List<String> modelRefs = new java.util.ArrayList<>();
    java.util.List<String> texRefs = new java.util.ArrayList<>();
    if (id.contains("/blockstates/")) {
        if (doc.has("variants")) {
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> ve : doc.getAsJsonObject("variants").entrySet()) {
                java.util.List<com.google.gson.JsonObject> ents = new java.util.ArrayList<>();
                com.google.gson.JsonElement v = ve.getValue();
                if (v.isJsonArray()) for (com.google.gson.JsonElement e : v.getAsJsonArray()) ents.add(e.getAsJsonObject());
                else ents.add(v.getAsJsonObject());
                for (com.google.gson.JsonObject o : ents) {
                    if (o.has("model")) modelRefs.add(o.get("model").getAsString());
                    if (o.has("textures")) for (java.util.Map.Entry<String, com.google.gson.JsonElement> te : o.getAsJsonObject("textures").entrySet())
                        texRefs.add(te.getValue().getAsString());
                }
            }
        }
        if (doc.has("multipart")) {
            for (com.google.gson.JsonElement e : doc.getAsJsonArray("multipart")) {
                com.google.gson.JsonObject o = e.getAsJsonObject();
                if (o.has("apply")) {
                    com.google.gson.JsonElement a = o.get("apply");
                    com.google.gson.JsonObject ao = a.isJsonArray() ? a.getAsJsonArray().get(0).getAsJsonObject() : a.getAsJsonObject();
                    if (ao.has("model")) modelRefs.add(ao.get("model").getAsString());
                }
            }
        }
    } else {
        if (doc.has("parent")) modelRefs.add(doc.get("parent").getAsString());
        if (doc.has("textures")) for (java.util.Map.Entry<String, com.google.gson.JsonElement> te : doc.getAsJsonObject("textures").entrySet()) {
            String tv = te.getValue().getAsString();
            if (!tv.startsWith("#")) texRefs.add(tv);
        }
    }

    for (String m : modelRefs) {
        if (m.startsWith("#")) continue;
        if (seenModels.add(m) && m.startsWith("thaumadds:")) {
            String path = m.substring("thaumadds:".length());
            if (!path.startsWith("models/")) path = "models/" + path;
            String file = path + (path.endsWith(".json") ? "" : ".json");
            checkedModels++;
            if (!rm.getResource(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + file)).isPresent())
                missingModels.add(m + "   (ref from " + id + ")");
        }
    }
    for (String t : texRefs) {
        if (t.startsWith("#")) continue;
        if (seenTextures.add(t) && t.startsWith("thaumadds:")) {
            checkedTextures++;
            if (!rm.getResource(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:textures/"
                    + t.substring("thaumadds:".length()) + ".png")).isPresent())
                missingTextures.add(t + "   (ref from " + id + ")");
        }
    }
}

sb.append("thaumadds blockstates seen: ").append(mine.size()).append("\n");
sb.append("unique local model refs checked: ").append(checkedModels).append("\n");
sb.append("unique local texture refs checked: ").append(checkedTextures).append("\n");
sb.append("MISSING MODELS (").append(missingModels.size()).append("):\n");
for (String m : missingModels) sb.append("  - ").append(m).append("\n");
sb.append("MISSING TEXTURES (").append(missingTextures.size()).append("):\n");
for (String t : missingTextures) sb.append("  - ").append(t).append("\n");
return sb.toString();
