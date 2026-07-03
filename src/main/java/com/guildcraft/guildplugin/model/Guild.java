package com.guildcraft.guildplugin.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Guild {

    private String name;               // megjelenítési név (eredeti case-szel)
    private String color;              // HEX szín, # nélkül, pl. "FFFFFF"
    private UUID leader;
    private final Set<UUID> coLeaders = new LinkedHashSet<>();
    private final Set<UUID> members = new LinkedHashSet<>();   // sima tagok (nem leader, nem co-leader)
    private final Set<String> allies = new LinkedHashSet<>();  // szövetséges guildek id-je (lowercase)
    private int kills;
    private int deaths;

    public Guild(String name, UUID leader, String color) {
        this.name = name;
        this.leader = leader;
        this.color = color;
    }

    public String getId() {
        return name.toLowerCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getCoLeaders() {
        return coLeaders;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKills(int amount) {
        this.kills = Math.max(0, this.kills + amount);
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void addDeaths(int amount) {
        this.deaths = Math.max(0, this.deaths + amount);
    }

    public double getKd() {
        if (deaths <= 0) {
            return kills;
        }
        return (double) kills / (double) deaths;
    }

    /**
     * Az összes tag UUID-je (leader + co-leaderek + tagok), leader elsőként.
     */
    public Set<UUID> getAllMembers() {
        Set<UUID> all = new LinkedHashSet<>();
        all.add(leader);
        all.addAll(coLeaders);
        all.addAll(members);
        return all;
    }

    public int getMemberCount() {
        return getAllMembers().size();
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public boolean isCoLeader(UUID uuid) {
        return coLeaders.contains(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isLeaderOrCoLeader(UUID uuid) {
        return isLeader(uuid) || isCoLeader(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return getAllMembers().contains(uuid);
    }

    public boolean isAllyWith(String otherGuildId) {
        return allies.contains(otherGuildId.toLowerCase());
    }
}
