package dev.tc4port.hclite.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import dev.tc4port.hclite.HCLite;
import dev.tc4port.hclite.net.HCLiteNet;
import dev.tc4port.hclite.net.HCPacket;
import dev.tc4port.hclite.net.PacketChannel;
import dev.tc4port.hclite.platform.HCLitePlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.common.Mod;

/** Forge binding for HCLite: boots the SimpleChannel-backed {@link PacketChannel}. */
@Mod(HCLite.MOD_ID)
public final class HCLiteForge {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HCLite.MOD_ID, "main"), () -> "1", "1"::equals, "1"::equals);
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    public HCLiteForge() {
        HCLitePlatform.Holder.bootstrap(new HCLitePlatform() {
            @Override
            public CompoundTag saveBlockEntityNbt(BlockEntity be) {
                return be.saveWithFullMetadata();
            }

            @Override
            public void loadBlockEntityNbt(BlockEntity be, CompoundTag tag) {
                // handleUpdateTag is the vanilla client-side BE sync path, public on both loaders.
                be.handleUpdateTag(tag);
            }

            @Override
            public CompoundTag readNbt(FriendlyByteBuf buf) {
                return buf.readAnySizeNbt();
            }
        });

        HCLiteNet.bootstrap(new PacketChannel() {
            @Override
            public <T extends HCPacket> void register(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
                int wireId = NEXT_ID.getAndIncrement();
                if (wireId != id)
                    throw new IllegalStateException("Packet " + type.getSimpleName() + " registered out of order: expected " + wireId + ", got " + id);
                CHANNEL.registerMessage(wireId, type,
                        (msg, buf) -> msg.encode(buf),
                        decoder,
                        (msg, ctx) -> {
                            var networkCtx = ctx.get();
                            networkCtx.enqueueWork(() -> {
                                if (networkCtx.getDirection().getReceptionSide().isServer())
                                    msg.handleServer(networkCtx.getSender());
                                else
                                    msg.handleClient();
                            });
                            networkCtx.setPacketHandled(true);
                        });
            }

            @Override
            public void sendToServer(HCPacket packet) {
                CHANNEL.sendToServer(packet);
            }

            @Override
            public void sendToPlayer(ServerPlayer player, HCPacket packet) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }

            @Override
            public void sendToTracking(BlockEntity be, HCPacket packet) {
                if (be.getLevel() == null)
                    return;
                CHANNEL.send(PacketDistributor.TRACKING_CHUNK
                        .with(() -> be.getLevel().getChunkAt(be.getBlockPos())), packet);
            }
        });
    }
}
