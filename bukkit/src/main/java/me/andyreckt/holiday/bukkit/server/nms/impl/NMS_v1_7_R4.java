package me.andyreckt.holiday.bukkit.server.nms.impl;

import lombok.SneakyThrows;
import me.andyreckt.holiday.bukkit.server.nms.INMS;
import me.andyreckt.holiday.bukkit.user.disguise.Disguise;
import me.andyreckt.holiday.bukkit.util.other.Tasks;
import me.andyreckt.holiday.bukkit.util.player.Skin;
import net.minecraft.server.v1_7_R4.*;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NMS_v1_7_R4 implements INMS {

    private final Map<UUID, GameProfile> originalProfiles = new HashMap<>();
    
    @Override
    public void removeExecute(final Player player) {
        MinecraftServer.getServer().getPlayerList().sendAll(PacketPlayOutPlayerInfo.removePlayer(((CraftPlayer)player).getHandle()));
    }

    @Override
    public void addExecute(final Player player) {
        MinecraftServer.getServer().getPlayerList().sendAll(PacketPlayOutPlayerInfo.addPlayer(((CraftPlayer)player).getHandle()));
    }

    @Override
    public void respawnPlayer(Player player) {
        Location previousLocation = player.getLocation().clone();
        final EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        entityPlayer.playerConnection.sendPacket(
                new PacketPlayOutRespawn(entityPlayer.getWorld().worldProvider.dimension,
                        entityPlayer.getWorld().difficulty,
                        entityPlayer.getWorld().worldData.getType(),
                        EnumGamemode.valueOf(entityPlayer.getBukkitEntity().getGameMode().name())));
        player.teleport(previousLocation);
    }

    @Override
    public void updatePlayer(final Player player) {
        player.updateInventory();
        player.setGameMode(player.getGameMode());

        PlayerInventory inventory = player.getInventory();
        inventory.setHeldItemSlot(inventory.getHeldItemSlot());

        double oldHealth = player.getHealth();

        int oldFood = player.getFoodLevel();
        float oldSat = player.getSaturation();
        player.setFoodLevel(20);
        player.setFoodLevel(oldFood);
        player.setSaturation(5.0F);
        player.setSaturation(oldSat);

        player.setMaxHealth(player.getMaxHealth());

        player.setHealth(20.0F);
        player.setHealth(oldHealth);

        float experience = player.getExp();
        int totalExperience = player.getTotalExperience();
        player.setExp(experience);
        player.setTotalExperience(totalExperience);

        player.setWalkSpeed(player.getWalkSpeed());
    }
    
    @Override
    public void clearDataWatcher(Player player) {
        ((CraftPlayer) player).getHandle().getDataWatcher().watch(9, (byte) 0);
    }

    @Override
    public double[] recentTps() {
        return MinecraftServer.getServer().recentTps;
    }


    @Override @SneakyThrows
    public void disguise(Disguise disguise) {
        Player player = Bukkit.getPlayer(disguise.getUuid());
        if (player == null) return;
        GameProfile gameProfile = new GameProfile(disguise.getUuid(), disguise.getDisplayName());
        Skin skin = disguise.getSkin();
        gameProfile.getProperties().put("textures", new Property("textures", skin.getValue(), skin.getSignature()));

        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        GameProfile oldProfile = new GameProfile(entityPlayer.getProfile().getId(), entityPlayer.getProfile().getName());
        entityPlayer.getProfile().getProperties().entries().forEach(entry -> oldProfile.getProperties().put(entry.getKey(), entry.getValue()));
        originalProfiles.put(disguise.getUuid(), oldProfile);

        this.setName(player, gameProfile, disguise.getDisplayName());
    }

    @Override
    public void unDisguise(Disguise player) {
        GameProfile gameProfile = originalProfiles.remove(player.getUuid());
        setName(Bukkit.getPlayer(player.getUuid()), gameProfile, gameProfile.getName());
    }

    @SneakyThrows
    private void setName(Player player, GameProfile profile, String name) {
        final EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        Field field = EntityHuman.class.getDeclaredField("i");
        field.setAccessible(true);

        GameProfile currentProfile = (GameProfile) field.get(entityPlayer);

        currentProfile.getProperties().clear();
        for (Property property : profile.getProperties().values()) {
            currentProfile.getProperties().put(property.getName(), property);
        }
        Field modifiersField = Field.class.getDeclaredField("modifiers");

        // wrapping setAccessible
        AccessController.doPrivileged((PrivilegedAction) () -> {
            modifiersField.setAccessible(true);
            return null;
        });

        Field nameField = GameProfile.class.getDeclaredField("name");
        modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);
        nameField.setAccessible(true);
        nameField.set(profile, name);

        field.set(entityPlayer, profile);
        this.sendPlayerUpdate(player, name);
    }

    private void sendPlayerUpdate(Player player, String name) {
        Tasks.run(() -> {
            final org.bukkit.entity.Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.eject();
            }

            this.removeExecute(player);
            this.addExecute(player);
            this.respawnPlayer(player);
            this.updatePlayer(player);

            Bukkit.getOnlinePlayers().stream()
                    .filter(other -> !other.equals(player))
                    .filter(other -> other.canSee(player))
                    .forEach(other -> {
                        other.hidePlayer(player);
                        other.showPlayer(player);
                    });

            player.setDisplayName(name);
            //TODO: Bukkit displayname & tablist name
//            plugin.getProfileHandler().getByPlayer(player).setBukkitDisplayName();
//            plugin.getProfileHandler().getByPlayer(player).setPlayerListName();
        });
    }

}
