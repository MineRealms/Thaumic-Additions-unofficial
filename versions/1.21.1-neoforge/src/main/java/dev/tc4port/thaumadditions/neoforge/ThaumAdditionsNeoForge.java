package dev.tc4port.thaumadditions.neoforge;

import dev.tc4port.thaumadditions.ThaumAdditions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ThaumAdditions.MOD_ID)
public final class ThaumAdditionsNeoForge {
    public ThaumAdditionsNeoForge(IEventBus modBus) {
        modBus.addListener(this::commonSetup);
        ThaumAdditions.init("neoforge", "1.21.1");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }
}
