package me.andyreckt.holiday.database.redis.subscribers;

import me.andyreckt.holiday.Holiday;
import me.andyreckt.holiday.database.redis.packet.BroadcastPacket;
import me.andyreckt.holiday.player.Profile;
import me.andyreckt.holiday.player.ProfileHandler;
import me.andyreckt.holiday.utils.CC;
import me.andyreckt.holiday.utils.packets.handler.IncomingPacketHandler;
import me.andyreckt.holiday.utils.packets.listener.PacketListener;
import org.bukkit.Bukkit;

public class BroadcastSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onBroadcast(BroadcastPacket packet) {
        ProfileHandler ph = Holiday.getInstance().getProfileHandler();
        switch (packet.getType()){
            case ALL: {
                Bukkit.broadcastMessage(CC.translate(packet.getMessage()));
                break;
            }
            case STAFF: {
                ph.getOnlineProfiles().forEach(profile -> {
                    if(profile.isStaff() && profile.getPlayer() != null) profile.getPlayer().sendMessage(CC.translate(packet.getMessage()));
                });
                break;
            }
            case ADMIN: {
                ph.getOnlineProfiles().forEach(profile -> {
                    if(profile.isAdmin() && profile.getPlayer() != null) profile.getPlayer().sendMessage(CC.translate(packet.getMessage()));
                });
                break;
            }
            case OP: {
                ph.getOnlineProfiles().forEach(profile -> {
                    if(profile.isOp() && profile.getPlayer() != null) profile.getPlayer().sendMessage(CC.translate(packet.getMessage()));
                });
                break;
            }
        }
    }
}
