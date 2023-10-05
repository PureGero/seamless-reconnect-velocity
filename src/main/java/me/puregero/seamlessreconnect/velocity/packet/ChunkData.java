package me.puregero.seamlessreconnect.velocity.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import me.puregero.seamlessreconnect.velocity.ChunkPos;

public class ChunkData {

    private final ChunkPos pos;

    public ChunkData(ByteBuf byteBuf, ProtocolVersion protocolVersion) {
        this.pos = new ChunkPos(byteBuf.readInt(), byteBuf.readInt());
    }

    public ChunkPos pos() {
        return pos;
    }
}
