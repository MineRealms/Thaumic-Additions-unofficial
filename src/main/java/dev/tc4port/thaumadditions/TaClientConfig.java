package dev.tc4port.thaumadditions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

/**
 * The port's client settings, in {@code config/thaumadds-client.json}.
 *
 * <p>Written by hand rather than through a loader's config system, because the two loaders have one
 * each and they are not the same one. This file is small, it is read once, and it belongs to
 * neither loader — which is the same reason everything else here that can be shared is.</p>
 *
 * <p>The file is written out with its defaults the first time the game looks at it, so the switch
 * can be found and turned off without anyone having to guess its name.</p>
 */
public final class TaClientConfig {
    /** The one setting so far. */
    public static final String PORTAL_WINDOW = "portalWindow";

    private static final String FILE_NAME = "thaumadds-client.json";
    private static JsonObject values;

    private TaClientConfig() {
    }

    /**
     * Whether a bound portal draws what is on the other side of it.
     *
     * <p>On by default, as it was upstream. Off is the escape hatch: rendering a second view of the
     * world every frame is the single most expensive thing this mod does, and a player who would
     * rather have the frames can say so.</p>
     */
    public static boolean portalWindow() {
        return read().has(PORTAL_WINDOW) && read().get(PORTAL_WINDOW).getAsBoolean();
    }

    /** The settings, loaded on first use and written out if they were not there. */
    private static synchronized JsonObject read() {
        if (values != null) {
            return values;
        }
        Path path = path();
        JsonObject loaded = null;
        if (path != null && Files.isRegularFile(path)) {
            try {
                loaded = JsonParser.parseString(
                        new String(Files.readAllBytes(path), StandardCharsets.UTF_8))
                        .getAsJsonObject();
            } catch (IOException | RuntimeException unreadable) {
                // A file that cannot be read is no worse than no file: the defaults stand, and the
                // next run writes them back.
                loaded = null;
            }
        }
        values = loaded != null ? loaded : defaults();
        if (loaded == null && path != null) {
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, values.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException unwritable) {
                // Nothing to do about it, and nothing depends on it having been written.
            }
        }
        return values;
    }

    private static JsonObject defaults() {
        JsonObject out = new JsonObject();
        out.addProperty(PORTAL_WINDOW, true);
        return out;
    }

    private static Path path() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null || minecraft.gameDirectory == null
                ? null : minecraft.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }
}
