package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.essentia.EssentiaApi;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
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
public abstract class TileEssentia extends SyncedBlockEntity implements EssentiaTransport, TaTickables.Ticking {
    private final int capacity;
    private final int suction;

    protected TileEssentia(BlockEntityType<?> type, BlockPos pos, BlockState state,
                           int capacity, int suction) {
        super(type, pos, state);
        this.capacity = capacity;
        this.suction = suction;
    }

    /**
     * 1.12 drove every machine from {@code TileEntity.update()}; 1.20.1 only ticks a type that was
     * registered with a ticker (see {@link TaTickables}). Machines override {@link #machineTick}.
     */
    @Override
    public final void tick(Level level, BlockPos pos, BlockState state) {
        machineTick(level, pos, state);
    }

    /** Per-tick work. Runs on both sides unless the override says otherwise. */
    protected void machineTick(Level level, BlockPos pos, BlockState state) {
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

    // ------------------------------------------------------------------ pulling

    /**
     * 1.12's {@code EssentiaHandler.drainEssentia(this, aspect, null, suction, amount)}: walk the
     * six neighbours, ask each one whether it offers this aspect out of the face that meets this
     * block, and take the first that does.
     *
     * <p>This is deliberately a directional walk rather than {@code EssentiaApi.extract}'s range
     * search. TC4R keeps {@code EssentiaSource} — the interface a range search looks for — separate
     * from {@code EssentiaTransport}, so that a tube endpoint does not become remotely drainable
     * just because it moves essentia. Jars, alembics and tubes are all transports, so a machine
     * that wants to drink from one has to reach across the face that touches it, which is what 1.12
     * did anyway.</p>
     *
     * @return how much was actually drawn; {@code 0} when nothing in reach offers it.
     */
    public static int pullFromNeighbours(Level level, BlockPos pos, AspectId aspect, int amount) {
        if (level.isClientSide || aspect == null || amount <= 0) {
            return 0;
        }
        for (Direction face : Direction.values()) {
            EssentiaTransport neighbour = EssentiaApi.transport(level, pos.relative(face), face.getOpposite());
            if (neighbour == null || !neighbour.canOutputTo(face.getOpposite())) {
                continue;
            }
            if (!aspect.equals(neighbour.essentiaType(face.getOpposite()))) {
                continue;
            }
            int taken = EssentiaApi.take(level, neighbour, aspect, amount, face.getOpposite(),
                    EssentiaTransferMode.EXECUTE);
            if (taken > 0) {
                return taken;
            }
        }
        return 0;
    }
}
