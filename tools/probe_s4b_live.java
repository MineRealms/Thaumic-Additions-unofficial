// S4b verification, part 2: does each machine this batch added actually do its job in a world?
//
// Ticks are driven by hand so every step is deterministic. Block entities are reached reflectively:
// they extend HCLite's SyncedBlockEntity, which the bridge's in-memory compiler does not carry.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var player = srv.getPlayerList().getPlayers().get(0);
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var items = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
var bet = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE);

java.util.function.BiFunction<String, net.minecraft.core.BlockPos,
        net.minecraft.world.level.block.entity.BlockEntity> place = (name, pos) -> {
    var id = net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + name);
    var block = blocks.get(id);
    if (block == null) return null;
    level.setBlockAndUpdate(pos, block.defaultBlockState());
    return level.getBlockEntity(pos);
};
java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, String> typeOf = be -> {
    for (var t : bet) if (t.isValid(be.getBlockState())) return String.valueOf(bet.getKey(t));
    return "?";
};
var ignis = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis");
var telum = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("telum");
var salt = items.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:salt_essence"));

// --- A) the aura disperser: consume a pack, then smear its aspect over the player ---
try {
    var base = new net.minecraft.core.BlockPos(2000, 100, 8);
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
        for (int dy = -2; dy <= 3; dy++)
            level.setBlockAndUpdate(base.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    var state = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:aura_disperser"))
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                    net.minecraft.core.Direction.UP);
    level.setBlockAndUpdate(base, state);
    var be = level.getBlockEntity(base);
    var cls = be.getClass();
    sb.append("[A] disperser be=").append(typeOf.apply(be)).append("\n");

    var saltStack = new net.minecraft.world.item.ItemStack(salt);
    dev.tc4port.thaumadditions.items.AspectItems.setAspect(saltStack, ignis);
    var setItem = cls.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class);
    setItem.invoke(be, 0, saltStack);
    sb.append("    salt in slot 0 carries ").append(
            dev.tc4port.thaumadditions.items.AspectItems.aspectOf(((net.minecraft.world.Container) be).getItem(0)))
      .append("\n");

    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    var aspects = cls.getMethod("aspects");
    var timer = cls.getMethod("timer");

    tick.invoke(be, level, base, level.getBlockState(base));
    sb.append("    after 1 tick: aspects=").append(aspects.invoke(be))
      .append(" timer=").append(timer.invoke(be))
      .append(" slot0=").append(((net.minecraft.world.Container) be).getItem(0).getCount()).append("\n");

    // Stand the player inside the box the disperser covers and let it work for a second.
    player.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
    player.removeAllEffects();
    for (int i = 0; i < 25; i++) tick.invoke(be, level, base, level.getBlockState(base));
    var resist = player.getEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
    sb.append("    after 25 ticks: timer=").append(timer.invoke(be))
      .append(" fire_resistance=").append(resist == null ? "NONE" : resist.getDuration() + "t")
      .append(" aspects=").append(aspects.invoke(be)).append("\n");

    // Run it dry and confirm the machine empties itself.
    for (int i = 0; i < 600; i++) tick.invoke(be, level, base, level.getBlockState(base));
    sb.append("    after 625 ticks: aspects=").append(aspects.invoke(be))
      .append(" timer=").append(timer.invoke(be)).append("\n");
} catch (Throwable t) {
    sb.append("[A] disperser FAILED: ").append(t).append("\n");
}

