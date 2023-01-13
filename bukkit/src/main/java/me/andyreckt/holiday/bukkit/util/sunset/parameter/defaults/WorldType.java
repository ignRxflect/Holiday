package me.andyreckt.holiday.bukkit.util.sunset.parameter.defaults;

import me.andyreckt.holiday.bukkit.util.sunset.parameter.PType;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WorldType implements PType<World> {
    @Override
    public World transform(CommandSender sender, String source) {
        World world = Bukkit.getWorld(source);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "This world doesn't exist");
            return (null);
        }

        return (world);
    }
    @Override
    public List<String> complete(Player sender,  String source) {
        List<String> completions = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            if (StringUtils.startsWithIgnoreCase(world.getName(), source)) {
                completions.add(world.getName());
            }
        }

        return (completions);
    }

}