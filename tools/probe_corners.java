// What the portal window's last frame did, and where it would draw.
StringBuilder out = new StringBuilder();
var window = Class.forName("dev.tc4port.thaumadditions.client.render.PortalWindow");
out.append("enabled: ").append(window.getMethod("enabled").invoke(null)).append("\n");
out.append("texture: ").append(window.getMethod("texture").invoke(null)).append("\n");
out.append("reason: ").append(window.getMethod("lastReason").invoke(null)).append("\n");
var corners = (float[][]) window.getMethod("corners").invoke(null);
if (corners == null) {
    out.append("corners: none\n");
} else {
    for (float[] corner : corners) {
        out.append(String.format("  %.2f %.2f %.2f  uv %.2f %.2f",
                corner[0], corner[1], corner[2], corner[3], corner[4])).append("\n");
    }
}
return out.toString();
