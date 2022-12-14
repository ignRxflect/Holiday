package me.andyreckt.holiday.core.server;

import lombok.Getter;
import me.andyreckt.holiday.api.server.IServer;
import me.andyreckt.holiday.core.HolidayAPI;
import me.andyreckt.holiday.core.util.redis.pubsub.packets.ServerKeepAlivePacket;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ServerManager {

    private final HolidayAPI api;

    @Getter
    private final HashMap<String, IServer> servers;

    public ServerManager(HolidayAPI api) {
        this.api = api;
        this.servers = new HashMap<>();
        this.load();
    }

    private void load() {
        api.getMidnight().getAllAsync("servers", Server.class).whenComplete((o, t) -> {
            if (t != null) {
                t.printStackTrace();
                return;
            }
            servers.putAll(o);
        });
    }


    public IServer getServer(String serverId) {
        return this.servers.get(serverId);
    }

    public IServer getServer(UUID playerId) {
        return this.getServer(api.getOnlinePlayers().get(playerId));
    }

    public void keepAlive(Server server) {
        api.getMidnight().cache("servers", server.getServerId(), server);
        new ServerKeepAlivePacket(server);
    }
}