// --- B) the crystal lamp: an aspect, its essence slot, and the redstone gate ---
try {
    var base = new net.minecraft.core.BlockPos(2010, 100, 8);
    level.setBlockAndUpdate(base, blocks.get(
            net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crystal_lamp")).defaultBlockState());
    var be = level.getBlockEntity(base);
    var cls = be.getClass();
    sb.append("[B] lamp be=").append(typeOf.apply(be)).append(" aspect before=")
      .append(cls.getMethod("readStoredAspect").invoke(be)).append("\n");
    cls.getMethod("setAspect", dev.tc4port.thaumcraft.api.aspect.AspectId.class).invoke(be, ignis);
    var slot0 = ((net.minecraft.world.Container) be).getItem(0);
    sb.append("    after setAspect(ignis): aspect=").append(cls.getMethod("getAspect").invoke(be))
      .append(" slot0=").append(slot0.getCount()).append("x ")
      .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(slot0.getItem()))
      .append(" colour=").append(Integer.toHexString((Integer) cls.getMethod("aspectColor").invoke(be)))
      .append("\n");
    level.setBlockAndUpdate(base.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
    sb.append("    powered: aspect=").append(cls.getMethod("getAspect").invoke(be))
      .append(" stored=").append(cls.getMethod("readStoredAspect").invoke(be)).append("\n");
    level.setBlockAndUpdate(base.east(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
} catch (Throwable t) {
    sb.append("[B] lamp FAILED: ").append(t).append("\n");
}

// --- C) the shadow enchanter: price a recipe, start it, then pay for it out of a jar ---
try {
    var base = new net.minecraft.core.BlockPos(2020, 100, 8);
    for (int dy = -1; dy <= 2; dy++) level.setBlockAndUpdate(base.above(dy),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    var be = place.apply("shadow_enchanter", base);
    var cls = be.getClass();
    sb.append("[C] enchanter be=").append(typeOf.apply(be)).append("\n");

    var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    cls.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class).invoke(be, 0, sword);

    var upLevel = cls.getMethod("upLevel", net.minecraft.world.level.Level.class,
            net.minecraft.resources.ResourceLocation.class);
    var downLevel = cls.getMethod("downLevel", net.minecraft.world.level.Level.class,
            net.minecraft.resources.ResourceLocation.class);
    var sharpness = net.minecraft.resources.ResourceLocation.parse("minecraft:sharpness");
    var mending = net.minecraft.resources.ResourceLocation.parse("minecraft:mending");
    upLevel.invoke(be, level, sharpness);
    upLevel.invoke(be, level, sharpness);
    sb.append("    after 2x upLevel(sharpness): ").append(cls.getMethod("enchants").invoke(be)).append("\n");
    upLevel.invoke(be, level, mending);
    sb.append("    +mending: ").append(cls.getMethod("enchants").invoke(be))
      .append("  (mending should be refused: it is incompatible with sharpness)\n");
    downLevel.invoke(be, level, sharpness);
    sb.append("    -sharpness: ").append(cls.getMethod("enchants").invoke(be))
      .append("  cost=").append(cls.getMethod("calculateAspects").invoke(be)).append("\n");

    cls.getMethod("startCraft", net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class)
            .invoke(be, level, base);
    sb.append("    startCraft: infusing=").append(cls.getMethod("infusing").invoke(be))
      .append(" pending=").append(cls.getMethod("pending").invoke(be)).append("\n");

    // Put the full price in a jar underneath and let the machine drain it.
    var jarBlock = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:jar_brass"));
    level.setBlockAndUpdate(base.below(), jarBlock.defaultBlockState());
    var jar = level.getBlockEntity(base.below());
    var jarCls = jar.getClass();
    var needed = (dev.tc4port.thaumcraft.api.aspect.AspectList) cls.getMethod("pending").invoke(be);
    jarCls.getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class)
            .invoke(jar, needed.aspectsSorted().get(0), jarCls.getMethod("getCapacity").invoke(jar));

    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    int finished = 0;
    for (int i = 0; i < 400 && finished == 0; i++) {
        tick.invoke(be, level, base, level.getBlockState(base));
        if (!((net.minecraft.world.Container) be).getItem(1).isEmpty()) finished = i + 1;
    }
    var out = ((net.minecraft.world.Container) be).getItem(1);
    sb.append("    finished after ").append(finished).append(" ticks: out=").append(out.getCount()).append("x ")
      .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(out.getItem()))
      .append(" ench=").append(out.getEnchantmentTags())
      .append("  in=").append(((net.minecraft.world.Container) be).getItem(0).getCount()).append("\n");
} catch (Throwable t) {
    sb.append("[C] enchanter FAILED: ").append(t).append("\n");
}

return sb.toString();
