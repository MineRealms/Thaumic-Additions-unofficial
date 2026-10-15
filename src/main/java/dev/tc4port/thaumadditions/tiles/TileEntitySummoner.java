package dev.tc4port.thaumadditions.tiles;

import java.util.ArrayList;
import java.util.List;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectContainerView;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import dev.tc4port.thaumcraft.api.aspect.AspectQueryApi;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import dev.tc4port.thaumcraft.api.flux.FluxApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileEntitySummoner}: read the aspects off a captured creature and pay them back in
 * essentia to put one — or four — of that creature back into the world.
 *
 * <p>The price is the creature's own aspect profile, so the machine has to build a throwaway copy of
 * it to ask. 1.12 read the profile out of TC's entity-tag table by entity name; TC4R keeps entity
 * aspects as a data pack and answers per entity, so the port makes a preview entity from the cell's
 * stored data and asks {@link AspectQueryApi} about that.</p>
 *
 * <p>Two details are the port's rather than 1.12's, and both come from the cell being richer here:
 * the port's entity cell stores the captured creature's whole NBT rather than just its type, and
 * the summoner loads it onto each spawn the same way the cell does when used by hand. 1.12 spawned
 * a bare creature of the right type. The other is the spill on breaking the block: 1.12 polluted
 * the aura with however much essentia had been banked, and that is kept.</p>
 */
