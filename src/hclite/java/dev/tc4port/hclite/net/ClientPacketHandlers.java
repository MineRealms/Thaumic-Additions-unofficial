package dev.tc4port.hclite.net;

import dev.tc4port.hclite.tile.SyncedBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Client-only packet work. Loaded lazily from handleClient paths, never on the server. */
final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    static void resyncBlockEntity(BlockPos pos, CompoundTag tag) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SyncedBlockEntity synced)
            synced.reloadFromNbt(tag);
        level.sendBlockUpdated(pos, be != null ? be.getBlockState() : level.getBlockState(pos),
                level.getBlockState(pos), 2);
    }
}
