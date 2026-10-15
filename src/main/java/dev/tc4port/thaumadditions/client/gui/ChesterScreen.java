package dev.tc4port.thaumadditions.client.gui;

import dev.tc4port.thaumadditions.gui.ChesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12 {@code GuiChester}: the chest's four rows of nine.
 *
 * <p>Upstream drew the panel and then a second texture over it holding the slot wells, which is why
 * there are two files; the port does the same two blits.</p>
 */
public class ChesterScreen extends AbstractContainerScreen<ChesterMenu> {
    private static final ResourceLocation BASE =
            ResourceLocation.tryParse("thaumadds:textures/gui/gui_chester_base.png");
    private static final ResourceLocation SLOTS =
            ResourceLocation.tryParse("thaumadds:textures/gui/gui_chester_slots.png");

    public ChesterScreen(ChesterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = ChesterMenu.WIDTH;
        this.imageHeight = ChesterMenu.HEIGHT;
        this.inventoryLabelY = 74;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BASE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(SLOTS, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
