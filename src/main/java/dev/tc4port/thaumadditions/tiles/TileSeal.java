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

    /** Where this seal's other half is, for the seal that pairs with one. */
    public BlockPos portalTarget() {
        if (!getPersistentData().contains("PortalTarget")) {
            return null;
        }
        long packed = getPersistentData().getLong("PortalTarget");
        return BlockPos.of(packed);
    }

    /** The resolved behaviour, or {@code null} while the symbols mean nothing. */
    public dev.tc4port.thaumadditions.seals.SealInstance instance() {
        if (instance == null) {
            instance = dev.tc4port.thaumadditions.seals.SealManager.makeInstance(this);
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
    public void setRemoved() {
        if (instance != null) {
            instance.onSealBreak();
        }
        super.setRemoved();
    }

    @Override
    public void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        // The symbols can change under the machine, so the behaviour is re-resolved whenever they do.
        if (instance == null) {
            instance = dev.tc4port.thaumadditions.seals.SealManager.makeInstance(this);
        }
        if (instance != null) {
            instance.step();
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
