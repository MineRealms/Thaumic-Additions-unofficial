package dev.tc4port.thaumadditions.client.gui;

import dev.tc4port.thaumadditions.gui.MachineMenus;
import dev.tc4port.thaumadditions.tiles.TileAuraDisperser;
import dev.tc4port.thaumadditions.tiles.TileCrystalCrusher;
import dev.tc4port.thaumadditions.tiles.TileGrowthChamber;
import dev.tc4port.thaumadditions.tiles.TileShadowEnchanter;
import dev.tc4port.thaumadditions.tiles.TileSmelter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The five machine screens, each drawing the background its 1.12 GUI drew and the gauges over it.
 *
 * <p>Every gauge is read straight off the machine: the port pushes the whole block entity to the
 * client, so a screen needs no synchronised fields of its own — which is the one place this differs
 * from 1.12, where each container carried its own copy of the values.</p>
 *
 * <p>Two of the five have no texture to draw: 1.12's smelter GUI pointed at a Thaumcraft file that
 * the modern Thaumcraft does not ship, and the crusher's GUI never had one at all. Those two draw a
 * flat panel instead, which is honest about being a stand-in and keeps them readable.</p>
 */
public final class MachineScreens {
    private MachineScreens() {
    }

    /** The pixel size every 1.12 machine GUI used, and the one the portable screens keep. */
    static final int WIDTH = 176;
    static final int HEIGHT = 166;

    private static ResourceLocation texture(String path) {
        return ResourceLocation.tryParse(path);
    }

