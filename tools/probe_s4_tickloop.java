// S4 diagnostics: walk the jar's ticker one tick at a time and show what changes.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);

var jarBlock = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:jar_brass"));
var base = srv.getPlayerList().getPlayers().get(0).blockPosition().above(30).offset(8, 0, 0);
level.setBlockAndUpdate(base, jarBlock.defaultBlockState());
level.setBlockAndUpdate(base.above(), jarBlock.defaultBlockState());

net.minecraft.world.level.block.entity.BlockEntity lower = level.getBlockEntity(base);
net.minecraft.world.level.block.entity.BlockEntity upper = level.getBlockEntity(base.above());
if (lower == null || upper == null) return "jars did not place";

var cls = lower.getClass();
var setEssentia = cls.getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class);
var getCapacity = cls.getMethod("getCapacity");
var getAmount = cls.getMethod("getAmount");
var getAspect = cls.getMethod("getAspect");
var suctionAmount = cls.getMethod("suctionAmount", net.minecraft.core.Direction.class);
var aspect = dev.tc4port.thaumcraft.api.aspect.AspectApi.primals().iterator().next();
setEssentia.invoke(upper, aspect, getCapacity.invoke(upper));

var fillTickField = cls.getDeclaredField("fillTick");
fillTickField.setAccessible(true);

var state = level.getBlockState(base);
if (!(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb)) return "no EntityBlock";
@SuppressWarnings({"unchecked", "rawtypes"})
var ticker = eb.getTicker(level, state, (net.minecraft.world.level.block.entity.BlockEntityType) lower.getType());
if (ticker == null) return "no ticker";

sb.append("tick | fillTick | lower amount | aspect | upper amount\n");
for (int i = 1; i <= 20; i++) {
    try {
        ticker.tick(level, base, state, lower);
    } catch (Throwable t) {
        var c = t;
        while (c.getCause() != null) c = c.getCause();
        sb.append("tick ").append(i).append(" THREW ").append(c).append("\n");
        var st = c.getStackTrace();
        for (int k = 0; k < Math.min(5, st.length); k++) sb.append("      at ").append(st[k]).append("\n");
        break;
    }
    if (i % 5 == 0 || i <= 6) {
        sb.append(String.format("%4d | %8s | %12s | %-18s | %s%n", i, fillTickField.get(lower),
                getAmount.invoke(lower), String.valueOf(getAspect.invoke(lower)),
                getAmount.invoke(upper)));
    }
}
sb.append("lower suction(up)=").append(suctionAmount.invoke(lower, net.minecraft.core.Direction.UP)).append("\n");
sb.append("upper suction(down)=").append(suctionAmount.invoke(upper, net.minecraft.core.Direction.DOWN)).append("\n");

level.removeBlock(base.above(), false);
level.removeBlock(base, false);
return sb.toString();
