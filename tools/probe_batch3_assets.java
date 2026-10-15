StringBuilder sb = new StringBuilder();

// --- gear hooks: vis discount + goggles overlay on the adaminite pieces ---
net.minecraft.world.entity.player.Player player = null;
try {
    net.minecraft.server.MinecraftServer srv = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
    if (srv != null) player = srv.getPlayerList().getPlayers().isEmpty() ? null : srv.getPlayerList().getPlayers().get(0);
} catch (Throwable t) {
    sb.append("player lookup failed: ").append(t).append("\n");
}

String[] armor = {"adaminite_hood", "adaminite_robe", "adaminite_belt", "adaminite_boots"};
for (String n : armor) {
    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + n)).orElse(null);
    sb.append(n);
    sb.append(" | VisDiscountGear=").append(item instanceof dev.tc4port.thaumcraft.api.item.VisDiscountGear);
    sb.append(" | GogglesOverlayGear=").append(item instanceof dev.tc4port.thaumcraft.api.item.GogglesOverlayGear);
    if (item instanceof dev.tc4port.thaumcraft.api.item.VisDiscountGear && player != null) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        sb.append(" | discount=").append(((dev.tc4port.thaumcraft.api.item.VisDiscountGear) item)
                .thaumcraftVisDiscount(stack, player, dev.tc4port.thaumcraft.api.aspect.VisChannel.AER));
        sb.append(" | goggles=").append(((dev.tc4port.thaumcraft.api.item.GogglesOverlayGear) item)
                .showsThaumcraftGogglesOverlay(stack, player));
    }
    sb.append("\n");
}

// --- resource resolution ---
net.minecraft.server.packs.resources.ResourceManager rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
String[] paths = {
        "models/item/void_elemental_pickaxe.json", "models/item/void_elemental_axe.json",
        "models/item/void_elemental_shovel.json", "models/item/void_elemental_hoe.json",
        "models/item/adaminite_sword.json", "models/item/adaminite_hood.json",
        "models/item/adaminite_robe.json", "models/item/adaminite_belt.json",
        "models/item/adaminite_boots.json",
        "textures/item/void_elemental_pickaxe.png", "textures/item/adaminite_sword.png",
        "textures/models/armor/adaminite_layer_1.png", "textures/models/armor/adaminite_layer_2.png"};
for (String p : paths) {
    boolean ok = rm.getResource(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + p)).isPresent();
    sb.append(p).append(" = ").append(ok ? "OK" : "MISSING").append("\n");
}
return sb.toString();
