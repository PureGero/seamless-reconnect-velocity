package me.puregero.seamlessreconnect.velocity.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class ConfirmTeleport {

    private final int teleportId;

    public ConfirmTeleport(int teleportId) {
        this.teleportId = teleportId;
    }

    public int teleportId() {
        return teleportId;
    }

    public Object serialize(ByteBuf byteBuf, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeVarInt(byteBuf, getProtocolId(protocolVersion));
        ProtocolUtils.writeVarInt(byteBuf, teleportId);
        return byteBuf;
    }

    private int getProtocolId(ProtocolVersion protocolVersion) {
        return 0x00;
    }
}
