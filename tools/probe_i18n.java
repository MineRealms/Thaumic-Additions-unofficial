// Does the game actually show the port's own text, in both languages?
//
// The unit tests prove the two language files carry the same keys; this proves the game reads them
// — which is the difference between a translated file and a translated screen.
StringBuilder out = new StringBuilder();
var minecraft = net.minecraft.client.Minecraft.getInstance();

var keys = java.util.List.of(
        "item.thaumadds.crystal_bag",
        "seal.thaumadds.portal",
        "jei.thaumadds.flux_concentrator",
        "tc.research_page.TAR_CRYSTAL_WATER.1",
        "tc.research_page.TAR_CRYSTAL_LAMP.2",
        "block.thaumadds.crystal_block");

java.util.function.Function<String, String> sample = language -> {
    StringBuilder shown = new StringBuilder();
    for (String key : keys) {
        String text = net.minecraft.network.chat.Component.translatable(key).getString();
        // A key with no translation comes back as the key itself.
        shown.append("\n  ").append(key).append(" = ")
             .append(text.equals(key) ? "MISSING" : text.substring(0, Math.min(48, text.length())));
    }
    return shown.toString();
};

var current = minecraft.getLanguageManager().getSelected();
out.append("language now: ").append(current).append(sample.apply(current));

// Switch to Chinese, if the game knows the locale, and read the same keys again.
var manager = minecraft.getLanguageManager();
boolean chinese = manager.getLanguages().containsKey("zh_cn");
out.append("\nzh_cn known to the game: ").append(chinese).append("\n");

if (chinese && !"zh_cn".equals(current)) {
    manager.setSelected("zh_cn");
    minecraft.options.languageCode = "zh_cn";
    minecraft.options.save();
    // setSelected only swaps the table once the resources reload, which this does synchronously.
    manager.onResourceManagerReload(
            net.minecraft.client.Minecraft.getInstance().getResourceManager());
    out.append("language after switch: ").append(manager.getSelected()).append(sample.apply("zh_cn"));

    // Put it back the way it was found.
    manager.setSelected("en_us");
    minecraft.options.languageCode = "en_us";
    minecraft.options.save();
    manager.onResourceManagerReload(
            net.minecraft.client.Minecraft.getInstance().getResourceManager());
    out.append("\nrestored: ").append(manager.getSelected()).append("\n");
}
return out.toString();
