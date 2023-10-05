package me.puregero.seamlessreconnect.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InitialServerFinder {
    private static final ChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("seamlessreconnect:initialserverquery");

    private final Map<UUID, CompletableFuture<String>> responseListeners = new ConcurrentHashMap<>();
    private final SeamlessReconnectVelocity plugin;
    private final ProxyServer server;

    public InitialServerFinder(SeamlessReconnectVelocity plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;

        this.server.getEventManager().register(this.plugin, this);
        this.server.getChannelRegistrar().register(IDENTIFIER);
    }

    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection)) {
            // We only want to listen to messages from servers
            return;
        }

        if (!event.getIdentifier().equals(IDENTIFIER)) {
            return;
        }

        String data = new String(event.getData(), StandardCharsets.UTF_8);
        String[] parts = data.split("\t");
        UUID uuid = UUID.fromString(parts[0]);
        String serverName = parts[1];

        responseListeners.computeIfPresent(uuid, (key, future) -> {
            future.complete(serverName);
            return null;
        });

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Subscribe
    public void onKick(PlayerChooseInitialServerEvent event) {
        List<RegisteredServer> servers = new ArrayList<>(server.getAllServers());
        servers.removeIf(server -> server.getPlayersConnected().isEmpty());

        if (servers.isEmpty()) {
            return;
        }

        RegisteredServer server = servers.get((int) (Math.random() * servers.size()));

        CompletableFuture<String> future = new CompletableFuture<>();
        responseListeners.put(event.getPlayer().getUniqueId(), future);

        boolean wasSent = server.sendPluginMessage(IDENTIFIER, event.getPlayer().getUniqueId().toString().getBytes(StandardCharsets.UTF_8));

        if (!wasSent) {
            return;
        }

        String serverName = future.completeOnTimeout(null, 3, TimeUnit.SECONDS).join(); // Short delay so that we don't block this pooled thread too long (what happens if we end up accidentally blocking every thread?)
        RegisteredServer serverToSendTo = Optional.ofNullable(serverName).flatMap(this.server::getServer).orElse(null);

        if (serverToSendTo == null) {
            event.getPlayer().disconnect(Component.text("Could not find a server to send you to.\nAre the servers under heavy load?").color(NamedTextColor.RED));
            event.setInitialServer(null);
            return;
        }

        event.setInitialServer(serverToSendTo);
    }
}
