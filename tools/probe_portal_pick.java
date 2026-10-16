// Why is no portal being chosen? Prints every condition the choice makes.
StringBuilder out = new StringBuilder();
var minecraft = net.minecraft.client.Minecraft.getInstance();
var camera = minecraft.gameRenderer.getMainCamera();
var eye = camera.getPosition();
var look = camera.getLookVector();
out.append("camera at ").append(eye).append("  look ").append(look).append("\n");

var window = Class.forName("dev.tc4port.thaumadditions.client.render.PortalWindow");
for (Object seal : (java.util.List<?>) window.getMethod("loadedSeals").invoke(null)) {
    var tile = (net.minecraft.world.level.block.entity.BlockEntity) seal;
    var pos = tile.getBlockPos();
    if (pos.getY() < 130) {
        continue;
    }
    var bound = tile.getClass().getMethod("portalBound").invoke(tile);
    var hole = (Integer) tile.getClass().getMethod("holeTarget").invoke(tile);
    var toPortal = pos.getCenter().subtract(eye);
    double distance = toPortal.length();
    double alignment = distance < 0.05D ? 0.0D : toPortal.normalize().dot(
            new net.minecraft.world.phys.Vec3(look.x(), look.y(), look.z()).normalize());
    out.append("seal ").append(pos).append(" bound=").append(bound)
       .append(" hole=").append(hole)
       .append(" distance=").append(String.format("%.1f", distance))
       .append(" alignment=").append(String.format("%.2f", alignment)).append("\n");
}
return out.toString();
