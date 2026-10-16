StringBuilder out = new StringBuilder();
var minecraft = net.minecraft.client.Minecraft.getInstance();
var clientLevel = minecraft.level;
var window = Class.forName("dev.tc4port.thaumadditions.client.render.PortalWindow");
for (Object seal : (java.util.List<?>) window.getMethod("loadedSeals").invoke(null)) {
    var tile = (net.minecraft.world.level.block.entity.BlockEntity) seal;
    if (tile.getLevel() != clientLevel || tile.getBlockPos().getY() < 130) {
        continue;
    }
    var persistent = tile.getPersistentData();
    StringBuilder symbols = new StringBuilder();
    for (int slot = 0; slot < 3; slot++) {
        symbols.append(tile.getClass().getMethod("symbol", int.class).invoke(tile, slot)).append(" ");
    }
    out.append(tile.getBlockPos())
       .append(" Bound=").append(persistent.contains("Bound"))
       .append(" HoleTarget=").append(persistent.getInt("HoleTarget"))
       .append(" symbols=").append(symbols)
       .append(" instance=").append(tile.getClass().getMethod("instance").invoke(tile))
       .append(" portalBound=").append(tile.getClass().getMethod("portalBound").invoke(tile))
       .append("\n");
}
return out.toString();
