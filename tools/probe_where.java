StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);
out.append("player ").append(player.blockPosition()).append(" eye ")
   .append(String.format("%.1f", player.getEyeY()))
   .append(" yaw ").append(player.getYRot()).append(" pitch ").append(player.getXRot()).append("\n");
for (var at : java.util.List.of(new net.minecraft.core.BlockPos(2067, 141, 10),
        new net.minecraft.core.BlockPos(2067, 141, 20))) {
    var state = level.getBlockState(at);
    var tile = level.getBlockEntity(at);
    out.append(at).append(" -> ").append(state.getBlock())
       .append(" facing=").append(state.hasProperty(
               net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
               ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
               : "?");
    if (tile != null) {
        out.append(" hole=").append(tile.getClass().getMethod("holeTarget").invoke(tile));
    }
    out.append("\n");
}
return out.toString();
