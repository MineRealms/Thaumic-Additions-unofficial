package dev.tc4port.hclite.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import dev.tc4port.hclite.HCLite;
import dev.tc4port.hclite.net.HCLiteNet;
import dev.tc4port.hclite.net.HCLitePackets;
import dev.tc4port.hclite.net.HCPacket;
import dev.tc4port.hclite.net.PacketChannel;
import dev.tc4port.hclite.platform.HCLitePlatform;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge binding for HCLite: one envelope payload ({@code hclite:main}) carries the numeric
 * packet id plus the encoded bytes, mirroring the Forge node's SimpleChannel ids.
 */
@Mod(HCLite.MOD_ID)
public final class HCLiteNeoForge {
    private static final Map<Class<?>, Integer> PACKET_IDS = new HashMap<>();
    private static final Map<Integer, Function<FriendlyByteBuf, ? extends HCPacket>> DECODERS = new HashMap<>();

    public record Envelope(int id, byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Envelope> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HCLite.MOD_ID, "main"));
        public static final StreamCodec<FriendlyByteBuf, Envelope> CODEC = StreamCodec.of(
                Envelope::encode, Envelope::decode);

        private static void encode(FriendlyByteBuf buf, Envelope envelope) {
            buf.writeVarInt(envelope.id);
            buf.writeByteArray(envelope.data);
        }

        private static Envelope decode(FriendlyByteBuf buf) {
            return new Envelope(buf.readVarInt(), buf.readByteArray());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public HCLiteNeoForge(IEventBus modBus) {
        HCLitePlatform.Holder.bootstrap(new HCLitePlatform() {
            @Override
            public CompoundTag saveBlockEntityNbt(BlockEntity be) {
                return be.saveWithFullMetadata(be.getLevel().registryAccess());
            }

            @Override
            public void loadBlockEntityNbt(BlockEntity be, CompoundTag tag) {
                // handleUpdateTag is the vanilla client-side BE sync path, public on both loaders.
                be.handleUpdateTag(tag, be.getLevel() != null ? be.getLevel().registryAccess() : null);
            }

            @Override
            public CompoundTag readNbt(FriendlyByteBuf buf) {
                return buf.readNbt();
            }

            @Override
            public CompoundTag itemTag(net.minecraft.world.item.ItemStack stack) {
                net.minecraft.world.item.component.CustomData data =
                        stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                return data == null ? null : data.copyTag();
            }

            @Override
            public void updateItemTag(net.minecraft.world.item.ItemStack stack,
                                      java.util.function.Consumer<CompoundTag> mutator) {
                net.minecraft.world.item.component.CustomData.update(
                        net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, mutator);
            }
        });

        HCLiteNet.bootstrap(new PacketChannel() {
            @Override
            public <T extends HCPacket> void register(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
                PACKET_IDS.put(type, id);
                DECODERS.put(id, decoder);
            }

            @Override
            public void sendToServer(HCPacket packet) {
                PacketDistributor.sendToServer(envelope(packet));
            }

            @Override
            public void sendToPlayer(ServerPlayer player, HCPacket packet) {
                PacketDistributor.sendToPlayer(player, envelope(packet));
            }

            @Override
            public void sendToTracking(BlockEntity be, HCPacket packet) {
                if (be.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                    PacketDistributor.sendToPlayersTrackingChunk(level,
                            new ChunkPos(be.getBlockPos()), envelope(packet));
            }
        });

        // The channel only knows the packets registered here; SyncedBlockEntity.sync() throws
        // "Invalid message" without this.
        HCLitePackets.registerAll();

        modBus.addListener(HCLiteNeoForge::registerPayloads);
    }

    private static Envelope envelope(HCPacket packet) {
        Integer id = PACKET_IDS.get(packet.getClass());
        if (id == null)
            throw new IllegalStateException("Packet not registered: " + packet.getClass().getName());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buf);
            return new Envelope(id, buf.array());
        } finally {
            buf.release();
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // One Type may only be registered once; playBidirectional covers both flows.
        // SERVERBOUND flow = received on the server.
        registrar.playBidirectional(Envelope.TYPE, Envelope.CODEC, (payload, context) -> context.enqueueWork(() -> {
            HCPacket packet = decode(payload);
            if (packet == null)
                return;
            if (context.flow() == net.minecraft.network.protocol.PacketFlow.SERVERBOUND)
                packet.handleServer((ServerPlayer) context.player());
            else
                packet.handleClient();
        }));
    }

    private static HCPacket decode(Envelope envelope) {
        Function<FriendlyByteBuf, ? extends HCPacket> decoder = DECODERS.get(envelope.id());
        if (decoder == null)
            return null;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(envelope.data()));
        try {
            return decoder.apply(buf);
        } finally {
            buf.release();
        }
    }
}
