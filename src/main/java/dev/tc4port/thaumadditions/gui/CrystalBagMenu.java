package dev.tc4port.thaumadditions.gui;

import dev.tc4port.thaumadditions.items.CrystalBagStore;
import dev.tc4port.thaumadditions.items.ThaumcraftItems;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The crystal bag's nine slots.
 *
 * <p>The slots are not real: a bag holds aspects, not stacks, so each slot stands for one aspect
 * the bag is carrying and shows a crystallised essence of it. Taking from a slot takes that many
 * out of the bag; putting into one is refused, because the bag fills itself by absorbing what its
 * owner walks over.</p>
 *
 * <p>1.12's bag worked the same way — its container built the same virtual stacks — and this is the
 * half the port was missing, which had made the bag a one-way trip.</p>
 */
public class CrystalBagMenu extends AbstractContainerMenu {
    /** 1.12 gave the bag nine slots. */
    public static final int SLOTS = 9;
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;

    /** Which hand the bag is in, so the menu can find it again on both sides. */
    private final InteractionHand hand;
    private final Player owner;

    /** The aspects this menu is showing, in the order the bag holds them. */
    private final List<AspectId> shown = new ArrayList<>();

    /** Client side: the hand arrives in the opening data. */
    public CrystalBagMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data == null ? InteractionHand.MAIN_HAND
                : InteractionHand.values()[data.readVarInt()]);
    }

    private CrystalBagMenu(int windowId, Inventory inventory, InteractionHand hand) {
        super(Content.CRYSTAL_BAG_MENU.get(), windowId);
        this.hand = hand;
        this.owner = inventory.player;

        refresh();
        for (int index = 0; index < SLOTS; index++) {
            addSlot(new BagSlot(index, 8 + (index % 3) * 18, 18 + (index / 3) * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** Re-reads which aspects the bag is holding. */
    private void refresh() {
        shown.clear();
        shown.addAll(CrystalBagStore.contents(bag()).keySet());
    }

    /** The bag stack itself. */
    private ItemStack bag() {
        return owner == null ? ItemStack.EMPTY : owner.getItemInHand(hand);
    }

    /** Opens the bag on the server, telling the client which hand it is in. */
    public static void open(ServerPlayer player, InteractionHand hand) {
        dev.tc4port.thaumadditions.platform.TaPlatform.Active.get().openMenu(player,
                new net.minecraft.world.MenuProvider() {
                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return net.minecraft.network.chat.Component.translatable("item.thaumadds.crystal_bag");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player who) {
                        return new CrystalBagMenu(windowId, inventory, hand);
                    }
                },
                buf -> buf.writeVarInt(hand.ordinal()));
    }

    /** The aspects on show, for the screen's tooltips. */
    public List<AspectId> shown() {
        return shown;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    /** A slot standing for one aspect in the bag; taking from it takes out of the bag. */
    private class BagSlot extends Slot {
        private final int index;

        BagSlot(int index, int x, int y) {
            super(new net.minecraft.world.SimpleContainer(1), 0, x, y);
            this.index = index;
        }

        @Override
        public ItemStack getItem() {
            if (index >= shown.size()) {
                return ItemStack.EMPTY;
            }
            AspectId aspect = shown.get(index);
            int count = CrystalBagStore.contents(bag()).getOrDefault(aspect, 0);
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            // One essence per point, capped so a nearly full bag still shows a sane stack.
            return ThaumcraftItems.crystalEssence(aspect, Math.min(64, count));
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public ItemStack remove(int amount) {
            if (index >= shown.size()) {
                return ItemStack.EMPTY;
            }
            AspectId aspect = shown.get(index);
            int taken = CrystalBagStore.remove(bag(), aspect, amount);
            refresh();
            return taken <= 0 ? ItemStack.EMPTY
                    : ThaumcraftItems.crystalEssence(aspect, taken);
        }

        @Override
        public void set(ItemStack stack) {
            // The bag is filled by absorbing, never by hand.
        }

        @Override
        public void setByPlayer(ItemStack stack) {
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Nothing here moves between halves: the bag's slots are views, not storage.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !bag().isEmpty();
    }
}
