// Is the portal window actually producing a frame?
StringBuilder out = new StringBuilder();
var window = Class.forName("dev.tc4port.thaumadditions.client.render.PortalWindow");
out.append("enabled: ").append(window.getMethod("enabled").invoke(null)).append("\n");
out.append("texture id: ").append(window.getMethod("texture").invoke(null)).append("\n");
out.append("loaded seals: ").append(window.getMethod("loadedSeals").invoke(null)).append("\n");
var config = Class.forName("dev.tc4port.thaumadditions.TaClientConfig");
out.append("config portalWindow: ").append(config.getMethod("portalWindow").invoke(null)).append("\n");
return out.toString();
