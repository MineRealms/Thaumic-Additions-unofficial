// S4c verification: the three machines added after the last boot, plus the two drain fixes.
//
// [A] crusher and enchanter both drain from a jar below them now that the pull uses the neighbour
//     walk rather than a range search for an EssentiaSource.
// [B] the crystal bore drinks vis and harvests a cluster.
// [C] the essentia sink fills a shadow beam staff from a jar on its listening face.
// [D] the flux concentrator spreads a field and converts a pumpkin.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var items = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

java.util.function.Function<String, net.minecraft.world.level.block.Block> block = id ->
        blocks.get(net.minecraft.resources.ResourceLocation.tryParse(id));
java.util.function.Function<dev.tc4port.thaumcraft.api.aspect.AspectList, String> describe = list -> {
    if (list == null) return "null";
    StringBuilder out = new StringBuilder();
    for (var id : list.aspectsSorted()) {
        if (out.length() > 0) out.append(", ");
        out.append(id).append(" x").append(list.amount(id));
    }
    return out.toString();
};
java.util.function.BiFunction<net.minecraft.world.level.block.entity.BlockEntity, Integer, Integer> amount =
        (be, unused) -> {
            try { return (Integer) be.getClass().getMethod("getAmount").invoke(be); }
            catch (Throwable t) { return -1; }
        };
java.util.function.BiConsumer<net.minecraft.core.BlockPos, Integer> clearBox = (centre, radius) -> {
    for (int dx = -radius; dx <= radius; dx++)
        for (int dy = -radius; dy <= radius; dy++)
            for (int dz = -radius; dz <= radius; dz++)
                level.setBlockAndUpdate(centre.offset(dx, dy, dz), air);
};
var ignis = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis");

// --- A) the two drain fixes ---
try {
    var base = new net.minecraft.core.BlockPos(3000, 100, 8);
    clearBox.accept(base, 3);
    level.setBlockAndUpdate(base, block.apply("thaumadds:crystal_crusher").defaultBlockState());
    level.setBlockAndUpdate(base.below(), block.apply("thaumadds:jar_brass").defaultBlockState());
    var crusher = level.getBlockEntity(base);
    var jar = level.getBlockEntity(base.below());
    var machina = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("machina");
    jar.getClass().getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class)
            .invoke(jar, machina, 10);
    crusher.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class)
            .invoke(crusher, level, base, level.getBlockState(base));
    sb.append("[A] crusher: fuel=").append(crusher.getClass().getMethod("crushes").invoke(crusher))
      .append(" jar left=").append(amount.apply(jar, 0)).append("   (want fuel=3, jar=9)\n");
} catch (Throwable t) {
    sb.append("[A] crusher FAILED: ").append(t).append("\n");
}
try {
    var base = new net.minecraft.core.BlockPos(3010, 100, 8);
    clearBox.accept(base, 3);
    level.setBlockAndUpdate(base, block.apply("thaumadds:shadow_enchanter").defaultBlockState());
    level.setBlockAndUpdate(base.below(), block.apply("thaumadds:jar_brass").defaultBlockState());
    var enchanter = level.getBlockEntity(base);
    var jar = level.getBlockEntity(base.below());
    var cls = enchanter.getClass();
    cls.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class)
            .invoke(enchanter, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
    cls.getMethod("upLevel", net.minecraft.world.level.Level.class, net.minecraft.resources.ResourceLocation.class)
            .invoke(enchanter, level, net.minecraft.resources.ResourceLocation.parse("minecraft:sharpness"));
    var pending = (dev.tc4port.thaumcraft.api.aspect.AspectList) cls.getMethod("calculateAspects").invoke(enchanter);
    jar.getClass().getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class)
            .invoke(jar, pending.aspectsSorted().get(0), 50);
    cls.getMethod("startCraft", net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class)
            .invoke(enchanter, level, base);
    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    int finished = 0;
    for (int i = 0; i < 400; i++) {
        tick.invoke(enchanter, level, base, level.getBlockState(base));
        if (!((net.minecraft.world.Container) enchanter).getItem(1).isEmpty()) { finished = i + 1; break; }
    }
    var out = ((net.minecraft.world.Container) enchanter).getItem(1);
    sb.append("[A] enchanter: cost=").append(describe.apply(pending))
      .append(" finished after ").append(finished).append(" ticks, out=").append(out.getCount()).append("x ")
      .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(out.getItem()))
      .append(" ench=").append(out.getEnchantmentTags())
      .append(" jar left=").append(amount.apply(jar, 0)).append("\n");
} catch (Throwable t) {
    sb.append("[A] enchanter FAILED: ").append(t).append("\n");
}

