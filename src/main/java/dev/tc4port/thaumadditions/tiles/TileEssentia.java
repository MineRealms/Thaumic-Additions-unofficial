package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for the essentia machines: one aspect, one amount, and the
 * directional {@link EssentiaTransport} endpoint TC4R tubes and neighbours talk to.
 *
 * <p>The 1.12 machines pulled essentia through TC6's {@code EssentiaHandler} with a
 * hard-coded suction and then pushed it onward; that is exactly what this class
 * exposes, so every machine only has to decide which faces are open and how much
 * suction it advertises.</p>
 */
public abstract class TileEssentia extends SyncedBlockEntity implements EssentiaTransport {
    private final int capacity;
    private final int suction;

    protected TileEssentia(BlockEntityType<?> type, BlockPos pos, BlockState state,
                           int capacity, int suction) {
        super(type, pos, state);
        this.capacity = capacity;
        this.suction = suction;
    }

    /**
     * State lives in the loader's persistent-data tag. 1.20.1 and 1.21.1 disagree on the
     * {@code saveAdditional}/{@code loadAdditional} signatures (1.21 added a
     * {@code HolderLookup.Provider}), so a shared override is impossible; the
     * {@code NeoForgeData} tag is the one persistence hook both loaders keep stable.
     */
    private CompoundTag data() {
        return getPersistentData();
    }

    /** The faces essentia may be inserted through; {@code Direction.values()} by default. */
    protected boolean accepts(Direction face) {
        return true;
    }

    /** The faces essentia may be pulled out through; {@code Direction.values()} by default. */
    protected boolean emits(Direction face) {
        return true;
    }

    public int getCapacity() {
        return capacity;
    }

    public AspectId getAspect() {
        return TaAspect.readAspect(data(), "Aspect");
    }

    public int getAmount() {
        return data().getInt("Amount");
    }

    public void setEssentia(AspectId id, int value) {
        TaAspect.writeAspect(data(), "Aspect", id);
        data().putInt("Amount", Math.max(0, Math.min(capacity, value)));
        setChanged();
        sync();
    }

    /** Adds essentia, returning how much actually fit. */
    public int insert(AspectId id, int value) {
        if (id == null || value <= 0) {
            return 0;
        }
        AspectId current = getAspect();
        if (current == null) {
            TaAspect.writeAspect(data(), "Aspect", id);
        } else if (!current.equals(id)) {
            return 0;
        }
        int amount = getAmount();
        int accepted = Math.min(value, capacity - amount);
        data().putInt("Amount", amount + accepted);
        setChanged();
        sync();
        return accepted;
    }

    /** Removes essentia, returning how much was actually taken. */
    public int extract(AspectId id, int value) {
        AspectId current = getAspect();
        int amount = getAmount();
        if (id == null || current == null || !current.equals(id) || value <= 0) {
            return 0;
        }
        int taken = Math.min(value, amount);
        if (amount - taken == 0) {
            TaAspect.writeAspect(data(), "Aspect", null);
        }
        data().putInt("Amount", amount - taken);
        setChanged();
        sync();
        return taken;
    }

    // ------------------------------------------------------------------ EssentiaTransport

    @Override
    public boolean isConnectable(Direction face) {
        return true;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return accepts(face);
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return emits(face);
    }

    @Override
    public AspectId suctionType(Direction face) {
        return getAmount() < capacity ? getAspect() : null;
    }

    @Override
    public int suctionAmount(Direction face) {
        return suction;
    }

    @Override
    public int takeEssentia(AspectId requested, int value, Direction face, EssentiaTransferMode mode) {
        int taken = extract(requested, value);
        if (mode == EssentiaTransferMode.EXECUTE) {
            return taken;
        }
        // Simulation must not mutate; put it back so the caller's preview matches.
        insert(requested, taken);
        return taken;
    }

    @Override
    public int addEssentia(AspectId requested, int value, Direction face, EssentiaTransferMode mode) {
        int accepted = insert(requested, value);
        if (mode == EssentiaTransferMode.EXECUTE) {
            return accepted;
        }
        extract(requested, accepted);
        return accepted;
    }

    @Override
    public AspectId essentiaType(Direction face) {
        return getAspect();
    }

    @Override
    public int essentiaAmount(Direction face) {
        return getAmount();
    }

    @Override
    public int minimumSuction() {
        return 1;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    /** Reads a stored aspect amount for tooltips and probes. */
    public static int readAmount(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : 0;
    }
}
