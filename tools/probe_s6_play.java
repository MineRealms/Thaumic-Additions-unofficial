// S6: play the new logic live. Seals, the portal pair, and the entities.
//
// Everything that lives on HCLite is reached reflectively: the bridge's in-memory compiler does not
// carry that module, so TileSeal cannot even be named as a type here.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var player = srv.getPlayerList().getPlayers().get(0);
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);

java.util.function.Function<String, net.minecraft.world.level.block.Block> block = id ->
        blocks.get(net.minecraft.resources.ResourceLocation.tryParse(id));
java.util.function.BiConsumer<net.minecraft.core.BlockPos, Integer> clearBox = (centre, radius) -> {
    for (int dx = -radius; dx <= radius; dx++)
        for (int dy = -radius; dy <= radius; dy++)
            for (int dz = -radius; dz <= radius; dz++)
                level.setBlockAndUpdate(centre.offset(dx, dy, dz),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
};
java.util.function.BiConsumer<net.minecraft.core.BlockPos, Integer> floor = (centre, radius) -> {
    for (int dx = -radius; dx <= radius; dx++)
        for (int dz = -radius; dz <= radius; dz++)
            level.setBlockAndUpdate(centre.offset(dx, -1, dz),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
};

// The tile's own methods, found once by name so the probe can drive it without naming its class.
java.util.function.Function<Object, Object> call = null;
java.util.List<net.minecraft.core.BlockPos> placed = new java.util.ArrayList<>();

java.util.function.BiFunction<net.minecraft.core.BlockPos, String[], Object> placeSeal =
        (at, symbols) -> {
    clearBox.accept(at, 4);
    floor.accept(at, 4);
    level.setBlockAndUpdate(at, block.apply("thaumadds:seal").defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                    net.minecraft.core.Direction.UP));
    Object seal = level.getBlockEntity(at);
    if (seal == null) return null;
    try {
        var setSymbol = seal.getClass().getMethod("setSymbol", int.class,
                dev.tc4port.thaumcraft.api.aspect.AspectId.class);
        for (int slot = 0; slot < 3; slot++) {
            var aspect = symbols[slot] == null || symbols[slot].isEmpty() ? null
                    : dev.tc4port.thaumcraft.api.aspect.AspectId.parse(symbols[slot]);
            setSymbol.invoke(seal, slot, aspect);
        }
    } catch (Throwable t) {
        sb.append("      setSymbol failed: ").append(t).append("\n");
    }
    return seal;
};
java.util.function.BiConsumer<Object, net.minecraft.core.BlockPos> tickSeal = (seal, at) -> {
    try {
        seal.getClass().getMethod("tick", net.minecraft.world.level.Level.class,
                        net.minecraft.core.BlockPos.class,
                        net.minecraft.world.level.block.state.BlockState.class)
                .invoke(seal, level, at, level.getBlockState(at));
    } catch (Throwable ignored) {
    }
};
java.util.function.Function<Object, Object> instance = seal -> {
    try {
        return seal.getClass().getMethod("instance").invoke(seal);
    } catch (Throwable t) {
        return null;
    }
};

// --- A) every seal combination resolves to its behaviour ---
try {
    var base = new net.minecraft.core.BlockPos(5200, 100, 8);
    String[][] combos = {
        {"aqua", "", ""}, {"terra", "terra", "terra"}, {"victus", "herba", ""},
        {"instrumentum", "herba", ""}, {"instrumentum", "herba", "ordo"},
        {"vacuos", "vacuos", "vacuos"}, {"praecantatio", "aer", "ordo"},
        {"mortuus", "mortuus", ""},
    };
    for (int index = 0; index < combos.length; index++) {
        var at = base.offset(0, 0, index * 3);
        Object seal = placeSeal.apply(at, combos[index]);
        Object behaviour = seal == null ? null : instance.apply(seal);
        sb.append("[A] ").append(java.util.Arrays.toString(combos[index])).append(" -> ")
          .append(behaviour == null ? "NO SEAL" : behaviour.getClass().getSimpleName()).append("\n");
    }
} catch (Throwable t) {
    sb.append("[A] FAILED: ").append(t).append("\n");
}

// --- B) the tilling seal actually tills ---
try {
    var base = new net.minecraft.core.BlockPos(5240, 100, 8);
    Object seal = placeSeal.apply(base, new String[]{"terra", "terra", "terra"});
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
        level.setBlockAndUpdate(base.offset(dx, 2, dz),
                net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
    int dirt = 0;
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
        if (level.getBlockState(base.offset(dx, 2, dz)).is(net.minecraft.world.level.block.Blocks.DIRT)) dirt++;
    for (int i = 0; i < 300; i++) tickSeal.accept(seal, base);
    int farmland = 0;
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
        if (level.getBlockState(base.offset(dx, 2, dz)).is(net.minecraft.world.level.block.Blocks.FARMLAND)) farmland++;
    sb.append("[B] tilling seal: ").append(dirt).append(" dirt -> ").append(farmland)
      .append(" farmland after 300 ticks\n");
} catch (Throwable t) {
    sb.append("[B] FAILED: ").append(t).append("\n");
}

// --- C) the watering seal wets farmland, and the fertilising one grows a crop ---
try {
    var base = new net.minecraft.core.BlockPos(5260, 100, 8);
    Object seal = placeSeal.apply(base, new String[]{"aqua", "", ""});
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
        level.setBlockAndUpdate(base.offset(dx, 2, dz),
                net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState());
    for (int i = 0; i < 100; i++) tickSeal.accept(seal, base);
    int wet = 0;
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
        var state = level.getBlockState(base.offset(dx, 2, dz));
        if (state.is(net.minecraft.world.level.block.Blocks.FARMLAND)
                && state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) == 7) wet++;
    }
    sb.append("[C] watering seal: ").append(wet).append(" of 49 farmland blocks at moisture 7\n");

    var cropBase = new net.minecraft.core.BlockPos(5290, 100, 8);
    Object fertiliser = placeSeal.apply(cropBase, new String[]{"victus", "herba", ""});
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
        level.setBlockAndUpdate(cropBase.offset(dx, 2, dz),
                net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(cropBase.offset(dx, 3, dz),
                net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState());
    }
    for (int i = 0; i < 400; i++) tickSeal.accept(fertiliser, cropBase);
    int grown = 0;
    for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
        var state = level.getBlockState(cropBase.offset(dx, 3, dz));
        if (state.is(net.minecraft.world.level.block.Blocks.WHEAT)
                && state.getValue(net.minecraft.world.level.block.CropBlock.AGE) > 0) grown++;
    }
    sb.append("[C] fertilising seal: ").append(grown).append(" of 49 wheat advanced past age 0\n");
} catch (Throwable t) {
    sb.append("[C] FAILED: ").append(t).append("\n");
}

