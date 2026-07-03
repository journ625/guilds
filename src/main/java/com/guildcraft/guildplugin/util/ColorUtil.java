package com.guildcraft.guildplugin.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{6}$");

    /**
     * Ellenőrzi, hogy a megadott string egy érvényes 6 jegyű HEX szín-e (# nélkül).
     */
    public static boolean isValidHex(String hex) {
        if (hex == null) return false;
        return HEX_PATTERN.matcher(hex).matches();
    }

    /**
     * HEX kódból (pl. "FF00AA") Minecraft-kompatibilis ChatColor objektumot készít.
     */
    public static ChatColor fromHex(String hex) {
        if (!isValidHex(hex)) {
            hex = "FFFFFF";
        }
        return ChatColor.of("#" + hex);
    }

    /**
     * Legacy (§) színkóddá alakítja a szöveg elejét a megadott HEX szín alapján,
     * majd hozzáfűzi a szöveget, a végén resetel.
     */
    public static String colorize(String hex, String text) {
        ChatColor color = fromHex(hex);
        return color + text + ChatColor.RESET;
    }

    /**
     * & karakterrel írt színkódokat legacy § kóddá alakít (üzenetekhez).
     */
    public static String translateAmpersand(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