    /** Shared drawing: the panel, the player-slot guides, and a bottom-up fill bar. */
    public abstract static class MachineScreen<T extends dev.tc4port.thaumadditions.gui.MachineMenu>
            extends AbstractContainerScreen<T> {
        private final ResourceLocation panel;

        protected MachineScreen(T menu, Inventory player, Component title, ResourceLocation panel,
                                int width, int height) {
            super(menu, player, title);
            this.panel = panel;
            this.imageWidth = width;
            this.imageHeight = height;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            int x = leftPos;
            int y = topPos;
            if (panel != null) {
                graphics.blit(panel, x, y, 0, 0, imageWidth, imageHeight);
            } else {
                flatPanel(graphics, x, y, imageWidth, imageHeight);
            }
        }

        /** A panel drawn from rectangles, for the GUIs that have no texture to blit. */
        static void flatPanel(GuiGraphics graphics, int x, int y, int width, int height) {
            graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);
            graphics.fill(x, y, x + width, y + 1, 0xFF000000);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFF000000);
            graphics.fill(x, y, x + 1, y + height, 0xFF000000);
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFF000000);
            // Slot wells, matching every machine's inventory at (8, y) plus the hotbar.
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    graphics.fill(x + 7 + column * 18, y + 83 + row * 18,
                            x + 25 + column * 18, y + 101 + row * 18, 0xFF8B8B8B);
                }
            }
            for (int column = 0; column < 9; column++) {
                graphics.fill(x + 7 + column * 18, y + 141, x + 25 + column * 18, y + 159, 0xFF8B8B8B);
            }
        }

        /**
         * A gauge that fills from the bottom, as every one of the 1.12 machines' did.
         *
         * @param fill {@code 0..1}
         */
        static void fillBar(GuiGraphics graphics, int x, int y, int width, int height, float fill,
                            int filledColour, int emptyColour) {
            int filled = Math.round(Math.max(0.0F, Math.min(1.0F, fill)) * height);
            graphics.fill(x, y, x + width, y + height, emptyColour);
            graphics.fill(x, y + height - filled, x + width, y + height, filledColour);
        }

        /** The machine behind this screen, or {@code null} before the client has loaded it. */
        protected BlockEntity machine() {
            return menu.machine();
        }
    }

    /** 1.12 {@code GuiAbstractSmelter}: a flame for the fuel and an arrow for the cook. */
    public static final class Smelter extends MachineScreen<MachineMenus.Smelter> {
        public Smelter(MachineMenus.Smelter menu, Inventory player, Component title) {
            super(menu, player, title, null, WIDTH, HEIGHT);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            super.renderBg(graphics, partialTick, mouseX, mouseY);
            if (!(machine() instanceof TileSmelter smelter)) {
                return;
            }
            // 1.12 drew the flame at 80,26 and the cook arrow at 106,13.
            float burn = smelter.currentBurnTime() <= 0 ? 0.0F
                    : (float) smelter.burnTime() / smelter.currentBurnTime();
            fillBar(graphics, leftPos + 80, topPos + 26, 14, 20, burn, 0xFFFFAA00, 0xFF6E6E6E);
            float cook = smelter.smeltTime() <= 0 ? 0.0F
                    : (float) smelter.cookTime() / smelter.smeltTime();
            graphics.fill(leftPos + 106, topPos + 13, leftPos + 115, topPos + 59, 0xFF6E6E6E);
            graphics.fill(leftPos + 106, topPos + 13, leftPos + 106 + Math.round(9 * cook * 0.5F),
                    topPos + 59, 0xFF3AA0FF);
        }
    }

    /** 1.12 {@code GuiCrystalCrusher}: one bar for the crush in progress. */
    public static final class CrystalCrusher extends MachineScreen<MachineMenus.CrystalCrusher> {
        public CrystalCrusher(MachineMenus.CrystalCrusher menu, Inventory player, Component title) {
            super(menu, player, title, null, WIDTH, HEIGHT);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            super.renderBg(graphics, partialTick, mouseX, mouseY);
            if (machine() instanceof TileCrystalCrusher crusher) {
                // The 1.12 arrow between the two slots, driven by the hundred-tick crush.
                float progress = crusher.craftTime() / 100.0F;
                graphics.fill(leftPos + 72, topPos + 34, leftPos + 92, topPos + 42, 0xFF6E6E6E);
                graphics.fill(leftPos + 72, topPos + 34, leftPos + 72 + Math.round(20 * progress),
                        topPos + 42, 0xFF55D0FF);
            }
        }
    }

    /** 1.12 {@code GuiGrowthChamber}: a column that fills as the vis is spent. */
    public static final class GrowthChamber extends MachineScreen<MachineMenus.GrowthChamber> {
        private static final ResourceLocation PANEL =
                texture("thaumadds:textures/gui/growth_chamber.png");

        public GrowthChamber(MachineMenus.GrowthChamber menu, Inventory player, Component title) {
            super(menu, player, title, PANEL, WIDTH, HEIGHT);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            super.renderBg(graphics, partialTick, mouseX, mouseY);
            if (machine() instanceof TileGrowthChamber chamber) {
                // 1.12 filled the column at 47,13 from the bottom over fifty pixels.
                fillBar(graphics, leftPos + 47, topPos + 13, 8, 50, chamber.growProgress(),
                        0xFF8A2BE2, 0x00000000);
            }
        }
    }

    /** 1.12 {@code GuiAuraDisperser}: the same column, driven by the pack's charge. */
    public static final class AuraDisperser extends MachineScreen<MachineMenus.AuraDisperser> {
        private static final ResourceLocation PANEL =
                texture("thaumadds:textures/gui/aura_disperser.png");
        /** 1.12 {@code TileAuraDisperser.secondsPerSalt}, and so the tallest the column gets. */
        private static final float FULL_SECONDS = 20.0F;

        public AuraDisperser(MachineMenus.AuraDisperser menu, Inventory player, Component title) {
            super(menu, player, title, PANEL, WIDTH, HEIGHT);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            super.renderBg(graphics, partialTick, mouseX, mouseY);
            if (machine() instanceof TileAuraDisperser disperser) {
                fillBar(graphics, leftPos + 38, topPos + 13, 8, 50,
                        disperser.timer() / FULL_SECONDS, 0xFFFFE066, 0x00000000);
            }
        }
    }

    /** 1.12 {@code GuiShadowEnchanter}: a column per outstanding aspect, and the recipe list. */
    public static final class ShadowEnchanter extends MachineScreen<MachineMenus.ShadowEnchanter> {
        private static final ResourceLocation PANEL =
                texture("thaumadds:textures/gui/shadow_enchanter.png");

        public ShadowEnchanter(MachineMenus.ShadowEnchanter menu, Inventory player, Component title) {
            super(menu, player, title, PANEL, 176, 186);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            super.renderBg(graphics, partialTick, mouseX, mouseY);
            if (!(machine() instanceof TileShadowEnchanter enchanter)) {
                return;
            }
            var pending = enchanter.pending();
            if (pending == null) {
                return;
            }
            // 1.12 drew one eight-pixel column per outstanding aspect at x = 3.
            var aspects = pending.aspectsSortedByAmount();
            int shown = Math.min(aspects.size(), 6);
            for (int index = 0; index < shown; index++) {
                var aspect = aspects.get(index);
                float fill = Math.min(1.0F, pending.amount(aspect) / 32.0F);
                int colour = 0xFF000000 | dev.tc4port.thaumadditions.aspect.TaAspect.color(aspect);
                fillBar(graphics, leftPos + 3, topPos + 6 + index * 10, 8, 8, fill, colour, 0x00000000);
            }
        }
    }
}
