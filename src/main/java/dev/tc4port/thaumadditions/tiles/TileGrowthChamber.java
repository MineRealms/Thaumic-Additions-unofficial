package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.items.ThaumcraftItems;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.aspect.VisAction;
import dev.tc4port.thaumcraft.api.flux.FluxApi;
import dev.tc4port.thaumcraft.api.node.VisNetworkApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileGrowthChamber}: pull in the essentia of the crystal block's aspect, spend vis, and
 * grow crystallised essence into the output slot.
 *
 * <p>One 1.12 branch has no TC4R counterpart: when the chamber has nothing to grow it used to hand
 * its unused vis budget back to the chunk with {@code AuraHelper.addVis}. TC4R publishes drains on
 * the vis network but no way to add to it, so the port lets the budget sit instead. The rest maps
 * straight across: {@code AuraHelper.drainVis} is {@code VisNetworkApi.drain} on the channel the
 * crystal's aspect belongs to, and the pollution on the idle branch is {@code FluxApi.add}.</p>
 */
public class TileGrowthChamber extends TileEssentia implements Container {
    public static final int SLOT_CRYSTAL = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int SLOTS = 2;

    private static final String KEY_ITEMS = "Items";

    /** 1.12 {@code MAX_AURA_DRAIN}: the vis budget one piece of essence costs. */
    private static final float MAX_AURA_DRAIN = 20.0F;
    /** 1.12 spread that budget over twenty seconds at twenty ticks a second. */
    private static final float DRAIN_TICKS = 20.0F * 60.0F;
    /** 1.12 {@code capacity}: how much essentia the chamber holds before it can grow. */
    private static final int CAPACITY = 6;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    /** Essentia of the crystal's aspect held for the next grow. */
    private int amount;
    /** Vis budget left before the next piece of essence is ready. */
    private float toDrainAura = MAX_AURA_DRAIN;
    /** Centivis owed but not yet drained: the per-tick rate is fractional, drains are not. */
    private float drainCarry;
    /** The aspect the crystal in the input slot stands for; null when there is no crystal. */
    private AspectId slotAspect;

    public TileGrowthChamber(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CAPACITY, 128);
        readState();
    }

    /** Re-reads the fields this machine caches; see the crusher's twin for why. */
    private void readState() {
        this.amount = getPersistentData().getInt("Amount");
        this.toDrainAura = getPersistentData().contains("Aura")
                ? getPersistentData().getFloat("Aura") : MAX_AURA_DRAIN;
        HCLitePlatform.Holder.get().loadItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    protected boolean accepts(Direction face) {
        return true;
    }

    @Override
    protected boolean emits(Direction face) {
        return false;
    }

    public int stored() {
        return amount;
    }

    public AspectId slotAspect() {
        return slotAspect;
    }

    @Override
    protected void machineTick(Level level, BlockPos pos, BlockState state) {
        ItemStack output = inventory.get(SLOT_OUTPUT);
        if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
            return;
        }

        // 1.12 read the aspect off the crystal block item, and rolled a random one when the item
        // carried none — which is every crystal block the port places, since its item is plain.
        ItemStack crystal = inventory.get(SLOT_CRYSTAL);
        boolean isCrystal = !crystal.isEmpty() && crystal.is(Content.CRYSTAL_BLOCK.get().asItem());
        AspectId wanted = isCrystal ? aspectOfCrystal(crystal) : null;
        boolean aspectChanged = !java.util.Objects.equals(slotAspect, wanted);
        slotAspect = wanted;

        if (slotAspect == null) {
            if (level.isClientSide) {
                return;
            }
            if (amount > 0) {
                // Nothing to grow into: what was collected spoils the aura, as in 1.12.
                if (level instanceof ServerLevel server) {
                    FluxApi.add(server, pos, amount);
                }
                amount = 0;
                store();
            }
            return;
        }

        if (level.isClientSide) {
            return;
        }

        boolean dirty = aspectChanged;
        int drawn = pullFromNeighbours(level, pos, slotAspect, CAPACITY - amount);
        if (drawn > 0) {
            amount += drawn;
            dirty = true;
        }

        if (canCraft(output)) {
            if (drainVis(level, pos)) {
                toDrainAura -= MAX_AURA_DRAIN / DRAIN_TICKS;
                dirty = true;
            }
            if (toDrainAura <= 0.0F) {
                craft();
                amount--;
                toDrainAura += MAX_AURA_DRAIN;
                dirty = true;
            }
        }

        if (dirty) {
            store();
        }
    }

    private boolean canCraft(ItemStack output) {
        return slotAspect != null && amount > 0
                && (output.isEmpty() || output.getCount() < output.getMaxStackSize());
    }

    /** 1.12 {@code craft}: one crystal essence of the crystal's aspect into the output slot. */
    private void craft() {
        ItemStack essence = ThaumcraftItems.crystalEssence(slotAspect, 1);
        if (essence.isEmpty()) {
            return;
        }
        ItemStack output = inventory.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.set(SLOT_OUTPUT, essence);
        } else {
            output.grow(1);
        }
        saveInventory();
    }

    /**
     * 1.12's vis drain: a twentieth of the budget each second, asked for as an integer because
     * {@code VisNetworkApi.drain} counts centivis, tracked exactly across ticks.
     */
    private boolean drainVis(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        VisChannel channel = VisChannel.fromAspectId(slotAspect).orElse(null);
        if (channel == null) {
            return false;
        }
        drainCarry += MAX_AURA_DRAIN / DRAIN_TICKS * 100.0F;
        int want = (int) drainCarry;
        if (want <= 0) {
            return false;
        }
        int drained = VisNetworkApi.drain(server, pos, channel, want, VisAction.EXECUTE);
        if (drained <= 0) {
            return false;
        }
        drainCarry -= drained;
        return true;
    }

    /**
     * The aspect the crystal block item carries, or a random one when it carries none.
     *
     * <p>The aspect has to be read the way the item stores it — see
     * {@link ThaumcraftItems#aspectOfCrystalEssence} — or every crystal looks aspect-less and the
     * chamber grows a random one instead of the crystal's own.</p>
     */
    private static AspectId aspectOfCrystal(ItemStack crystal) {
        AspectId tagged = ThaumcraftItems.aspectOfCrystalEssence(crystal);
        return tagged != null ? tagged : TaAspect.random(new java.util.Random());
    }

    private void store() {
        getPersistentData().putInt("Amount", amount);
        getPersistentData().putFloat("Aura", toDrainAura);
        setChanged();
        sync();
    }

    @Override
    protected void afterReload() {
        readState();
    }

    private void saveInventory() {
        HCLitePlatform.Holder.get().saveItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
        // The slot change is only visible to the other side once the block entity is pushed;
        // setChanged on its own just marks it for saving.
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
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

    public NonNullList<ItemStack> inventory() {
        return inventory;
    }
}
