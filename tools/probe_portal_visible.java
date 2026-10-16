// Leaves a bound portal standing in front of the player, so the renderer is exercised for real.
StringBuilder out = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayers().get(0);

var sealBlock = dev.tc4port.thaumadditions.registry.Content.SEAL.get();
var facing = player.getDirection();
var front = player.blockPosition().relative(facing, 3);
var behind = front.relative(facing, 2);

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

var here = place.apply(front, facing.getOpposite());
var there = place.apply(behind, facing);

var setSymbol = here.getClass().getMethod("setSymbol", int.class,
        dev.tc4port.thaumcraft.api.aspect.AspectId.class);
var setOwner = here.getClass().getMethod("setOwner", java.util.UUID.class);
var instance = here.getClass().getMethod("instance");
var tick = here.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
        net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);

var p = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("praecantatio");
var a = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("aer");
var key = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis");
for (Object tile : java.util.List.of(here, there)) {
    setSymbol.invoke(tile, 0, p);
    setSymbol.invoke(tile, 1, a);
    setSymbol.invoke(tile, 2, key);
    setOwner.invoke(tile, player.getUUID());
}

tick.invoke(here, level, front, level.getBlockState(front));
tick.invoke(there, level, behind, level.getBlockState(behind));

dev.tc4port.thaumadditions.seals.SealInstance behaviour =
        (dev.tc4port.thaumadditions.seals.SealInstance) instance.invoke(here);
out.append("bound=").append(behaviour.onSealActivated(level, front, player)).append("\n");

// Something in the seal, so the hole opens and stays open while it is looked at.
var item = new net.minecraft.world.entity.item.ItemEntity(level,
        front.getX() + 0.5D, front.getY() + 0.5D, front.getZ() + 0.5D,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
item.setNoGravity(true);
item.setInvulnerable(true);
level.addFreshEntity(item);

out.append("portal at ").append(front).append(" facing ").append(facing.getOpposite())
   .append(", twin at ").append(behind).append("\n");
return out.toString();
