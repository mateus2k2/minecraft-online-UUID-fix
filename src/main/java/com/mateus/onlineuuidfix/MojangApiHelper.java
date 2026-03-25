package com.mateus.onlineuuidfix;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MojangApiHelper {

    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Path CACHE_FILE = Paths.get("config", "online-uuid-fix", "uuid-cache.json");

    // In-memory cache: lowercase username -> online UUID
    private static final Map<String, UUID> cache = new ConcurrentHashMap<>();

    static {
        loadCache();
    }

    /**
     * Returns the Mojang/Microsoft UUID for the given username, or null if
     * the player doesn't exist or the API is unreachable.
     * Results are cached in memory and persisted to disk.
     */
    public static UUID fetchOnlineUuid(String username) {
        String key = username.toLowerCase();

        UUID cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL + username).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "OnlineUuidFix-FabricMod/1.0");

            int status = conn.getResponseCode();

            if (status == 200) {
                String body = readStream(conn.getInputStream());
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                UUID uuid = insertDashes(json.get("id").getAsString());
                cache.put(key, uuid);
                saveCache();
                OnlineUuidFixMod.LOGGER.info("[OnlineUuidFix] Resolved {} -> {}", username, uuid);
                return uuid;

            } else if (status == 404) {
                // Username doesn't exist on Mojang — cracked/non-Mojang account
                OnlineUuidFixMod.LOGGER.warn("[OnlineUuidFix] '{}' has no Mojang account; falling back to offline UUID.", username);

            } else {
                OnlineUuidFixMod.LOGGER.warn("[OnlineUuidFix] Mojang API returned HTTP {} for '{}'; falling back.", status, username);
            }

        } catch (Exception e) {
            OnlineUuidFixMod.LOGGER.error("[OnlineUuidFix] API call failed for '{}': {}; falling back to offline UUID.", username, e.getMessage());
        }

        return null; // caller uses offline UUID as fallback
    }

    // --- helpers ---

    /** Converts a raw 32-char hex UUID (no dashes) to a proper UUID. */
    private static UUID insertDashes(String raw) {
        return UUID.fromString(
            raw.substring(0, 8) + "-" +
            raw.substring(8, 12) + "-" +
            raw.substring(12, 16) + "-" +
            raw.substring(16, 20) + "-" +
            raw.substring(20)
        );
    }

    private static String readStream(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    /** Loads the persistent UUID cache from disk on class init. */
    private static void loadCache() {
        if (!Files.exists(CACHE_FILE)) return;
        try {
            String content = Files.readString(CACHE_FILE);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            json.entrySet().forEach(entry -> {
                try {
                    cache.put(entry.getKey(), UUID.fromString(entry.getValue().getAsString()));
                } catch (Exception ignored) {}
            });
            OnlineUuidFixMod.LOGGER.info("[OnlineUuidFix] Loaded {} cached UUID(s) from disk.", cache.size());
        } catch (Exception e) {
            OnlineUuidFixMod.LOGGER.error("[OnlineUuidFix] Failed to load UUID cache: {}", e.getMessage());
        }
    }

    /** Saves the in-memory cache to disk (called after every new lookup). */
    private static synchronized void saveCache() {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            JsonObject json = new JsonObject();
            cache.forEach((k, v) -> json.addProperty(k, v.toString()));
            Files.writeString(CACHE_FILE, json.toString());
        } catch (Exception e) {
            OnlineUuidFixMod.LOGGER.error("[OnlineUuidFix] Failed to save UUID cache: {}", e.getMessage());
        }
    }
}
