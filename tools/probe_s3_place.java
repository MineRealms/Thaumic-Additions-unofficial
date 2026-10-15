// S3 verification, part 2: place every thaumadds block, confirm a block entity attaches exactly
// where a BE type claims the block, and confirm that each BE's persistent data reaches the tag its
// save method writes. Blocks go high above the player so nothing in the dev world is disturbed.
StringBuilder sb = new StringBuilder();
var mc = net.minecraft.client.Minecraft.getInstance();
var srv = mc.getSingleplayerServer();
if (srv == null) return "NO SERVER";
var level = srv.overworld();
var ra = srv.registryAccess();
var blocks = ra.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
var bet = ra.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE);

java.util.List<String> names = new java.util.ArrayList<>();
for (var e : blocks.entrySet())
    if (e.getKey().location().getNamespace().equals("thaumadds")) names.add(e.getKey().location().getPath());
java.util.Collections.sort(names);

var anchor = srv.getPlayerList().getPlayers().get(0).blockPosition().above(40);
int i = 0, beOk = 0, beMissing = 0, beUnexpected = 0, saveBad = 0;
java.util.List<String> problems = new java.util.ArrayList<>();

for (String n : names) {
    var id = net.minecraft.resources.ResourceLocation.tryParse("thaumadds:" + n);
    var blk = blocks.get(id);
    if (blk == null) { problems.add(n + " NOT IN REGISTRY"); continue; }
    var state = blk.defaultBlockState();
    var pos = anchor.offset(i % 12, 0, i / 12);
    i++;

    boolean wantsBe = false;
    for (var t : bet) if (t.isValid(state)) { wantsBe = true; break; }

    try {
        level.setBlockAndUpdate(pos, state);
        var be = level.getBlockEntity(pos);
        if (wantsBe && be == null) { beMissing++; problems.add(n + " a BE type claims it but none attached"); }
        else if (!wantsBe && be != null) { beUnexpected++; problems.add(n + " attached an unexpected BE " + be.getClass().getSimpleName()); }
        else if (be != null) {
            beOk++;
            // The BE's own save path must carry its persistent data into the written tag.
            be.getPersistentData().putString("__s3probe", n);
            var saved = be.saveWithoutMetadata();
            // Persistent data lands in a loader-named sub-tag (ForgeData on 1.20.1, NeoForgeData on
            // 1.21.1), so look for the marker there rather than at the root.
            String marker = null;
            for (String key : saved.getAllKeys())
                if (saved.getCompound(key).getString("__s3probe").length() > 0)
                    marker = saved.getCompound(key).getString("__s3probe");
            if (saved == null || !n.equals(marker)) {
                saveBad++;
                problems.add(n + " persistent data did not reach saveWithoutMetadata()");
            }
        }
        level.removeBlock(pos, false);
    } catch (Throwable t) {
        problems.add(n + " THREW " + t.getClass().getSimpleName() + ": " + t.getMessage());
    }
}

sb.append("blocks placed ").append(names.size()).append("\n");
sb.append("BEs attached where expected: ").append(beOk).append("\n");
sb.append("BE claimed but missing: ").append(beMissing).append("\n");
sb.append("BE attached unexpectedly: ").append(beUnexpected).append("\n");
sb.append("save-path problems: ").append(saveBad).append("\n");
sb.append("PROBLEMS(").append(problems.size()).append("):\n");
for (String p : problems) sb.append("   ").append(p).append("\n");
return sb.toString();
