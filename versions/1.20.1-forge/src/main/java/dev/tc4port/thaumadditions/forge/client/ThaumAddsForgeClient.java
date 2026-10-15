package dev.tc4port.thaumadditions.forge.client;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.client.render.ItemRenderers;
import dev.tc4port.thaumadditions.client.render.MachineColors;
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
        event.registerBlockEntityRenderer(Content.ENCHANTER_BE.get(), context -> new ItemRenderers.ShadowEnchanter());
        event.registerBlockEntityRenderer(Content.SUMMONER_BE.get(), context -> new ItemRenderers.MobSummoner());
        event.registerEntityRenderer(Content.BLUE_WOLF.get(), BlueWolfRenderer::new);
        event.registerEntityRenderer(Content.CHESTER_ENTITY.get(),
                dev.tc4port.thaumadditions.client.render.ChesterRenderer::new);
    }

    /** The arcane cake's top face takes the colour of whatever was last eaten off it. */
    @SubscribeEvent
    public static void registerBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(MachineColors.CAKE, Content.CAKE.get());
    }

    /**
     * The five machine screens. 1.20.1 Forge has no menu-screen event; the screens go in through
     * {@code MenuScreens.register} on the client-setup thread instead, which is what the sibling
     * ports in this workspace do too.
     */
    @SubscribeEvent
    public static void registerScreens(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(Content.SMELTER_MENU.get(),
                    dev.tc4port.thaumadditions.client.gui.MachineScreens.Smelter::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.CRUSHER_MENU.get(),
                    dev.tc4port.thaumadditions.client.gui.MachineScreens.CrystalCrusher::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.GROWTH_MENU.get(),
                    dev.tc4port.thaumadditions.client.gui.MachineScreens.GrowthChamber::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.DISPERSER_MENU.get(),
                    dev.tc4port.thaumadditions.client.gui.MachineScreens.AuraDisperser::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.ENCHANTER_MENU.get(),
                    dev.tc4port.thaumadditions.client.gui.MachineScreens.ShadowEnchanter::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.CHESTER_MENU.get(),
                    dev.tc4port.thaumadditions.client.gui.ChesterScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(Content.CRYSTAL_BAG_MENU.get(),
                    dev.tc4port.thaumadditions.client.gui.CrystalBagScreen::new);
        });
    }
}
