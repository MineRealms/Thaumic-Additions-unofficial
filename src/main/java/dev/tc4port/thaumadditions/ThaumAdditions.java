package dev.tc4port.thaumadditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared entry point for the Thaumic Additions: Reconstructed port.
 *
 * <p>Loader-specific bootstrap lives in the node adapters
 * ({@code versions/1.20.1-forge/.../forge} and {@code versions/1.21.1-neoforge/.../neoforge}),
 * which call {@link #init(String, String)} during construction.
 */
public final class ThaumAdditions {
    public static final String MOD_ID = "thaumadds";
    /** The mod's logger; public so a part of the mod that has no better place to put one can use it. */
    public static final Logger LOGGER = LoggerFactory.getLogger("ThaumicAdditions");

    private static boolean initialized;

    private ThaumAdditions() {
    }

    /** Called once from the loader adapter's mod constructor. */
    public static void init(String loader, String minecraftVersion) {
        if (initialized) return;
        initialized = true;
        LOGGER.info("[thaumadds] Bootstrapping Thaumic Additions port ({} {}, stage: project setup)", loader, minecraftVersion);
    }
}
