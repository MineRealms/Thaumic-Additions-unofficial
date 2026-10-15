// S4b verification, part 1: registry wiring, the two new aspect tables, and the ticker check for
// every block entity block.
//
// Nothing here touches a world beyond reading registries, so a failure in part 1 keeps part 2's
// results meaningful.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var bet = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE);

// --- 1) every machine block still hands back a ticker ---
java.util.Map<String, String> tickers = new java.util.TreeMap<>();
for (var e : blocks.entrySet()) {
    if (!e.getKey().location().getNamespace().equals("thaumadds")) continue;
    var block = e.getValue();
    if (!(block instanceof net.minecraft.world.level.block.EntityBlock eb)) continue;
    var state = block.defaultBlockState();
    for (var t : bet) {
        if (!t.isValid(state)) continue;
        tickers.put(e.getKey().location().getPath(), eb.getTicker(level, state, t) == null ? "NULL" : "ok");
        break;
    }
}
long ok = tickers.values().stream().filter("ok"::equals).count();
sb.append("[1] BE blocks with a ticker: ").append(ok).append("/").append(tickers.size()).append("\n");
for (var en : tickers.entrySet()) if (!"ok".equals(en.getValue())) sb.append("    NO TICKER: ").append(en.getKey()).append("\n");

// --- 2) the aspect ids the new tables name are all live ---
var api = dev.tc4port.thaumcraft.api.aspect.AspectApi.class;
java.util.function.Function<String, Boolean> live = name -> {
    try { return dev.tc4port.thaumcraft.api.aspect.AspectApi.contains(
            dev.tc4port.thaumcraft.api.aspect.AspectId.parse(name)); }
    catch (RuntimeException bad) { return false; }
};
String[] wanted = {"ignis","aqua","vinculum","sensus","tenebrae","permutatio","potentia","instrumentum",
        "mortuus","motus","fames","tutamen","auram","vitium","victus","cognitio",
        "telum","bestia","alienis","volatus","aer","ordo","gelum"};
java.util.List<String> missing = new java.util.ArrayList<>();
for (String w : wanted) if (!live.apply(w)) missing.add(w);
sb.append("[2] aspect ids resolved: ").append(wanted.length - missing.size()).append("/").append(wanted.length);
if (!missing.isEmpty()) sb.append("  MISSING: ").append(missing);
sb.append("\n");

// --- 3) the combiner's combination rule ---
try {
    var combiner = Class.forName("dev.tc4port.thaumadditions.tiles.TileAspectCombiner");
    var combine = combiner.getMethod("combine", dev.tc4port.thaumcraft.api.aspect.AspectId.class,
            dev.tc4port.thaumcraft.api.aspect.AspectId.class);
    var ignis = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis");
    var terra = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("terra");
    var ignisTerra = combine.invoke(null, ignis, terra);
    var ignisIgnis = combine.invoke(null, ignis, ignis);
    var ignisAqua = combine.invoke(null, ignis, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("aqua"));
    sb.append("[3] combine(ignis,terra)=").append(ignisTerra)
      .append("  combine(ignis,ignis)=").append(ignisIgnis)
      .append("  combine(ignis,aqua)=").append(ignisAqua).append("\n");
} catch (Throwable t) {
    sb.append("[3] combiner not loadable: ").append(t).append("\n");
}

// --- 4) the shadow enchanter's price table ---
try {
    var se = Class.forName("dev.tc4port.thaumadditions.aspect.ShadowEnchantment");
    var registry = se.getMethod("registry");
    var costOf = se.getMethod("costOf", net.minecraft.resources.ResourceLocation.class, int.class);
    var list = (java.util.List<?>) registry.invoke(null);
    sb.append("[4] shadow-enchantment entries: ").append(list.size()).append("\n");
    Object sharpness = costOf.invoke(null, net.minecraft.resources.ResourceLocation.parse("minecraft:sharpness"), 3);
    Object infinity = costOf.invoke(null, net.minecraft.resources.ResourceLocation.parse("minecraft:infinity"), 1);
    Object mending = costOf.invoke(null, net.minecraft.resources.ResourceLocation.parse("minecraft:mending"), 1);
    sb.append("    sharpness@3=").append(sharpness).append("  infinity@1=").append(infinity)
      .append("  mending@1=").append(mending).append("\n");
} catch (Throwable t) {
    sb.append("[4] shadow-enchantment table not loadable: ").append(t).append("\n");
}

// --- 5) the edible-aspect table's coverage ---
try {
    var ea = Class.forName("dev.tc4port.thaumadditions.aspect.EdibleAspect");
    var isEdible = ea.getMethod("isEdible", dev.tc4port.thaumcraft.api.aspect.AspectId.class);
    java.util.List<String> edible = new java.util.ArrayList<>();
    java.util.List<String> inert = new java.util.ArrayList<>();
    for (String w : wanted) {
        var id = dev.tc4port.thaumcraft.api.aspect.AspectId.parse(w);
        (Boolean.TRUE.equals(isEdible.invoke(null, id)) ? edible : inert).add(w);
    }
    sb.append("[5] edible aspects: ").append(edible.size()).append(" ").append(edible).append("\n");
    sb.append("    inert aspects: ").append(inert).append("\n");
} catch (Throwable t) {
    sb.append("[5] edible-aspect table not loadable: ").append(t).append("\n");
}

// --- 6) the machines this batch added are registered ---
String[] names = {"aura_disperser", "crystal_lamp", "shadow_enchanter", "growth_chamber",
        "aspect_combiner", "crystal_crusher", "crystal_block"};
for (String n : names) {
    var id = net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + n);
    sb.append("[6] ").append(n).append(": block=").append(blocks.containsKey(id));
    var state = blocks.containsKey(id) ? blocks.get(id).defaultBlockState() : null;
    String type = "?";
    if (state != null) for (var t : bet) if (t.isValid(state)) { type = String.valueOf(bet.getKey(t)); break; }
    sb.append(" be=").append(type).append("\n");
}

return sb.toString();
