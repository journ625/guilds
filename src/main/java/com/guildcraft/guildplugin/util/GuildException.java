package com.guildcraft.guildplugin.util;

/**
 * Felhasználónak szánt hibaüzenetek jelzésére a GuildManager műveleteiben.
 * A message már a végleges, játékosnak megjeleníthető (színkódolt) szöveg.
 */
public class GuildException extends RuntimeException {
    public GuildException(String message) {
        super(message);
    }
}
