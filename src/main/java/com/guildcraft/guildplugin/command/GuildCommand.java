package com.guildcraft.guildplugin.command;

import com.guildcraft.guildplugin.GuildPlugin;
import com.guildcraft.guildplugin.manager.GuildManager;
import com.guildcraft.guildplugin.model.Guild;
import com.guildcraft.guildplugin.util.ColorUtil;
import com.guildcraft.guildplugin.util.GuildException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuildCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§6Guild§8]§r ";

    private final GuildPlugin plugin;
    private final GuildManager manager;

    public GuildCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGuildManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (sub) {
                case "create" -> handleCreate(sender, rest);
                case "info" -> handleInfo(sender, rest);
                case "accept" -> handleAccept(sender, rest);
                case "color" -> handleColor(sender, rest);
                case "invite" -> handleInvite(sender, rest);
                case "kick" -> handleKick(sender, rest);
                case "coleader" -> handleColeader(sender, rest);
                case "demote" -> handleDemote(sender, rest);
                case "leave" -> handleLeave(sender, rest);
                case "ally" -> handleAlly(sender, rest);
                case "allyaccept" -> handleAllyAccept(sender, rest);
                case "allydeny" -> handleAllyDeny(sender, rest);
                case "revoke" -> handleRevoke(sender, rest);
                case "rename" -> handleRename(sender, rest);
                case "disband" -> handleDisband(sender, rest);
                case "top" -> handleTop(sender, rest);
                case "admin" -> handleAdmin(sender, rest);
                default -> sendHelp(sender);
            }
        } catch (GuildException ex) {
            sender.sendMessage(PREFIX + ex.getMessage());
        }

        return true;
    }

    // ==================== SÚGÓ ====================

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§6§lGuildPlugin §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7/guild info <guild> §8- §7Guild adatainak lekerdezese");
        sender.sendMessage("§7/guild create <nev> §8- §7Guild letrehozasa");
        sender.sendMessage("§7/guild accept §8- §7Meghivo elfogadasa");
        sender.sendMessage("§7/guild color <HEX> §8- §7Guild szinenek beallitasa");
        sender.sendMessage("§7/guild invite <jatekos> §8- §7Jatekos meghivasa");
        sender.sendMessage("§7/guild kick <jatekos> §8- §7Jatekos kirugasa");
        sender.sendMessage("§7/guild coleader <jatekos> §8- §7Co-leaderre leptetes");
        sender.sendMessage("§7/guild demote <jatekos> §8- §7Co-leader lefokozasa");
        sender.sendMessage("§7/guild leave §8- §7Kilepes a guildbol");
        sender.sendMessage("§7/guild ally <guild> §8- §7Szovetsegi kerelem kuldese");
        sender.sendMessage("§7/guild allyaccept <guild> §8- §7Szovetsegi kerelem elfogadasa");
        sender.sendMessage("§7/guild allydeny <guild> §8- §7Szovetsegi kerelem elutasitasa");
        sender.sendMessage("§7/guild revoke <guild> §8- §7Szovetseg megszuntetese");
        sender.sendMessage("§7/guild rename <uj nev> §8- §7Guild atnevezese");
        sender.sendMessage("§7/guild disband IGEN §8- §7Guild torlese");
        sender.sendMessage("§7/guild top §8- §7Toplista");
        if (sender.hasPermission("guild.admin")) {
            sender.sendMessage("§c/guild admin ... §8- §7Admin parancsok");
        }
        sender.sendMessage("§8§m----------------------------------------");
    }

    // ==================== ALAP PARANCSOK ====================

    private void handleCreate(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild create <nev>");
            return;
        }
        Guild guild = manager.createGuild(args[0], player.getUniqueId());
        sender.sendMessage(PREFIX + "§aSikeresen letrehoztad a(z) §f" + guild.getName() + " §aguildet!");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Guild guild;
        if (args.length >= 1) {
            guild = manager.getGuild(args[0]);
            if (guild == null) {
                sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
                return;
            }
        } else {
            Player player = requirePlayer(sender);
            guild = manager.getGuildByPlayer(player.getUniqueId());
            if (guild == null) {
                sender.sendMessage(PREFIX + "§cNincs guildben! Hasznald: /guild info <guild>");
                return;
            }
        }

        String coloredName = ColorUtil.colorize(guild.getColor(), guild.getName());
        String leaderName = offlineName(guild.getLeader());
        String coLeaders = guild.getCoLeaders().isEmpty() ? "§7-" :
                guild.getCoLeaders().stream().map(this::offlineName).collect(Collectors.joining("§7, §f"));
        String members = guild.getMembers().isEmpty() ? "§7-" :
                guild.getMembers().stream().map(this::offlineName).collect(Collectors.joining("§7, §f"));
        String allies = guild.getAllies().isEmpty() ? "§7-" :
                guild.getAllies().stream()
                        .map(manager::getGuild)
                        .filter(Objects::nonNull)
                        .map(g -> ColorUtil.colorize(g.getColor(), g.getName()))
                        .collect(Collectors.joining("§7, "));

        sender.sendMessage("§8====[§r" + coloredName + "§8]====");
        sender.sendMessage("§7Leader §8> §f" + leaderName);
        sender.sendMessage("§7Co-Leader §8> §f" + coLeaders);
        sender.sendMessage("§7Members §8> §f" + members);
        sender.sendMessage("§7Szovetsegesek §8> §f" + allies);
        sender.sendMessage("§7Olesek §8> §f" + guild.getKills());
        sender.sendMessage("§7Halalok §8> §f" + guild.getDeaths());
        sender.sendMessage("§7Helyezes §8> §f#" + manager.getRating(guild));
    }

    private void handleAccept(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = manager.acceptInvite(player.getUniqueId());
        sender.sendMessage(PREFIX + "§aCsatlakoztal a(z) §f" + guild.getName() + " §aguildhez!");
    }

    private void handleColor(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeaderOrCoLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild color <HEX> §7(pl. FF00AA)");
            return;
        }
        String hex = args[0].replace("#", "");
        manager.setColor(guild, hex);
        sender.sendMessage(PREFIX + "§aA guild szine beallitva: §f#" + hex);
    }

    private void handleInvite(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeaderOrCoLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild invite <jatekos>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(PREFIX + "§cEz a jatekos nincs fenn a szerveren!");
            return;
        }
        manager.invitePlayer(guild, target.getUniqueId());
        sender.sendMessage(PREFIX + "§aMeghivtad §f" + target.getName() + " §ajatekost a guildbe!");
        target.sendMessage(PREFIX + "§f" + player.getName() + " §ameghivott a(z) §f" + guild.getName() +
                " §aguildbe! Fogadd el: §f/guild accept");
    }

    private void handleKick(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeaderOrCoLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild kick <jatekos>");
            return;
        }
        UUID target = resolveUuid(args[0]);
        manager.kickMember(guild, player.getUniqueId(), target);
        sender.sendMessage(PREFIX + "§f" + args[0] + " §akirugva a guildbol!");
    }

    private void handleColeader(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild coleader <jatekos>");
            return;
        }
        UUID target = resolveUuid(args[0]);
        manager.promoteToCoLeader(guild, target);
        sender.sendMessage(PREFIX + "§f" + args[0] + " §amostantol co-leader!");
    }

    private void handleDemote(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild demote <jatekos>");
            return;
        }
        UUID target = resolveUuid(args[0]);
        manager.demoteToMember(guild, target);
        sender.sendMessage(PREFIX + "§f" + args[0] + " §alefokozva tagra!");
    }

    private void handleLeave(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        manager.leaveGuild(guild, player.getUniqueId());
        sender.sendMessage(PREFIX + "§aElhagytad a(z) §f" + guild.getName() + " §aguildet!");
    }

    private void handleAlly(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeaderOrCoLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild ally <guild>");
            return;
        }
        Guild other = manager.getGuild(args[0]);
        if (other == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        manager.requestAlly(guild, other);

        if (guild.isAllyWith(other.getId())) {
            // a masik guild mar kuldott kerelmet -> automatikusan letrejott a szovetseg
            broadcastToGuild(guild, PREFIX + "§aSzovetseg letrejott §f" + other.getName() + " §aguilddel!");
            broadcastToGuild(other, PREFIX + "§aSzovetseg letrejott §f" + guild.getName() + " §aguilddel!");
        } else {
            sender.sendMessage(PREFIX + "§aSzovetsegi kerelmet kuldtel §f" + other.getName() + " §aguildnek!");
            broadcastToGuild(other, PREFIX + "§f" + guild.getName() + " §aszovetsegi kerelmet kuldott! Elfogadas: §f/guild allyaccept " +
                    guild.getName() + " §a| Elutasitas: §f/guild allydeny " + guild.getName());
        }
    }

    private void handleAllyAccept(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeaderOrCoLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild allyaccept <guild>");
            return;
        }
        Guild other = manager.getGuild(args[0]);
        if (other == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        manager.acceptAllyRequest(guild, other);
        broadcastToGuild(guild, PREFIX + "§aSzovetseg letrejott §f" + other.getName() + " §aguilddel!");
        broadcastToGuild(other, PREFIX + "§aSzovetseg letrejott §f" + guild.getName() + " §aguilddel!");
    }

    private void handleAllyDeny(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeaderOrCoLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild allydeny <guild>");
            return;
        }
        Guild other = manager.getGuild(args[0]);
        if (other == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        manager.denyAllyRequest(guild, other);
        sender.sendMessage(PREFIX + "§cElutasitottad §f" + other.getName() + " §cszovetsegi kerelmet!");
        broadcastToGuild(other, PREFIX + "§f" + guild.getName() + " §celutasitotta a szovetsegi kerelmedet.");
    }

    private void handleRevoke(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeaderOrCoLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild revoke <guild>");
            return;
        }
        Guild other = manager.getGuild(args[0]);
        if (other == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        manager.revoke(guild, other);
        sender.sendMessage(PREFIX + "§aMegszuntetted a szovetseget §f" + other.getName() + " §aguilddel!");
    }

    private void handleRename(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeader(guild, player);

        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild rename <uj nev>");
            return;
        }
        String old = guild.getName();
        manager.renameGuild(guild, args[0]);
        sender.sendMessage(PREFIX + "§aA guild neve megvaltozott: §f" + old + " §7-> §f" + guild.getName());
    }

    private void handleDisband(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        Guild guild = requireGuild(player);
        requireLeader(guild, player);

        if (args.length < 1 || !args[0].equals("IGEN")) {
            sender.sendMessage(PREFIX + "§cA guild vegleges torlesehez irt: §f/guild disband IGEN");
            return;
        }
        manager.disbandGuild(guild);
        sender.sendMessage(PREFIX + "§aA(z) §f" + guild.getName() + " §aguild torolve lett!");
    }

    private void handleTop(CommandSender sender, String[] args) {
        int size = plugin.getConfig().getInt("top-list-size", 10);
        List<Guild> top = manager.getTopGuilds(size);

        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§6§lGuild Toplista §7(killek alapjan)");
        int pos = 1;
        for (Guild g : top) {
            String coloredName = ColorUtil.colorize(g.getColor(), g.getName());
            sender.sendMessage("§7#" + pos + " §8- " + coloredName + " §7- §6" + g.getKills() + " kill");
            pos++;
        }
        if (top.isEmpty()) {
            sender.sendMessage("§7Meg nincs egy guild sem letrehozva.");
        }
        sender.sendMessage("§8§m----------------------------------------");
    }

    // ==================== ADMIN PARANCSOK ====================

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            sender.sendMessage(PREFIX + "§cNincs jogosultsagod ehhez!");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild admin <rename|disband|addkills|removekills|resetkills|adddeaths|removedeaths|resetdeaths|coleader|leader|member>");
            return;
        }

        String sub = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (sub) {
            case "rename" -> adminRename(sender, rest);
            case "disband" -> adminDisband(sender, rest);
            case "addkills" -> adminKillsDeaths(sender, rest, true, true);
            case "removekills" -> adminKillsDeaths(sender, rest, true, false);
            case "resetkills" -> adminReset(sender, rest, true);
            case "adddeaths" -> adminKillsDeaths(sender, rest, false, true);
            case "removedeaths" -> adminKillsDeaths(sender, rest, false, false);
            case "resetdeaths" -> adminReset(sender, rest, false);
            case "coleader", "leader", "member" -> adminSetRank(sender, sub, rest);
            default -> sender.sendMessage(PREFIX + "§cIsmeretlen admin parancs!");
        }
    }

    private void adminRename(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild admin rename <guild> <uj nev>");
            return;
        }
        Guild guild = manager.getGuild(args[0]);
        if (guild == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        String old = guild.getName();
        manager.renameGuild(guild, args[1]);
        sender.sendMessage(PREFIX + "§a[ADMIN] Atnevezve: §f" + old + " §7-> §f" + guild.getName());
    }

    private void adminDisband(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild admin disband <guild>");
            return;
        }
        Guild guild = manager.getGuild(args[0]);
        if (guild == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        manager.disbandGuild(guild);
        sender.sendMessage(PREFIX + "§a[ADMIN] A(z) §f" + guild.getName() + " §aguild torolve lett!");
    }

    private void adminKillsDeaths(CommandSender sender, String[] args, boolean isKills, boolean isAdd) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild admin " + (isKills ? "add/removekills" : "add/removedeaths") + " <guild> <mennyiseg>");
            return;
        }
        Guild guild = manager.getGuild(args[0]);
        if (guild == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIX + "§cA mennyisegnek szamnak kell lennie!");
            return;
        }

        if (isKills) {
            if (isAdd) manager.addKills(guild, amount); else manager.removeKills(guild, amount);
        } else {
            if (isAdd) manager.addDeaths(guild, amount); else manager.removeDeaths(guild, amount);
        }
        sender.sendMessage(PREFIX + "§a[ADMIN] " + (isKills ? "Kill" : "Halal") + "ok modositva: §f" + guild.getName());
    }

    private void adminReset(CommandSender sender, String[] args, boolean isKills) {
        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild admin " + (isKills ? "resetkills" : "resetdeaths") + " <guild>");
            return;
        }
        Guild guild = manager.getGuild(args[0]);
        if (guild == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        if (isKills) manager.resetKills(guild); else manager.resetDeaths(guild);
        sender.sendMessage(PREFIX + "§a[ADMIN] " + (isKills ? "Kill" : "Halal") + "ok visszaallitva: §f" + guild.getName());
    }

    private void adminSetRank(CommandSender sender, String rank, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§cHasznalat: /guild admin " + rank + " <guild> <jatekos>");
            return;
        }
        Guild guild = manager.getGuild(args[0]);
        if (guild == null) {
            sender.sendMessage(PREFIX + "§cEz a guild nem letezik!");
            return;
        }
        UUID target = resolveUuid(args[1]);
        manager.setRankAdmin(guild, target, rank);
        sender.sendMessage(PREFIX + "§a[ADMIN] §f" + args[1] + " §arangja: " + rank);
    }

    // ==================== SEGÉD METÓDUSOK ====================

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new GuildException("§cEzt a parancsot csak jatekoskent hasznalhatod!");
        }
        return player;
    }

    private Guild requireGuild(Player player) {
        Guild guild = manager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            throw new GuildException("§cNem vagy tagja egyetlen guildnek sem!");
        }
        return guild;
    }

    private void requireLeader(Guild guild, Player player) {
        if (!guild.isLeader(player.getUniqueId())) {
            throw new GuildException("§cCsak a guild vezetoje hasznalhatja ezt a parancsot!");
        }
    }

    private void requireLeaderOrCoLeader(Guild guild, Player player) {
        if (!guild.isLeaderOrCoLeader(player.getUniqueId())) {
            throw new GuildException("§cCsak a guild vezetoje vagy co-leaderei hasznalhatjak ezt a parancsot!");
        }
    }

    private UUID resolveUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline.getUniqueId();
        }
        throw new GuildException("§cEz a jatekos nem talalhato!");
    }

    private String offlineName(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        String name = p.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private void broadcastToGuild(Guild guild, String message) {
        for (UUID uuid : guild.getAllMembers()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                online.sendMessage(message);
            }
        }
    }

    // ==================== TAB COMPLETE ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            options.addAll(Arrays.asList("info", "create", "accept", "color", "invite", "kick", "coleader",
                    "demote", "leave", "ally", "allyaccept", "allydeny", "revoke", "rename", "disband", "top"));
            if (sender.hasPermission("guild.admin")) options.add("admin");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Set.of("invite", "kick", "coleader", "demote").contains(sub)) {
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            } else if (Set.of("info", "ally", "allyaccept", "allydeny", "revoke").contains(sub)) {
                options.addAll(manager.getGuilds().stream().map(Guild::getName).collect(Collectors.toList()));
            } else if (sub.equals("disband")) {
                options.add("IGEN");
            } else if (sub.equals("admin")) {
                options.addAll(Arrays.asList("rename", "disband", "addkills", "removekills", "resetkills",
                        "adddeaths", "removedeaths", "resetdeaths", "coleader", "leader", "member"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            String adminSub = args[1].toLowerCase();
            if (Set.of("rename", "disband", "addkills", "removekills", "resetkills",
                    "adddeaths", "removedeaths", "resetdeaths", "coleader", "leader", "member").contains(adminSub)) {
                options.addAll(manager.getGuilds().stream().map(Guild::getName).collect(Collectors.toList()));
            }
        }

        String current = args[args.length - 1].toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(current)).collect(Collectors.toList());
    }
}
