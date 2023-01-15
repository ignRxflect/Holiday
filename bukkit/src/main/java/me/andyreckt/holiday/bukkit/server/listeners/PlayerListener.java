package me.andyreckt.holiday.bukkit.server.listeners;

import me.andyreckt.holiday.api.user.IGrant;
import me.andyreckt.holiday.api.user.IPunishment;
import me.andyreckt.holiday.api.user.IRank;
import me.andyreckt.holiday.api.user.Profile;
import me.andyreckt.holiday.bukkit.Holiday;
import me.andyreckt.holiday.bukkit.user.UserConstants;
import me.andyreckt.holiday.core.user.disguise.Disguise;
import me.andyreckt.holiday.bukkit.util.files.Locale;
import me.andyreckt.holiday.bukkit.util.files.Perms;
import me.andyreckt.holiday.bukkit.util.player.PermissionUtils;
import me.andyreckt.holiday.bukkit.util.player.PlayerUtils;
import me.andyreckt.holiday.bukkit.util.text.CC;
import me.andyreckt.holiday.core.server.Server;
import me.andyreckt.holiday.core.user.UserProfile;
import me.andyreckt.holiday.core.user.grant.Grant;
import me.andyreckt.holiday.core.util.duration.TimeUtil;
import me.andyreckt.holiday.core.util.enums.AlertType;
import me.andyreckt.holiday.core.util.redis.pubsub.packets.BroadcastPacket;
import me.andyreckt.holiday.core.util.text.HashUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLoginUserProfile(AsyncPlayerPreLoginEvent event) {
        Profile profile = Holiday.getInstance().getApi().getProfile(event.getUniqueId());

        if (!profile.getName().equalsIgnoreCase(event.getName())) {
            profile.setName(event.getName());
        }

        if (!profile.getIp().equalsIgnoreCase(HashUtils.hash(event.getAddress().getHostAddress()))) {
            profile.addNewCurrentIP(event.getAddress().getHostAddress());
        }


        Holiday.getInstance().getApi().getAllProfiles().whenCompleteAsync((map, ignored) -> {
            for (Map.Entry<UUID, Profile> entry : map.entrySet()) {
                Profile alt = entry.getValue();

                if (alt.getUuid().equals(profile.getUuid())) {
                    continue;
                }

                if (alt.getIp().equalsIgnoreCase(profile.getIp())) {
                    alt.getAlts().add(profile.getUuid());
                    profile.getAlts().add(alt.getUuid());
                    Holiday.getInstance().getApi().saveProfile(alt);
                }

            }

            Holiday.getInstance().getApi().saveProfile(profile);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoinStartupCheck(PlayerLoginEvent event) {
        if (!Holiday.getInstance().isJoinable()) {
            event.setKickMessage(CC.translate("&cServer is still starting up."));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onLoginPunishments(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        Profile profile = Holiday.getInstance().getApi().getProfile(player.getUniqueId());

        IPunishment punishment = profile.getActivePunishments().stream()
                .filter(pun -> pun.getType() == IPunishment.PunishmentType.BAN ||
                        pun.getType() == IPunishment.PunishmentType.IP_BAN ||
                        pun.getType() == IPunishment.PunishmentType.BLACKLIST)
                .findFirst().orElse(null);

        if (punishment == null) return;
        if (punishment.getType() == IPunishment.PunishmentType.BAN && Locale.BANNED_JOIN.getBoolean()) return;

        Locale locale;
        switch (punishment.getType()) {
            case BAN:
                locale = punishment.getDuration() == TimeUtil.PERMANENT ? Locale.PUNISHMENT_BAN_KICK : Locale.PUNISHMENT_TEMP_BAN_KICK;
                break;
            case IP_BAN:
                locale = Locale.PUNISHMENT_IP_BAN_KICK;
                break;
            case BLACKLIST:
                locale = Locale.PUNISHMENT_BLACKLIST_KICK;
                break;
            default:
                return;
        }

        punishment.check();
        if (!punishment.isActive()) {
            Holiday.getInstance().getApi().savePunishment(punishment);
            return;
        }


        String kickMessage = locale.getStringNetwork()
                .replace("%reason%", punishment.getAddedReason())
                .replace("%duration%", TimeUtil.getDuration(punishment.getRemainingTime()));
        event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        event.setKickMessage(CC.translate(kickMessage));
        String toSend = Locale.PUNISHMENT_BANNED_LOGIN_ALERT.getString()
                .replace("%player%", player.getName());
        Holiday.getInstance().getApi().getRedis().sendPacket(
                new BroadcastPacket(toSend, Perms.ADMIN_VIEW_NOTIFICATIONS.get(), AlertType.BANNED_LOGIN));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLoginWhitelist(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        Profile profile = Holiday.getInstance().getApi().getProfile(player.getUniqueId());

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;

        Server server = Holiday.getInstance().getThisServer();
        if (!server.isWhitelisted()) return;
        IRank rank = server.getWhitelistRank();

        if (profile.getHighestRank().isAboveOrEqual(rank)) return;
        if (server.getWhitelistedPlayers().contains(player.getUniqueId())) return;

        event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
        event.setKickMessage(CC.translate(Locale.LOGIN_WHITELIST.getString().replace("%rank%", rank.getDisplayName())));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoinDisguise(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Profile profile = Holiday.getInstance().getApi().getProfile(player.getUniqueId());

        if (profile.isDisguised()) {
            player.setDisplayName(profile.getDisguise().getDisplayName());
            Holiday.getInstance().getDisguiseManager().disguise((Disguise) profile.getDisguise(), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoinPermissions(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Profile profile = Holiday.getInstance().getApi().getProfile(player.getUniqueId());

        if (profile.isOp()) {
            player.setOp(true);
        }

        PermissionUtils.updatePermissions(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinAlts(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Profile profile = Holiday.getInstance().getApi().getProfile(player.getUniqueId());

        if (profile.getAlts().stream().map(Holiday.getInstance().getApi()::getProfile).anyMatch(Profile::isBanned)) {
            String toSend = Locale.PUNISHMENT_ALT_LOGIN_ALERT.getString()
                    .replace("%player%", player.getName())
                    .replace("%alts%", profile.getAlts().stream()
                            .map(Holiday.getInstance().getApi()::getProfile)
                            .filter(Profile::isBanned)
                            .map(Profile::getName)
                            .collect(Collectors.joining(", ")));
            Holiday.getInstance().getApi().getRedis().sendPacket(
                    new BroadcastPacket(toSend, Perms.ADMIN_VIEW_NOTIFICATIONS.get(), AlertType.ALT_LOGIN));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinOther(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Profile profile = Holiday.getInstance().getApi().getProfile(player.getUniqueId());

        if (profile.isLiked()) return;

        PlayerUtils.hasVotedOnNameMC(player.getUniqueId()).whenCompleteAsync((voted, ignored) -> {
            if (!voted) return;

            player.sendMessage(Locale.NAMEMC_MESSAGE.getString());
            profile.setLiked(true);
            Holiday.getInstance().getApi().saveProfile(profile);

            if (!Locale.NAMEMC_RANK_ENABLED.getBoolean()) return;

            IRank rank = Holiday.getInstance().getApi().getRank(Locale.NAMEMC_RANK_NAME.getString());
            IGrant grant = new Grant(
                    profile.getUuid(),
                    rank,
                    UserProfile.getConsoleProfile().getUuid(),
                    "Liked on NameMC",
                    "$undefined",
                    TimeUtil.PERMANENT
            ); //TODO: Add NameMC login message and toggle

            Holiday.getInstance().getApi().saveGrant(grant);
        });
        UserConstants.reloadPlayer(player);
    }


}