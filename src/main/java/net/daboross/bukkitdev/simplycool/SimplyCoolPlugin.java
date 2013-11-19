/*
 * Copyright (C) 2013 Dabo Ross <http://www.daboross.net/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.daboross.bukkitdev.simplycool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SimplyCoolPlugin extends JavaPlugin implements Listener {

    private int cooldown;
    private Map<String, String> treatAs = new HashMap<>();
    private Map<String, Long> cooldowns = new HashMap<>();
    private final String FORMAT = ChatColor.DARK_RED + "Please wait " + ChatColor.RED + "%s" + ChatColor.DARK_RED + " before using " + ChatColor.RED + "%s" + ChatColor.DARK_RED + ".";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        cooldown = config.getInt("cooldown");
        final ConfigurationSection section = config.getConfigurationSection("equiv-commands");
        if (section != null) {
            getServer().getScheduler().runTask(this, new Runnable() {
                @Override
                public void run() {
                    for (String s : section.getKeys(false)) {
                        treatAs.put(getCmd(s), getCmd(section.getString(s)));
                    }
                }
            });
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent evt) {
        final String command = getTreatedCommand(evt.getMessage());
        if (cooldowns.containsKey(command)) {
            evt.getPlayer().sendMessage(String.format(FORMAT, relativeFormat(cooldown - System.currentTimeMillis()), command));
            evt.setCancelled(true);
        } else {
            cooldowns.put(command, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cooldown));
            new BukkitRunnable() {
                @Override
                public void run() {
                    cooldowns.remove(command);
                }
            }.runTaskLater(this, cooldown * 20);
        }
    }

    private String getCmd(String cmd) {
        PluginCommand command = getServer().getPluginCommand(cmd);
        return (command == null ? cmd : command.getName()).toLowerCase();
    }

    private String getTreatedCommand(String message) {
        String cmd = getCmd(message.split(" ")[0].substring(1));
        String as = treatAs.get(cmd);
        return as == null ? cmd : as;
    }

    public static String relativeFormat(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("millis < 0");
        } else if (millis == 0) {
            return "Not that long";
        }
        long years, days, hours, minutes, seconds;

        years = TimeUnit.MILLISECONDS.toDays(millis) / 365;
        days = TimeUnit.MILLISECONDS.toDays(millis);
        hours = TimeUnit.MILLISECONDS.toHours(millis);
        minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        minutes -= TimeUnit.HOURS.toMinutes(hours);
        hours -= TimeUnit.DAYS.toHours(days);
        days %= 365;
        StringBuilder resultBuilder = new StringBuilder();
        if (years > 0) {
            resultBuilder.append(years).append(years == 1 ? " year" : " years");
            if (days > 0) {
                resultBuilder.append(" and ");
            }
        }
        if (days > 0) {
            resultBuilder.append(days).append(days == 1 ? " day" : " days");
            if (hours > 0 && years <= 0) {
                resultBuilder.append(" and ");
            }
        }
        if (years <= 0) {
            if (hours > 0) {
                resultBuilder.append(hours).append(hours == 1 ? " hour" : " hours");
                if (minutes > 0 && days <= 0) {
                    resultBuilder.append(" and ");
                }
            }
            if (days <= 0) {
                if (minutes > 0) {
                    resultBuilder.append(minutes).append(minutes == 1 ? " minute" : " minutes");
                } else if (seconds > 0 && hours <= 0) {
                    resultBuilder.append(seconds).append(seconds == 1 ? " second" : " seconds");
                }
            }
        }
        return resultBuilder.toString();
    }
}
