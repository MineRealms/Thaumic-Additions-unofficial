package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.TaFx;
import dev.tc4port.thaumadditions.aspect.EdibleAspect;
import dev.tc4port.thaumadditions.items.AspectItems;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * 1.12 {@code TileAuraDisperser}: burn a bag of salt essence and smear its aspect over every living
 * thing in a box in front of the block, for twenty seconds.
 *
 * <p>The machine converts one salt into an {@link AspectList}, then every second hands that list to
 * {@link EdibleAspect} for each entity in range. Only a second in which something actually happened
 * spends the pack's charge, so an idle disperser keeps its salt. A redstone signal of any strength
 * stops the effect without spending anything.</p>
 *
 * <p>1.12's aspects came from the whole item NBT of {@code ItemSaltEssence}; the port's salt carries
 * one aspect, so that is what a pack holds. Its coloured FX is not reproduced — the port spawns
 * plain enchant particles — because the coloured-dust particle class changed shape between the two
 * targets.</p>
 */
public class TileAuraDisperser extends SyncedBlockEntity
        implements TaTickables.Ticking, Container {
    /** 1.12 {@code secondsPerSalt}: a pack of salt buys twenty seconds of aura. */
    private static final int SECONDS_PER_SALT = 20;

    /** 1.12 {@code atTickRate(20)}: the effect is re-applied once a second, not once a tick. */
    private static final int APPLY_INTERVAL = 20;

    /** How many motes the disperser throws when a second of dispersal lands. */
    private static final int FX_COUNT = 12;

    /** 1.12's box: five blocks forward, four up, grown by four/three/four. */
    private static final int REACH_HORIZONTAL = 5;
    private static final int REACH_VERTICAL = 4;
    private static final double BOX_X = 4.0D;
    private static final double BOX_Y = 3.0D;
    private static final double BOX_Z = 4.0D;

    private static final int SLOTS = 9;
    private static final String KEY_ITEMS = "Items";

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    /** The aspect list currently being dispersed; {@code null} when the machine is idle. */
    private AspectList aspects;
    /** Seconds of charge left in the current pack. */
    private int timer;
    /** Ticks of charge left, kept in step with {@link #timer} for the client's readout. */
    private int tickTimer;
    /** Whether the last application second actually did anything. */
    private boolean auraApplied;

    /** Shared clock for the once-a-second gate; 1.12 got this from {@code atTickRate}. */
    private int tickCounter;

    public TileAuraDisperser(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.timer = getPersistentData().getInt("Timer");
        this.tickTimer = getPersistentData().getInt("TickTimer");
        this.auraApplied = getPersistentData().getBoolean("AuraApplied");
        this.aspects = readAspects(getPersistentData());
        HCLitePlatform.Holder.get().loadItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    private static AspectList readAspects(net.minecraft.nbt.CompoundTag tag) {
        if (!tag.contains("Aspects")) {
            return null;
        }
        AspectList list = new AspectList();
        for (AspectId id : dev.tc4port.thaumadditions.aspect.TaAspect.readAspects(tag, "Aspects")) {
            list.add(id, 1);
        }
        return list.visSize() > 0 ? list : null;
    }

    private void store() {
        getPersistentData().putInt("Timer", timer);
        getPersistentData().putInt("TickTimer", tickTimer);
        getPersistentData().putBoolean("AuraApplied", auraApplied);
        if (aspects == null) {
            getPersistentData().remove("Aspects");
        } else {
            java.util.List<AspectId> ids = new java.util.ArrayList<>();
            java.util.List<Integer> amounts = new java.util.ArrayList<>();
            for (AspectId id : aspects.aspectsSortedByAmount()) {
                ids.add(id);
                amounts.add(aspects.amount(id));
            }
            dev.tc4port.thaumadditions.aspect.TaAspect.writeAspects(
                    getPersistentData(), "Aspects", ids, amounts);
        }
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        ItemStack selected = select();

        // 1.12 consumed a pack only on the server, and only when it had nothing left to disperse.
        if (!level.isClientSide && !selected.isEmpty()
                && (aspects == null || aspects.visSize() == 0)) {
            aspects = aspectsOf(selected);
            selected.shrink(1);
            if (selected.isEmpty()) {
                saveInventory();
            }
            timer = SECONDS_PER_SALT;
            tickTimer = timer * 20;
            store();
        }

        if (aspects != null && aspects.visSize() > 0 && level.getBestNeighborSignal(pos) == 0) {
            applyAura(level, pos, state);
        }
    }

    /** 1.12 {@code applyAura}. */
    private void applyAura(Level level, BlockPos pos, BlockState state) {
        if (tickTimer > 0 && auraApplied) {
            --tickTimer;
        }

        if (level.isClientSide || timer <= 0 || aspects == null) {
            return;
        }
        if (++tickCounter < APPLY_INTERVAL) {
            return;
        }
        tickCounter = 0;

        Direction facing = state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
        AABB box = new AABB(pos)
                .move(facing.getStepX() * REACH_HORIZONTAL, facing.getStepY() * REACH_VERTICAL,
                        facing.getStepZ() * REACH_HORIZONTAL)
                .inflate(BOX_X, BOX_Y, BOX_Z);

        boolean did = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            did |= EdibleAspect.execute(entity, aspects).visSize() > 0;
        }

        if (did) {
            // 1.12's disperser FX: a puff of motes in the colour of whatever is being dispersed.
            AspectId dispersed = aspects.aspectsSorted().isEmpty()
                    ? null : aspects.aspectsSorted().get(0);
            double x = pos.getX() + 0.5D + facing.getStepX() * 1.5D;
            double y = pos.getY() + 0.5D + facing.getStepY() * 1.5D;
            double z = pos.getZ() + 0.5D + facing.getStepZ() * 1.5D;
            for (int index = 0; index < FX_COUNT; index++) {
                TaFx.aspectDust(level, x, y, z, dispersed,
                        (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                        (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                        (level.getRandom().nextDouble() - 0.5D) * 0.8D);
            }
        }

        int before = tickTimer;
        if (did) {
            --timer;
        }
        tickTimer = timer * 20;
        if (timer == 0) {
            aspects = null;
        }
        auraApplied = did;

        if (did || before != tickTimer) {
            store();
        }
    }

    // ------------------------------------------------------------------ inventory

    /** 1.12 {@code select}: the first salt essence in the nine slots, or empty. */
    private ItemStack select() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty() && stack.is(Content.SALT_ESSENCE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The aspect one pack of salt disperses: the item carries one, at one point. */
    private static AspectList aspectsOf(ItemStack salt) {
        AspectId id = AspectItems.aspectOf(salt);
        if (id == null) {
            return null;
        }
        AspectList list = new AspectList();
        list.add(id, 1);
        return list;
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

    /** Seconds of charge left, for the GUI. */
    public int timer() {
        return timer;
    }

    public int tickTimer() {
        return tickTimer;
    }

    public AspectList aspects() {
        return aspects;
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
}
