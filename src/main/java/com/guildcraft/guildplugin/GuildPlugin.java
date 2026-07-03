package com.guildcraft.guildplugin;

import com.guildcraft.guildplugin.command.GuildCommand;
import com.guildcraft.guildplugin.listener.PlayerListener;
import com.guildcraft.guildplugin.manager.GuildManager;
import com.guildcraft.guildplugin.placeholder.GuildPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class GuildPlugin extends JavaPlugin {

    private GuildManager guildManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.guildManager = new GuildManager(this);

        GuildCommand guildCommand = new GuildCommand(this);
        getCommand("guild").setExecutor(guildCommand);
        getCommand("guild").setTabCompleter(guildCommand);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(guildManager), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new GuildPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion regisztralva.");
        } else {
            getLogger().info("PlaceholderAPI nem talalhato - placeholderek nem lesznek elerhetok.");
        }

        getLogger().info("GuildPlugin bekapcsolva - v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (guildManager != null) {
            guildManager.save();
        }
        getLogger().info("GuildPlugin kikapcsolva.");
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }
}
