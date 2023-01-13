package me.andyreckt.holiday.core.util.redis.pubsub.subscribers;

import me.andyreckt.holiday.core.HolidayAPI;
import me.andyreckt.holiday.core.user.rank.RankManager;
import me.andyreckt.holiday.core.util.redis.messaging.IncomingPacketHandler;
import me.andyreckt.holiday.core.util.redis.messaging.PacketListener;
import me.andyreckt.holiday.core.util.redis.pubsub.packets.RankUpdatePacket;

public class RankUpdateSubscriber implements PacketListener {
    @IncomingPacketHandler
    public void onReceive(RankUpdatePacket packet) {
        RankManager rankManager = HolidayAPI.getUnsafeAPI().getRankManager();

        rankManager.getRanks().removeIf(rank -> rank.getName().equals(packet.getRank().getName()));
        if (packet.isDelete()) return;

        rankManager.getRanks().add(packet.getRank());
    }
}
