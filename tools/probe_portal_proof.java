// A wall of one colour right behind the far portal. If the near portal shows that colour, the
// window is looking at the far side; if it shows grey, it is not.
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);

// The near portal is at (2067,141,10) facing south; the far one at (2067,141,20) facing south.
// A wall of magenta goes two blocks behind the far portal, and one of lime behind the player.
var far = new net.minecraft.core.BlockPos(2067, 141, 20);
for (int dx = -3; dx <= 3; dx++) {
    for (int dy = 0; dy <= 4; dy++) {
        level.setBlockAndUpdate(far.offset(dx, dy, 2),
                net.minecraft.world.level.block.Blocks.MAGENTA_CONCRETE.defaultBlockState());
    }
}
// And a tall pillar of lime just outside, for the same reason, seen around rather than through.
for (int dy = 0; dy <= 6; dy++) {
    level.setBlockAndUpdate(far.offset(4, dy, 8),
            net.minecraft.world.level.block.Blocks.LIME_CONCRETE.defaultBlockState());
}

// Stand where the near portal fills a good part of the view.
player.teleportTo(2067.5D, 141.0D, 4.0D);
player.setYRot(0.0F);
player.setXRot(0.0F);
level.setDayTime(4000L);

out.append("player at ").append(player.blockPosition()).append(" looking south\n");
out.append("near portal (2067,141,10), far portal (2067,141,20), magenta wall behind the far one\n");
return out.toString();
