// Does crystal water stand in the world, and does the chamber see it?
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);
var centre = player.blockPosition().offset(-6, 3, -6);
for (int dx = -3; dx <= 3; dx++) {
    for (int dz = -3; dz <= 3; dz++) {
        for (int dy = -1; dy <= 1; dy++) {
            level.setBlockAndUpdate(centre.offset(dx, dy, dz),
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }
}

var water = (net.minecraft.world.level.material.FlowingFluid)
        net.minecraft.core.registries.BuiltInRegistries.FLUID
                .get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crystal_water"));
var sourceState = water.getSource(false).createLegacyBlock();

// What a bucket of it does to the world: a source block, in its own block, level 0.
level.setBlockAndUpdate(centre, sourceState);
var placed = level.getBlockState(centre);
out.append("placed=").append(placed)
   .append(" isSource=").append(level.getFluidState(centre).isSource()).append("\n");
out.append("countCentre(with 1 source)=")
   .append(dev.tc4port.thaumadditions.blocks.BlockCrystalWater.countSources(level, centre)).append("\n");

// The ring the growth chamber reads: eight sources around a middle block.
for (int dx = -1; dx <= 1; dx++) {
    for (int dz = -1; dz <= 1; dz++) {
        if (dx != 0 || dz != 0) {
            level.setBlockAndUpdate(centre.offset(dx, 0, dz), sourceState);
        }
    }
}
out.append("countRing(with 8 sources)=")
   .append(dev.tc4port.thaumadditions.blocks.BlockCrystalWater.countSources(level, centre)).append("\n");

// A flowing edge must not count; only sources do.
level.setBlockAndUpdate(centre.offset(1, 0, 1), water.getFlowing(4, false).createLegacyBlock());
out.append("countWithOneFlowing=")
   .append(dev.tc4port.thaumadditions.blocks.BlockCrystalWater.countSources(level, centre)).append("\n");

for (int dx = -2; dx <= 2; dx++) {
    for (int dz = -2; dz <= 2; dz++) {
        level.setBlockAndUpdate(centre.offset(dx, 0, dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
}
return out.toString();
