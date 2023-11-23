package me.puregero.seamlessreconnect.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChat;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdate;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import me.puregero.seamlessreconnect.velocity.packet.ChunkData;
import me.puregero.seamlessreconnect.velocity.packet.ConfirmTeleport;
import me.puregero.seamlessreconnect.velocity.packet.PacketRegistry;
import me.puregero.seamlessreconnect.velocity.packet.SynchronizePlayerPosition;
import me.puregero.seamlessreconnect.velocity.packet.UnloadChunk;
import me.puregero.seamlessreconnect.velocity.packet.UpdateRecipes;
import me.puregero.seamlessreconnect.velocity.packet.UpdateTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

final class PlayerChannelHandler extends ChannelOutboundHandlerAdapter {

    private final SeamlessReconnectVelocity plugin;
    private final Player player;
    private final Logger logger;
    private boolean reconnecting = false;
    private boolean reconnectingConfiguration = false;
    private boolean reconnectingPosition = false;
    private boolean reconnectingUpdateTags = false;
    private boolean reconnectingUpdateRecipes = false;
    private Set<ChunkPos> visibleChunks = new HashSet<>();
    private Set<ChunkPos> reconnectingChunks = new HashSet<>();
    private boolean configurationState = false;

    PlayerChannelHandler(SeamlessReconnectVelocity plugin, Player player, Logger logger) {
        this.plugin = plugin;
        this.player = player;
        this.logger = logger;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        if (packet instanceof ByteBuf byteBuf && !configurationState) {
            try {
                tryDecode(ctx, byteBuf, promise);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        // --- Configuration stuff --- //

        if (packet instanceof FinishedUpdate) {
            configurationState = false;
            if (reconnectingConfiguration) {
                ((ConnectedPlayer) player).getConnection().getChannel().pipeline().fireChannelRead(new FinishedUpdate());
                reconnectingConfiguration = false;
                return;
            }
        }

        if (packet instanceof StartUpdate) {
            configurationState = true;
            if (reconnectingConfiguration) {
                ((ConnectedPlayer) player).getConnection().getChannel().pipeline().fireChannelRead(new FinishedUpdate());
                return;
            }
        }

        if (configurationState && reconnectingConfiguration) {
            return; // Don't need to send configuration packets during a seamless reconnect
        }

        // --- Play stuff --- //

        if (packet instanceof Respawn) {
            visibleChunks.clear();
        }

        if (packet instanceof SystemChat systemChat) {
            String message = PlainTextComponentSerializer.plainText().serialize(systemChat.getComponent());
            if (message.contains("sendto:")) {
                packet = new SystemChat(Component.text("You have been moved to " + message.split("sendto:")[1]).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC), systemChat.getType());
                player.sendActionBar(Component.text("Connected to " + message.split("sendto:")[1]).color(NamedTextColor.GRAY));
            }
        }

        if (reconnecting) {
            if (packet instanceof JoinGame) {
                // First velocity sends a JoinGame packet
                return;
            }
            if (packet instanceof Respawn) {
                // Then velocity sends a Respawn packet
                reconnecting = false;
                return;
            }
        }

        super.write(ctx, packet, promise);
    }

    private void tryDecode(ChannelHandlerContext ctx, ByteBuf byteBuf, ChannelPromise promise) throws Exception {
        int originalReaderIndex = byteBuf.readerIndex();
        int packetId = ProtocolUtils.readVarInt(byteBuf);

        Object packet = PacketRegistry.readPacket(this.player.getProtocolVersion(), packetId, byteBuf);
        boolean shouldWrite = true;

        if (packet instanceof UpdateTags && reconnectingUpdateTags) {
            // This packet causes a lag spike when reconnecting between servers
            shouldWrite = false;
            reconnectingUpdateTags = false;
        }

        if (packet instanceof UpdateRecipes && reconnectingUpdateRecipes) {
            // This packet causes a lag spike when reconnecting between servers
            shouldWrite = false;
            reconnectingUpdateRecipes = false;
        }

        if (packet instanceof SynchronizePlayerPosition synchronizePlayerPosition && reconnectingPosition) {
            // Let's try not to teleport the client during a reconnect
            // Often the server will teleport the client anyway straight afterwards,
            // but occasionally you get a beautiful seamless reconnect with no jittery teleportation :drool:
            player.getCurrentServer().ifPresent(serverConnection -> {
                if (serverConnection instanceof VelocityServerConnection velocityServerConnection && velocityServerConnection.getConnection() != null) {
                    velocityServerConnection.getConnection().write(new ConfirmTeleport(synchronizePlayerPosition.teleportId()).serialize(ctx.alloc().buffer(), this.player.getProtocolVersion()));
                }
            });
            shouldWrite = false;
            reconnectingPosition = false;
        }

        if (packet instanceof UnloadChunk unloadChunk) {
            // Keep track of loaded chunks
            visibleChunks.remove(unloadChunk.pos());
        }

        if (packet instanceof ChunkData chunkData) {
            // Keep track of loaded chunks
            promise = promise.unvoid();
            promise.addListener(future -> {
                if (future.isSuccess()) {
                    // Only mark the chunk as visible if it actually gets sent
                    visibleChunks.add(chunkData.pos());
                }
            });

            if (reconnectingChunks.remove(chunkData.pos())) {
                // Let's not send the player chunks that they already have loaded during a reconnect as this uses a lot of bandwidth and causes a brief period of lag
                shouldWrite = false;
            }
        }

        if (shouldWrite) {
            byteBuf.readerIndex(originalReaderIndex);
            super.write(ctx, byteBuf, promise);
        } else {
            byteBuf.release();
            promise.setSuccess();
        }
    }

    public void startSeamlessReconnect() {
        reconnecting = true;
        reconnectingConfiguration = true;
        reconnectingPosition = true;
        reconnectingUpdateTags = true;
        reconnectingUpdateRecipes = true;
        reconnectingChunks = visibleChunks;
        visibleChunks = new HashSet<>();
    }
}