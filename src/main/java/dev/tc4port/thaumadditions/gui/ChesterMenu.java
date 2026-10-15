package dev.tc4port.thaumadditions.gui;

import dev.tc4port.thaumadditions.entity.Chester;
import dev.tc4port.thaumadditions.registry.Content;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Chester's four rows of nine.
 *
 * <p>A menu for an entity rather than a block, so the opening data carries the entity's id instead
 * of a position. Everything else is the same shape as the machine menus: the entity's slots first,
 * the player's below, and shift-click moving between them.</p>
 */
public class ChesterMenu extends AbstractContainerMenu {
    /** 1.12 laid the chest out nine to a row, four rows deep. */
    public static final int COLUMNS = 9;
    public static final int ROWS = 4;
    public static final int CONTAINER_SLOTS = COLUMNS * ROWS;
    /** The panel is 176 wide and 168 tall: four rows plus the player's inventory. */
    public static final int WIDTH = 176;
    public static final int HEIGHT = 168;

    private final Container container;
    private final int entityId;

    /** Client side: the entity arrives by id in the opening data. */
    public ChesterMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data == null ? -1 : data.readVarInt());
    }

    private ChesterMenu(int windowId, Inventory inventory, int entityId) {
        super(Content.CHESTER_MENU.get(), windowId);
        this.entityId = entityId;
        Level level = inventory.player.level();
        Entity found = entityId < 0 || level == null ? null : level.getEntity(entityId);
        this.container = found instanceof Chester chester ? chester : new SimpleContainer(CONTAINER_SLOTS);

        // The chest itself, at 1.12's offset inside the panel.
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(container, column + row * COLUMNS, 8 + column * 18, 18 + row * 18));
            }
        }
        // The player's thirty-six, then the hotbar.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** Opens the menu on the server, telling the client which entity it belongs to. */
    public static void open(Player player, Chester chester) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) {
            return;
        }
        chester.setOpen(true);
        dev.tc4port.thaumadditions.platform.TaPlatform.Active.get().openMenu(server,
                new net.minecraft.world.MenuProvider() {
                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return net.minecraft.network.chat.Component.translatable("entity.thaumadds.chester");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player who) {
                        return new ChesterMenu(windowId, inventory, chester.getId());
                    }
                },
                buf -> buf.writeVarInt(chester.getId()));
    }

    /** The entity this menu is showing, or {@code null} when the client has not got it. */
    public Chester chester() {
        return container instanceof Chester chester ? chester : null;
    }

    public int entityId() {
        return entityId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack before = inSlot.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(inSlot, CONTAINER_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, 0, CONTAINER_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // The lid closes with the menu, which is what the renderer watches.
        if (container instanceof Chester chester) {
            chester.setOpen(false);
        }
    }
}
