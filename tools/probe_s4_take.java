// S4 diagnostics: why does a jar pull only one unit?
//
// Two jars, the upper one full so it advertises no suction. Then walk the exact chain the port's
// fillFromAbove walks, printing each step, and call EssentiaApi.take repeatedly.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);

var jarBlock = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:jar_brass"));
var base = srv.getPlayerList().getPlayers().get(0).blockPosition().above(30).offset(6, 0, 0);
level.setBlockAndUpdate(base, jarBlock.defaultBlockState());
level.setBlockAndUpdate(base.above(), jarBlock.defaultBlockState());

net.minecraft.world.level.block.entity.BlockEntity lower = level.getBlockEntity(base);
net.minecraft.world.level.block.entity.BlockEntity upper = level.getBlockEntity(base.above());
if (lower == null || upper == null) return "jars did not place";

var upperCls = upper.getClass();
var setEssentia = upperCls.getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class);
var getCapacity = upperCls.getMethod("getCapacity");
var getAmount = upperCls.getMethod("getAmount");
var aspect = dev.tc4port.thaumcraft.api.aspect.AspectApi.primals().iterator().next();
setEssentia.invoke(upper, aspect, getCapacity.invoke(upper));

var up = net.minecraft.core.Direction.UP;
var down = net.minecraft.core.Direction.DOWN;
var abovePos = base.above();

var transport = dev.tc4port.thaumcraft.api.essentia.EssentiaApi.transport(level, abovePos, down);
sb.append("transport above = ").append(transport == null ? "NULL" : transport.getClass().getName()).append("\n");
if (transport != null) {
    sb.append("  canOutputTo(DOWN)=").append(transport.canOutputTo(down))
      .append(" essentiaType(DOWN)=").append(transport.essentiaType(down))
      .append(" amount=").append(transport.essentiaAmount(down)).append("\n");
    sb.append("  availableEssentia(aer,DOWN)=").append(transport.availableEssentia(aspect, down))
      .append(" extractableAspect(DOWN)=").append(transport.extractableAspect(down)).append("\n");
    sb.append("  suctionAmount(DOWN)=").append(transport.suctionAmount(down)).append("\n");
    var view = transport.transferView();
    sb.append("  transferView=").append(view == null ? "NULL" : view.getClass().getName())
      .append(" view.availableEssentia=").append(view == null ? "-" : view.availableEssentia(aspect, down)).append("\n");
}

for (int i = 0; i < 5; i++) {
    int taken = dev.tc4port.thaumcraft.api.essentia.EssentiaApi.take(level, transport, aspect, 1, down,
            dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
    sb.append("take #").append(i).append(" -> ").append(taken)
      .append("  (upper now ").append(getAmount.invoke(upper)).append(")").append("\n");
}

level.removeBlock(base.above(), false);
level.removeBlock(base, false);
return sb.toString();
