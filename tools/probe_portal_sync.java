// Does the client's copy of the seal know the hole is open?
StringBuilder out = new StringBuilder();
var minecraft = net.minecraft.client.Minecraft.getInstance();
var clientLevel = minecraft.level;
var server = minecraft.getSingleplayerServer();
var window = Class.forName("dev.tc4port.thaumadditions.client.render.PortalWindow");
for (Object seal : (java.util.List<?>) window.getMethod("loadedSeals").invoke(null)) {
    var tile = (net.minecraft.world.level.block.entity.BlockEntity) seal;
    if (tile.getBlockPos().getY() < 130) {
        continue;
    }
    String side = tile.getLevel() == clientLevel ? "client"
            : (server != null && tile.getLevel() == server.overworld() ? "server" : "other");
    var persistent = tile.getPersistentData();
    out.append(side).append(" ").append(tile.getBlockPos())
       .append(" HoleTarget=").append(persistent.getInt("HoleTarget"))
       .append(" Bound=").append(persistent.contains("Bound"))
       .append("\n");
}
return out.toString();
