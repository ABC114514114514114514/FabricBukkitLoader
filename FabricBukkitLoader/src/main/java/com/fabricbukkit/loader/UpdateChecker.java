package com.fabricbukkit.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Checks for updates to loaded mods by querying external APIs
 */
public class UpdateChecker {
    
    private final FabricBukkitLoader plugin;
    private final Gson gson;
    private final Map<String, String> latestVersions;
    
    public UpdateChecker(FabricBukkitLoader plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.latestVersions = new HashMap<>();
    }
    
    /**
     * Check for updates for all loaded mods
     */
    public CompletableFuture<Map<String, UpdateInfo>> checkAllUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, UpdateInfo> updateResults = new HashMap<>();
            
            for (LoadedMod mod : plugin.getLoadedMods().values()) {
                try {
                    UpdateInfo updateInfo = checkForUpdate(mod);
                    if (updateInfo != null) {
                        updateResults.put(mod.getId(), updateInfo);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, 
                        "Failed to check updates for mod: " + mod.getId(), e);
                }
            }
            
            return updateResults;
        });
    }
    
    /**
     * Check for updates for a specific mod
     */
    public UpdateInfo checkForUpdate(LoadedMod mod) {
        // Get update information from mod metadata
        Map<String, Object> metadata = mod.getMetadata();
        if (metadata == null) {
            return null;
        }
        
        // Check if mod has update JSON URL specified
        Object updateJsonUrlObj = metadata.get("custom");
        if (updateJsonUrlObj instanceof Map) {
            Map<?, ?> customData = (Map<?, ?>) updateJsonUrlObj;
            Object updateJsonUrl = customData.get("update_json_url");
            if (updateJsonUrl instanceof String) {
                return checkUsingUpdateJson(mod, (String) updateJsonUrl);
            }
        }
        
        // Alternative: Check using modrinth or curseforge IDs if available
        Object modrinthId = metadata.get("modrinth");
        if (modrinthId instanceof String) {
            return checkUsingModrinth(mod, (String) modrinthId);
        }
        
        Object curseforgeId = metadata.get("curseforge");
        if (curseforgeId instanceof String) {
            return checkUsingCurseforge(mod, (String) curseforgeId);
        }
        
        // If no external update source, return no update info
        return new UpdateInfo(mod.getId(), mod.getVersion(), mod.getVersion(), false);
    }
    
    /**
     * Check for updates using a custom update JSON URL
     */
    private UpdateInfo checkUsingUpdateJson(LoadedMod mod, String updateJsonUrl) {
        try {
            URL url = new URL(updateJsonUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "FabricBukkitLoader/" + plugin.getDescription().getVersion());
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                
                // Look for version information in the response
                // Standard format: { "latest": "version_number", "versions": { "channel": "version" } }
                String latestVersion = null;
                
                if (jsonResponse.has("latest")) {
                    latestVersion = jsonResponse.get("latest").getAsString();
                } else if (jsonResponse.has("versions")) {
                    JsonObject versions = jsonResponse.getAsJsonObject("versions");
                    if (versions.has("release")) {
                        latestVersion = versions.get("release").getAsString();
                    } else if (versions.has("stable")) {
                        latestVersion = versions.get("stable").getAsString();
                    } else {
                        // Get the first available version
                        for (Map.Entry<String, com.google.gson.JsonElement> entry : versions.entrySet()) {
                            latestVersion = entry.getValue().getAsString();
                            break;
                        }
                    }
                }
                
                if (latestVersion != null) {
                    boolean hasUpdate = isNewerVersion(latestVersion, mod.getVersion());
                    return new UpdateInfo(mod.getId(), mod.getVersion(), latestVersion, hasUpdate);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Failed to check updates using update JSON for mod: " + mod.getId(), e);
        }
        
        return new UpdateInfo(mod.getId(), mod.getVersion(), mod.getVersion(), false);
    }
    
    /**
     * Check for updates using Modrinth API
     */
    private UpdateInfo checkUsingModrinth(LoadedMod mod, String modrinthId) {
        try {
            String apiUrl = "https://api.modrinth.com/v2/project/" + modrinthId + "/version";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "FabricBukkitLoader/" + plugin.getDescription().getVersion());
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                com.google.gson.JsonArray versions = JsonParser.parseString(response).getAsJsonArray();
                
                if (versions.size() > 0) {
                    // Get the latest version (first in the list, as Modrinth returns versions in reverse chronological order)
                    JsonObject latestVersionInfo = versions.get(0).getAsJsonObject();
                    String latestVersion = latestVersionInfo.get("version_number").getAsString();
                    
                    boolean hasUpdate = isNewerVersion(latestVersion, mod.getVersion());
                    return new UpdateInfo(mod.getId(), mod.getVersion(), latestVersion, hasUpdate);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Failed to check updates using Modrinth API for mod: " + mod.getId(), e);
        }
        
        return new UpdateInfo(mod.getId(), mod.getVersion(), mod.getVersion(), false);
    }
    
    /**
     * Check for updates using CurseForge API (simplified, as it requires API key)
     */
    private UpdateInfo checkUsingCurseforge(LoadedMod mod, String curseforgeId) {
        // Note: CurseForge API requires an API key for most endpoints
        // For now, we'll log that we identified the mod but skip the check
        plugin.debug("Identified CurseForge mod: " + mod.getId() + " (ID: " + curseforgeId + "), but skipping update check (requires API key)");
        return new UpdateInfo(mod.getId(), mod.getVersion(), mod.getVersion(), false);
    }
    
    /**
     * Compare two version strings to determine if the new version is newer
     * Returns true if newVersion is newer than currentVersion
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        if (newVersion == null || currentVersion == null) {
            return false;
        }
        
        // Handle exact match
        if (newVersion.equals(currentVersion)) {
            return false;
        }
        
        // Split version strings into components
        String[] newParts = newVersion.replace('-', '.').split("[.\\-_]");
        String[] currentParts = currentVersion.replace('-', '.').split("[.\\-_]");
        
        int maxLength = Math.max(newParts.length, currentParts.length);
        
        for (int i = 0; i < maxLength; i++) {
            String newPart = i < newParts.length ? newParts[i] : "0";
            String currentPart = i < currentParts.length ? currentParts[i] : "0";
            
            // Try to parse as numbers first
            try {
                int newNum = Integer.parseInt(newPart);
                int currentNum = Integer.parseInt(currentPart);
                
                if (newNum > currentNum) {
                    return true;
                } else if (newNum < currentNum) {
                    return false;
                }
                // If equal, continue to next component
            } catch (NumberFormatException e) {
                // If not numbers, compare as strings
                int result = newPart.compareTo(currentPart);
                if (result > 0) {
                    return true;
                } else if (result < 0) {
                    return false;
                }
                // If equal, continue to next component
            }
        }
        
        // If all components are equal, versions are the same
        return false;
    }
    
    /**
     * Get update status report for all mods
     */
    public String getUpdateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Mod Update Report ===\n");
        
        try {
            // Synchronously check updates for a report
            Map<String, UpdateInfo> updates = new HashMap<>();
            
            for (LoadedMod mod : plugin.getLoadedMods().values()) {
                UpdateInfo info = checkForUpdate(mod);
                if (info != null) {
                    updates.put(mod.getId(), info);
                }
            }
            
            int updateAvailableCount = 0;
            for (UpdateInfo info : updates.values()) {
                report.append("Mod: ").append(info.getModId()).append("\n");
                report.append("  Current: ").append(info.getCurrentVersion()).append("\n");
                report.append("  Latest:  ").append(info.getLatestVersion()).append("\n");
                report.append("  Update Available: ").append(info.isUpdateAvailable() ? "Yes" : "No").append("\n");
                
                if (info.isUpdateAvailable()) {
                    updateAvailableCount++;
                    report.append("  >> Update recommended! <<\n");
                }
                report.append("\n");
            }
            
            report.append("Total mods checked: ").append(updates.size()).append("\n");
            report.append("Mods with updates available: ").append(updateAvailableCount).append("\n");
            
        } catch (Exception e) {
            report.append("Error generating update report: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Inner class to hold update information for a mod
     */
    public static class UpdateInfo {
        private final String modId;
        private final String currentVersion;
        private final String latestVersion;
        private final boolean updateAvailable;
        
        public UpdateInfo(String modId, String currentVersion, String latestVersion, boolean updateAvailable) {
            this.modId = modId;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.updateAvailable = updateAvailable;
        }
        
        public String getModId() { return modId; }
        public String getCurrentVersion() { return currentVersion; }
        public String getLatestVersion() { return latestVersion; }
        public boolean isUpdateAvailable() { return updateAvailable; }
        
        @Override
        public String toString() {
            return "UpdateInfo{" +
                    "modId='" + modId + '\'' +
                    ", currentVersion='" + currentVersion + '\'' +
                    ", latestVersion='" + latestVersion + '\'' +
                    ", updateAvailable=" + updateAvailable +
                    '}';
        }
    }
}