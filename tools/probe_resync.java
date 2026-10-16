// Re-sends the seals to the client, now that the client is actually tracking the chunks.
//
// A sync packet is sent to whoever is tracking the block entity at the moment it is sent, and a
// seal that was placed and painted in the same instant as the player was moved into its chunk has
// no trackers yet — so the packet goes nowhere. Touching the symbols again sends another.
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
for (var at : java.util.List.of(new net.minecraft.core.BlockPos(2067, 141, 10),
        new net.minecraft.core.BlockPos(2067, 141, 20))) {
    var tile = level.getBlockEntity(at);
    if (tile == null) {
        continue;
    }
    var setSymbol = tile.getClass().getMethod("setSymbol", int.class,
            dev.tc4port.thaumcraft.api.aspect.AspectId.class);
    setSymbol.invoke(tile, 0, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("praecantatio"));
    setSymbol.invoke(tile, 1, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("aer"));
    setSymbol.invoke(tile, 2, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("permutatio"));
    out.append("re-sent ").append(at).append("\n");
}
return out.toString();
