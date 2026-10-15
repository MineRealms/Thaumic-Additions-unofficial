package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileCrystalBlock} / {@code TileCrystalLamp}: one stored aspect.
 *
 * <p>The crystal block was also a TC6 infusion stabiliser and the lamp was a
 * coloured light source; the aspect storage is what the block state and the
 * essentia transport need, so that is what ships here.</p>
 */
public class TileCrystal extends SyncedBlockEntity {
    public TileCrystal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AspectId getAspect() {
        return TaAspect.readAspect(getPersistentData(), "Aspect");
    }

    public void setAspect(AspectId id) {
        TaAspect.writeAspect(getPersistentData(), "Aspect", id);
        setChanged();
        sync();
    }

    /** 1.12 {@code TileArcaneCake}: the aspects of every bite eaten so far. */
    public static class Cake extends SyncedBlockEntity {
        public Cake(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public java.util.List<AspectId> aspects() {
            CompoundTag tag = getPersistentData();
            return TaAspect.readAspects(tag, "Aspects");
        }

        public void setAspects(java.util.List<AspectId> ids) {
            java.util.List<Integer> amounts = new java.util.ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                amounts.add(1);
            }
            TaAspect.writeAspects(getPersistentData(), "Aspects", ids, amounts);
            setChanged();
            sync();
        }

        /** The direction the cake bites from is always up in vanilla; kept for parity. */
        public Direction biteDirection() {
            return Direction.UP;
        }
    }
}
