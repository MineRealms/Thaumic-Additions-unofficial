// Does a portal seal still work after the hole and the persistence were added?
//
// The tile is on the hclite source set, which this compiler does not carry, so it is reached
// reflectively; the behaviour class is shared source and can be named.
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);
var random = new java.util.Random();

var sealBlock = dev.tc4port.thaumadditions.registry.Content.SEAL.get();
var east = player.blockPosition().offset(8, 1, 8);
level.setBlockAndUpdate(east, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
level.setBlockAndUpdate(east.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
level.setBlockAndUpdate(east.below(2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());

var north = east.relative(net.minecraft.core.Direction.NORTH, 3);
for (var at : java.util.List.of(east, north)) {
    for (int dy = -1; dy <= 1; dy++) {
        level.setBlockAndUpdate(at.offset(0, dy, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
    level.setBlockAndUpdate(at.below(2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
}

java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.core.Direction, Object> place =
        (at, face) -> {
            var state = sealBlock.defaultBlockState();
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                state = state.setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, face);
            }
            level.setBlockAndUpdate(at, state);
            return level.getBlockEntity(at);
        };

var here = place.apply(east, net.minecraft.core.Direction.NORTH);
var there = place.apply(north, net.minecraft.core.Direction.SOUTH);
out.append("placed here=").append(here != null).append(" there=").append(there != null).append("\n");

java.util.List<Object> tiles = java.util.List.of(here, there);
var setSymbol = here.getClass().getMethod("setSymbol", int.class,
        dev.tc4port.thaumcraft.api.aspect.AspectId.class);
var setOwner = here.getClass().getMethod("setOwner", java.util.UUID.class);
var instance = here.getClass().getMethod("instance");
var tick = here.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
        net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);

var p = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("praecantatio");
var a = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("aer");
var key = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis");
for (Object tile : tiles) {
    setSymbol.invoke(tile, 0, p);
    setSymbol.invoke(tile, 1, a);
    setSymbol.invoke(tile, 2, key);
    setOwner.invoke(tile, player.getUUID());
}

var positions = java.util.List.of(east, north);
for (int i = 0; i < tiles.size(); i++) {
    tick.invoke(tiles.get(i), level, positions.get(i), level.getBlockState(positions.get(i)));
}

dev.tc4port.thaumadditions.seals.SealInstance hereBehaviour =
        (dev.tc4port.thaumadditions.seals.SealInstance) instance.invoke(here);
out.append("hereBehaviour=").append(hereBehaviour == null ? "none" : hereBehaviour.descriptionKey()).append("\n");

if (hereBehaviour != null) {
    boolean bound = hereBehaviour.onSealActivated(level, east, player);
    out.append("bound=").append(bound).append("\n");
}

// The binding has to be in the tile's own data, since that is what the game saves and syncs.
var persistent = ((net.minecraft.world.level.block.entity.BlockEntity) here).getPersistentData();
var persistentThere = ((net.minecraft.world.level.block.entity.BlockEntity) there).getPersistentData();
out.append("savedBound=").append(persistent.contains("Bound") ? persistent.getLong("Bound") : "none")
   .append(" expected=").append(north.asLong()).append("\n");
out.append("savedBoundThere=").append(persistentThere.contains("Bound") ? persistentThere.getLong("Bound") : "none")
   .append(" expected=").append(east.asLong()).append("\n");

// The hole: shut with nothing in the seal, open with something standing in it.
var item = new net.minecraft.world.entity.item.ItemEntity(level,
        east.getX() + 0.5D, east.getY() + 0.5D, east.getZ() + 0.5D,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
level.addFreshEntity(item);
for (int i = 0; i < 7; i++) {
    tick.invoke(here, level, east, level.getBlockState(east));
}
out.append("occupied target=").append(((net.minecraft.world.level.block.entity.BlockEntity) here)
        .getPersistentData().getInt("HoleTarget"))
   .append(" openness=").append(hereBehaviour.holeOpenness(0.0F)).append("\n");

item.discard();
for (int i = 0; i < 7; i++) {
    tick.invoke(here, level, east, level.getBlockState(east));
}
out.append("empty target=").append(((net.minecraft.world.level.block.entity.BlockEntity) here)
        .getPersistentData().getInt("HoleTarget"))
   .append(" openness=").append(hereBehaviour.holeOpenness(0.0F)).append("\n");

// Put the world back.
for (var at : java.util.List.of(east, north)) {
    for (int dy = -1; dy <= 1; dy++) {
        level.setBlockAndUpdate(at.offset(0, dy, 0),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
}
return out.toString();
