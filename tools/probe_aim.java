// Points the camera, from the client side.
//
// The server's player is not the camera: a client owns its own rotation and overwrites whatever the
// server was told on the next tick, so every probe that set the rotation server-side was aiming
// nothing. This one runs on the render thread and sets the client's own player.
StringBuilder out = new StringBuilder();
var minecraft = net.minecraft.client.Minecraft.getInstance();
float yaw = 0.0F;
float pitch = 0.0F;
double x = 2067.5D;
double y = 141.0D;
double z = 8.0D;
minecraft.execute(() -> {
    if (minecraft.player != null) {
        minecraft.player.setPos(x, y, z);
        minecraft.player.setYRot(yaw);
        minecraft.player.setXRot(pitch);
        minecraft.player.setYHeadRot(yaw);
        minecraft.player.yRotO = yaw;
        minecraft.player.xRotO = pitch;
    }
});
out.append("client camera aimed at yaw ").append(yaw).append(" pitch ").append(pitch)
   .append(" from ").append(x).append(",").append(y).append(",").append(z).append("\n");
return out.toString();
