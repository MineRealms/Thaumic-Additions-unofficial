StringBuilder sb = new StringBuilder();
String[] names = {"mithminite_hood", "mithminite_robe", "mithminite_belt", "mithminite_boots",
        "blue_wolf_mask", "blue_wolf_body", "blue_wolf_legs", "blue_wolf_feetpaws"};

for (String n : names) {
    net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + n);
    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    if (item == null) {
        sb.append(n).append(" = MISSING\n");
        continue;
    }
    net.minecraft.world.item.ArmorItem a = (net.minecraft.world.item.ArmorItem) item;
    sb.append(n).append(" | slot=").append(a.getType());
    sb.append(" | maxDamage=").append(a.getMaxDamage());
    sb.append(" | defense=").append(a.getDefense());
    sb.append(" | toughness=").append(a.getToughness());
    sb.append(" | ench=").append(a.getEnchantmentValue());
    sb.append(" | set=").append(((dev.tc4port.thaumadditions.platform.TaArmorSetMember) a).armorSetId());
    sb.append(" | name=").append(item.getDescription().getString());
    sb.append("\n");
}

// --- gear hooks ---
net.minecraft.server.MinecraftServer srv = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
net.minecraft.world.entity.player.Player p = srv.getPlayerList().getPlayers().get(0);
for (String n : names) {
    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + n)).get();
    net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
    sb.append(n);
    sb.append(" | discount=").append(((dev.tc4port.thaumcraft.api.item.VisDiscountGear) item)
            .thaumcraftVisDiscount(stack, p, dev.tc4port.thaumcraft.api.aspect.VisChannel.AER));
    sb.append(" | goggles=").append(((dev.tc4port.thaumcraft.api.item.GogglesOverlayGear) item)
            .showsThaumcraftGogglesOverlay(stack, p));
    sb.append("\n");
}

// --- resources ---
net.minecraft.server.packs.resources.ResourceManager rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
String[] paths = {"models/item/mithminite_hood.json", "models/item/mithminite_robe.json",
        "models/item/mithminite_belt.json", "models/item/mithminite_boots.json",
        "models/item/blue_wolf_mask.json", "models/item/blue_wolf_body.json",
        "models/item/blue_wolf_legs.json", "models/item/blue_wolf_feetpaws.json",
        "textures/item/mithminite_hood.png", "textures/item/blue_wolf_mask.png",
        "textures/models/armor/mithminite_layer_1.png", "textures/models/armor/mithminite_layer_2.png",
        "textures/models/armor/invisible_layer_1.png", "textures/models/armor/invisible_layer_2.png"};
for (String path : paths)
    sb.append(path).append(" = ").append(rm.getResource(net.minecraft.resources.ResourceLocation.tryParse(
            (path.contains("invisible") ? "thaumcraft:" : "thaumadds:") + path)).isPresent() ? "OK" : "MISSING").append("\n");
return sb.toString();
