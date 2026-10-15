package dev.tc4port.thaumadditions.gui;

import dev.tc4port.thaumadditions.items.ThaumcraftItems;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumadditions.tiles.TileCrystalCrusher;
import dev.tc4port.thaumadditions.tiles.TileGrowthChamber;
import dev.tc4port.thaumadditions.tiles.TileShadowEnchanter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The five machine menus, each laid out where its 1.12 container put it.
 *
 * <p>Slot positions are the upstream pixel positions, and the screens draw their backgrounds to
 * match them. A machine's slots come first so {@link MachineMenu#quickMoveStack} can move between
 * the two halves without knowing which machine it is.</p>
 */
public final class MachineMenus {
    private MachineMenus() {
    }

    /** 1.12 {@code ContainerAbstractSmelter}: what to burn, and what to burn it with. */
    public static final class Smelter extends MachineMenu {
        public Smelter(int id, Inventory player, FriendlyByteBuf data) {
            super(Content.SMELTER_MENU.get(), id, player, data, 2);
        }

        /** The same menu, opened by the server, which already knows where the machine is. */
        public Smelter(int id, Inventory player, net.minecraft.core.BlockPos pos) {
            super(Content.SMELTER_MENU.get(), id, player, pos, 2);
        }

        @Override
        protected int playerSlotsY() {
            return 84;
        }

        @Override
        protected void addMachineSlots(Container container) {
            addSlot(new Slot(container, 0, 80, 8));
            addSlot(new Slot(container, 1, 80, 48));
        }
    }

    /** 1.12 {@code ContainerCrystalCrusher}: a crystallised essence in, a salt essence out. */
    public static final class CrystalCrusher extends MachineMenu {
        public CrystalCrusher(int id, Inventory player, FriendlyByteBuf data) {
            super(Content.CRUSHER_MENU.get(), id, player, data, 2);
        }

        /** The same menu, opened by the server, which already knows where the machine is. */
        public CrystalCrusher(int id, Inventory player, net.minecraft.core.BlockPos pos) {
            super(Content.CRUSHER_MENU.get(), id, player, pos, 2);
        }

        @Override
        protected int playerSlotsY() {
            return 82;
        }

        @Override
        protected void addMachineSlots(Container container) {
            addSlot(new Slot(container, TileCrystalCrusher.SLOT_INPUT, 44, 32) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ThaumcraftItems.crystalEssence());
                }
            });
            addSlot(new OutputSlot(container, TileCrystalCrusher.SLOT_OUTPUT, 116, 32));
        }
    }

    /** 1.12 {@code ContainerGrowthChamber}: a crystal block in, grown essence out. */
    public static final class GrowthChamber extends MachineMenu {
        public GrowthChamber(int id, Inventory player, FriendlyByteBuf data) {
            super(Content.GROWTH_MENU.get(), id, player, data, 2);
        }

        /** The same menu, opened by the server, which already knows where the machine is. */
        public GrowthChamber(int id, Inventory player, net.minecraft.core.BlockPos pos) {
            super(Content.GROWTH_MENU.get(), id, player, pos, 2);
        }

        @Override
        protected int playerSlotsY() {
            return 84;
        }

        @Override
        protected void addMachineSlots(Container container) {
            addSlot(new Slot(container, TileGrowthChamber.SLOT_CRYSTAL, 21, 30) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Content.CRYSTAL_BLOCK.get().asItem());
                }
            });
            addSlot(new OutputSlot(container, TileGrowthChamber.SLOT_OUTPUT, 131, 26));
        }
    }

    /** 1.12 {@code ContainerAuraDisperser}: nine bags of salt, three by three. */
    public static final class AuraDisperser extends MachineMenu {
        public static final int SLOTS = 9;

        public AuraDisperser(int id, Inventory player, FriendlyByteBuf data) {
            super(Content.DISPERSER_MENU.get(), id, player, data, SLOTS);
        }

        /** The same menu, opened by the server, which already knows where the machine is. */
        public AuraDisperser(int id, Inventory player, net.minecraft.core.BlockPos pos) {
            super(Content.DISPERSER_MENU.get(), id, player, pos, SLOTS);
        }

        @Override
        protected int playerSlotsY() {
            return 84;
        }

        @Override
        protected void addMachineSlots(Container container) {
            for (int index = 0; index < SLOTS; index++) {
                addSlot(new Slot(container, index, 62 + (index % 3) * 18, 12 + (index / 3) * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(Content.SALT_ESSENCE.get());
                    }
                });
            }
        }
    }

    /** 1.12 {@code ContainerShadowEnchanter}: the item to write on, and where the result lands. */
    public static final class ShadowEnchanter extends MachineMenu {
        public ShadowEnchanter(int id, Inventory player, FriendlyByteBuf data) {
            super(Content.ENCHANTER_MENU.get(), id, player, data, 2);
        }

        /** The same menu, opened by the server, which already knows where the machine is. */
        public ShadowEnchanter(int id, Inventory player, net.minecraft.core.BlockPos pos) {
            super(Content.ENCHANTER_MENU.get(), id, player, pos, 2);
        }

        @Override
        protected int playerSlotsY() {
            return 104;
        }

        @Override
        protected void addMachineSlots(Container container) {
            addSlot(new Slot(container, TileShadowEnchanter.SLOT_INPUT, 8, 17) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.isEnchantable();
                }
            });
            addSlot(new OutputSlot(container, TileShadowEnchanter.SLOT_OUTPUT, 152, 17));
        }
    }

    /** A slot the player may take from but not put into, which is what every output slot is. */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
