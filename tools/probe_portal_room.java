// Builds a plain room with a portal in it, so the window can be looked at without trees and
// hills in the way, and puts the player in front of the portal facing squarely into it.
//
// The earlier probe dropped a pair wherever the player happened to be standing, which is fine for
// checking that the seal works and useless for judging what it looks like.
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);

// A platform high in the air, well clear of anything.
int y = 140;
var floor = new net.minecraft.core.BlockPos(player.blockPosition().getX(), y, player.blockPosition().getZ());
for (int dx = -12; dx <= 12; dx++) {
    for (int dz = -12; dz <= 24; dz++) {
        for (int dy = 0; dy <= 6; dy++) {
            level.setBlockAndUpdate(floor.offset(dx, dy, dz),
                    dy == 0 ? net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState()
                            : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }
}

// The player stands at one end, the portal pair runs away from them down the middle.
var stand = floor.offset(0, 1, 0);
player.teleportTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
player.setYRot(0.0F);
player.setXRot(0.0F);

var near = floor.offset(0, 1, 8);
var far = floor.offset(0, 1, 18);

var sealBlock = dev.tc4port.thaumadditions.registry.Content.SEAL.get();
var place = new java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.core.Direction, Object>() {
    @Override
    public Object apply(net.minecraft.core.BlockPos at, net.minecraft.core.Direction face) {
        var state = sealBlock.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, face);
        level.setBlockAndUpdate(at, state);
        return level.getBlockEntity(at);
    }
};
// Both faces point back toward the player, so both are seen head on.
var here = place.apply(near, net.minecraft.core.Direction.SOUTH);
var there = place.apply(far, net.minecraft.core.Direction.SOUTH);

var setSymbol = here.getClass().getMethod("setSymbol", int.class,
        dev.tc4port.thaumcraft.api.aspect.AspectId.class);
var setOwner = here.getClass().getMethod("setOwner", java.util.UUID.class);
var instance = here.getClass().getMethod("instance");
var tick = here.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
        net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);

var first = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("praecantatio");
var second = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("aer");
// A network of its own: every earlier probe left seals behind in this world, they share the
// symbol an earlier run used, and the binding walks to whichever peer comes next in the list.
var key = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("permutatio");
var owner = java.util.UUID.randomUUID();
for (Object tile : java.util.List.of(here, there)) {
    setSymbol.invoke(tile, 0, first);
    setSymbol.invoke(tile, 1, second);
    setSymbol.invoke(tile, 2, key);
    setOwner.invoke(tile, owner);
}
tick.invoke(here, level, near, level.getBlockState(near));
tick.invoke(there, level, far, level.getBlockState(far));

dev.tc4port.thaumadditions.seals.SealInstance behaviour =
        (dev.tc4port.thaumadditions.seals.SealInstance) instance.invoke(here);
out.append("bound=").append(behaviour.onSealActivated(level, near, player)).append("\n");

// Something in the near seal's box, so its hole is open and there is a window to look at — but
// one block in front of the seal rather than inside it. A portal carries through anything that
// touches it, and the block that opens the hole was being carried through it and closing it again.
// The seal watches one block either side of itself for something to open for, and reaches 0.4 of
// a block past its own for something to carry. Between those two is a band where a thing holds the
// hole open without falling through it, and that is where this goes.
var watcher = near.relative(net.minecraft.core.Direction.SOUTH);
var item = new net.minecraft.world.entity.item.ItemEntity(level,
        watcher.getX() + 0.5D, watcher.getY() + 0.5D, watcher.getZ() + 0.7D,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
item.setNoGravity(true);
item.setInvulnerable(true);
level.addFreshEntity(item);

// And something to see through it: a pillar on the far side of the far portal.
for (int dy = 1; dy <= 4; dy++) {
    level.setBlockAndUpdate(far.offset(0, dy, 6),
            dy % 2 == 0 ? net.minecraft.world.level.block.Blocks.RED_CONCRETE.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.WHITE_CONCRETE.defaultBlockState());
}

// Let the server tick the seal a moment, so its hole opens before anyone looks.
for (int i = 0; i < 12; i++) {
    for (var at : java.util.List.of(near, far)) {
        var be = (net.minecraft.world.level.block.entity.BlockEntity) level.getBlockEntity(at);
        if (be != null) {
            be.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
                    net.minecraft.core.BlockPos.class,
                    net.minecraft.world.level.block.state.BlockState.class)
                    .invoke(be, level, at, level.getBlockState(at));
        }
    }
}
var still = level.getEntities((net.minecraft.world.entity.Entity) null,
        new net.minecraft.world.phys.AABB(near).inflate(1.0D), e -> true);
out.append("things standing in the near seal: ").append(still.size()).append("\n");
var tile = level.getBlockEntity(near);
out.append("holeTarget: ").append(tile.getClass().getMethod("holeTarget").invoke(tile)).append("\n");

out.append("player at ").append(stand).append(", near portal at ").append(near)
   .append(", far portal at ").append(far).append("\n");
out.append("stand facing south (yaw 0) and look straight ahead: the portal is 8 blocks away.\n");
return out.toString();
