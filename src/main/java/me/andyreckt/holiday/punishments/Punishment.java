package me.andyreckt.holiday.punishments;

import com.mongodb.Block;
import com.mongodb.DBCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import me.andyreckt.holiday.database.redis.Redis;
import me.andyreckt.holiday.database.redis.packets.PunishmentPacket;
import me.andyreckt.holiday.database.mongo.MongoUtils;
import me.andyreckt.holiday.player.Profile;
import me.andyreckt.holiday.utils.TimeUtil;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class Punishment {

    public Punishment(Profile punished, Profile issuer, PunishmentType punishmentType, String reason, Boolean silent) {
        PunishData punishData = new PunishData(punished, punishmentType, issuer, reason, System.currentTimeMillis(), TimeUtil.PERMANENT, silent, punished.getIp());
        Redis.getPidgin().sendPacket(new PunishmentPacket(punishData));
        addPunishment(punishData);
    }

    public Punishment(Profile punished, Profile issuer, PunishmentType punishmentType, String duration, String reason, Boolean silent) {
        PunishData punishData = new PunishData(punished, punishmentType, issuer, reason, System.currentTimeMillis(), TimeUtil.getDuration(duration), silent, punished.getIp());
        Redis.getPidgin().sendPacket(new PunishmentPacket(punishData));
        addPunishment(punishData);
    }

    void addPunishment(PunishData punishData) {
        String punishId = UUID.randomUUID().toString().substring(0, 28);
        Document document = new Document("_id", punishId)
                .append("punished", punishData.getPunished().getUuid().toString())
                .append("addedBy", punishData.getAddedBy().getUuid().toString())
                .append("type", punishData.getType().toString())
                .append("addedReason", punishData.getAddedReason())
                .append("addedAt", punishData.getAddedAt())
                .append("duration", punishData.getDuration())
                .append("silent", punishData.isSilent())
                .append("ip", punishData.getPunished().getIp())
                .append("removed", false)
                .append("removedAt", null)
                .append("removedBy", null)
                .append("removedReason", null);

        MongoUtils.submitToThread(() -> MongoUtils.getPunishmentsCollection().replaceOne(Filters.eq("_id", punishId), document, new ReplaceOptions().upsert(true)));
    }

    public static List<Document> getAllPunishments() {
        List<Document> list = new ArrayList<>();

        MongoUtils.submitToThread(() -> {
            DBCursor cursor = (DBCursor) MongoUtils.getPunishmentsCollection().find();
            while(cursor.hasNext()) {
                list.add((Document) cursor.getQuery());
            }
        });

        return list;
    }

    public static List<Document> getAllPunishments(Player player) {
        List<Document> list = new ArrayList<>();

        MongoUtils.submitToThread(() -> MongoUtils.getPunishmentsCollection()
                .find(Filters.eq("punished", player.getUniqueId().toString()))
                .forEach((Block<Document>) list::add));

        return list;
    }

    public static List<Document> getAllPunishments(UUID uuid) {
        List<Document> list = new ArrayList<>();

        MongoUtils.submitToThread(() -> MongoUtils.getPunishmentsCollection()
                .find(Filters.eq("punished", uuid.toString()))
                .forEach((Block<Document>) list::add));

        return list;
    }

    public static List<Document> getAllPunishments(String ip) {
        List<Document> list = new ArrayList<>();

        MongoUtils.submitToThread(() -> MongoUtils.getPunishmentsCollection()
                .find(Filters.eq("ip", ip))
                .forEach((Block<Document>) list::add));

        return list;
    }

    public static List<Document> getAllPunishments(Profile profile) {
        List<Document> list = new ArrayList<>();

        MongoUtils.submitToThread(() -> MongoUtils.getPunishmentsCollection()
                .find(Filters.eq("punished", profile.getUuid().toString()))
                .forEach((Block<Document>) list::add));

        return list;
    }





}



