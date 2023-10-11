package me.puregero.seamlessreconnect.velocity.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static com.velocitypowered.api.network.ProtocolVersion.*;

public class PacketRegistry {
    private static final Map<ProtocolVersion, Map<Integer, BiFunction<ByteBuf, ProtocolVersion, Object>>> packetRegistry = new HashMap<>();

    static {
        register(UnloadChunk::new,
                map(MINECRAFT_1_20, 0x1E),
                map(MINECRAFT_1_20_2, 0x1F));
        register(ChunkData::new,
                map(MINECRAFT_1_20, 0x24),
                map(MINECRAFT_1_20_2, 0x25));
        register(SynchronizePlayerPosition::new,
                map(MINECRAFT_1_20, 0x3C),
                map(MINECRAFT_1_20_2, 0x3E));
        register(UpdateRecipes::new,
                map(MINECRAFT_1_20, 0x6D),
                map(MINECRAFT_1_20_2, 0x6F));
        register(UpdateTags::new,
                map(MINECRAFT_1_20, 0x6E),
                map(MINECRAFT_1_20_2, 0x70));
    }

    public static Object readPacket(ProtocolVersion protocolVersion, int packetId, ByteBuf byteBuf) {
        Map<Integer, BiFunction<ByteBuf, ProtocolVersion, Object>> versionedRegistry = packetRegistry.get(protocolVersion);

        if (versionedRegistry != null) {
            BiFunction<ByteBuf, ProtocolVersion, Object> factory = versionedRegistry.get(packetId);

            if (factory != null) {
                return factory.apply(byteBuf, protocolVersion);
            }
        }

        return null;
    }

    private static void register(BiFunction<ByteBuf, ProtocolVersion, Object> packetFactory, ProtocolIdVersionMapping... packetIdMappings) {
        for (ProtocolIdVersionMapping mapping : packetIdMappings) {
            packetRegistry.computeIfAbsent(mapping.protocolVersion(), key -> new HashMap<>())
                    .put(mapping.packetId(), packetFactory);
        }
    }

    private static ProtocolIdVersionMapping map(ProtocolVersion protocolVersion, int packedId) {
        return new ProtocolIdVersionMapping(protocolVersion, packedId);
    }


    private static record ProtocolIdVersionMapping(ProtocolVersion protocolVersion, int packetId) {
    }
}
