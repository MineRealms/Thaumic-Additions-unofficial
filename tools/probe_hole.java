// Is the near portal's hole open, and is anything in it?
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var near = new net.minecraft.core.BlockPos(2067, 141, 10);
var tile = level.getBlockEntity(near);
out.append("tile: ").append(tile == null ? "none" : "present").append("\n");
if (tile != null) {
    var hole = tile.getClass().getMethod("holeTarget").invoke(tile);
    var bound = tile.getClass().getMethod("portalBound").invoke(tile);
    out.append("holeTarget: ").append(hole).append("  bound: ").append(bound).append("\n");
    var persistent = ((net.minecraft.world.level.block.entity.BlockEntity) tile).getPersistentData();
    out.append("persistent HoleTarget: ").append(persistent.getInt("HoleTarget"))
       .append("  Bound: ").append(persistent.contains("Bound")).append("\n");
}
var box = new net.minecraft.world.phys.AABB(near).inflate(2.0D);
var entities = level.getEntities((net.minecraft.world.entity.Entity) null, box,
        entity -> !entity.isSpectator());
out.append("entities in the seal: ").append(entities.size()).append("\n");
for (var entity : entities) {
    out.append("  ").append(entity.getType()).append(" at ").append(entity.position()).append("\n");
}
return out.toString();
