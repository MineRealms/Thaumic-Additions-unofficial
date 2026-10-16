// Stand the player right in front of the portal, so the window fills the view.
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);
player.teleportTo(2067.5D, 141.0D, 6.0D);
player.setYRot(0.0F);
player.setXRot(0.0F);
out.append("player at ").append(player.blockPosition())
   .append(" looking south at the portal two blocks away\n");
return out.toString();
