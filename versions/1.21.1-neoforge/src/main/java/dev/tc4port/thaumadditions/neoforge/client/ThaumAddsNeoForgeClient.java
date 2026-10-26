package dev.tc4port.thaumadditions.neoforge.client;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.client.render.MachineItemRenderer;
import dev.tc4port.thaumadditions.client.render.ItemRenderers;
import dev.tc4port.thaumadditions.client.render.MachineColors;
import dev.tc4port.thaumadditions.client.render.MachineRenderers;
import dev.tc4port.thaumadditions.client.render.ScytheRenderer;
import dev.tc4port.thaumadditions.registry.Content;
import net.minecraft.client.renderer.entity.NoopRenderer;
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
    /** The portal window's shared half is handed its node-specific half once. */
    @SubscribeEvent
    public static void installPortalWindow(
            net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        dev.tc4port.thaumadditions.client.render.PortalWindow.install(
                new PortalWindowRenderer());
        dev.tc4port.thaumadditions.client.render.ClientDraw.Holder.install(new ClientDrawImpl());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.BORE_BE.get(), context -> new MachineRenderers.CrystalBore());
        event.registerBlockEntityRenderer(Content.COMBINER_BE.get(), context -> new MachineRenderers.AspectCombiner());
        event.registerBlockEntityRenderer(Content.CHARGER_BE.get(), context -> new MachineRenderers.AuraCharger());
        event.registerBlockEntityRenderer(Content.CRUSHER_BE.get(), context -> new MachineRenderers.CrystalCrusher());
        event.registerBlockEntityRenderer(Content.CONCENTRATOR_BE.get(), context -> new MachineRenderers.FluxConcentrator());
        event.registerBlockEntityRenderer(Content.ENCHANTER_BE.get(), context -> new ItemRenderers.ShadowEnchanter());
        event.registerBlockEntityRenderer(Content.SUMMONER_BE.get(),
                context -> new ItemRenderers.MobSummoner(new TesseractLines()));
        event.registerEntityRenderer(Content.BLUE_WOLF.get(), BlueWolfRenderer::new);
        event.registerEntityRenderer(Content.CHESTER_ENTITY.get(),
                dev.tc4port.thaumadditions.client.render.ChesterRenderer::new);
        // Both of these were registered without a renderer, and an entity type with no renderer
        // crashes the client the moment one exists: EntityRenderDispatcher#shouldRender
        // dereferences the null it looked up. The shot drew nothing in 1.12 either — its trail of
        // aspect dust was the whole visual — so vanilla's no-op renderer is what it wants.
        event.registerEntityRenderer(Content.ESSENTIA_SHOT.get(), NoopRenderer::new);
        event.registerEntityRenderer(Content.MITHMINITE_SCYTHE_ENTITY.get(),
                context -> new ScytheRenderer(context, Content.MITHMINITE_SCYTHE.get()));
        event.registerBlockEntityRenderer(Content.JAR_BE.get(),
                dev.tc4port.thaumadditions.client.render.JarRenderer::new);
        event.registerBlockEntityRenderer(Content.SEAL_BE.get(),
                dev.tc4port.thaumadditions.neoforge.client.SealRenderer::new);
    }

    /** The arcane cake's top face takes the colour of whatever was last eaten off it. */
    @SubscribeEvent
    public static void registerBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(MachineColors.CAKE, Content.CAKE.get());
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

    /** The five machine screens; 1.21.1 spells the same event over here. */
    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(Content.SMELTER_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.MachineScreens.Smelter::new);
        event.register(Content.CRUSHER_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.MachineScreens.CrystalCrusher::new);
        event.register(Content.GROWTH_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.MachineScreens.GrowthChamber::new);
        event.register(Content.DISPERSER_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.MachineScreens.AuraDisperser::new);
        event.register(Content.ENCHANTER_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.MachineScreens.ShadowEnchanter::new);
        event.register(Content.CHESTER_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.ChesterScreen::new);
        event.register(Content.CRYSTAL_BAG_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.CrystalBagScreen::new);
        event.register(Content.PISTOL_MENU.get(),
                dev.tc4port.thaumadditions.client.gui.PistolScreen::new);
    }
}
