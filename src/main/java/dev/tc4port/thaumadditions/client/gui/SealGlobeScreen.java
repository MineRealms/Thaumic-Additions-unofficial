package dev.tc4port.thaumadditions.client.gui;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.seals.SealManager;
import dev.tc4port.thaumadditions.tiles.TileSeal;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12 {@code GuiSealGlobe}: what the seal's three symbols add up to.
 *
 * <p>Upstream opened this from a packet, because 1.12's screen wanted the tile entity itself. The
 * port opens it straight from the block's right-click instead: the seal's symbols are already
 * pushed to the client with the block entity, so the screen needs no container, no menu, and
 * nothing for the server to send.</p>
 */
public class SealGlobeScreen extends Screen {
    private static final ResourceLocation PANEL =
            ResourceLocation.tryParse("thaumadds:textures/gui/seal_globe.png");
    private static final int WIDTH = 192;
    private static final int HEIGHT = 228;

    private final TileSeal seal;

    public SealGlobeScreen(TileSeal seal) {
        super(Component.translatable("gui.thaumadds.seal_globe"));
        this.seal = seal;
    }

    /**
     * Opens the globe over a seal block, if the client has one there.
     *
     * <p>Called only from the client half of the right-click handler, so a dedicated server never
     * loads this class.</p>
     */
    public static void tryOpen(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileSeal seal) {
            Minecraft.getInstance().setScreen(new SealGlobeScreen(seal));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // A hand-drawn dim rather than renderBackground, whose argument list changed between
        // the two versions this port targets.
        graphics.fill(0, 0, width, height, 0xC0101010);
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        graphics.blit(PANEL, x, y, 0, 0, WIDTH, HEIGHT, 256, 256);

        for (int slot = 0; slot < TileSeal.SLOTS; slot++) {
            AspectId symbol = seal.symbol(slot);
            int colour = symbol == null ? 0x808080 : 0xFF000000 | TaAspect.color(symbol);
            graphics.fill(x + 88, y + 30 + slot * 22, x + 104, y + 46 + slot * 22, colour);
            String name = symbol == null
                    ? Component.translatable("gui.thaumadds.seal_globe.empty").getString()
                    : TaAspect.displayName(symbol);
            graphics.drawString(font, name, x + 110, y + 38 + slot * 22, 0x404040, false);
        }

        String description = SealManager.descriptionKey(seal);
        Component line = description == null
                ? Component.translatable("gui.thaumadds.seal_globe.unknown")
                : Component.translatable(description);
        int lineY = y + 116;
        for (var wrapped : font.split(line, WIDTH - 24)) {
            graphics.drawString(font, wrapped, x + 12, lineY, 0x303030, false);
            lineY += 10;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
