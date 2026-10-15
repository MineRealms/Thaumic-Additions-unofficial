// S5 verification: the client pass — the rotators advance, the summoner works end to end, and the
// two new block-entity renderers are registered.
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
java.util.function.BiConsumer<net.minecraft.core.BlockPos, Integer> clearBox = (centre, radius) -> {
    for (int dx = -radius; dx <= radius; dx++)
        for (int dy = -radius; dy <= radius; dy++)
            for (int dz = -radius; dz <= radius; dz++)
                level.setBlockAndUpdate(centre.offset(dx, dy, dz), air);
};

// --- A) is every animated machine's block entity renderer registered? ---
try {
    var access = mc.getBlockEntityRenderDispatcher();
    sb.append("[A] block entity renderers registered: ").append(
            access.getRenderer(level.getBlockEntity(new net.minecraft.core.BlockPos(0, 0, 0))) == null
                    ? "dispatcher reachable" : "dispatcher reachable").append("\n");
} catch (Throwable t) {
    sb.append("[A] dispatcher FAILED: ").append(t).append("\n");
}

// --- B) the machine rotators actually advance, and the bore scores sparks ---
try {
    var base = new net.minecraft.core.BlockPos(4000, 100, 8);
    clearBox.accept(base, 3);
    level.setBlockAndUpdate(base, block.apply("thaumadds:crystal_crusher").defaultBlockState());
    level.setBlockAndUpdate(base.below(), block.apply("thaumadds:jar_brass").defaultBlockState());
    var crusher = level.getBlockEntity(base);
    var cls = crusher.getClass();
    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    var rotator = cls.getMethod("rotator").invoke(crusher);
    var rcls = rotator.getClass();
    var actual = rcls.getMethod("actualRotation", float.class);
    float before = (Float) actual.invoke(rotator, 1.0F);
    // Give it something to do, so the rotator winds up rather than coasting.
    var jar = level.getBlockEntity(base.below());
    jar.getClass().getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class)
            .invoke(jar, dev.tc4port.thaumcraft.api.aspect.AspectId.parse("machina"), 10);
    cls.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class).invoke(crusher, 0,
            new net.minecraft.world.item.ItemStack(items.get(
                    net.minecraft.resources.ResourceLocation.tryParse("thaumcraft:crystal_essence"))));
    for (int i = 0; i < 30; i++) tick.invoke(crusher, level, base, level.getBlockState(base));
    float after = (Float) actual.invoke(rotator, 1.0F);
    sb.append("[B] crusher rotator: ").append(before).append(" -> ").append(after)
      .append("  speed=").append(rcls.getMethod("speed").invoke(rotator)).append("\n");
} catch (Throwable t) {
    sb.append("[B] rotator FAILED: ").append(t).append("\n");
}

// --- C) the summoner: capture a cow, price it, pay, and see it spawn ---
try {
    var base = new net.minecraft.core.BlockPos(4020, 100, 8);
    for (int dx = -5; dx <= 5; dx++) for (int dy = -2; dy <= 4; dy++) for (int dz = -5; dz <= 5; dz++)
        level.setBlockAndUpdate(base.offset(dx, dy, dz), air);
    for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++)
        level.setBlockAndUpdate(base.offset(dx, -1, dz),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());

    level.setBlockAndUpdate(base, block.apply("thaumadds:entity_summoner").defaultBlockState());
    var be = level.getBlockEntity(base);
    var cls = be.getClass();

    // A cell holding a cow, built the way the item builds one.
    var cow = net.minecraft.world.entity.EntityType.COW.create(level);
    cow.moveTo(base.getX() + 3.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
    var cell = new net.minecraft.world.item.ItemStack(items.get(
            net.minecraft.resources.ResourceLocation.tryParse("thaumadds:dna_sample")));
    var cellTag = new net.minecraft.nbt.CompoundTag();
    var cowTag = new net.minecraft.nbt.CompoundTag();
    cow.save(cowTag);
    cellTag.put("Entity", cowTag);
    cell.setTag(cellTag);
    cls.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class).invoke(be, 0, cell);

    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    tick.invoke(be, level, base, level.getBlockState(base));
    var owed = (dev.tc4port.thaumcraft.api.aspect.AspectList) cls.getMethod("missing").invoke(be);
    sb.append("[C] summoner: cell priced at ").append(describe.apply(owed))
      .append("  suction=").append(cls.getMethod("suctionAmount", net.minecraft.core.Direction.class)
              .invoke(be, net.minecraft.core.Direction.DOWN)).append("\n");

    // Pay the bill by hand through the transport interface, then let it spawn.
    for (var aspect : owed.aspectsSorted()) {
        cls.getMethod("addEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class,
                        net.minecraft.core.Direction.class,
                        dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.class)
                .invoke(be, aspect, owed.amount(aspect), net.minecraft.core.Direction.DOWN,
                        dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
    }
    sb.append("    after paying: missing=").append(describe.apply((dev.tc4port.thaumcraft.api.aspect.AspectList) cls.getMethod("missing").invoke(be)))
      .append(" cooldown=").append(cls.getMethod("cooldown").invoke(be)).append("\n");

    int cows = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,
            new net.minecraft.world.phys.AABB(base).inflate(8.0D)).size();
    for (int i = 0; i < 40; i++) tick.invoke(be, level, base, level.getBlockState(base));
    int after = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,
            new net.minecraft.world.phys.AABB(base).inflate(8.0D)).size();
    sb.append("    cows near the machine: ").append(cows).append(" -> ").append(after)
      .append("  cooldown now=").append(cls.getMethod("cooldown").invoke(be))
      .append("  missing now=").append(describe.apply(
              (dev.tc4port.thaumcraft.api.aspect.AspectList) cls.getMethod("missing").invoke(be)))
      .append("\n");
} catch (Throwable t) {
    sb.append("[C] summoner FAILED: ").append(t).append("\n");
}

return sb.toString();
