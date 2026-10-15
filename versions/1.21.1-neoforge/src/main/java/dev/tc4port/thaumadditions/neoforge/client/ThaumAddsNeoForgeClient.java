package dev.tc4port.thaumadditions.neoforge.client;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.client.render.MachineItemRenderer;
import dev.tc4port.thaumadditions.client.render.ItemRenderers;
import dev.tc4port.thaumadditions.client.render.MachineRenderers;
import dev.tc4port.thaumadditions.registry.Content;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Supplier;

/**
 * The NeoForge node's renderer registration; see the Forge twin for why this is per-node.
 */
@EventBusSubscriber(modid = ThaumAdditions.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ThaumAddsNeoForgeClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.BORE_BE.get(), context -> new MachineRenderers.CrystalBore());
        event.registerBlockEntityRenderer(Content.COMBINER_BE.get(), context -> new MachineRenderers.AspectCombiner());
        event.registerBlockEntityRenderer(Content.CHARGER_BE.get(), context -> new MachineRenderers.AuraCharger());
        event.registerBlockEntityRenderer(Content.CRUSHER_BE.get(), context -> new MachineRenderers.CrystalCrusher());
        event.registerBlockEntityRenderer(Content.CONCENTRATOR_BE.get(), context -> new MachineRenderers.FluxConcentrator());
        event.registerBlockEntityRenderer(Content.ENCHANTER_BE.get(), context -> new ItemRenderers.ShadowEnchanter());
        event.registerBlockEntityRenderer(Content.SUMMONER_BE.get(), context -> new ItemRenderers.MobSummoner());
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(machine(MachineItemRenderer::bore), Content.CRYSTAL_BORE_ITEM.get());
        event.registerItem(machine(MachineItemRenderer::combiner), Content.ASPECT_COMBINER_ITEM.get());
        event.registerItem(machine(MachineItemRenderer::charger), Content.AURA_CHARGER_ITEM.get());
        event.registerItem(machine(MachineItemRenderer::crusher), Content.CRYSTAL_CRUSHER_ITEM.get());
        event.registerItem(machine(MachineItemRenderer::concentrator), Content.FLUX_CONCENTRATOR_ITEM.get());
    }

    /** The five machines draw their own inventory icon; see {@link MachineItemRenderer}. */
    private static IClientItemExtensions machine(Supplier<BlockEntityWithoutLevelRenderer> renderer) {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer.get();
            }
        };
    }
}