public class TileEntitySummoner extends SyncedBlockEntity
        implements TaTickables.Ticking, Container, EssentiaTransport, AspectContainerView {
    private static final int SLOTS = 1;
    private static final String KEY_ITEMS = "Items";
    private static final String KEY_ENTITY = "Entity";
    private static final String KEY_REQUIREMENTS = "Requirements";
    private static final String KEY_ACCUMULATED = "Accumulated";
    private static final String KEY_COOLDOWN = "Cooldown";

    /** 1.12 drained one unit at a time with suction 8, every third tick. */
    private static final int SUCTION = 8;
    private static final int DRAIN_INTERVAL = 3;
    /** 1.12 spawned one to four creatures within four blocks of the machine. */
    private static final int MIN_SPAWN = 1;
    private static final int SPAWN_SPREAD = 4;
    /** 1.12 waited between eighty and a hundred and thirty ticks after a spawn. */
    private static final int COOLDOWN_BASE = 80;
    private static final int COOLDOWN_JITTER = 50;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    /** What the captured creature costs, or {@code null} while there is no cell in the slot. */
    private AspectList requirements;
    /** What has been paid so far. */
    private AspectList accumulated = new AspectList();

    private int cooldown;
    private int drainTicker;
    /** The tesseract's twist, for the renderer. */
    private final TaRotator rotator = new TaRotator();

    public TileEntitySummoner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        CompoundTag tag = getPersistentData();
        this.requirements = readAspects(tag, KEY_REQUIREMENTS);
        this.accumulated = readAspects(tag, KEY_ACCUMULATED);
        if (this.accumulated == null) {
            this.accumulated = new AspectList();
        }
        this.cooldown = tag.getInt(KEY_COOLDOWN);
        HCLitePlatform.Holder.get().loadItemStacks(this, tag, KEY_ITEMS, inventory);
    }

    public int cooldown() {
        return cooldown;
    }

    /** The tesseract; the renderer twists it by half this angle, as 1.12 did. */
    public TaRotator rotator() {
        return rotator;
    }

    /** What the cell still owes, or an empty list when there is nothing to summon. */
    public AspectList missing() {
        AspectList owed = new AspectList();
        if (requirements == null) {
            return owed;
        }
        for (AspectId aspect : requirements.aspectsSorted()) {
            int left = requirements.amount(aspect) - accumulated.amount(aspect);
            if (left > 0) {
                owed.add(aspect, left);
            }
        }
        return owed;
    }

    /** 1.12 {@code getAspects}: the machine advertises what it still wants, not what it holds. */
    @Override
    public AspectAmounts visibleAspects() {
        AspectList owed = missing();
        return owed.visSize() > 0 ? new AspectAmounts(owed.asMap()) : AspectAmounts.EMPTY;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        rotator.update();
        rotator.speedup(0.4F);
        if (cooldown > 0) {
            --cooldown;
        }

        // The recipe changes the moment the cell does.
        AspectList wanted = requirementsFor(level);
        if (!sameAs(wanted, requirements)) {
            requirements = wanted;
            accumulated = new AspectList();
            drainTicker = 0;
            store();
        }

        if (level.isClientSide || !canSpawn(level, pos) || requirements == null) {
            return;
        }

        AspectList owed = missing();
        if (owed.visSize() == 0) {
            if (cooldown == 0 && ++drainTicker >= 20) {
                drainTicker = 0;
                if (performSpawn(level, pos)) {
                    cooldown += COOLDOWN_BASE + level.getRandom().nextInt(COOLDOWN_JITTER);
                    accumulated = new AspectList();
                    store();
                }
            }
            return;
        }

        if (++drainTicker < DRAIN_INTERVAL) {
            return;
        }
        drainTicker = 0;

        // 1.12 always reached for the alphabetically last outstanding aspect.
        List<AspectId> sorted = owed.aspectsSorted();
        AspectId wanted2 = sorted.get(sorted.size() - 1);
        int taken = TileEssentia.pullFromNeighbours(level, pos, wanted2, 1);
        if (taken > 0) {
            accumulated.add(wanted2, taken);
            store();
        }
    }

    /** 1.12 {@code canSpawn}: unpowered, and a filled entity cell in the slot. */
    private boolean canSpawn(Level level, BlockPos pos) {
        if (level.getBestNeighborSignal(pos) > 0) {
            return false;
        }
        ItemStack cell = inventory.get(0);
        return !cell.isEmpty() && entityTypeOf(level, cell) != null;
    }

    /** 1.12 {@code getAspectsForDNA}: the captured creature's own aspect profile. */
    private AspectList requirementsFor(Level level) {
        ItemStack cell = inventory.get(0);
        if (cell.isEmpty()) {
            return null;
        }
        EntityType<?> type = entityTypeOf(level, cell);
        if (type == null) {
            return null;
        }
        Entity preview = type.create(level);
        if (preview == null) {
            return null;
        }
        CompoundTag data = HCLitePlatform.Holder.get().itemTag(cell);
        if (data != null && data.contains(KEY_ENTITY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            // Loading the captured data is what makes the aspect query see the right creature.
            preview.load(data.getCompound(KEY_ENTITY));
        }
        AspectAmounts amounts = AspectQueryApi.entity(preview);
        AspectList list = new AspectList();
        for (var entry : amounts.amounts().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                list.add(entry.getKey(), entry.getValue());
            }
        }
        preview.discard();
        return list.visSize() > 0 ? list : null;
    }

    private static EntityType<?> entityTypeOf(Level level, ItemStack cell) {
        CompoundTag tag = HCLitePlatform.Holder.get().itemTag(cell);
        if (tag == null || !tag.contains(KEY_ENTITY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return null;
        }
        return EntityType.by(tag.getCompound(KEY_ENTITY)).orElse(null);
    }

    private static boolean sameAs(AspectList first, AspectList second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.asMap().equals(second.asMap());
    }

    /** 1.12 {@code performSpawn}: one to four creatures scattered around the machine. */
    private boolean performSpawn(Level level, BlockPos pos) {
        ItemStack cell = inventory.get(0);
        CompoundTag tag = HCLitePlatform.Holder.get().itemTag(cell);
        if (tag == null) {
            return false;
        }
        CompoundTag data = tag.getCompound(KEY_ENTITY);
        EntityType<?> type = EntityType.by(data).orElse(null);
        if (type == null) {
            return false;
        }
        int count = MIN_SPAWN + level.getRandom().nextInt(SPAWN_SPREAD - MIN_SPAWN + 1);
        int spawned = 0;
        for (int index = 0; index < count; index++) {
            Entity entity = type.create(level);
            if (entity == null) {
                continue;
            }
            entity.load(data);
            // 1.12 gave every spawn a fresh identity before placing it. Without this the whole
            // batch would share the cell's UUID and only the first would survive.
            entity.setUUID(java.util.UUID.randomUUID());
            double x = pos.getX() + (level.getRandom().nextDouble() - level.getRandom().nextDouble())
                    * SPAWN_SPREAD + 0.5D;
            double y = pos.getY() + level.getRandom().nextInt(3);
            double z = pos.getZ() + (level.getRandom().nextDouble() - level.getRandom().nextDouble())
                    * SPAWN_SPREAD + 0.5D;
            entity.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
            if (level.addFreshEntity(entity)) {
                ++spawned;
            }
        }
        if (spawned > 0) {
            for (int index = 0; index < 8; index++) {
                level.addParticle(ParticleTypes.EXPLOSION,
                        pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                        0.0D, 0.0D, 0.0D);
            }
        }
        return spawned > 0;
    }

    /** 1.12's drop: whatever had been banked spoils the aura when the block goes. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server && accumulated.visSize() > 0) {
            FluxApi.add(server, getBlockPos(), accumulated.visSize());
        }
    }

    // ------------------------------------------------------------------ EssentiaTransport

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public AspectId suctionType(Direction face) {
        if (!canInputFrom(face)) {
            return null;
        }
        List<AspectId> sorted = missing().aspectsSorted();
        return sorted.isEmpty() ? null : sorted.get(sorted.size() - 1);
    }

    @Override
    public int suctionAmount(Direction face) {
        return suctionType(face) == null ? 0 : SUCTION;
    }

    @Override
    public int takeEssentia(AspectId aspect, int amount, Direction face, EssentiaTransferMode mode) {
        return 0;
    }

    @Override
    public int addEssentia(AspectId aspect, int amount, Direction face, EssentiaTransferMode mode) {
        if (!canInputFrom(face) || aspect == null || amount <= 0) {
            return 0;
        }
        int owed = missing().amount(aspect);
        int accepted = Math.min(owed, amount);
        if (accepted > 0 && mode == EssentiaTransferMode.EXECUTE) {
            accumulated.add(aspect, accepted);
            store();
        }
        return accepted;
    }

    @Override
    public AspectId essentiaType(Direction face) {
        return suctionType(face);
    }

    @Override
    public int essentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int minimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ------------------------------------------------------------------ state

    private void store() {
        CompoundTag tag = getPersistentData();
        writeAspects(tag, KEY_REQUIREMENTS, requirements);
        writeAspects(tag, KEY_ACCUMULATED, accumulated);
        tag.putInt(KEY_COOLDOWN, cooldown);
        setChanged();
        sync();
    }

    private void saveInventory() {
        HCLitePlatform.Holder.get().saveItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
        // The slot change is only visible to the other side once the block entity is pushed;
        // setChanged on its own just marks it for saving.
        setChanged();
        sync();
    }

    private static void writeAspects(CompoundTag tag, String key, AspectList list) {
        if (list == null || list.visSize() == 0) {
            tag.remove(key);
            return;
        }
        List<AspectId> ids = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        for (AspectId id : list.aspectsSortedByAmount()) {
            int amount = list.amount(id);
            if (amount > 0) {
                ids.add(id);
                amounts.add(amount);
            }
        }
        TaAspect.writeAspects(tag, key, ids, amounts);
    }

    private static AspectList readAspects(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return null;
        }
        List<AspectId> ids = TaAspect.readAspects(tag, key);
        List<Integer> amounts = TaAspect.readAspectAmounts(tag, key);
        if (ids.isEmpty()) {
            return null;
        }
        AspectList list = new AspectList();
        for (int i = 0; i < ids.size() && i < amounts.size(); i++) {
            list.add(ids.get(i), amounts.get(i));
        }
        return list.visSize() > 0 ? list : null;
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

    public NonNullList<ItemStack> inventory() {
        return inventory;
    }
}
