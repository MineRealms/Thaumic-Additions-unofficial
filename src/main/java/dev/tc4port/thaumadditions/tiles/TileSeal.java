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
public class TileSeal extends SyncedBlockEntity {
    private static final int SLOTS = 3;

    public TileSeal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AspectId symbol(int slot) {
        return TaAspect.readAspect(getPersistentData(), "Symbol" + slot);
    }

    /** @return the symbol that was replaced, if any */
    public AspectId setSymbol(int slot, AspectId id) {
        AspectId previous = symbol(slot);
        TaAspect.writeAspect(getPersistentData(), "Symbol" + slot, id);
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
