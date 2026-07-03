package com.guildcraft.guildplugin.placeholder;

import com.guildcraft.guildplugin.GuildPlugin;
import com.guildcraft.guildplugin.manager.GuildManager;
import com.guildcraft.guildplugin.model.Guild;
import com.guildcraft.guildplugin.util.ColorUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;

public class GuildPlaceholders extends PlaceholderExpansion {

    private final GuildPlugin plugin;
    private final GuildManager manager;

    public GuildPlaceholders(GuildPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGuildManager();
    }

    @Override
    public String getIdentifier() {
        return "guild";
    }

    @Override
    public String getAuthor() {
        return "GuildCraft";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        params = params.toLowerCase(Locale.ROOT);

        // ---- Toplistás placeholderek: guild_top_name_<n>, guild_top_kills_<n>, guild_top_formatted_<n> ----
        if (params.startsWith("top_name_")) {
            return topName(parseIndex(params, "top_name_"));
        }
        if (params.startsWith("top_kills_")) {
            return topKills(parseIndex(params, "top_kills_"));
        }
        if (params.startsWith("top_formatted_")) {
            return topFormatted(parseIndex(params, "top_formatted_"));
        }

        // ---- Jatekoshoz kotott guild placeholderek ----
        if (player == null) return "";
        Guild guild = manager.getGuildByPlayer(player.getUniqueId());

        return switch (params) {
            case "name" -> guild == null ? "" : "§8[§r" + ColorUtil.colorize(guild.getColor(), guild.getName()) + "§8]§r";
            case "members_count" -> guild == null ? "0/" + maxMembers() : guild.getMemberCount() + "/" + maxMembers();
            case "rating" -> guild == null ? "-" : String.valueOf(manager.getRating(guild));
            case "kd" -> guild == null ? "0.00" : String.format(Locale.US, "%.2f", guild.getKd());
            case "kills" -> guild == null ? "0" : String.valueOf(guild.getKills());
            case "deaths" -> guild == null ? "0" : String.valueOf(guild.getDeaths());
            default -> null;
        };
    }

    private int maxMembers() {
        return plugin.getConfig().getInt("max-members", 10);
    }

    private int parseIndex(String params, String prefix) {
        try {
            return Integer.parseInt(params.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String topName(int index) {
        Guild g = topGuildAt(index);
        return g == null ? "" : g.getName();
    }

    private String topKills(int index) {
        Guild g = topGuildAt(index);
        return g == null ? "0" : String.valueOf(g.getKills());
    }

    private String topFormatted(int index) {
        Guild g = topGuildAt(index);
        return g == null ? "" : "§6" + g.getName() + " §7- §6" + g.getKills();
    }

    private Guild topGuildAt(int index) {
        if (index < 1) return null;
        List<Guild> top = manager.getTopGuilds(Math.max(index, plugin.getConfig().getInt("top-list-size", 10)));
        if (index > top.size()) return null;
        return top.get(index - 1);
    }
}
