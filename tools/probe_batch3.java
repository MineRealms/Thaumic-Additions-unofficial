StringBuilder sb = new StringBuilder();
String[] names = {"void_elemental_pickaxe", "void_elemental_axe", "void_elemental_shovel", "void_elemental_hoe",
        "adaminite_sword", "adaminite_hood", "adaminite_robe", "adaminite_belt", "adaminite_boots"};

for (String n : names) {
    net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + n);
    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    if (item == null) {
        sb.append(n).append(" = MISSING\n");
        continue;
    }
    sb.append(n).append(" | ").append(item.getClass().getSimpleName());
    sb.append(" | maxDamage=").append(item.getMaxDamage());
    sb.append(" | ench=").append(item.getEnchantmentValue());
    if (item instanceof net.minecraft.world.item.TieredItem) {
        net.minecraft.world.item.Tier t = ((net.minecraft.world.item.TieredItem) item).getTier();
        sb.append(" | tierUses=").append(t.getUses());
        sb.append(" | tierSpeed=").append(t.getSpeed());
        sb.append(" | tierBonus=").append(t.getAttackDamageBonus());
        sb.append(" | tierLvl=").append(t.getLevel());
    }
    if (item instanceof net.minecraft.world.item.ArmorItem) {
        net.minecraft.world.item.ArmorItem a = (net.minecraft.world.item.ArmorItem) item;
        sb.append(" | slot=").append(a.getType());
        sb.append(" | defense=").append(a.getDefense());
        sb.append(" | toughness=").append(a.getToughness());
    }
    double atk = 0;
    for (net.minecraft.world.entity.ai.attributes.AttributeModifier m : item.getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
            .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))
        atk += m.getAmount();
    sb.append(" | atk=").append(atk);
    sb.append(" | name=").append(item.getDescription().getString());
    sb.append("\n");
}
return sb.toString();
