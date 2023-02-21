package me.andyreckt.holiday.bukkit.commands;

import lombok.SneakyThrows;
import me.andyreckt.holiday.api.server.IServer;
import me.andyreckt.holiday.bukkit.Holiday;
import me.andyreckt.holiday.bukkit.server.menu.server.ServerListMenu;
import me.andyreckt.holiday.bukkit.server.redis.packet.CrossServerCommandPacket;
import me.andyreckt.holiday.bukkit.user.UserConstants;
import me.andyreckt.holiday.bukkit.util.Logger;
import me.andyreckt.holiday.bukkit.util.files.Locale;
import me.andyreckt.holiday.bukkit.util.files.Perms;
import me.andyreckt.holiday.bukkit.util.sunset.annotations.Command;
import me.andyreckt.holiday.bukkit.util.sunset.annotations.MainCommand;
import me.andyreckt.holiday.bukkit.util.sunset.annotations.Param;
import me.andyreckt.holiday.bukkit.util.sunset.annotations.SubCommand;
import me.andyreckt.holiday.bukkit.util.text.CC;
import me.andyreckt.holiday.core.server.Server;
import me.andyreckt.holiday.core.util.enums.AlertType;
import me.andyreckt.holiday.core.util.json.GsonProvider;
import me.andyreckt.holiday.core.util.redis.messaging.PacketHandler;
import me.andyreckt.holiday.core.util.redis.pubsub.packets.BroadcastPacket;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

@MainCommand(names = {"servermanager", "sm"}, permission = Perms.SERVERMANAGER, description = "Server Manager")
public class ServerManagerCommand {

    @SneakyThrows
    @SubCommand(names = {"command", "runcommand", "cmd"}, description = "Run a command on a me.andyreckt.holiday.server, or all the servers.", async = true)
    public void runCmd(CommandSender sender, @Param(name = "me.andyreckt.holiday.server") String serverid, @Param(name = "command", wildcard = true) String command) {
        if (!serverid.equalsIgnoreCase("ALL")) {
            IServer server = Holiday.getInstance().getApi().getServer(serverid);
            if (server == null) {
                sender.sendMessage(Locale.SERVER_NOT_FOUND.getString());
                return;
            }
            String toSend = Locale.STAFF_SERVER_MANAGER_RUN_SERVER.getString()
                    .replace("%me.andyreckt.holiday.server%", Holiday.getInstance().getThisServer().getServerName())
                    .replace("%serverid%", server.getServerId())
                    .replace("%executor%", sender instanceof ConsoleCommandSender ? "Console" : UserConstants.getNameWithColor(Holiday.getInstance().getApi().getProfile(((Player) sender).getUniqueId())))
                    .replace("%command%", command);
            PacketHandler.send(
                    new BroadcastPacket(toSend, Perms.ADMIN_VIEW_NOTIFICATIONS.get(), AlertType.SERVER_MANAGER));
            sender.sendMessage(Locale.PLAYER_SERVER_MANAGER_RUN_SERVER.getString()
                    .replace("%me.andyreckt.holiday.server%", server.getServerName())
                    .replace("%command%", command));
            PacketHandler.send(new CrossServerCommandPacket(command, server.getServerId()));
        } else {
            String toSend = Locale.STAFF_SERVER_MANAGER_RUN_ALL.getString()
                    .replace("%me.andyreckt.holiday.server%", Holiday.getInstance().getThisServer().getServerName())
                    .replace("%executor%", sender instanceof ConsoleCommandSender ? "Console" : UserConstants.getNameWithColor(Holiday.getInstance().getApi().getProfile(((Player) sender).getUniqueId())))
                    .replace("%command%", command);
            PacketHandler.send(
                    new BroadcastPacket(toSend, Perms.ADMIN_VIEW_NOTIFICATIONS.get(), AlertType.SERVER_MANAGER));
            sender.sendMessage(Locale.PLAYER_SERVER_MANAGER_RUN_ALL.getString()
                    .replace("%command%", command));
            PacketHandler.send(new CrossServerCommandPacket(command, "ALL"));

        }
    }

    @SubCommand(names = {"info", "status"}, description = "Get information about a me.andyreckt.holiday.server.", async = true)
    public void info(CommandSender sender, @Param(name = "me.andyreckt.holiday.server") String serverid) {
        IServer server = Holiday.getInstance().getApi().getServer(serverid);
        if (server == null || !server.isOnline()) {
            sender.sendMessage(Locale.SERVER_NOT_FOUND.getString());
            return;
        }

        String status = server.isWhitelisted() ? CC.CHAT + "Whitelisted" : CC.GREEN + "Online";

        StringBuilder sb = new StringBuilder(" ");
        for (double tps : server.getTps()) {
            sb.append(CC.formatTps(tps));
            sb.append(", ");
        }
        String tps = sb.substring(0, sb.length() - 2);

        Locale.PLAYER_SERVER_MANAGER_INFO.getStringList().forEach(s -> {
            s = s.replace("%name%", server.getServerName())
                    .replace("%id%", server.getServerId())
                    .replace("%status%", status)
                    .replace("%players%", server.getPlayerCount() + "")
                    .replace("%maxplayers%", server.getMaxPlayers() + "")
                    .replace("%tps%", tps);
            sender.sendMessage(CC.translate(s));
        });
    }

    @Command(names = {"serverlist", "serverlistgui", "slgui", "servers"}, description = "Open the me.andyreckt.holiday.server list gui.", permission = Perms.SERVERMANAGER)
    @SubCommand(names = {"list", "servers"}, description = "Get a list of all the servers.")
    public void list(Player sender) {
        new ServerListMenu(Holiday.getInstance().getApi().getServers().values()).openMenu(sender);
    }




}
