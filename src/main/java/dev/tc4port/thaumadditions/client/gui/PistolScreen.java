package dev.tc4port.thaumadditions.client.gui;

import dev.tc4port.thaumadditions.gui.PistolMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12 {@code GuiEssentiaPistol}: one slot to feed the pistol with.
 *
 * <p>Upstream's own panel, drawn at its own size; the slot coordinates in the menu are upstream's
 * too, so the wells in the texture line up with where the slots actually are.</p>
 */
public class PistolScreen extends AbstractContainerScreen<PistolMenu> {
    private static final net.minecraft.resources.ResourceLocation PANEL =
            net.minecraft.resources.ResourceLocation.tryParse(
                    "thaumadds:textures/gui/essentia_pistol.png");

    public PistolScreen(PistolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PistolMenu.WIDTH;
        this.imageHeight = PistolMenu.HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        graphics.drawString(font, Component.translatable("gui.thaumadds.pistol.load"),
                leftPos + 8, topPos + 12, 0x404040, false);
    }
}
