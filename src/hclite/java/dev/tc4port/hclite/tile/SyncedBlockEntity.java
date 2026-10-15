package dev.tc4port.hclite.tile;

import dev.tc4port.hclite.net.HCLiteNet;
import dev.tc4port.hclite.net.SyncBlockEntityPacket;
import dev.tc4port.hclite.platform.HCLitePlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * HCLite replacement for HammerCore's {@code TileSyncable}: a block entity that can push a
 * full-NBT resync to the clients tracking it. Machines extend this and call {@link #sync()}
 * after changing client-relevant state.
 */
public abstract class SyncedBlockEntity extends BlockEntity {
    protected SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Server side: resync this block entity to tracking clients. */
    public void sync() {
        Level level = getLevel();
        if (level != null && !level.isClientSide)
            HCLiteNet.channel().sendToTracking(this, new SyncBlockEntityPacket(this));
    }

    /**
     * Client side: re-applies a server-provided NBT snapshot. The vanilla loader visibility
     * and mapping drift are wrapped by the platform binding.
     */
    public final void reloadFromNbt(CompoundTag tag) {
        HCLitePlatform.Holder.get().loadBlockEntityNbt(this, tag);
        setChanged();
    }
}
