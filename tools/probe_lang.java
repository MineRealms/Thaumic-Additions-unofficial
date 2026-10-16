// Does the game show the words, not the keys?
StringBuilder out = new StringBuilder();
String[][] samples = {
    {"chat.thaumadds.thaumometer.target", "Stone"},
    {"chat.thaumadds.thaumometer.empty", ""},
    {"tooltip.thaumadds.aspect", "Fire"},
    {"tooltip.thaumadds.essentia", "5"},
    {"gui.thaumadds.knowledge_tome.store", "3"},
    {"gui.thaumadds.knowledge_tome.restored", "3"},
    {"tooltip.thaumadds.knowledge_tome.entries", "7"},
    {"item.thaumadds.crystal_bag", ""},
    {"seal.thaumadds.portal", ""},
};
for (String[] sample : samples) {
    var text = sample[1].isEmpty()
            ? net.minecraft.network.chat.Component.translatable(sample[0])
            : net.minecraft.network.chat.Component.translatable(sample[0], sample[1]);
    String shown = text.getString();
    out.append(sample[0]).append(" = ").append(shown)
       .append(shown.equals(sample[0]) ? "   <-- STILL A KEY" : "").append("\n");
}
return out.toString();
