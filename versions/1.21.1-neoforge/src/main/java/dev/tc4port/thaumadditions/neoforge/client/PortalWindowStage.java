package dev.tc4port.thaumadditions.neoforge.client;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.client.render.PortalWindow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Runs the portal window's second pass, once a frame; see the Forge twin for why it is its own
 * class and why it is this stage.
 */
@EventBusSubscriber(modid = ThaumAdditions.MOD_ID, value = Dist.CLIENT)
public final class PortalWindowStage {
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            // 1.21 hands the stage a delta tracker where 1.20 handed a partial tick.
            PortalWindow.renderFrame(
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
            // Then the window itself, in the same pass and the same transform the far side was
            // rendered with.
            PortalWindowRenderer.draw();
        }
    }
}
