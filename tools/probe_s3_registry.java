// S3 verification, part 1: dump the live registry for thaumadds and check that every
// item/block resolves a translated name (an unresolved key comes back as the raw
// "item.thaumadds.foo", which is exactly the naming defect this sweep hunts).
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var ra = srv.registryAccess();
var items = ra.registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
var blocks = ra.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var bet = ra.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE);

java.util.List<String> li = new java.util.ArrayList<>();
java.util.List<String> lb = new java.util.ArrayList<>();
java.util.List<String> lt = new java.util.ArrayList<>();
for (var e : items.entrySet()) if (e.getKey().location().getNamespace().equals("thaumadds")) li.add(e.getKey().location().getPath());
for (var e : blocks.entrySet()) if (e.getKey().location().getNamespace().equals("thaumadds")) lb.add(e.getKey().location().getPath());
for (var e : bet.entrySet()) if (e.getKey().location().getNamespace().equals("thaumadds")) lt.add(e.getKey().location().getPath());
java.util.Collections.sort(li); java.util.Collections.sort(lb); java.util.Collections.sort(lt);

sb.append("ITEMS(").append(li.size()).append("): ").append(String.join(" ", li)).append("\n");
sb.append("BLOCKS(").append(lb.size()).append("): ").append(String.join(" ", lb)).append("\n");
sb.append("BES(").append(lt.size()).append("): ").append(String.join(" ", lt)).append("\n");

// --- untranslated names ---
java.util.List<String> badNames = new java.util.ArrayList<>();
for (String p : li) {
    var it = items.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + p));
    if (it == null) continue;
    String n = it.getDescription().getString();
    if (n.startsWith("item.") || n.startsWith("block.") || n.contains("thaumadds")) badNames.add(p + " -> " + n);
}
for (String p : lb) {
    var b = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + p));
    if (b == null) continue;
    String n = b.getName().getString();
    if (n.startsWith("block.") || n.startsWith("tile.") || n.contains("thaumadds")) badNames.add(p + " -> " + n);
}
sb.append("UNTRANSLATED(").append(badNames.size()).append("):\n");
for (String b : badNames) sb.append("   ").append(b).append("\n");

// --- item models resolve? ---
var rm = mc.getResourceManager();
java.util.List<String> noModel = new java.util.ArrayList<>();
int checked = 0;
for (String p : li) {
    if (!rm.getResource(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:models/item/" + p + ".json")).isPresent())
        noModel.add(p);
    checked++;
}
sb.append("item models checked ").append(checked).append(", MISSING(").append(noModel.size()).append("): ")
  .append(String.join(" ", noModel)).append("\n");

// --- blockstates resolve? ---
java.util.List<String> noState = new java.util.ArrayList<>();
for (String p : lb)
    if (!rm.getResource(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:blockstates/" + p + ".json")).isPresent())
        noState.add(p);
sb.append("blockstates MISSING(").append(noState.size()).append("): ").append(String.join(" ", noState)).append("\n");

return sb.toString();
