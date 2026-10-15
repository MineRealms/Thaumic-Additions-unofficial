package dev.tc4port.thaumadditions.client.gui;

import dev.tc4port.thaumadditions.gui.CrystalBagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12 {@code GuiCrystalBag}: nine slots showing what the bag is carrying.
 *
 * <p>When the bag is empty the screen says so rather than showing nine blank slots, because the way
 * a bag fills is by walking over crystallised essence and that is not obvious from the panel.</p>
 */
public class CrystalBagScreen extends AbstractContainerScreen<CrystalBagMenu> {
    private static final ResourceLocation PANEL =
            ResourceLocation.tryParse("thaumadds:textures/gui/crystal_bag.png");

    public CrystalBagScreen(CrystalBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = CrystalBagMenu.WIDTH;
        this.imageHeight = CrystalBagMenu.HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The 1.12 texture is a quarter of the screen; the panel is the top-left of it.
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.shown().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.thaumadds.crystal_bag.empty"),
                    leftPos + 62, topPos + 24, 0x404040, false);
        }
    }
}
