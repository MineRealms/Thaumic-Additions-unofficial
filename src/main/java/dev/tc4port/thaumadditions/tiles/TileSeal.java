package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.12 {@code TileSeal}: three symbol slots plus the owner, and the combination
 * lookup that resolves them into a {@code SealInstance}.
 *
 * <p>The seal subsystem (earth/life/magic/tool/water/woid behaviours) is a
 * self-contained 8-class tree with no TC6 dependency, so it is the cheapest of
 * the remaining subsystems to port. This tile carries the data it needs.</p>
 */
public class TileSeal extends SyncedBlockEntity implements TaTickables.Ticking {
    /** 1.12's seal had three symbol slots. */
    public static final int SLOTS = 3;

    /** The behaviour the three symbols mean, or {@code null} while they mean nothing. */
    private dev.tc4port.thaumadditions.seals.SealInstance instance;

    /**
     * Every seal that is loaded, on either side.
     *
     * <p>The portal window needs to know where the seals are, once a frame, before the level's block
     * entities are drawn — and at that moment there is no block entity renderer to be handed one.
     * Keeping a set is far cheaper than the alternative, which is asking every block entity in the
     * chunk whether it happens to be a seal.</p>
     */
    private static final java.util.Set<TileSeal> LOADED =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /** Every loaded seal; the portal window reads this and nothing else should. */
    public static java.util.Set<TileSeal> loaded() {
        return LOADED;
    }

    public TileSeal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The face the seal points out of. */
    public net.minecraft.core.Direction orientation() {
        BlockState state = getBlockState();
        return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                : net.minecraft.core.Direction.UP;
    }

    /**
     * Where the portal behind this seal leads, or {@code null} when it is not one or not bound.
     *
     * <p>Asked of the seal rather than of any one behaviour, so the renderer does not have to know
     * that portals are the only seal with a destination.</p>
     */
    public BlockPos portalBound() {
        var behaviour = instance();
        return behaviour == null ? null : behaviour.portalBound();
    }

    /**
     * How far open the portal's hole is heading: 0 shut, up to {@code 5} fully open.
     *
     * <p>It lives on the tile rather than in the behaviour because it is the one piece of a seal's
     * state the client needs while the behaviour is being resolved again — and because the sync
     * packet carries the tile's data, not the behaviour's.</p>
     */
    public int holeTarget() {
        return getPersistentData().getInt("HoleTarget");
    }

    /** Stores the hole's target and pushes it to clients when it moved. */
    public void setHoleTarget(int target) {
        if (holeTarget() == target) {
            return;
        }
        getPersistentData().putInt("HoleTarget", target);
        setChanged();
        sync();
    }

    /** The resolved behaviour, or {@code null} while the symbols mean nothing. */
    public dev.tc4port.thaumadditions.seals.SealInstance instance() {
        if (instance == null) {
            instance = dev.tc4port.thaumadditions.seals.SealManager.makeInstance(this);
            if (instance != null) {
                // The behaviour keeps its own state beside the symbols; see SealInstance.loadState.
                instance.loadState();
            }
        }
        return instance;
    }

    /** 1.12's {@code Block#onEntityCollision}; the portal seal teleports on it. */
    public void onEntityInside(net.minecraft.world.level.Level level,
                               net.minecraft.world.entity.Entity entity) {
        var behaviour = instance();
        if (behaviour != null) {
            behaviour.onEntityCollisionWithSeal(level, getBlockPos(), entity);
        }
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (level != null) {
            LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        if (instance != null) {
            instance.onSealBreak();
        }
        super.setRemoved();
    }

    @Override
    public void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        // The symbols can change under the machine, so the behaviour is re-resolved whenever they do.
        var behaviour = instance();
        if (behaviour != null) {
            behaviour.step();
        }
    }

    public AspectId symbol(int slot) {
        return TaAspect.readAspect(getPersistentData(), "Symbol" + slot);
    }

    /** @return the symbol that was replaced, if any */
    public AspectId setSymbol(int slot, AspectId id) {
        AspectId previous = symbol(slot);
        TaAspect.writeAspect(getPersistentData(), "Symbol" + slot, id);
        // A different symbol means a different seal, so the resolved behaviour is thrown away.
        this.instance = null;
        setChanged();
        sync();
        return previous;
    }

    public boolean isComplete() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (symbol(slot) == null) {
                return false;
            }
        }
        return true;
    }

    public UUID getOwner() {
        return getPersistentData().hasUUID("Owner") ? getPersistentData().getUUID("Owner") : null;
    }

    public void setOwner(UUID id) {
        if (id == null) {
            getPersistentData().remove("Owner");
        } else {
            getPersistentData().putUUID("Owner", id);
        }
        setChanged();
        sync();
    }
}
