// S4b verification, part 4: the disperser's cadence, one tick at a time.
//
// Part 2 saw the pack never spent after 625 ticks with the player standing in the box; part 3 saw
// it spend itself in under 40. Both cannot be right, so this walks tick by tick and prints what the
// machine and the player look like after each one.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var player = srv.getPlayerList().getPlayers().get(0);
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var items = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
var ignis = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("ignis");

java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, String> typeOf = be -> "?";

var base = new net.minecraft.core.BlockPos(2060, 100, 8);
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = -2; dy <= 3; dy++)
    level.setBlockAndUpdate(base.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

var state = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:aura_disperser"))
        .defaultBlockState()
        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                net.minecraft.core.Direction.UP);
level.setBlockAndUpdate(base, state);
var be = level.getBlockEntity(base);
var cls = be.getClass();
var tick = cls.getMethod("tick", net.minecraft.world.level.Level.class,
        net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
var timer = cls.getMethod("timer");
var aspects = cls.getMethod("aspects");

var salt = new net.minecraft.world.item.ItemStack(
        items.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:salt_essence")));
dev.tc4port.thaumadditions.items.AspectItems.setAspect(salt, ignis);
cls.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class).invoke(be, 0, salt);

player.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
player.removeAllEffects();

sb.append("before: timer=").append(timer.invoke(be))
  .append(" aspects=").append(aspects.invoke(be) == null ? "null" : "set")
  .append(" player=").append(player.position()).append("\n");

for (int i = 1; i <= 45; i++) {
    tick.invoke(be, level, base, level.getBlockState(base));
    var fx = player.getEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
    boolean interesting = i <= 3 || i % 5 == 0 || fx != null;
    if (interesting) {
        sb.append("  tick ").append(i)
          .append(": timer=").append(timer.invoke(be))
          .append(" aspects=").append(aspects.invoke(be) == null ? "null" : "set")
          .append(" fx=").append(fx == null ? "-" : fx.getDuration())
          .append(" slot0=").append(((net.minecraft.world.Container) be).getItem(0).getCount())
          .append("\n");
    }
}
return sb.toString();
