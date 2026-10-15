package dev.tc4port.thaumadditions.forge.client;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.client.render.MachineRenderers;
import dev.tc4port.thaumadditions.registry.Content;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Forge node's renderer registration. The event lives in a different package on NeoForge, so
 * neither node can be reached from the shared source; this class only loads on the client.
 *
 * <p>The five machine inventory icons are not registered here: Forge 1.20.1 has no client
 * extension event, so they hang off the items themselves (see {@code TaPlatformForge.machineItem}).</p>
 */
@Mod.EventBusSubscriber(modid = ThaumAdditions.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ThaumAddsForgeClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.BORE_BE.get(), context -> new MachineRenderers.CrystalBore());
        event.registerBlockEntityRenderer(Content.COMBINER_BE.get(), context -> new MachineRenderers.AspectCombiner());
        event.registerBlockEntityRenderer(Content.CHARGER_BE.get(), context -> new MachineRenderers.AuraCharger());
        event.registerBlockEntityRenderer(Content.CRUSHER_BE.get(), context -> new MachineRenderers.CrystalCrusher());
        event.registerBlockEntityRenderer(Content.CONCENTRATOR_BE.get(), context -> new MachineRenderers.FluxConcentrator());
    }
}