// --- D) the portal pair carries something across ---
try {
    var a = new net.minecraft.core.BlockPos(5320, 100, 8);
    var b = new net.minecraft.core.BlockPos(5360, 100, 8);
    Object sealA = placeSeal.apply(a, new String[]{"praecantatio", "aer", "ordo"});
    Object sealB = placeSeal.apply(b, new String[]{"praecantatio", "aer", "ordo"});
    for (Object seal : new Object[]{sealA, sealB}) {
        try {
            seal.getClass().getMethod("setOwner", java.util.UUID.class).invoke(seal, player.getUUID());
        } catch (Throwable ignored) {
        }
    }
    for (int i = 0; i < 5; i++) { tickSeal.accept(sealA, a); tickSeal.accept(sealB, b); }
    Object behaviourA = instance.apply(sealA);
    boolean bound = (Boolean) behaviourA.getClass().getMethod("onSealActivated",
            net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class,
            net.minecraft.world.entity.player.Player.class).invoke(behaviourA, level, a, player);
    sb.append("[D] portal: bound=").append(bound).append("\n");

    var cow = net.minecraft.world.entity.EntityType.COW.create(level);
    cow.moveTo(a.getX() + 0.5, a.getY() + 0.1, a.getZ() + 0.5, 0.0F, 0.0F);
    level.addFreshEntity(cow);
    for (int i = 0; i < 5; i++) { tickSeal.accept(sealA, a); tickSeal.accept(sealB, b); }
    try {
        sealA.getClass().getMethod("onEntityInside", net.minecraft.world.level.Level.class,
                        net.minecraft.world.entity.Entity.class)
                .invoke(sealA, level, cow);
    } catch (Throwable t) {
        sb.append("      onEntityInside failed: ").append(t).append("\n");
    }
    double moved = cow.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(a));
    sb.append("    a cow that stood on A moved ").append(String.format("%.1f", moved))
      .append(" blocks (41 means it went to B, 0 means it stayed)\n");
    cow.discard();
} catch (Throwable t) {
    sb.append("[D] portal FAILED: ").append(t).append("\n");
}

// --- E) the entities: the shot flies, the bone turns a wolf, Chester carries ---
try {
    var at = new net.minecraft.core.BlockPos(5400, 100, 8);
    clearBox.accept(at, 6);
    floor.accept(at, 6);

    var payload = new dev.tc4port.thaumcraft.api.aspect.AspectList();
    payload.add(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis"), 5);
    var shot = dev.tc4port.thaumadditions.registry.Content.ESSENTIA_SHOT_FACTORY
            .create(level, player, payload);
    shot.setPos(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
    shot.shootFromRotation(player, 0.0F, 0.0F, 0.0F, 1.5F, 0.0F);
    level.addFreshEntity(shot);
    sb.append("[E] essentia shot spawned, carrying ").append(shot.payload().visSize())
      .append(" points of ignis\n");

    var wolf = net.minecraft.world.entity.EntityType.WOLF.create(level);
    wolf.moveTo(at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
    level.addFreshEntity(wolf);
    boolean turned = dev.tc4port.thaumadditions.entity.BlueWolf.transform(wolf);
    int blueWolves = level.getEntitiesOfClass(dev.tc4port.thaumadditions.entity.BlueWolf.class,
            new net.minecraft.world.phys.AABB(at).inflate(8.0D)).size();
    sb.append("[E] blue bone on an untamed wolf: turned=").append(turned)
      .append("  blue wolves nearby=").append(blueWolves)
      .append("  vanilla wolves nearby=").append(level.getEntitiesOfClass(
              net.minecraft.world.entity.animal.Wolf.class,
              new net.minecraft.world.phys.AABB(at).inflate(8.0D)).size()).append("\n");

    var chester = dev.tc4port.thaumadditions.entity.Chester.summon(level, player,
            net.minecraft.world.phys.Vec3.atBottomCenterOf(at.offset(0, 0, 3)));
    if (chester != null) {
        chester.setItem(0, new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.DIAMOND, 7));
        sb.append("[E] chester summoned: slots=").append(chester.getContainerSize())
          .append("  slot0=").append(chester.getItem(0).getCount())
          .append("  owner=").append(chester.ownedBy(player)).append("\n");
        chester.discard();
    }
    shot.discard();
} catch (Throwable t) {
    sb.append("[E] FAILED: ").append(t).append("\n");
}

return sb.toString();
