// S4 verification: does the smelter burn fuel, cook the input down to its aspects, and bank them?
//
// The ticker is driven by hand so the whole check runs in one call on the server thread. The
// smelter only cooks while its ENABLED block state is set, which is how 1.12 gated it too.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var blocks = srv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);

var block = blocks.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:mithrillium_smelter"));
if (block == null) return "no smelter block";
var base = srv.getPlayerList().getPlayers().get(0).blockPosition().above(30).offset(10, 0, 0);
var state = block.defaultBlockState();
if (state.hasProperty(dev.tc4port.thaumadditions.blocks.TaBlocks.EnableableBlock.ENABLED)) {
    state = state.setValue(dev.tc4port.thaumadditions.blocks.TaBlocks.EnableableBlock.ENABLED, true);
}
level.setBlockAndUpdate(base, state);

net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(base);
if (be == null) return "no block entity";
if (!(be instanceof net.minecraft.world.Container container)) return "block entity is not a Container";

// Pick an input that actually yields aspects, and a fuel that burns.
net.minecraft.world.item.ItemStack input = net.minecraft.world.item.ItemStack.EMPTY;
for (String id : new String[]{"minecraft:cobblestone", "minecraft:stick", "minecraft:oak_log",
        "minecraft:iron_ingot", "minecraft:redstone", "minecraft:coal"}) {
    var stack = new net.minecraft.world.item.ItemStack(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.tryParse(id)));
    if (stack.isEmpty()) continue;
    var aspects = dev.tc4port.thaumcraft.api.aspect.AspectQueryApi.item(stack);
    if (!aspects.amounts().isEmpty()) {
        input = stack.copyWithCount(8);
        sb.append("input: ").append(id).append(" -> ").append(aspects.amounts()).append("\n");
        break;
    }
}
if (input.isEmpty()) {
    sb.append("no probe item in the registry yields aspects; cannot test the melt\n");
} else {
    container.setItem(0, input);
}
container.setItem(1, new net.minecraft.world.item.ItemStack(
        net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.tryParse("minecraft:coal")), 8));
sb.append("fuel slot: ").append(container.getItem(1)).append("\n");

if (!(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb)) return "not an EntityBlock";
@SuppressWarnings({"unchecked", "rawtypes"})
var ticker = eb.getTicker(level, state, (net.minecraft.world.level.block.entity.BlockEntityType) be.getType());
if (ticker == null) return "no ticker";

var cls = be.getClass();
var getAmount = cls.getMethod("getAmount");
var burnTime = cls.getMethod("burnTime");
var cookTime = cls.getMethod("cookTime");
var smeltTime = cls.getMethod("smeltTime");

for (int i = 1; i <= 400; i++) {
    try {
        ticker.tick(level, base, state, be);
    } catch (Throwable t) {
        var c = t;
        while (c.getCause() != null) c = c.getCause();
        sb.append("tick ").append(i).append(" THREW ").append(c).append("\n");
        var st = c.getStackTrace();
        for (int k = 0; k < Math.min(5, st.length); k++) sb.append("      at ").append(st[k]).append("\n");
        break;
    }
    if (i % 50 == 0) {
        sb.append(String.format("tick %3d | burn=%3s cook=%3s/%s | stored=%s | input=%s fuel=%s%n",
                i, burnTime.invoke(be), cookTime.invoke(be), smeltTime.invoke(be), getAmount.invoke(be),
                container.getItem(0).getCount(), container.getItem(1).getCount()));
    }
}
sb.append("final stored essentia: ").append(getAmount.invoke(be)).append("\n");
sb.append("input left: ").append(container.getItem(0)).append(" fuel left: ").append(container.getItem(1)).append("\n");

level.removeBlock(base, false);
return sb.toString();
