package dev.tc4port.thaumadditions.forge.client;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.client.render.PortalWindow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Runs the portal window's second pass, once a frame, between the level's own passes.
 *
 * <p>Its own class because it listens on the game bus while the client class listens on the mod
 * bus, and a bus rejects a listener for an event that belongs to the other one.</p>
 *
 * <p>The stage is the last before block entities are drawn: late enough that the world being
 * rendered has finished being built, and early enough that the seal, which draws the result, has
 * something to draw by the time its own renderer runs.</p>
 */
@Mod.EventBusSubscriber(modid = ThaumAdditions.MOD_ID, value = Dist.CLIENT)
public final class PortalWindowStage {
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            PortalWindow.renderFrame(event.getPartialTick());
            // Then the window itself, in the same pass and the same transform the far side was
            // rendered with.
            PortalWindowRenderer.draw();
        }
    }
}
