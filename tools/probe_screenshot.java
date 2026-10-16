// Takes a screenshot of whatever the game is showing, so the effect can be looked at from here.
//
// The bridge runs on the server thread and the screenshot must happen on the render thread, so it
// is scheduled there and the file is left for whoever asked.
StringBuilder out = new StringBuilder();
var minecraft = net.minecraft.client.Minecraft.getInstance();
var name = "thaumadds-" + System.currentTimeMillis();
minecraft.execute(() -> {
    try {
        net.minecraft.client.Screenshot.grab(minecraft.gameDirectory, name,
                minecraft.getMainRenderTarget(), component -> {
                });
    } catch (Throwable failure) {
        dev.tc4port.thaumadditions.ThaumAdditions.LOGGER.error("screenshot failed", failure);
    }
});
out.append("screenshots/").append(name).append(".png\n");
return out.toString();
