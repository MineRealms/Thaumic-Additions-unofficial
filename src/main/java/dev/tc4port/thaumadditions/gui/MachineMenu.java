package dev.tc4port.thaumadditions.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared plumbing for the machine menus: resolve the machine, lay out its slots, lay out the
 * player's, and move stacks between the two.
 *
 * <p>1.12's containers fed the GUI through synchronised fields on the container. The port does not
 * need that: the machines already push their whole state to the client (see
 * {@code SyncedBlockEntity}), so a screen reads the block entity's own getters for its gauges and
 * the menu only has to carry slots.</p>
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    /** Where the menu was opened; the client uses it to find its copy of the machine. */
    protected final BlockPos pos;
    /** The machine, or {@code null} while the client has not loaded that chunk yet. */
    protected final BlockEntity machine;
    protected final Inventory playerInventory;

    /** The range of slots that belong to the machine, for {@link #quickMoveStack}. */
    private final int machineSlots;

    /** Client side: the position arrives in the buffer the server filled when opening the menu. */
    protected MachineMenu(MenuType<?> type, int id, Inventory playerInventory, FriendlyByteBuf data,
                          int machineSlots) {
        this(type, id, playerInventory, data == null ? BlockPos.ZERO : data.readBlockPos(), machineSlots);
    }

    /** Server side: the position is already known, so no buffer is needed. */
    protected MachineMenu(MenuType<?> type, int id, Inventory playerInventory, BlockPos pos,
                          int machineSlots) {
        super(type, id);
        this.playerInventory = playerInventory;
        this.pos = pos;
        this.machineSlots = machineSlots;
        Level level = playerInventory.player.level();
        this.machine = level == null ? null : level.getBlockEntity(pos);

        Container container = machine instanceof Container found ? found : new SimpleContainer(machineSlots);
        addMachineSlots(container);
        addPlayerSlots(playerSlotsY());
    }

    /** Where this machine puts the player's inventory. */
    protected abstract int playerSlotsY();

    /** Adds the machine's own slots, at the pixel positions the 1.12 container used. */
    protected abstract void addMachineSlots(Container container);

    /** The 1.12 {@code ContainerTAR} layout: three rows of nine, then the hotbar. */
    protected void addPlayerSlots(int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, y + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return machine != null
                ? Container.stillValidBlockEntity(machine, player)
                : player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    /** The machine's own slots come first, so shift-click moves between the two halves. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack before = inSlot.copy();
        int playerStart = machineSlots;
        int playerEnd = slots.size();

        if (index < playerStart) {
            // Machine to player.
            if (!moveItemStackTo(inSlot, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, 0, playerStart, false)) {
            // Player to machine, then between the player's own halves as vanilla does.
            if (index < playerEnd - 9) {
                if (!moveItemStackTo(inSlot, playerEnd - 9, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(inSlot, playerStart, playerEnd - 9, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }

    /** The machine this menu drives, or {@code null} on a client that has not loaded it. */
    public BlockEntity machine() {
        return machine;
    }
}
