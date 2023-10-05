package me.puregero.seamlessreconnect.velocity.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import me.puregero.seamlessreconnect.velocity.ChunkPos;

public class SynchronizePlayerPosition {

    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final byte flags;
    private final int teleportId;

    public SynchronizePlayerPosition(ByteBuf byteBuf, ProtocolVersion protocolVersion) {
        this.x = byteBuf.readDouble();
        this.y = byteBuf.readDouble();
        this.z = byteBuf.readDouble();
        this.yaw = byteBuf.readFloat();
        this.pitch = byteBuf.readFloat();
        this.flags = byteBuf.readByte();
        this.teleportId = ProtocolUtils.readVarInt(byteBuf);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public byte flags() {
        return flags;
    }

    public int teleportId() {
        return teleportId;
    }
}
