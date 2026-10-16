// Does the charger still charge, and does its effect line run without throwing?
//
// The sparks are broadcast rather than kept, so the thing worth proving here is that the branch
// they sit on still does what it did: a machine that charged a node before and now throws on the
// way out would be a worse machine than one with no particles at all.
StringBuilder out = new StringBuilder();
var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);

var base = player.blockPosition().offset(0, 2, 8);
for (int dx = -2; dx <= 2; dx++) {
    for (int dz = -2; dz <= 2; dz++) {
        for (int dy = 0; dy <= 2; dy++) {
            level.setBlockAndUpdate(base.offset(dx, dy, dz),
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }
}

// A node within reach, so the charger has something to spend its charge on.
Object node = null;
for (var type : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
    var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(type);
    if ("thaumcraft".equals(id.getNamespace()) && id.getPath().contains("aura_node")) {
        level.setBlockAndUpdate(base.offset(2, 0, 0), type.defaultBlockState());
        node = level.getBlockEntity(base.offset(2, 0, 0));
        out.append("node: ").append(id).append(" placed=").append(node != null).append("\n");
        break;
    }
}

var charger = (net.minecraft.world.level.block.Block)
        dev.tc4port.thaumadditions.registry.Content.AURA_CHARGER.get();
level.setBlockAndUpdate(base, charger.defaultBlockState());
var tile = (net.minecraft.world.level.block.entity.BlockEntity) level.getBlockEntity(base);
out.append("charger placed: ").append(tile != null).append("\n");

String failure = "none";
if (tile != null) {
    try {
        var tick = tile.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
                net.minecraft.core.BlockPos.class,
                net.minecraft.world.level.block.state.BlockState.class);
        for (int i = 0; i < 130; i++) {
            var be = (net.minecraft.world.level.block.entity.BlockEntity) level.getBlockEntity(base);
            tick.invoke(be, level, base, level.getBlockState(base));
        }
    } catch (Throwable thrown) {
        failure = String.valueOf(thrown.getCause() == null ? thrown : thrown.getCause());
    }
}
out.append("130 ticks: ").append(failure).append("\n");

for (int dx = -2; dx <= 2; dx++) {
    for (int dz = -2; dz <= 2; dz++) {
        for (int dy = 0; dy <= 2; dy++) {
            level.setBlockAndUpdate(base.offset(dx, dy, dz),
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }
}
return out.toString();
