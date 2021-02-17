/*
 * Copyright (c) 2019 RoccoDev
 * All rights reserved.
 */

package dev.rocco.bukkit.practice.commands;

import dev.rocco.bukkit.practice.PracticePlugin;
import dev.rocco.bukkit.practice.arena.listener.PlayerListener;
import dev.rocco.bukkit.practice.utils.GetPlayer;
import dev.rocco.bukkit.practice.utils.config.ConfigEntries;
import dev.rocco.bukkit.practice.utils.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(Permission.PING.assertHasPerm(sender)) return true;

        Player player;

        if(args.length != 0 && Permission.PING_OTHERS.has(sender)) {
            player = GetPlayer.get(sender, args[0]);
        }
        else if(sender instanceof Player) {
            player = (Player) sender;
        }
        else return true;
        if(player == null) return true;
        sender.sendMessage(buildPing(player));
        return true;
    }

    private String buildPing(Player player) {
        int ping = ((CraftPlayer)player).getHandle().ping;
        return String.format(ConfigEntries.PING_RESULT, player.getName(), ping);
    }
}
