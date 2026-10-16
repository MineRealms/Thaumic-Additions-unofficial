StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
server.overworld().setDayTime(4000L);
out.append("time set to morning\n");
return out.toString();
