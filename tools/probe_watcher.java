// Is the thing that holds the hole open still there, and where?
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var near = new net.minecraft.core.BlockPos(2067, 141, 10);
for (int dz = -3; dz <= 4; dz++) {
    var at = near.offset(0, 0, dz);
    var box = new net.minecraft.world.phys.AABB(at);
    var found = level.getEntities((net.minecraft.world.entity.Entity) null, box, e -> true);
    if (!found.isEmpty()) {
        out.append(at).append(": ").append(found.size()).append(" -> ")
           .append(found.get(0).getType()).append(" at ").append(found.get(0).position()).append("\n");
    }
}
var all = level.getEntities((net.minecraft.world.entity.Entity) null,
        new net.minecraft.world.phys.AABB(near).inflate(24.0D),
        e -> e instanceof net.minecraft.world.entity.item.ItemEntity);
out.append("items within 8 blocks: ").append(all.size()).append("\n");
for (var entity : all) {
    out.append("  ").append(entity.position()).append("\n");
}
return out.toString();
