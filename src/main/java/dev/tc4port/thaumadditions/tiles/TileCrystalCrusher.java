package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumadditions.items.AspectItems;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectItemView;
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
 * 1.12 {@code TileCrystalCrusher}: spend MECHANISM essentia to grind crystallised essence into the
 * salt essence of the same aspect.
 *
 * <p>1.12 asked TC's {@code EssentiaHandler} to drain from whatever jar or tube was in reach;
 * {@link TileEssentia#pullFromNeighbours} does the same walk here. The crystal essence's aspect is
 * read through TC4R's {@code AspectItemView}, because the item belongs to TC4R and keeps its aspect
 * in its own item data rather than the port's NBT shape. The salt essence it produces is the port's
 * own item, so its aspect moves in the port's shape.</p>
 */
public class TileCrystalCrusher extends TileEssentia implements Container {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int SLOTS = 2;

    private static final String KEY_ITEMS = "Items";

    /** 1.12 {@code drainEssentia(this, Aspect.MECHANISM, null, 12, 1)}: suction 12, one unit. */
    private static final int DRAIN_SUCTION = 12;
    /** Each drain buys three crushes. */
    private static final int CRUSHES_PER_DRAIN = 3;
    /** How many pieces of the crystal fly off each time it is ground down. */
    private static final int CRUSH_PARTICLES = 8;
    /** 1.12 {@code craftTime >= 100}. */
    private static final int CRAFT_TICKS = 100;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    /** 1.12 calls this "Fuel"; it counts crushes, not fuel. */
    private int crushes;
    private int craftTime;

    /** Drives the renderer's rotator; 1.12 wound it up client-side only. */
    private final TaRotator rotator = TaRotator.randomised(new java.util.Random());

    public TileCrystalCrusher(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 64, 1);
        readState();
    }

    /**
     * Re-reads every field the machine keeps a copy of. The loader only writes the block entity's
     * tag, so anything cached in a field has to be pulled back out of it after a reload — which is
     * what the constructor and {@link #afterReload()} both do.
     */
    private void readState() {
        this.crushes = getPersistentData().getInt("Fuel");
        this.craftTime = getPersistentData().getInt("CraftTime");
        HCLitePlatform.Holder.get().loadItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    @Override
    protected boolean accepts(net.minecraft.core.Direction face) {
        return false;
    }

    @Override
    protected boolean emits(net.minecraft.core.Direction face) {
        return false;
    }

    public int crushes() {
        return crushes;
    }

    public int craftTime() {
        return craftTime;
    }

    /** The head's spin, for the renderer. */
    public TaRotator rotator() {
        return rotator;
    }

    @Override
    protected void machineTick(Level level, BlockPos pos, BlockState state) {
        rotator.update();

        // 1.12 only spun the rotator on the client, and only while it was working.
        if (level.isClientSide) {
            if (canCraft()) {
                rotator.speedup(0.4F);
            }
            return;
        }

        // 1.12 stopped the whole machine on a redstone signal.
        if (level.hasNeighborSignal(pos)) {
            return;
        }

        boolean dirty = false;
        if (crushes <= 0 && drainMachina(level, pos)) {
            crushes += CRUSHES_PER_DRAIN;
            dirty = true;
        }

        if (canCraft()) {
            ++craftTime;
            if (craftTime >= CRAFT_TICKS) {
                craft();
                dirty = true;
            }
        } else if (craftTime > 0) {
            --craftTime;
            dirty = true;
        }

        if (dirty) {
            getPersistentData().putInt("Fuel", crushes);
            getPersistentData().putInt("CraftTime", craftTime);
            setChanged();
            sync();
        }
    }

    /** 1.12 {@code EssentiaHandler.drainEssentia(this, Aspect.MECHANISM, null, 12, 1)}. */
    private boolean drainMachina(Level level, BlockPos pos) {
        AspectId machina = machinaAspect();
        if (machina == null) {
            return false;
        }
        return pullFromNeighbours(level, pos, machina, 1) == 1;
    }

    /** 1.12 {@code canCraft}: fuel left, a crystallised essence in, and room for the salt essence. */
    private boolean canCraft() {
        if (crushes <= 0) {
            return false;
        }
        AspectId in = inputAspect();
        if (in == null) {
            return false;
        }
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(Content.SALT_ESSENCE.get())
                && in.equals(AspectItems.aspectOf(out))
                && out.getCount() < out.getMaxStackSize();
    }

    /** 1.12 {@code craft}: one crystallised essence becomes one salt essence of the same aspect. */
    private void craft() {
        AspectId aspect = inputAspect();
        if (aspect == null) {
            return;
        }
        // 1.12 crumbled the crystal it had just ground up. It did that with a particle of its own
        // that drew the item's own texture breaking apart; the modern game has that particle built
        // in, so the crystal's own icon flies off in pieces.
        if (getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
            ItemStack ground = inventory.get(SLOT_INPUT);
            if (!ground.isEmpty()) {
                server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(
                                net.minecraft.core.particles.ParticleTypes.ITEM, ground.copyWithCount(1)),
                        getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.75D,
                        getBlockPos().getZ() + 0.5D, CRUSH_PARTICLES, 0.25D, 0.25D, 0.25D, 0.05D);
            }
        }
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            ItemStack salt = new ItemStack(Content.SALT_ESSENCE.get());
            AspectItems.setAspect(salt, aspect);
            inventory.set(SLOT_OUTPUT, salt);
        } else {
            out.grow(1);
        }
        inventory.get(SLOT_INPUT).shrink(1);
        if (inventory.get(SLOT_INPUT).isEmpty()) {
            inventory.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        crushes--;
        craftTime = 0;
        saveInventory();
    }

    /** The aspect of the crystallised essence in the input slot, read through TC4R's own view. */
    private AspectId inputAspect() {
        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.isEmpty() || !in.is(dev.tc4port.thaumadditions.items.ThaumcraftItems.crystalEssence())) {
            return null;
        }
        return dev.tc4port.thaumadditions.items.ThaumcraftItems.aspectOfCrystalEssence(in);
    }

    private static AspectId machinaAspect() {
        try {
            AspectId machina = AspectId.parse("machina");
            return AspectApi.contains(machina) ? machina : null;
        } catch (RuntimeException unknown) {
            return null;
        }
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