// --- B) the bore: a fire cluster above it, a chest below to catch the essence ---
try {
    var base = new net.minecraft.core.BlockPos(3020, 100, 8);
    clearBox.accept(base, 3);
    level.setBlockAndUpdate(base, block.apply("thaumadds:crystal_bore").defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                    net.minecraft.core.Direction.UP));
    level.setBlockAndUpdate(base.below(),
            net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
    var cluster = block.apply("thaumcraft:fire_crystal_cluster");
    var clusterState = cluster.defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                    net.minecraft.core.Direction.DOWN);
    level.setBlockAndUpdate(base.above(), clusterState);
    var be = level.getBlockEntity(base);
    var cls = be.getClass();
    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    var chest = level.getBlockEntity(base.below());
    sb.append("[B] bore: cluster=").append(net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getKey(level.getBlockState(base.above()).getBlock())).append("\n");
    int available = dev.tc4port.thaumcraft.api.node.VisNetworkApi.drain(
            srv.overworld(), base, dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS, 100,
            dev.tc4port.thaumcraft.api.aspect.VisAction.SIMULATE);
    sb.append("    vis available nearby (ignis, centivis)=").append(available).append("\n");
    for (int i = 0; i < 120; i++) tick.invoke(be, level, base, level.getBlockState(base));
    sb.append("    after 120 ticks: boring=").append(cls.getMethod("boring").invoke(be))
      .append("  (the vis network offers nothing at this spot, so the drill never turns)").append("\n");
    // Drive the harvest directly, which is the half of the cycle that does not depend on vis.
    var harvest = cls.getDeclaredMethod("harvest", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.core.Direction.class);
    harvest.setAccessible(true);
    harvest.invoke(be, level, base, net.minecraft.core.Direction.UP);
    var caught = ((net.minecraft.world.Container) chest).getItem(0);
    sb.append("    harvest(UP): cluster now=").append(net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getKey(level.getBlockState(base.above()).getBlock()))
      .append(" chest0=").append(caught.getCount()).append("x ")
      .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(caught.getItem()))
      .append(" aspect=").append(dev.tc4port.thaumadditions.items.AspectItems.aspectOf(caught)).append("\n");
} catch (Throwable t) {
    sb.append("[B] bore FAILED: ").append(t).append("\n");
}


// --- C) the sink: a jar of ELDRITCH on its listening face and a staff in its slot ---
try {
    var base = new net.minecraft.core.BlockPos(3040, 100, 8);
    clearBox.accept(base, 3);
    level.setBlockAndUpdate(base, block.apply("thaumadds:essentia_sink").defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                    net.minecraft.core.Direction.NORTH));
    var be = level.getBlockEntity(base);
    var face = (net.minecraft.core.Direction) be.getClass().getMethod("essentiaFace").invoke(be);
    level.setBlockAndUpdate(base.relative(face), block.apply("thaumadds:jar_brass").defaultBlockState());
    var jar = level.getBlockEntity(base.relative(face));
    var alienis = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("alienis");
    jar.getClass().getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class)
            .invoke(jar, alienis, 20);
    var staff = new net.minecraft.world.item.ItemStack(
            items.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:shadow_beam_staff")));
    be.getClass().getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class).invoke(be, 0, staff);
    var tick = be.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    sb.append("[C] sink: listening on ").append(face)
      .append(" suction=").append(be.getClass().getMethod("suctionAmount", net.minecraft.core.Direction.class)
              .invoke(be, face))
      .append(" request=").append(be.getClass().getMethod("suctionType", net.minecraft.core.Direction.class)
              .invoke(be, face))
      .append(" capacity left=").append(dev.tc4port.thaumadditions.items.AspectCharges
              .remainingCapacity(((net.minecraft.world.Container) be).getItem(0), alienis)).append("\n");
    for (int i = 0; i < 8; i++) tick.invoke(be, level, base, level.getBlockState(base));
    var charged = ((net.minecraft.world.Container) be).getItem(0);
    sb.append("    after 8 ticks: charge=")
      .append(dev.tc4port.thaumadditions.items.AspectCharges.charge(charged))
      .append(" jar left=").append(amount.apply(jar, 0)).append("   (want charge=8, jar=12)\n");
} catch (Throwable t) {
    sb.append("[C] sink FAILED: ").append(t).append("\n");
}

// --- D) the concentrator: a pumpkin above it, vitium in a jar below ---
try {
    var base = new net.minecraft.core.BlockPos(3060, 100, 8);
    clearBox.accept(base, 5);
    // The field is allowed to reach sixteen blocks, and any column open to the sky makes it
    // abandon the whole thing, so the roof has to be wider than that.
    for (int dx = -18; dx <= 18; dx++) for (int dz = -18; dz <= 18; dz++) {
        level.setBlockAndUpdate(base.offset(dx, -1, dz),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base.offset(dx, 4, dz),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    }
    level.setBlockAndUpdate(base, block.apply("thaumadds:flux_concentrator").defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                    net.minecraft.core.Direction.DOWN));
    level.setBlockAndUpdate(base.above(2), net.minecraft.world.level.block.Blocks.PUMPKIN.defaultBlockState());
    level.setBlockAndUpdate(base.below(), block.apply("thaumadds:jar_brass").defaultBlockState());
    var jar = level.getBlockEntity(base.below());
    var vitium = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("vitium");
    jar.getClass().getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class)
            .invoke(jar, vitium, jar.getClass().getMethod("getCapacity").invoke(jar));
    var be = level.getBlockEntity(base);
    var cls = be.getClass();
    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    for (int i = 0; i < 800; i++) tick.invoke(be, level, base, level.getBlockState(base));
    var covered = (java.util.List<?>) cls.getMethod("fluxed").invoke(be);
    sb.append("[D] concentrator: fuel=").append(cls.getMethod("fuel").invoke(be))
      .append(" vitium=").append(cls.getMethod("getAmount").invoke(be))
      .append(" field=").append(covered.size())
      .append(" jar left=").append(amount.apply(jar, 0)).append("\n");
    for (int dy = 1; dy <= 3; dy++) {
        sb.append("    above+").append(dy).append("=")
          .append(net.minecraft.core.registries.BuiltInRegistries.BLOCK
                  .getKey(level.getBlockState(base.above(dy)).getBlock())).append("\n");
    }
} catch (Throwable t) {
    sb.append("[D] concentrator FAILED: ").append(t).append("\n");
}

return sb.toString();
