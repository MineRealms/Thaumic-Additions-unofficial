package dev.tc4port.thaumadditions.client.gui;

import dev.tc4port.thaumadditions.gui.PistolMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12 {@code GuiEssentiaPistol}: one slot to feed the pistol with.
 *
 * <p>Upstream's panel points at Thaumcraft's own; the modern one has no equivalent file, so this
 * draws the same flat panel the two machine screens without textures draw.</p>
 */
public class PistolScreen extends AbstractContainerScreen<PistolMenu> {
    public PistolScreen(PistolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PistolMenu.WIDTH;
        this.imageHeight = PistolMenu.HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreens.MachineScreen.flatPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 79, topPos + 29, leftPos + 97, topPos + 47, 0xFF8B8B8B);
        graphics.drawString(font, Component.translatable("gui.thaumadds.pistol.load"),
                leftPos + 8, topPos + 12, 0x404040, false);
    }
}
