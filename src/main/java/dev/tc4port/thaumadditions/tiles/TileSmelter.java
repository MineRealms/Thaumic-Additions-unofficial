package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.blocks.TaBlocks;
import dev.tc4port.thaumadditions.blocks.TaMachineBlocks;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectQueryApi;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.flux.FluxApi;
import dev.tc4port.thaumcraft.api.machine.AlembicApi;
import dev.tc4port.thaumcraft.api.machine.ArcaneBellowsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileAbstractSmelter} with {@code TileSmelterImpl}'s per-variant numbers: melt an item
 * into its aspects and hand the result to the alembics stacked above.
 *
 * <p>Two 1.12 hooks have no TC4R counterpart and are dropped rather than faked. The smelter vent
 * ({@code BlocksTC.smelterVent}) used to swallow part of the flux a lossy smelter produced before it
 * reached the aura, and the smelter aux block ({@code BlocksTC.smelterAux}) let a smelter feed
 * alembics at its sides through it. TC4R ships neither, only the arcane alembic, so every lost unit
 * pollutes the aura and only the column above can be fed.</p>
 *
 * <p>The item in the fuel slot also loses 1.12's container-item swap (a lava bucket's empty bucket):
 * {@code Item#getCraftingRemainingItem} changed shape in 1.21 and no fuel the target pack uses
 * needs it.</p>
 */
public class TileSmelter extends TileEssentia implements Container {
    /** 1.12 {@code TileAbstractSmelter}: slot 0 is what gets melted, slot 1 is the fuel. */
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    private static final int SLOTS = 2;

    private static final String KEY_ITEMS = "Items";

    /** 1.12 rolled flux at 2/3 of the smelter's efficiency. */
    private static final float FLUX_ROLL_FACTOR = 0.66F;

    /** How far up to look for alembics; 1.12 walked until it hit a non-alembic. */
    private static final int ALEMBIC_REACH = 6;

    private final float efficiency;
    private final int speed;
    private final int capacity;
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    private int burnTime;
    private int currentBurnTime;
    private int cookTime;
    private int smeltTime;
    private int bellows;
    private boolean speedBoost;
    private int count;

    public TileSmelter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, capacityOf(state), 1);
        TaMachineBlocks.SmelterBlock block = (TaMachineBlocks.SmelterBlock) state.getBlock();
        this.efficiency = block.efficiency();
        this.speed = block.speed();
        this.capacity = block.capacity();
        this.burnTime = getPersistentData().getInt("BurnTime");
        this.currentBurnTime = getPersistentData().getInt("CurrentItemBurnTime");
        this.cookTime = getPersistentData().getInt("CookTime");
        HCLitePlatform.Holder.get().loadItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    private static int capacityOf(BlockState state) {
        return state.getBlock() instanceof TaMachineBlocks.SmelterBlock block ? block.capacity() : 1000;
    }

    public float efficiency() {
        return efficiency;
    }

    public int speed() {
        return speed;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    // ------------------------------------------------------------------ multi-aspect store
    //
    // 1.12's smelter held an AspectList: one item yields several aspects and it banked all of them,
    // which is why it can't use TileEssentia's single-aspect store. The faces still have to answer
    // one aspect at a time for the transport interface, so they report whichever aspect the store
    // offers first.

    /** Snapshot of the stored aspects, keyed by aspect. Never mutated in place. */
    private java.util.Map<AspectId, Integer> storage() {
        java.util.List<AspectId> ids = TaAspect.readAspects(getPersistentData(), "Aspects");
        java.util.List<Integer> amounts = TaAspect.readAspectAmounts(getPersistentData(), "Aspects");
        java.util.Map<AspectId, Integer> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < ids.size() && i < amounts.size(); i++) {
            map.merge(ids.get(i), amounts.get(i), Integer::sum);
        }
        return map;
    }

    private void store(java.util.Map<AspectId, Integer> map) {
        java.util.List<AspectId> ids = new java.util.ArrayList<>();
        java.util.List<Integer> amounts = new java.util.ArrayList<>();
        for (java.util.Map.Entry<AspectId, Integer> entry : map.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                ids.add(entry.getKey());
                amounts.add(entry.getValue());
            }
        }
        TaAspect.writeAspects(getPersistentData(), "Aspects", ids, amounts);
        setChanged();
        sync();
    }

    /** Total stored essentia, 1.12's {@code aspects.visSize()}. */
    @Override
    public int getAmount() {
        int total = 0;
        for (Integer amount : storage().values()) {
            if (amount != null && amount > 0) {
                total += amount;
            }
        }
        return total;
    }

    /** The aspect the transport faces report: the first one the store still holds. */
    @Override
    public AspectId getAspect() {
        for (java.util.Map.Entry<AspectId, Integer> entry : storage().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public int insert(AspectId id, int value) {
        if (id == null || value <= 0) {
            return 0;
        }
        int accepted = Math.min(value, Math.max(0, capacity - getAmount()));
        if (accepted > 0) {
            java.util.Map<AspectId, Integer> map = storage();
            map.merge(id, accepted, Integer::sum);
            store(map);
        }
        return accepted;
    }

    @Override
    public int extract(AspectId id, int value) {
        if (id == null || value <= 0) {
            return 0;
        }
        java.util.Map<AspectId, Integer> map = storage();
        int have = map.getOrDefault(id, 0);
        int taken = Math.min(value, Math.max(0, have));
        if (taken > 0) {
            map.put(id, have - taken);
            store(map);
        }
        return taken;
    }

    @Override
    public AspectId essentiaType(net.minecraft.core.Direction face) {
        return getAspect();
    }

    @Override
    public int essentiaAmount(net.minecraft.core.Direction face) {
        return getAmount();
    }

    @Override
    protected boolean accepts(net.minecraft.core.Direction face) {
        return face != net.minecraft.core.Direction.DOWN;
    }

    @Override
    protected boolean emits(net.minecraft.core.Direction face) {
        return face == net.minecraft.core.Direction.UP;
    }

    // ------------------------------------------------------------------ progress, for the GUI

    public int burnTime() {
        return burnTime;
    }

    public int currentBurnTime() {
        return currentBurnTime;
    }

    public int cookTime() {
        return cookTime;
    }

    public int smeltTime() {
        return smeltTime;
    }

    public int bellows() {
        return bellows;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void machineTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        boolean wasLit = burnTime > 0;
        boolean dirty = false;
        ++count;

        if (burnTime > 0) {
            --burnTime;
        }

        // 1.12 re-counted bellows whenever the cached value went negative; once every two seconds
        // picks up the same changes without walking the neighbours every tick.
        if (count % 40 == 0) {
            bellows = ArcaneBellowsApi.countAdjacentBlowingToward(level, pos);
        }

        if (count % Math.max(1, speed / 2) == 0 && pushEssentia(level, pos)) {
            dirty = true;
        }

        if (burnTime == 0 && canSmelt()) {
            currentBurnTime = burnTime = fuelValue(inventory.get(SLOT_FUEL));
            if (burnTime > 0) {
                dirty = true;
                // 1.12 flagged alumentum, which burns hotter than its burn time alone suggests.
                speedBoost = inventory.get(SLOT_FUEL).is(alumentum());
                inventory.get(SLOT_FUEL).shrink(1);
                if (inventory.get(SLOT_FUEL).isEmpty()) {
                    inventory.set(SLOT_FUEL, ItemStack.EMPTY);
                }
                saveInventory();
            }
        }

        boolean enabled = state.hasProperty(TaBlocks.EnableableBlock.ENABLED)
                && state.getValue(TaBlocks.EnableableBlock.ENABLED);
        if (enabled && canSmelt()) {
            ++cookTime;
            if (cookTime >= smeltTime) {
                cookTime = 0;
                smeltItem(level, pos);
                dirty = true;
            }
        } else if (cookTime != 0) {
            cookTime = 0;
            dirty = true;
        }

        if (wasLit != burnTime > 0) {
            dirty = true;
            if (state.hasProperty(TaBlocks.EnableableBlock.ENABLED)) {
                level.setBlock(pos, state.setValue(TaBlocks.EnableableBlock.ENABLED, burnTime > 0), 3);
            }
        }

        if (dirty) {
            getPersistentData().putInt("BurnTime", burnTime);
            getPersistentData().putInt("CurrentItemBurnTime", currentBurnTime);
            getPersistentData().putInt("CookTime", cookTime);
            setChanged();
            sync();
        }
    }

    /** 1.12 {@code canSmelt}: the input yields aspects that still fit, and the melt takes time. */
    private boolean canSmelt() {
        ItemStack input = inventory.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        int total = visSize(AspectQueryApi.item(input));
        if (total <= 0 || total > capacity - getAmount()) {
            return false;
        }
        // 1.12: smeltTime = total * 2 * (1 - 0.125 * bellows); alumentum burns hotter.
        int effective = speedBoost ? (int) (speed * 0.8F) : speed;
        smeltTime = Math.max(1, (int) (total * 2 * (1.0F - 0.125F * bellows) * speed / (float) effective));
        return true;
    }

    /** 1.12 {@code smeltItem}: roll each aspect against the efficiency, then bank what survived. */
    private void smeltItem(Level level, BlockPos pos) {
        ItemStack input = inventory.get(SLOT_INPUT);
        AspectAmounts source = AspectQueryApi.item(input);
        AspectId flux = fluxAspect();
        int lost = 0;

        for (java.util.Map.Entry<AspectId, Integer> entry : source.amounts().entrySet()) {
            AspectId aspect = entry.getKey();
            int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount <= 0) {
                continue;
            }
            int kept = amount;
            float chance = aspect.equals(flux) ? efficiency * FLUX_ROLL_FACTOR : efficiency;
            if (efficiency < 1.0F) {
                kept = 0;
                for (int i = 0; i < amount; i++) {
                    if (level.random.nextFloat() <= chance) {
                        kept++;
                    } else {
                        lost++;
                    }
                }
            } else if (efficiency > 1.0F) {
                for (int i = 0; i < amount; i++) {
                    if (level.random.nextFloat() + 1.0F < chance) {
                        kept++;
                    }
                }
            }
            if (kept > 0) {
                insert(aspect, kept);
            }
        }

        if (lost > 0 && level instanceof ServerLevel server) {
            // With no smelter vent to swallow it, all of the loss reaches the aura.
            FluxApi.add(server, pos, lost);
        }

        input.shrink(1);
        if (input.isEmpty()) {
            inventory.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        saveInventory();
    }

    /**
     * 1.12 {@code processAlembics}: walk the alembics stacked above and give one unit to the first
     * that takes it. Whether anything moved tells the caller the state changed.
     */
    private boolean pushEssentia(Level level, BlockPos pos) {
        if (getAmount() <= 0) {
            return false;
        }
        AspectId aspect = getAspect();
        if (aspect == null) {
            return false;
        }
        for (int deep = 1; deep <= ALEMBIC_REACH; deep++) {
            BlockPos target = pos.above(deep);
            if (AlembicApi.snapshot(level, target).isEmpty()) {
                return false;
            }
            if (AlembicApi.insertLoaded(level, target, aspect, 1, EssentiaTransferMode.EXECUTE) > 0) {
                extract(aspect, 1);
                return true;
            }
        }
        return false;
    }

    /**
     * How long the stack burns for. 1.12 asked {@code TileEntityFurnace.getItemBurnTime}, which
     * checked the vanilla fuel table; {@code ItemStack#getBurnTime} only reports what a
     * {@code FurnaceFuelBurnTimeEvent} listener claimed and answers -1 otherwise, so the furnace's
     * own table is consulted as well.
     */
    private static int fuelValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Integer fromFurnace = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
                .getFuel().get(stack.getItem());
        int vanilla = fromFurnace == null ? 0 : fromFurnace;
        int fromEvent = stack.getBurnTime(RecipeType.SMELTING);
        return Math.max(vanilla, fromEvent);
    }

    private static int visSize(AspectAmounts aspects) {
        int total = 0;
        for (Integer amount : aspects.amounts().values()) {
            if (amount != null && amount > 0) {
                total += amount;
            }
        }
        return total;
    }

    /** 1.12's alumentum, which the port resolves through TC4R rather than a hard reference. */
    private static net.minecraft.world.item.Item alumentum() {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse("thaumcraft:alumentum"))
                .orElse(net.minecraft.world.item.Items.AIR);
    }

    private static AspectId fluxAspect() {
        try {
            AspectId flux = AspectId.parse("vitium");
            return AspectApi.contains(flux) ? flux : null;
        } catch (RuntimeException unknown) {
            return null;
        }
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

    /** 1.12 {@code isItemValidForSlot}: slot 0 takes anything that yields aspects, slot 1 fuel. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) {
            return fuelValue(stack) > 0;
        }
        return !AspectQueryApi.item(stack).amounts().isEmpty();
    }

    /** The inventory as the GUI and the probes see it. */
    public NonNullList<ItemStack> inventory() {
        return inventory;
    }
}
