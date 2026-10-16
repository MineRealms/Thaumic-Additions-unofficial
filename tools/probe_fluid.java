// Is crystal water actually there — fluid, type, block, bucket, recipe, tag?
StringBuilder out = new StringBuilder();
var reg = net.minecraft.core.registries.BuiltInRegistries.FLUID;

var water = reg.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crystal_water"));
var flowing = reg.get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crystal_water_flowing"));
out.append("source=").append(water).append(" flowing=").append(flowing).append("\n");

if (water instanceof net.minecraft.world.level.material.FlowingFluid ff) {
    var type = ff.getFluidType();
    out.append("type=").append(type).append(" desc=").append(type.getDescriptionId()).append("\n");
    out.append("bucket=").append(ff.getBucket()).append(" density=").append(type.getDensity())
       .append(" viscosity=").append(type.getViscosity()).append("\n");
}

var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
        .get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crystal_water"));
out.append("block=").append(block).append(" class=").append(block.getClass().getName()).append("\n");

var bucket = net.minecraft.core.registries.BuiltInRegistries.ITEM
        .get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crystal_water_bucket"));
out.append("bucketItem=").append(bucket).append("\n");

var seal = net.minecraft.core.registries.BuiltInRegistries.ITEM
        .get(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:seal"));
out.append("sealItem=").append(seal).append("\n");

// What a source block of the fluid turns into, which is the block the world will hold.
if (block != null && water instanceof net.minecraft.world.level.material.FlowingFluid ff) {
    var state = ff.getSource(false);
    out.append("sourceFluidState=").append(state).append(" legacy=").append(state.createLegacyBlock()).append("\n");
    out.append("spreadState=").append(ff.getFlowing(1, false).createLegacyBlock()).append("\n");
}

// The crucible recipe, on both sides' recipe manager.
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
if (server != null) {
    var recipe = server.getRecipeManager()
            .byKey(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crucible/crystal_water"));
    out.append("crucible(server)=").append(recipe.isPresent()).append("\n");
    out.append("arcaneLoaded=").append(server.getRecipeManager()
            .byKey(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:arcane/seal")).isPresent())
       .append("\n");
}
var client = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
if (client != null) {
    out.append("crucible(client)=").append(client.getRecipeManager()
            .byKey(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:crucible/crystal_water")).isPresent())
       .append("\n");
}
return out.toString();
