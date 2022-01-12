package me.andyreckt.holiday.player.grant;

import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import lombok.Setter;
import me.andyreckt.holiday.Holiday;
import me.andyreckt.holiday.database.mongo.MongoUtils;
import me.andyreckt.holiday.database.redis.packet.GrantPacket;
import me.andyreckt.holiday.player.rank.Rank;
import me.andyreckt.holiday.utils.TimeUtil;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This Class is from Zowpy
 * All credits to him
 *
 * @author Zowpy
 */
@Getter @Setter
public class Grant {

     private final UUID uuid;
     private Rank rank;

     private UUID user, issuer;
     private boolean active;
     private long duration, executedAt;

    public Grant(UUID uuid) {
        this.uuid = uuid;
    }

    public Grant(UUID user, UUID issuer, Rank rank, long duration) {
        this.uuid = UUID.randomUUID();
        this.user = user;
        this.issuer = issuer;
        this.rank = rank;
        this.duration = duration;
        this.active = true;
        this.executedAt = System.currentTimeMillis();
    }

    public Grant(Document document) {
        this.uuid = UUID.fromString(document.getString("_id"));
        this.user = UUID.fromString(document.getString("user"));
        this.issuer = document.getString("issuer").equalsIgnoreCase("Console") ? null : UUID.fromString(document.getString("issuer"));
        this.rank = Holiday.getInstance().getRankHandler().getFromId(UUID.fromString(document.getString("rank")));
        this.active = document.getBoolean("active");
        this.duration = document.getLong("duration");
        this.executedAt = document.getLong("executedAt");
    }

    public int getPriority() {
        return rank == null ? 0 : rank.getPriority();
    }

    public boolean expired() {
        if(duration == TimeUtil.PERMANENT) return false;
        return (executedAt + duration) <= System.currentTimeMillis();
    }

    public void save() {
        MongoUtils.submitToThread(() -> MongoUtils.getGrantCollection().replaceOne(Filters.eq("_id", uuid.toString()), toBson(), new ReplaceOptions().upsert(true)));
        Holiday.getInstance().getRedis().sendPacket(new GrantPacket(this));
    }

    public Document toBson() {
        return new Document("_id", uuid.toString())
                .append("user", user.toString())
                .append("issuer", issuer == null ? "Console" : issuer.toString())
                .append("rank", rank == null ? "null" : rank.getUuid().toString())
                .append("active", active)
                .append("duration", duration)
                .append("executedAt", executedAt);
    }

    public boolean isActive() {
        if (expired()) setActive(false);
        return !expired() && isActive();
    }


}