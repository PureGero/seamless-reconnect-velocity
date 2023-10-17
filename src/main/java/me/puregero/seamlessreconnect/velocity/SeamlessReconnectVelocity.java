package me.puregero.seamlessreconnect.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

@Plugin(id = "seamless-reconnect-velocity", name = "SeamlessReconnectVelocity", version = "1.0.0", authors = {"PureGero"}, url = "https://github.com/PureGero/seamless-reconnect-velocity")
public class SeamlessReconnectVelocity {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public SeamlessReconnectVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        logger.info("Loaded");
    }

    public Logger getLogger() {
        return logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        new InitialServerFinder(this, server);
        new PlayerLimit(this, server);

        logger.info("Initialised");
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        event.getServerKickReason().ifPresent(component -> {
            // Convert the component to a readable string
            String message = PlainTextComponentSerializer.plainText().serialize(component);
            if (message.startsWith("sendto:")) {
                ((PlayerChannelHandler) ((ConnectedPlayer) event.getPlayer())
                        .getConnection()
                        .getChannel()
                        .pipeline()
                        .get("seamlessreconnect")).startSeamlessReconnect();
                String serverName = message.substring("sendto:".length());
                server.getServer(serverName).ifPresentOrElse(
                        registeredServer ->
                                event.setResult(KickedFromServerEvent.RedirectPlayer.create(registeredServer)),
                        () -> System.out.println("Could not find server to sendto " + serverName));
            }
        });
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        ((ConnectedPlayer) event.getPlayer())
                .getConnection()
                .getChannel()
                .pipeline()
                .addBefore(Connections.HANDLER, "seamlessreconnect", new PlayerChannelHandler(this, event.getPlayer(), logger));
    }

}
