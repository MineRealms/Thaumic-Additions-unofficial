// S4 verification, part 1: do the machines tick at all, and does a jar fill itself?
//
// The ticker is driven by hand rather than waiting on wall-clock ticks, so the whole check runs on
// the server thread in one call. A full jar advertises no suction (suctionAmount returns 0) while an
// empty one advertises 32, which is exactly the "out-suck the neighbour" condition the 1.12
// fillAbstractJar asks for, so two jars are enough to exercise the transfer.
//
// The essentia API is reached reflectively: the block entities extend HCLite's SyncedBlockEntity,
// and the bridge's in-memory compiler does not carry that module on its classpath.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var bet = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE);

// --- 1) every block that carries a block entity hands back a ticker ---
java.util.Map<String, String> tickers = new java.util.TreeMap<>();
for (var e : blocks.entrySet()) {
    if (!e.getKey().location().getNamespace().equals("thaumadds")) continue;
    var block = e.getValue();
    if (!(block instanceof net.minecraft.world.level.block.EntityBlock eb)) continue;
    var state = block.defaultBlockState();
    for (var t : bet) {
        if (!t.isValid(state)) continue;
        tickers.put(e.getKey().location().getPath(), eb.getTicker(level, state, t) == null ? "NULL" : "ok");
        break;
    }
}
sb.append("BE blocks with a ticker: ")
  .append(tickers.values().stream().filter("ok"::equals).count()).append("/").append(tickers.size()).append("\n");
for (var en : tickers.entrySet()) if (!"ok".equals(en.getValue())) sb.append("   NO TICKER: ").append(en.getKey()).append("\n");

// --- 2) two jars: fill the upper one, tick the lower one, see whether it pulls ---
var jarBlock = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:jar_brass"));
var base = srv.getPlayerList().getPlayers().get(0).blockPosition().above(30).offset(4, 0, 0);
level.setBlockAndUpdate(base, jarBlock.defaultBlockState());
level.setBlockAndUpdate(base.above(), jarBlock.defaultBlockState());

net.minecraft.world.level.block.entity.BlockEntity lower = level.getBlockEntity(base);
net.minecraft.world.level.block.entity.BlockEntity upper = level.getBlockEntity(base.above());

if (lower != null && upper != null) {
    var cls = lower.getClass();
    var setEssentia = cls.getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class);
    var getAmount = cls.getMethod("getAmount");
    var getCapacity = cls.getMethod("getCapacity");
    var getAspect = cls.getMethod("getAspect");
    var suctionAmount = cls.getMethod("suctionAmount", net.minecraft.core.Direction.class);

    var aspect = dev.tc4port.thaumcraft.api.aspect.AspectApi.primals().iterator().next();
    try {
        setEssentia.invoke(upper, aspect, getCapacity.invoke(upper));
        sb.append("aspect under test: ").append(aspect.value()).append("\n");
        sb.append("upper: ").append(getAmount.invoke(upper)).append("/").append(getCapacity.invoke(upper))
          .append(" suction(down)=").append(suctionAmount.invoke(upper, net.minecraft.core.Direction.DOWN)).append("\n");
        sb.append("lower before: ").append(getAmount.invoke(lower))
          .append(" suction(up)=").append(suctionAmount.invoke(lower, net.minecraft.core.Direction.UP)).append("\n");
    } catch (Throwable t) {
        var c = t;
        while (c.getCause() != null) c = c.getCause();
        sb.append("SETUP THREW: ").append(c).append("\n");
        var st = c.getStackTrace();
        for (int i = 0; i < Math.min(6, st.length); i++) sb.append("    at ").append(st[i]).append("\n");
        return sb.toString();
    }

    var state = level.getBlockState(base);
    var block = state.getBlock();
    if (block instanceof net.minecraft.world.level.block.EntityBlock eb) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        var ticker = eb.getTicker(level, state,
                (net.minecraft.world.level.block.entity.BlockEntityType) lower.getType());
        if (ticker == null) {
            sb.append("NO TICKER for the jar block\n");
        } else {
            for (int i = 0; i < 100; i++) {
                ticker.tick(level, base, state, lower);
            }
        }
    } else {
        sb.append("jar block is not an EntityBlock\n");
    }
    var lowerAspect = getAspect.invoke(lower);
    sb.append("lower after 100 ticks: ").append(getAmount.invoke(lower)).append("/").append(getCapacity.invoke(lower))
      .append(" aspect=").append(lowerAspect == null ? "-" : String.valueOf(lowerAspect)).append("\n");
    sb.append("upper after: ").append(getAmount.invoke(upper)).append("/").append(getCapacity.invoke(upper)).append("\n");
    sb.append("expected: the lower jar gained about 20, one unit every five ticks\n");

    setEssentia.invoke(lower, lowerAspect, getCapacity.invoke(lower));
    sb.append("lower when full: suction(up)=").append(suctionAmount.invoke(lower, net.minecraft.core.Direction.UP)).append("\n");
} else {
    sb.append("FAILED to place both jars: lower=").append(lower).append(" upper=").append(upper).append("\n");
}

level.removeBlock(base.above(), false);
level.removeBlock(base, false);
return sb.toString();
