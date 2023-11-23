package me.puregero.seamlessreconnect.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

public class PlayerLimit {
    private final static String PREFIX = "seamlessreconnect.maxplayers.";
    private static final String PERMISSION = "sr.bypassfulllimit";
    private final SeamlessReconnectVelocity plugin;
    private final ProxyServer server;

    public PlayerLimit(SeamlessReconnectVelocity seamlessReconnectVelocity, ProxyServer server) {
        this.plugin = seamlessReconnectVelocity;
        this.server = server;

        this.server.getEventManager().register(this.plugin, this);
    }

    @Subscribe
    public void onPostLogin(LoginEvent event) {
        int playerCount = server.getPlayerCount();

        if ("true".equalsIgnoreCase(System.getProperty("sr.disablemaxplayers"))) {
            return;
        }

        if (event.getPlayer().hasPermission(PERMISSION)) {
            return;
        }

        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
        } catch (ClassNotFoundException e) {
            return;
        }

        LuckPerms api = LuckPermsProvider.get();

        User user = api.getUserManager().getUser(event.getPlayer().getUniqueId());

        if (user == null) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("Your LuckPerms user data is not loaded.", NamedTextColor.RED)));
            return;
        }

        int maxPlayers = user.resolveInheritedNodes(QueryOptions.nonContextual()).stream()
                .filter(node -> node.getKey().startsWith(PREFIX))
                .map(node -> {
                    try {
                        return Integer.parseInt(node.getKey().substring(PREFIX.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(0);

        if (playerCount >= maxPlayers) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("The server is full.", NamedTextColor.RED)));
        }
    }

}
