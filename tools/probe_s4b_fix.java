// S4b verification, part 3: the two machines part 2 left unresolved.
//
// [A] the disperser found no entity. Print the box it covers and what is in it, then call
//     EdibleAspect.execute directly to separate "the table does not fire" from "the box is empty".
// [C] the enchanter was under-budgeted: sharpness(20 telum) + mending(200 fames + 500 permutatio)
//     is 720 points and the probe only ran 400 ticks. Re-run with a recipe that costs 20.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var player = srv.getPlayerList().getPlayers().get(0);
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var items = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
var bet = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE);
var ignis = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis");

java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, String> typeOf = be -> {
    for (var t : bet) if (t.isValid(be.getBlockState())) return String.valueOf(bet.getKey(t));
    return "?";
};
java.util.function.Function<dev.tc4port.thaumcraft.api.aspect.AspectList, String> describe = list -> {
    if (list == null) return "null";
    StringBuilder out = new StringBuilder();
    for (var id : list.aspectsSorted()) {
        if (out.length() > 0) out.append(", ");
        out.append(id).append(" x").append(list.amount(id));
    }
    return out.toString();
};

// --- A) is the edible-aspect table firing at all, and where is the player relative to the box? ---
try {
    var list = new dev.tc4port.thaumcraft.api.aspect.AspectList();
    list.add(ignis, 1);
    player.removeAllEffects();
    var used = dev.tc4port.thaumadditions.aspect.EdibleAspect.execute(player, list);
    var direct = player.getEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
    sb.append("[A] direct EdibleAspect.execute(ignis x1): used=").append(used.visSize())
      .append(" fire_resistance=").append(direct == null ? "NONE" : direct.getDuration() + "t").append("\n");

    var base = new net.minecraft.core.BlockPos(2000, 100, 8);
    var be = level.getBlockEntity(base);
    if (be == null) {
        sb.append("[A] no disperser at ").append(base).append("\n");
    } else {
        var facing = level.getBlockState(base)
                .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        var box = new net.minecraft.world.phys.AABB(base)
                .move(facing.getStepX() * 5, facing.getStepY() * 4, facing.getStepZ() * 5)
                .inflate(4.0, 3.0, 4.0);
        sb.append("[A] facing=").append(facing).append(" box=").append(box)
          .append(" player=").append(player.position()).append(" alive=").append(player.isAlive()).append("\n");
        var inBox = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box);
        sb.append("    entities in box: ").append(inBox.size());
        for (var e : inBox) sb.append(" ").append(e.getClass().getSimpleName()).append("@").append(e.blockPosition());
        sb.append("\n");

        // Run the machine's own applyAura by hand and see whether the aspect is spent.
        var cls = be.getClass();
        var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
                net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
        var timer = cls.getMethod("timer");
        var aspects = cls.getMethod("aspects");
        player.removeAllEffects();
        for (int i = 0; i < 40; i++) tick.invoke(be, level, base, level.getBlockState(base));
        var res = player.getEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
        sb.append("    after 40 ticks: timer=").append(timer.invoke(be))
          .append(" fire_resistance=").append(res == null ? "NONE" : res.getDuration() + "t")
          .append(" redstone=").append(level.getBestNeighborSignal(base)).append("\n");
    }
} catch (Throwable t) {
    sb.append("[A] FAILED: ").append(t).append("\n");
}

// --- C) a recipe the jar can actually pay for in a few hundred ticks ---
try {
    var base = new net.minecraft.core.BlockPos(2040, 100, 8);
    for (int dy = -1; dy <= 2; dy++) level.setBlockAndUpdate(base.above(dy),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    level.setBlockAndUpdate(base, blocks.get(
            net.minecraft.resources.ResourceLocation.tryParse("thaumadds:shadow_enchanter")).defaultBlockState());
    var be = level.getBlockEntity(base);
    var cls = be.getClass();
    sb.append("[C] enchanter be=").append(typeOf.apply(be)).append("\n");

    cls.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class)
            .invoke(be, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
    var upLevel = cls.getMethod("upLevel", net.minecraft.world.level.Level.class,
            net.minecraft.resources.ResourceLocation.class);
    var sharpness = net.minecraft.resources.ResourceLocation.parse("minecraft:sharpness");
    var smite = net.minecraft.resources.ResourceLocation.parse("minecraft:smite");
    upLevel.invoke(be, level, sharpness);
    upLevel.invoke(be, level, smite);
    sb.append("    sharpness then smite (mutually exclusive): ")
      .append(cls.getMethod("enchants").invoke(be)).append("\n");
    cls.getMethod("downLevel", net.minecraft.world.level.Level.class,
            net.minecraft.resources.ResourceLocation.class).invoke(be, level, smite);

    var pending = (dev.tc4port.thaumcraft.api.aspect.AspectList) cls.getMethod("calculateAspects").invoke(be);
    sb.append("    sharpness@1 cost: ").append(describe.apply(pending)).append("\n");
    cls.getMethod("startCraft", net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class)
            .invoke(be, level, base);
    sb.append("    infusing=").append(cls.getMethod("infusing").invoke(be)).append("\n");

    var jarBlock = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:jar_brass"));
    level.setBlockAndUpdate(base.below(), jarBlock.defaultBlockState());
    var jar = level.getBlockEntity(base.below());
    var jarCls = jar.getClass();
    jarCls.getMethod("setEssentia", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class)
            .invoke(jar, pending.aspectsSorted().get(0), jarCls.getMethod("getCapacity").invoke(jar));
    sb.append("    jar: ").append(jarCls.getMethod("getAspect").invoke(jar)).append(" x")
      .append(jarCls.getMethod("getAmount").invoke(jar)).append("\n");

    var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
    int finished = 0;
    for (int i = 0; i < 600; i++) {
        tick.invoke(be, level, base, level.getBlockState(base));
        if (!((net.minecraft.world.Container) be).getItem(1).isEmpty()) { finished = i + 1; break; }
    }
    var out = ((net.minecraft.world.Container) be).getItem(1);
    sb.append("    finished after ").append(finished).append(" ticks: out=").append(out.getCount()).append("x ")
      .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(out.getItem()))
      .append(" enchantments=").append(out.getEnchantmentTags())
      .append("  jar left=").append(jarCls.getMethod("getAmount").invoke(jar))
      .append("  input left=").append(((net.minecraft.world.Container) be).getItem(0).getCount()).append("\n");
} catch (Throwable t) {
    sb.append("[C] FAILED: ").append(t).append("\n");
}

return sb.toString();
