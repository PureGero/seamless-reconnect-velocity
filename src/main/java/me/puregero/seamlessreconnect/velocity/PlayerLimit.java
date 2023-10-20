package me.puregero.seamlessreconnect.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLimit {
    private static final ChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("seamlessreconnect:updatemaxplayers");
    private static final String PERMISSION = "sr.bypassfulllimit";
    private final SeamlessReconnectVelocity plugin;
    private final ProxyServer server;
    private int maxPlayers;
    private final Map<UUID, Long> loginTimes = new ConcurrentHashMap<>();

    public PlayerLimit(SeamlessReconnectVelocity seamlessReconnectVelocity, ProxyServer server) {
        this.plugin = seamlessReconnectVelocity;
        this.server = server;

        this.server.getEventManager().register(this.plugin, this);
        this.server.getChannelRegistrar().register(IDENTIFIER);
    }

    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection) || !(event.getTarget() instanceof ConnectedPlayer player)) {
            // We only want to listen to messages from servers
            return;
        }

        if (!event.getIdentifier().equals(IDENTIFIER)) {
            return;
        }

        this.maxPlayers = Integer.parseInt(new String(event.getData(), StandardCharsets.UTF_8));

        if (!"true".equalsIgnoreCase(System.getProperty("sr.disablemaxplayers")) && this.maxPlayers == 0 && !player.hasPermission(PERMISSION) && loginTimes.getOrDefault(player.getUniqueId(), Long.MAX_VALUE) > System.currentTimeMillis() - 30 * 1000) {
            player.disconnect(Component.text("The server is full.", NamedTextColor.RED));
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Subscribe
    public void onPostLogin(LoginEvent event) {
        loginTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());

        int playerCount = server.getPlayerCount();

        if (!"true".equalsIgnoreCase(System.getProperty("sr.disablemaxplayers")) && playerCount >= this.maxPlayers && !event.getPlayer().hasPermission(PERMISSION)) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("The server is full.", NamedTextColor.RED)));
        }
    }

}
