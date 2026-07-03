package com.guildcraft.guildplugin.manager;

import com.guildcraft.guildplugin.model.Guild;
import com.guildcraft.guildplugin.util.ColorUtil;
import com.guildcraft.guildplugin.util.GuildException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GuildManager {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    private final Map<String, Guild> guildsById = new LinkedHashMap<>();
    private final Map<UUID, String> playerToGuild = new HashMap<>();

    // meghívók: meghívott UUID -> [guildId, idoBelyeg (millis)]
    private final Map<UUID, PendingInvite> invites = new HashMap<>();

    // szövetségi kérések: célguild id -> kérelmező guildek id-jei
    private final Map<String, Set<String>> allyRequests = new HashMap<>();

    public GuildManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "guilds.yml");
        load();
    }

    // ==================== PERZISZTENCIA ====================

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nem sikerult letrehozni a guilds.yml fajlt: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        guildsById.clear();
        playerToGuild.clear();

        ConfigurationSection guildsSection = data.getConfigurationSection("guilds");
        if (guildsSection == null) return;

        for (String id : guildsSection.getKeys(false)) {
            ConfigurationSection s = guildsSection.getConfigurationSection(id);
            if (s == null) continue;

            String name = s.getString("name", id);
            String color = s.getString("color", plugin.getConfig().getString("default-color", "FFFFFF"));
            UUID leader = UUID.fromString(s.getString("leader"));

            Guild guild = new Guild(name, leader, color);
            guild.setKills(s.getInt("kills", 0));
            guild.setDeaths(s.getInt("deaths", 0));

            for (String u : s.getStringList("coleaders")) {
                guild.getCoLeaders().add(UUID.fromString(u));
            }
            for (String u : s.getStringList("members")) {
                guild.getMembers().add(UUID.fromString(u));
            }
            for (String a : s.getStringList("allies")) {
                guild.getAllies().add(a.toLowerCase());
            }

            guildsById.put(guild.getId(), guild);
        }

        // playerToGuild feltérképezése
        for (Guild g : guildsById.values()) {
            for (UUID u : g.getAllMembers()) {
                playerToGuild.put(u, g.getId());
            }
        }
    }

    public void save() {
        data = new YamlConfiguration();
        ConfigurationSection guildsSection = data.createSection("guilds");

        for (Guild g : guildsById.values()) {
            ConfigurationSection s = guildsSection.createSection(g.getId());
            s.set("name", g.getName());
            s.set("color", g.getColor());
            s.set("leader", g.getLeader().toString());
            s.set("kills", g.getKills());
            s.set("deaths", g.getDeaths());
            s.set("coleaders", g.getCoLeaders().stream().map(UUID::toString).collect(Collectors.toList()));
            s.set("members", g.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));
            s.set("allies", new ArrayList<>(g.getAllies()));
        }

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nem sikerult elmenteni a guilds.yml fajlt: " + e.getMessage());
        }
    }

    // ==================== LEKÉRDEZÉSEK ====================

    public Guild getGuild(String name) {
        if (name == null) return null;
        return guildsById.get(name.toLowerCase());
    }

    public Guild getGuildByPlayer(UUID uuid) {
        String id = playerToGuild.get(uuid);
        if (id == null) return null;
        return guildsById.get(id);
    }

    public boolean hasGuild(UUID uuid) {
        return playerToGuild.containsKey(uuid);
    }

    public Collection<Guild> getGuilds() {
        return guildsById.values();
    }

    public List<Guild> getTopGuilds(int limit) {
        return guildsById.values().stream()
                .sorted(Comparator.comparingInt(Guild::getKills).reversed()
                        .thenComparing(Guild::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * A guild helyezése (1-tol indexelve) a killek alapjan rendezett teljes listaban.
     * -1, ha a guild nem talalhato.
     */
    public int getRating(Guild guild) {
        List<Guild> sorted = guildsById.values().stream()
                .sorted(Comparator.comparingInt(Guild::getKills).reversed()
                        .thenComparing(Guild::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(guild.getId())) {
                return i + 1;
            }
        }
        return -1;
    }

    // ==================== GUILD LÉTREHOZÁS / TÖRLÉS / ÁTNEVEZÉS ====================

    public Guild createGuild(String name, UUID leader) {
        if (hasGuild(leader)) {
            throw new GuildException("§cMar tagja vagy egy guildnek!");
        }
        validateName(name);
        if (guildsById.containsKey(name.toLowerCase())) {
            throw new GuildException("§cEz a guild nev mar foglalt!");
        }

        String defaultColor = plugin.getConfig().getString("default-color", "FFFFFF");
        Guild guild = new Guild(name, leader, defaultColor);
        guildsById.put(guild.getId(), guild);
        playerToGuild.put(leader, guild.getId());
        save();
        return guild;
    }

    public void disbandGuild(Guild guild) {
        for (UUID u : guild.getAllMembers()) {
            playerToGuild.remove(u);
        }
        guildsById.remove(guild.getId());

        // szövetségek eltávolítása mindenhonnan
        for (Guild g : guildsById.values()) {
            g.getAllies().remove(guild.getId());
        }

        // fuggo meghivok torlese erre a guildre
        invites.entrySet().removeIf(e -> e.getValue().guildId.equals(guild.getId()));

        // fuggo szovetsegi kerelmek torlese erre a guildre (mint kerelmezo es mint cel)
        allyRequests.remove(guild.getId());
        allyRequests.values().forEach(set -> set.remove(guild.getId()));

        save();
    }

    public void renameGuild(Guild guild, String newName) {
        validateName(newName);
        String newId = newName.toLowerCase();
        if (!newId.equals(guild.getId()) && guildsById.containsKey(newId)) {
            throw new GuildException("§cEz a guild nev mar foglalt!");
        }

        String oldId = guild.getId();
        guildsById.remove(oldId);
        guild.setName(newName);
        guildsById.put(guild.getId(), guild);

        // szövetségekben lévő hivatkozások frissítése
        for (Guild g : guildsById.values()) {
            if (g.getAllies().remove(oldId)) {
                g.getAllies().add(guild.getId());
            }
        }

        // playerToGuild frissítése
        for (UUID u : guild.getAllMembers()) {
            playerToGuild.put(u, guild.getId());
        }

        // fuggo szovetsegi kerelmek kulcsainak/hivatkozasainak frissitese
        Set<String> pendingForOld = allyRequests.remove(oldId);
        if (pendingForOld != null) {
            allyRequests.put(guild.getId(), pendingForOld);
        }
        for (Set<String> requesters : allyRequests.values()) {
            if (requesters.remove(oldId)) {
                requesters.add(guild.getId());
            }
        }

        save();
    }

    private void validateName(String name) {
        int min = plugin.getConfig().getInt("name-min-length", 3);
        int max = plugin.getConfig().getInt("name-max-length", 16);
        if (name == null || name.length() < min || name.length() > max) {
            throw new GuildException("§cA guild nev hossza " + min + " es " + max + " karakter kozott lehet!");
        }
        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            throw new GuildException("§cA guild nev csak betuket, szamokat es alahuzast tartalmazhat!");
        }
    }

    // ==================== SZÍN ====================

    public void setColor(Guild guild, String hex) {
        if (!ColorUtil.isValidHex(hex)) {
            throw new GuildException("§cErvenytelen HEX szin! Pelda: FF00AA");
        }
        guild.setColor(hex);
        save();
    }

    // ==================== MEGHÍVÁS / CSATLAKOZÁS ====================

    public void invitePlayer(Guild guild, UUID target) {
        int max = plugin.getConfig().getInt("max-members", 10);
        if (guild.getMemberCount() >= max) {
            throw new GuildException("§cA guild mar eleri a maximalis taglétszamot! (" + max + ")");
        }
        if (hasGuild(target)) {
            throw new GuildException("§cEz a jatekos mar tagja egy guildnek!");
        }
        invites.put(target, new PendingInvite(guild.getId(), System.currentTimeMillis()));
    }

    public Guild acceptInvite(UUID player) {
        PendingInvite invite = invites.get(player);
        if (invite == null) {
            throw new GuildException("§cNincs fuggoben levo guild meghivod!");
        }

        int expireSeconds = plugin.getConfig().getInt("invite-expire-seconds", 60);
        if (expireSeconds > 0 && (System.currentTimeMillis() - invite.timestamp) > expireSeconds * 1000L) {
            invites.remove(player);
            throw new GuildException("§cA meghivo mar lejart!");
        }

        Guild guild = guildsById.get(invite.guildId);
        if (guild == null) {
            invites.remove(player);
            throw new GuildException("§cEz a guild mar nem letezik!");
        }

        int max = plugin.getConfig().getInt("max-members", 10);
        if (guild.getMemberCount() >= max) {
            invites.remove(player);
            throw new GuildException("§cA guild mar betelt!");
        }

        if (hasGuild(player)) {
            throw new GuildException("§cMar tagja vagy egy guildnek!");
        }

        guild.getMembers().add(player);
        playerToGuild.put(player, guild.getId());
        invites.remove(player);
        save();
        return guild;
    }

    // ==================== TAGSÁG KEZELÉSE ====================

    public void promoteToCoLeader(Guild guild, UUID target) {
        if (!guild.isMember(target) && !guild.getCoLeaders().contains(target)) {
            throw new GuildException("§cEz a jatekos nem tagja a guildnek!");
        }
        guild.getMembers().remove(target);
        guild.getCoLeaders().add(target);
        save();
    }

    public void demoteToMember(Guild guild, UUID target) {
        if (!guild.getCoLeaders().contains(target)) {
            throw new GuildException("§cEz a jatekos nem co-leader!");
        }
        guild.getCoLeaders().remove(target);
        guild.getMembers().add(target);
        save();
    }

    /**
     * Adminként bármilyen rangra állítja a játékost: "leader", "coleader" vagy "member".
     */
    public void setRankAdmin(Guild guild, UUID target, String rank) {
        if (!guild.hasMember(target)) {
            throw new GuildException("§cEz a jatekos nem tagja a guildnek!");
        }
        rank = rank.toLowerCase();

        guild.getCoLeaders().remove(target);
        guild.getMembers().remove(target);

        switch (rank) {
            case "leader" -> {
                UUID oldLeader = guild.getLeader();
                guild.getMembers().add(oldLeader);
                guild.setLeader(target);
            }
            case "coleader" -> guild.getCoLeaders().add(target);
            case "member" -> guild.getMembers().add(target);
            default -> throw new GuildException("§cErvenytelen rang! Hasznalhato: leader, coleader, member");
        }
        save();
    }

    public void leaveGuild(Guild guild, UUID player) {
        if (guild.isLeader(player)) {
            // ha van más tag, átadjuk a vezetést, kulonben feloszlik a guild
            Iterator<UUID> coLeaderIt = guild.getCoLeaders().iterator();
            if (coLeaderIt.hasNext()) {
                UUID newLeader = coLeaderIt.next();
                coLeaderIt.remove();
                guild.setLeader(newLeader);
                playerToGuild.remove(player);
                save();
                return;
            }
            Iterator<UUID> memberIt = guild.getMembers().iterator();
            if (memberIt.hasNext()) {
                UUID newLeader = memberIt.next();
                memberIt.remove();
                guild.setLeader(newLeader);
                playerToGuild.remove(player);
                save();
                return;
            }
            // nincs mas tag -> guild feloszlik
            disbandGuild(guild);
            return;
        }

        guild.getCoLeaders().remove(player);
        guild.getMembers().remove(player);
        playerToGuild.remove(player);
        save();
    }

    /**
     * Jatekos kirugasa a guildbol.
     * Leader barkit kirughat (kiveve sajat magat).
     * Co-Leader csak sima tagot rughat ki (masik Co-Leadert vagy a Leadert nem).
     */
    public void kickMember(Guild guild, UUID kicker, UUID target) {
        if (!guild.hasMember(target)) {
            throw new GuildException("§cEz a jatekos nem tagja a guildnek!");
        }
        if (guild.isLeader(target)) {
            throw new GuildException("§cA guild vezetojet nem lehet kirugni!");
        }
        if (kicker.equals(target)) {
            throw new GuildException("§cSajat magadat nem rughatod ki! Hasznald a /guild leave parancsot.");
        }
        boolean kickerIsLeader = guild.isLeader(kicker);
        if (!kickerIsLeader && guild.isCoLeader(target)) {
            throw new GuildException("§cCsak a guild vezetoje rughat ki egy Co-Leadert!");
        }

        guild.getCoLeaders().remove(target);
        guild.getMembers().remove(target);
        playerToGuild.remove(target);
        save();
    }

    // ==================== SZÖVETSÉG ====================

    /**
     * Szovetsegi kerelem kuldese a masik guildnek. A szovetseg csak akkor jon letre,
     * ha a masik guild elfogadja (acceptAllyRequest).
     */
    public void requestAlly(Guild requester, Guild target) {
        if (requester.getId().equals(target.getId())) {
            throw new GuildException("§cNem szovetkezhetsz sajat magaddal!");
        }
        if (requester.isAllyWith(target.getId())) {
            throw new GuildException("§cMar szovetsegben vagytok ezzel a guilddel!");
        }
        Set<String> incomingToTarget = allyRequests.computeIfAbsent(target.getId(), k -> new LinkedHashSet<>());
        if (!incomingToTarget.add(requester.getId())) {
            throw new GuildException("§cMar kuldtel szovetsegi kerelmet ennek a guildnek!");
        }

        // ha a masik guild is kuldott mar kerelmet nekunk, akkor ez automatikusan kolcsonos elfogadas
        Set<String> incomingToRequester = allyRequests.get(requester.getId());
        if (incomingToRequester != null && incomingToRequester.contains(target.getId())) {
            incomingToRequester.remove(target.getId());
            incomingToTarget.remove(requester.getId());
            confirmAlly(requester, target);
        }
        save();
    }

    public void acceptAllyRequest(Guild target, Guild requester) {
        Set<String> incoming = allyRequests.get(target.getId());
        if (incoming == null || !incoming.remove(requester.getId())) {
            throw new GuildException("§cNincs fuggoben levo szovetsegi kerelem ettol a guildtol!");
        }
        confirmAlly(target, requester);
        save();
    }

    public void denyAllyRequest(Guild target, Guild requester) {
        Set<String> incoming = allyRequests.get(target.getId());
        if (incoming == null || !incoming.remove(requester.getId())) {
            throw new GuildException("§cNincs fuggoben levo szovetsegi kerelem ettol a guildtol!");
        }
        save();
    }

    public boolean hasIncomingAllyRequest(Guild target, Guild requester) {
        Set<String> incoming = allyRequests.get(target.getId());
        return incoming != null && incoming.contains(requester.getId());
    }

    public Set<String> getIncomingAllyRequests(Guild target) {
        return allyRequests.getOrDefault(target.getId(), Collections.emptySet());
    }

    private void confirmAlly(Guild g1, Guild g2) {
        g1.getAllies().add(g2.getId());
        g2.getAllies().add(g1.getId());
    }

    public void revoke(Guild g1, Guild g2) {
        if (!g1.isAllyWith(g2.getId())) {
            throw new GuildException("§cNem vagytok szovetsegben ezzel a guilddel!");
        }
        g1.getAllies().remove(g2.getId());
        g2.getAllies().remove(g1.getId());
        save();
    }

    // ==================== KILL / DEATH MÓDOSÍTÁS ====================

    public void addKills(Guild guild, int amount) {
        guild.addKills(amount);
        save();
    }

    public void removeKills(Guild guild, int amount) {
        guild.addKills(-amount);
        save();
    }

    public void resetKills(Guild guild) {
        guild.setKills(0);
        save();
    }

    public void addDeaths(Guild guild, int amount) {
        guild.addDeaths(amount);
        save();
    }

    public void removeDeaths(Guild guild, int amount) {
        guild.addDeaths(-amount);
        save();
    }

    public void resetDeaths(Guild guild) {
        guild.setDeaths(0);
        save();
    }

    // ==================== BELSŐ OSZTÁLYOK ====================

    private static class PendingInvite {
        final String guildId;
        final long timestamp;

        PendingInvite(String guildId, long timestamp) {
            this.guildId = guildId;
            this.timestamp = timestamp;
        }
    }
}
