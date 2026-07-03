package com.guildcraft.guildplugin.listener;

import com.guildcraft.guildplugin.manager.GuildManager;
import com.guildcraft.guildplugin.model.Guild;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerListener implements Listener {

    private final GuildManager manager;

    public PlayerListener(GuildManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Guild victimGuild = manager.getGuildByPlayer(victim.getUniqueId());
        if (victimGuild != null) {
            manager.addDeaths(victimGuild, 1);
        }

        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            Guild killerGuild = manager.getGuildByPlayer(killer.getUniqueId());
            if (killerGuild != null) {
                manager.addKills(killerGuild, 1);
            }
        }
    }
}
