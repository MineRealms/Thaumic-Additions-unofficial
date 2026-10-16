// Builds the whole demonstration in one go, at fixed coordinates, in the session it is looked at.
//
// Everything it needs has to happen in the same session: a seal's symbols and its binding reach the
// client through a sync packet, and a world killed before it saves loses them, so a probe that
// builds the scene and a later session that looks at it are not the same scene.
//
//   (2067, 141, 10)  the near portal, facing south, so its face looks back at the player
//   (2067, 141, 20)  the far portal, facing south
//   (2067, 141, 22)  a wall of magenta behind the far one, which is what should be visible
//                    through the near portal
//   player stands at z = 4, four blocks in front of the near portal, looking straight at it
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);

var near = new net.minecraft.core.BlockPos(2067, 141, 10);
var far = new net.minecraft.core.BlockPos(2067, 141, 20);

for (int dx = -10; dx <= 10; dx++) {
    for (int dz = -4; dz <= 16; dz++) {
        for (int dy = 0; dy <= 6; dy++) {
            level.setBlockAndUpdate(near.offset(dx, dy, dz), dy == 0
                    ? net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }
}
level.setDayTime(4000L);

var sealBlock = dev.tc4port.thaumadditions.registry.Content.SEAL.get();
var place = new java.util.function.BiFunction<net.minecraft.core.BlockPos, Object, Object>() {
    @Override
    public Object apply(net.minecraft.core.BlockPos at, Object face) {
        var state = sealBlock.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                (net.minecraft.core.Direction) face);
        level.setBlockAndUpdate(at, state);
        return level.getBlockEntity(at);
    }
};
var here = place.apply(near, net.minecraft.core.Direction.SOUTH);
var there = place.apply(far, net.minecraft.core.Direction.SOUTH);

var setSymbol = here.getClass().getMethod("setSymbol", int.class,
        dev.tc4port.thaumcraft.api.aspect.AspectId.class);
var setOwner = here.getClass().getMethod("setOwner", java.util.UUID.class);
var tick = here.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
        net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);

var owner = java.util.UUID.randomUUID();
for (Object tile : java.util.List.of(here, there)) {
    setSymbol.invoke(tile, 0, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("praecantatio"));
    setSymbol.invoke(tile, 1, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("aer"));
    setSymbol.invoke(tile, 2, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("permutatio"));
    setOwner.invoke(tile, owner);
}
tick.invoke(here, level, near, level.getBlockState(near));
tick.invoke(there, level, far, level.getBlockState(far));

dev.tc4port.thaumadditions.seals.SealInstance behaviour =
        (dev.tc4port.thaumadditions.seals.SealInstance)
                here.getClass().getMethod("instance").invoke(here);
out.append("bound=").append(behaviour.onSealActivated(level, near, player)).append("\n");

// The wall to see through it, and something to see beside it for comparison.
for (int dx = -3; dx <= 3; dx++) {
    for (int dy = 0; dy <= 4; dy++) {
        level.setBlockAndUpdate(far.offset(dx, dy, 2),
                net.minecraft.world.level.block.Blocks.MAGENTA_CONCRETE.defaultBlockState());
    }
}
for (int dy = 0; dy <= 6; dy++) {
    level.setBlockAndUpdate(far.offset(4, dy, 8),
            net.minecraft.world.level.block.Blocks.LIME_CONCRETE.defaultBlockState());
}

// Something in the seal's box to hold its hole open — one block out, which is inside the box and
// outside the 0.4-block reach it carries things off with.
var watcher = near.relative(net.minecraft.core.Direction.SOUTH);
for (var existing : level.getEntities((net.minecraft.world.entity.Entity) null,
        new net.minecraft.world.phys.AABB(near).inflate(6.0D), e -> true)) {
    existing.discard();
}
var item = new net.minecraft.world.entity.item.ItemEntity(level,
        watcher.getX() + 0.5D, watcher.getY() + 0.5D, watcher.getZ() + 0.7D,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
item.setNoGravity(true);
item.setInvulnerable(true);
level.addFreshEntity(item);

for (int i = 0; i < 10; i++) {
    for (var at : java.util.List.of(near, far)) {
        var be = (net.minecraft.world.level.block.entity.BlockEntity) level.getBlockEntity(at);
        if (be != null) {
            tick.invoke(be, level, at, level.getBlockState(at));
        }
    }
}
out.append("hole=").append(level.getBlockEntity(near).getClass()
        .getMethod("holeTarget").invoke(level.getBlockEntity(near))).append("\n");

// And the camera, on the client side, which is the only side whose rotation is the camera's.
var minecraft = net.minecraft.client.Minecraft.getInstance();
minecraft.execute(() -> {
    if (minecraft.player != null) {
        minecraft.player.setPos(2067.5D, 141.0D, 4.0D);
        minecraft.player.setYRot(0.0F);
        minecraft.player.setXRot(0.0F);
        minecraft.player.setYHeadRot(0.0F);
        minecraft.player.yRotO = 0.0F;
        minecraft.player.xRotO = 0.0F;
    }
});
out.append("player at (2067.5, 141, 4) looking south; portals at z=10 and z=20, wall at z=22\n");
return out.toString();
