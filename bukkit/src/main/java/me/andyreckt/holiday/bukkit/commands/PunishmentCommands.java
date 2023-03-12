package me.andyreckt.holiday.bukkit.commands;

import me.andyreckt.holiday.api.API;
import me.andyreckt.holiday.api.user.IPunishment;
import me.andyreckt.holiday.api.user.Profile;
import me.andyreckt.holiday.bukkit.Holiday;
import me.andyreckt.holiday.bukkit.server.redis.packet.KickPlayerPacket;
import me.andyreckt.holiday.bukkit.user.UserConstants;
import me.andyreckt.holiday.bukkit.util.files.Locale;
import me.andyreckt.holiday.bukkit.util.files.Perms;
import me.andyreckt.holiday.bukkit.util.sunset.annotations.Command;
import me.andyreckt.holiday.bukkit.util.sunset.annotations.Flag;
import me.andyreckt.holiday.bukkit.util.sunset.annotations.Param;
import me.andyreckt.holiday.core.user.UserProfile;
import me.andyreckt.holiday.core.user.punishment.Punishment;
import me.andyreckt.holiday.core.util.duration.Duration;
import me.andyreckt.holiday.core.util.duration.TimeUtil;
import me.andyreckt.holiday.core.util.enums.AlertType;
import me.andyreckt.holiday.core.util.redis.messaging.PacketHandler;
import me.andyreckt.holiday.core.util.redis.pubsub.packets.BroadcastPacket;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class PunishmentCommands {

    @Command(names = {"ban", "b"}, async = true, permission = Perms.BAN)
    public void ban(CommandSender sender,
                           @Flag(name = "silent", identifier = 's') boolean silent,
                           @Param(name = "name") Profile target,
                           @Param(name = "reason", wildcard = true, baseValue = "Cheating") String reason) {
        API api = Holiday.getInstance().getApi();
        Profile profile = sender instanceof Player ? api.getProfile(((Player) sender).getUniqueId()) : UserProfile.getConsoleProfile();

        punish(profile, target, IPunishment.PunishmentType.BAN, Duration.PERMANENT, reason, silent, sender);
    }

    @Command(names = {"blacklist", "bl"}, async = true, permission = Perms.BLACKLIST)
    public void blacklist(CommandSender sender,
                                 @Flag(name = "silent", identifier = 's') boolean silent,
                                 @Param(name = "name") Profile target,
                                 @Param(name = "reason", wildcard = true, baseValue = "Cheating") String reason) {
        API api = Holiday.getInstance().getApi();
        Profile profile = sender instanceof Player ? api.getProfile(((Player) sender).getUniqueId()) : UserProfile.getConsoleProfile();

        punish(profile, target, IPunishment.PunishmentType.BLACKLIST, Duration.PERMANENT, reason, silent, sender);
    }

    @Command(names = {"ipban", "ipb", "banip", "ban-ip"}, async = true, permission = Perms.IPBAN)
    public void ipban(CommandSender sender,
                             @Flag(name = "silent", identifier = 's') boolean silent,
                             @Param(name = "name") Profile target,
                             @Param(name = "reason", wildcard = true, baseValue = "Cheating") String reason) {
        API api = Holiday.getInstance().getApi();
        Profile profile = sender instanceof Player ? api.getProfile(((Player) sender).getUniqueId()) : UserProfile.getConsoleProfile();

        punish(profile, target, IPunishment.PunishmentType.IP_BAN, Duration.PERMANENT, reason, silent, sender);
    }

    @Command(names = {"tempban", "tban", "tb"}, async = true, permission = Perms.TEMPBAN)
    public void tempban(CommandSender sender,
                               @Flag(name = "silent", identifier = 's') boolean silent,
                               @Param(name = "name") Profile target,
                               @Param(name = "time") Duration duration,
                               @Param(name = "reason", wildcard = true, baseValue = "Cheating") String reason) {
        API api = Holiday.getInstance().getApi();
        Profile profile = sender instanceof Player ? api.getProfile(((Player) sender).getUniqueId()) : UserProfile.getConsoleProfile();

        punish(profile, target, IPunishment.PunishmentType.BAN, duration, reason, silent, sender);
    }

    @Command(names = {"mute"}, async = true, permission = Perms.MUTE)
    public void mute(CommandSender sender,
                            @Flag(name = "silent", identifier = 's') boolean silent,
                            @Param(name = "name") Profile target,
                            @Param(name = "reason", wildcard = true, baseValue = "Cheating") String reason) {
        API api = Holiday.getInstance().getApi();
        Profile profile = sender instanceof Player ? api.getProfile(((Player) sender).getUniqueId()) : UserProfile.getConsoleProfile();

        punish(profile, target, IPunishment.PunishmentType.MUTE, Duration.PERMANENT, reason, silent, sender);
    }

    @Command(names = {"tempmute", "tmute"}, async = true, permission = Perms.TEMPMUTE)
    public void tempmute(CommandSender sender,
                                @Flag(name = "silent", identifier = 's') boolean silent,
                                @Param(name = "name") Profile target,
                                @Param(name = "time") Duration duration,
                                @Param(name = "reason", wildcard = true, baseValue = "Cheating") String reason) {
        API api = Holiday.getInstance().getApi();
        Profile profile = sender instanceof Player ? api.getProfile(((Player) sender).getUniqueId()) : UserProfile.getConsoleProfile();

        punish(profile, target, IPunishment.PunishmentType.MUTE, duration, reason, silent, sender);
    }

    @Command(names = {"kick"}, async = true, permission = Perms.KICK)
    public void kick(CommandSender sender,
                     @Flag(name = "silent", identifier = 's') boolean silent,
                     @Param(name = "name") Profile target,
                     @Param(name = "reason", wildcard = true, baseValue = "Misconduct") String reason) {
        API api = Holiday.getInstance().getApi();
        Profile profile = sender instanceof Player ? api.getProfile(((Player) sender).getUniqueId()) : UserProfile.getConsoleProfile();

        String issuerName = UserConstants.getNameWithColor(profile);
        String targetName = UserConstants.getDisplayNameWithColor(target);

        String kickBroadcast = Locale.PUNISHMENT_KICK_MESSAGE.getString();


        kickBroadcast = kickBroadcast.replace("%executor%", issuerName)
                .replace("%player%", targetName)
                .replace("%silent%", silent ? Locale.PUNISHMENT_SILENT_PREFIX.getString() : "")
                .replace("%reason%", reason);

        if (!silent) {
            PacketHandler.send(new BroadcastPacket(kickBroadcast));
        } else {
            PacketHandler.send(new BroadcastPacket(
                    kickBroadcast,
                    Perms.PUNISHMENTS_SILENT_VIEW.get(),
                    AlertType.SILENT_PUNISHMENT));
        }

        String toSend = Locale.PUNISHMENT_KICK_KICK_MESSAGE.getStringNetwork().replace("%reason%", reason);
        PacketHandler.send(new KickPlayerPacket(target.getUuid(), toSend));
    }


    private void punish(Profile issuer, Profile target, IPunishment.PunishmentType punishmentType, Duration duration, String reason, boolean silent, CommandSender sender) {
        if (!issuer.getHighestRank().isAboveOrEqual(target.getHighestRank())
                && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Locale.CANNOT_PUNISH_PLAYER.getString());
            return;
        }

        if (alreadyPunished(target, punishmentType)) {
            sender.sendMessage(Locale.PLAYER_ALREADY_PUNISHED.getString());
            return;
        }
        Punishment punishment = new Punishment(
                target.getUuid(),
                punishmentType,
                duration,
                issuer.getUuid(),
                reason,
                Holiday.getInstance().getThisServer().getServerName()
        );
        Holiday.getInstance().getApi().savePunishment(punishment);

        sendPunishmentBroadcast(punishment, silent);
        kickPlayer(punishment);
    }

    private void kickPlayer(Punishment punishment) {
        if (punishment.getType() == IPunishment.PunishmentType.MUTE) return;
        String toSend = "";
        switch (punishment.getType()) {
            case BAN:
                toSend = punishment.getDuration() == TimeUtil.PERMANENT ? Locale.PUNISHMENT_BAN_KICK.getStringNetwork() : Locale.PUNISHMENT_TEMP_BAN_KICK.getStringNetwork();
                break;
            case IP_BAN:
                toSend = Locale.PUNISHMENT_IP_BAN_KICK.getStringNetwork();
                break;
            case BLACKLIST:
                toSend = Locale.PUNISHMENT_BLACKLIST_KICK.getStringNetwork();
                break;
        }
        toSend = toSend.replace("%reason%", punishment.getAddedReason())
                .replace("%duration%", punishment.getDurationObject().toRoundedTime());
        PacketHandler.send(new KickPlayerPacket(punishment.getPunished(), toSend));

    }

    private void sendPunishmentBroadcast(Punishment punishment, boolean silent) {
        String toSend = "";
        switch (punishment.getType()) {
            case BAN:
                toSend = punishment.getDurationObject().isPermanent() ? Locale.PUNISHMENT_BAN_MESSAGE.getString() : Locale.PUNISHMENT_TEMP_BAN_MESSAGE.getString();
                break;
            case MUTE:
                toSend = punishment.getDurationObject().isPermanent() ? Locale.PUNISHMENT_MUTE_MESSAGE.getString() : Locale.PUNISHMENT_TEMP_MUTE_MESSAGE.getString();
                break;
            case BLACKLIST:
                toSend = Locale.PUNISHMENT_BLACKLIST_MESSAGE.getString();
                break;
            case IP_BAN:
                toSend = Locale.PUNISHMENT_IP_BAN_MESSAGE.getString();
                break;
        }

        if (toSend.equals("")) return;

        Profile issuer = Holiday.getInstance().getApi().getProfile(punishment.getAddedBy());
        Profile target = Holiday.getInstance().getApi().getProfile(punishment.getPunished());

        String issuerName = UserConstants.getNameWithColor(issuer);
        String targetName = UserConstants.getDisplayNameWithColor(target);

        toSend = toSend.replace("%executor%", issuerName)
                .replace("%player%", targetName)
                .replace("%silent%", silent ? Locale.PUNISHMENT_SILENT_PREFIX.getString() : "")
                .replace("%reason%", punishment.getAddedReason())
                .replace("%duration%", punishment.getDurationObject().getFormatted());

        if (!silent) {
            PacketHandler.send(new BroadcastPacket(toSend));
        } else {
            PacketHandler.send(new BroadcastPacket(
                    toSend,
                    Perms.PUNISHMENTS_SILENT_VIEW.get(),
                    AlertType.SILENT_PUNISHMENT));
        }
    }

    private boolean alreadyPunished(Profile target, IPunishment.PunishmentType type) {
        switch (type) {
            case MUTE: {
                return target.isMuted();
            }
            case BAN: {
                return target.isBanned();
            }
            case IP_BAN: {
                return target.isIpBanned();
            }
            case BLACKLIST: {
                return target.isBlacklisted();
            }
        }
        return false;
    }

}
