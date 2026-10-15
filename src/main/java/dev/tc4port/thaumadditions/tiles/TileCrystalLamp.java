package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumadditions.items.ThaumcraftItems;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileCrystalLamp}: a crystal block with a lamp on it — one stored aspect, a redstone
 * gate, and the crystallised essence of that aspect kept in a single slot.
 *
 * <p>The gate is the whole of the 1.12 behaviour that the block state cannot express: powering the
 * lamp drops its aspect to {@code null}, which is how the lamp goes dark. The rest of the machine
 * is the crystal block it extends.</p>
 *
 * <p>1.12 lit the world with a colour taken from the stored aspect through HammerCore's dynamic
 * light API. 1.20.1 has no equivalent that both targets share, so the lamp emits a plain light
 * level from its block state and the colour is only kept for the tooltip and the stored item.</p>
 */
public class TileCrystalLamp extends TileCrystal implements Container {
    private static final int SLOTS = 1;
    private static final String KEY_ITEMS = "Items";

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public TileCrystalLamp(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        HCLitePlatform.Holder.get().loadItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    /**
     * 1.12 {@code getAspect}: a powered lamp has no aspect, and a lamp with no aspect is dark.
     *
     * <p>The parent picks a random aspect for an untagged crystal; the lamp must not, or a lamp
     * nobody has filled would light up on its own.</p>
     */
    @Override
    public AspectId getAspect() {
        if (isPowered()) {
            return null;
        }
        return readStoredAspect();
    }

    /** The aspect actually stored, never the random one the crystal block invents. */
    public AspectId readStoredAspect() {
        return dev.tc4port.thaumadditions.aspect.TaAspect.readAspect(getPersistentData(), "Aspect");
    }

    private boolean isPowered() {
        Level level = getLevel();
        return level != null && level.getBestNeighborSignal(getBlockPos()) > 0;
    }

    /**
     * 1.12 {@code setAspect}: stores the aspect and lays a crystallised essence of it in the slot,
     * so the lamp can be emptied again by taking the item out.
     */
    @Override
    public void setAspect(AspectId id) {
        super.setAspect(id);
        ItemStack essence = id == null ? ItemStack.EMPTY : ThaumcraftItems.crystalEssence(id, 1);
        inventory.set(0, essence);
        saveInventory();
    }

    /** 0xRRGGBB of the stored aspect, for the GUI and the item tooltip. */
    public int aspectColor() {
        return dev.tc4port.thaumadditions.aspect.TaAspect.color(readStoredAspect());
    }

    @Override
    protected void afterReload() {
        HCLitePlatform.Holder.get().loadItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    private void saveInventory() {
        HCLitePlatform.Holder.get().saveItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
        // The slot change is only visible to the other side once the block entity is pushed;
        // setChanged on its own just marks it for saving.
        setChanged();
        sync();
    }

    public NonNullList<ItemStack> inventory() {
        return inventory;
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return inventory.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(inventory, slot, amount);
        if (!taken.isEmpty()) {
            saveInventory();
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = ContainerHelper.takeItem(inventory, slot);
        saveInventory();
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        saveInventory();
        setChanged();
    }

    /** 1.12 kept one essence in the slot and refused everything else. */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
        saveInventory();
        setChanged();
    }
}
