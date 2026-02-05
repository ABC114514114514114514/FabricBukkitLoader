package com.fabricbukkit.loader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manages the configuration for FabricBukkitLoader
 */
public class ModConfigManager {
    
    private final FabricBukkitLoader plugin;
    private FileConfiguration config;
    
    // Configuration values
    private boolean pluginLoadingEnabled;
    private boolean debugLoggingEnabled;
    private String pluginsDirectory;
    private boolean autoReloadPlugins;
    private int maxPlugins;
    private boolean allowIncompatiblePlugins;
    
    // Event adapter settings
    private boolean eventAdapterEnabled;
    private String priorityHandling;
    private boolean eventFiltering;
    
    // Command adapter settings
    private boolean commandAdapterEnabled;
    private boolean enableAliases;
    private boolean enablePermissions;
    
    // Logging settings
    private boolean logPluginLoading;
    private boolean logPluginErrors;
    private boolean logCommandExecution;
    private boolean logEventHandling;
    
    // Security settings
    private boolean verifyPluginSignatures;
    private boolean allowUnsignedPlugins;
    private List<String> whitelistPlugins;
    private List<String> blacklistPlugins;
    
    // Dependency settings
    private boolean enableDependencyCheck;
    private boolean enableConflictDetection;
    private boolean enforceVersionCompatibility;
    
    // Priority settings
    private boolean enablePriorityLoading;
    private java.util.List<String> priorityMods;
    
    // Item protection settings
    private boolean itemProtectionEnabled;
    private String itemProtectionAction;  // "protect", "replace", "remove", "warn"
    private boolean enableItemReplacement;
    private boolean enableItemRemoval;
    
    // Enhanced dependency settings
    private boolean enableEnhancedDependencyCheck;
    private boolean allowOptionalDependencies;
    private boolean useLooseVersionCheck;
    
    public ModConfigManager(FabricBukkitLoader plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load main settings
        pluginLoadingEnabled = config.getBoolean("enable-plugin-loading", true);
        debugLoggingEnabled = config.getBoolean("debug-logging", false);
        pluginsDirectory = config.getString("plugins-directory", "plugins");
        autoReloadPlugins = config.getBoolean("auto-reload-plugins", true);
        maxPlugins = config.getInt("max-plugins", 100);
        allowIncompatiblePlugins = config.getBoolean("allow-incompatible-plugins", false);
        
        // Load event adapter settings
        eventAdapterEnabled = config.getBoolean("event-adapter.enabled", true);
        priorityHandling = config.getString("event-adapter.priority-handling", "NORMAL");
        eventFiltering = config.getBoolean("event-adapter.event-filtering", true);
        
        // Load command adapter settings
        commandAdapterEnabled = config.getBoolean("command-adapter.enabled", true);
        enableAliases = config.getBoolean("command-adapter.enable-aliases", true);
        enablePermissions = config.getBoolean("command-adapter.enable-permissions", true);
        
        // Load logging settings
        logPluginLoading = config.getBoolean("logging.log-plugin-loading", true);
        logPluginErrors = config.getBoolean("logging.log-plugin-errors", true);
        logCommandExecution = config.getBoolean("logging.log-command-execution", false);
        logEventHandling = config.getBoolean("logging.log-event-handling", false);
        
        // Load security settings
        verifyPluginSignatures = config.getBoolean("security.verify-plugin-signatures", false);
        allowUnsignedPlugins = config.getBoolean("security.allow-unsigned-plugins", true);
        whitelistPlugins = config.getStringList("security.whitelist-plugins");
        blacklistPlugins = config.getStringList("security.blacklist-plugins");
        
        // Load dependency settings
        enableDependencyCheck = config.getBoolean("dependency-check.enabled", true);
        enableConflictDetection = config.getBoolean("dependency-check.conflict-detection", true);
        enforceVersionCompatibility = config.getBoolean("dependency-check.enforce-version-compatibility", true);
        
        // Load priority settings
        enablePriorityLoading = config.getBoolean("priority-loading.enabled", true);
        priorityMods = config.getStringList("priority-loading.priority-mods");
        
        // Load item protection settings
        itemProtectionEnabled = config.getBoolean("item-protection.enabled", true);
        itemProtectionAction = config.getString("item-protection.action", "protect");
        enableItemReplacement = config.getBoolean("item-protection.enable-replacement", false);
        enableItemRemoval = config.getBoolean("item-protection.enable-removal", false);
        
        // Load enhanced dependency settings
        enableEnhancedDependencyCheck = config.getBoolean("dependency-check.enhanced-check", true);
        allowOptionalDependencies = config.getBoolean("dependency-check.allow-optional", true);
        useLooseVersionCheck = config.getBoolean("dependency-check.loose-version-check", true);
        
        plugin.debug("Configuration loaded successfully");
    }
    
    public void saveConfig() {
        // Save main settings
        config.set("enable-plugin-loading", pluginLoadingEnabled);
        config.set("debug-logging", debugLoggingEnabled);
        config.set("plugins-directory", pluginsDirectory);
        config.set("auto-reload-plugins", autoReloadPlugins);
        config.set("max-plugins", maxPlugins);
        config.set("allow-incompatible-plugins", allowIncompatiblePlugins);
        
        // Save event adapter settings
        config.set("event-adapter.enabled", eventAdapterEnabled);
        config.set("event-adapter.priority-handling", priorityHandling);
        config.set("event-adapter.event-filtering", eventFiltering);
        
        // Save command adapter settings
        config.set("command-adapter.enabled", commandAdapterEnabled);
        config.set("command-adapter.enable-aliases", enableAliases);
        config.set("command-adapter.enable-permissions", enablePermissions);
        
        // Save logging settings
        config.set("logging.log-plugin-loading", logPluginLoading);
        config.set("logging.log-plugin-errors", logPluginErrors);
        config.set("logging.log-command-execution", logCommandExecution);
        config.set("logging.log-event-handling", logEventHandling);
        
        // Save security settings
        config.set("security.verify-plugin-signatures", verifyPluginSignatures);
        config.set("security.allow-unsigned-plugins", allowUnsignedPlugins);
        config.set("security.whitelist-plugins", whitelistPlugins);
        config.set("security.blacklist-plugins", blacklistPlugins);
        
        plugin.saveConfig();
        plugin.debug("Configuration saved successfully");
    }
    
    // Getters and setters
    public boolean isPluginLoadingEnabled() {
        return pluginLoadingEnabled;
    }
    
    public void setPluginLoadingEnabled(boolean enabled) {
        this.pluginLoadingEnabled = enabled;
        config.set("enable-plugin-loading", enabled);
    }
    
    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }
    
    public void setDebugLoggingEnabled(boolean enabled) {
        this.debugLoggingEnabled = enabled;
        config.set("debug-logging", enabled);
    }
    
    public String getPluginsDirectory() {
        return pluginsDirectory;
    }
    
    public void setPluginsDirectory(String directory) {
        this.pluginsDirectory = directory;
        config.set("plugins-directory", directory);
    }
    
    public boolean isAutoReloadPlugins() {
        return autoReloadPlugins;
    }
    
    public void setAutoReloadPlugins(boolean autoReload) {
        this.autoReloadPlugins = autoReload;
        config.set("auto-reload-plugins", autoReload);
    }
    
    public int getMaxPlugins() {
        return maxPlugins;
    }
    
    public void setMaxPlugins(int max) {
        this.maxPlugins = max;
        config.set("max-plugins", max);
    }
    
    public boolean isAllowIncompatiblePlugins() {
        return allowIncompatiblePlugins;
    }
    
    public void setAllowIncompatiblePlugins(boolean allow) {
        this.allowIncompatiblePlugins = allow;
        config.set("allow-incompatible-plugins", allow);
    }
    
    public boolean isEventAdapterEnabled() {
        return eventAdapterEnabled;
    }
    
    public void setEventAdapterEnabled(boolean enabled) {
        this.eventAdapterEnabled = enabled;
        config.set("event-adapter.enabled", enabled);
    }
    
    public String getPriorityHandling() {
        return priorityHandling;
    }
    
    public void setPriorityHandling(String priority) {
        this.priorityHandling = priority;
        config.set("event-adapter.priority-handling", priority);
    }
    
    public boolean isEventFiltering() {
        return eventFiltering;
    }
    
    public void setEventFiltering(boolean filtering) {
        this.eventFiltering = filtering;
        config.set("event-adapter.event-filtering", filtering);
    }
    
    public boolean isCommandAdapterEnabled() {
        return commandAdapterEnabled;
    }
    
    public void setCommandAdapterEnabled(boolean enabled) {
        this.commandAdapterEnabled = enabled;
        config.set("command-adapter.enabled", enabled);
    }
    
    public boolean isEnableAliases() {
        return enableAliases;
    }
    
    public void setEnableAliases(boolean enabled) {
        this.enableAliases = enabled;
        config.set("command-adapter.enable-aliases", enabled);
    }
    
    public boolean isEnablePermissions() {
        return enablePermissions;
    }
    
    public void setEnablePermissions(boolean enabled) {
        this.enablePermissions = enabled;
        config.set("command-adapter.enable-permissions", enabled);
    }
    
    public boolean isLogPluginLoading() {
        return logPluginLoading;
    }
    
    public void setLogPluginLoading(boolean log) {
        this.logPluginLoading = log;
        config.set("logging.log-plugin-loading", log);
    }
    
    public boolean isLogPluginErrors() {
        return logPluginErrors;
    }
    
    public void setLogPluginErrors(boolean log) {
        this.logPluginErrors = log;
        config.set("logging.log-plugin-errors", log);
    }
    
    public boolean isLogCommandExecution() {
        return logCommandExecution;
    }
    
    public void setLogCommandExecution(boolean log) {
        this.logCommandExecution = log;
        config.set("logging.log-command-execution", log);
    }
    
    public boolean isLogEventHandling() {
        return logEventHandling;
    }
    
    public void setLogEventHandling(boolean log) {
        this.logEventHandling = log;
        config.set("logging.log-event-handling", log);
    }
    
    public boolean isVerifyPluginSignatures() {
        return verifyPluginSignatures;
    }
    
    public void setVerifyPluginSignatures(boolean verify) {
        this.verifyPluginSignatures = verify;
        config.set("security.verify-plugin-signatures", verify);
    }
    
    public boolean isAllowUnsignedPlugins() {
        return allowUnsignedPlugins;
    }
    
    public void setAllowUnsignedPlugins(boolean allow) {
        this.allowUnsignedPlugins = allow;
        config.set("security.allow-unsigned-plugins", allow);
    }
    
    public List<String> getWhitelistPlugins() {
        return whitelistPlugins;
    }
    
    public void setWhitelistPlugins(List<String> whitelist) {
        this.whitelistPlugins = whitelist;
        config.set("security.whitelist-plugins", whitelist);
    }
    
    public List<String> getBlacklistPlugins() {
        return blacklistPlugins;
    }
    
    public void setBlacklistPlugins(List<String> blacklist) {
        this.blacklistPlugins = blacklist;
        config.set("security.blacklist-plugins", blacklist);
    }
    
    public boolean isItemProtectionEnabled() {
        return itemProtectionEnabled;
    }
    
    public void setItemProtectionEnabled(boolean enabled) {
        this.itemProtectionEnabled = enabled;
        config.set("item-protection.enabled", enabled);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public boolean isPluginWhitelisted(String pluginName) {
        if (whitelistPlugins.isEmpty()) {
            return true; // If whitelist is empty, allow all
        }
        return whitelistPlugins.contains(pluginName);
    }
    
    public boolean isPluginBlacklisted(String pluginName) {
        return blacklistPlugins.contains(pluginName);
    }
    
    public boolean canLoadPlugin(String pluginName) {
        return !isPluginBlacklisted(pluginName) && isPluginWhitelisted(pluginName);
    }
    
    // Dependency check settings
    public boolean isDependencyCheckEnabled() {
        return enableDependencyCheck;
    }
    
    public void setDependencyCheckEnabled(boolean enabled) {
        this.enableDependencyCheck = enabled;
        config.set("dependency-check.enabled", enabled);
    }
    
    public boolean isConflictDetectionEnabled() {
        return enableConflictDetection;
    }
    
    public void setConflictDetectionEnabled(boolean enabled) {
        this.enableConflictDetection = enabled;
        config.set("dependency-check.conflict-detection", enabled);
    }
    
    public boolean isEnforceVersionCompatibility() {
        return enforceVersionCompatibility;
    }
    
    public void setEnforceVersionCompatibility(boolean enabled) {
        this.enforceVersionCompatibility = enabled;
        config.set("dependency-check.enforce-version-compatibility", enabled);
    }
    
    // Priority loading settings
    public boolean isPriorityLoadingEnabled() {
        return enablePriorityLoading;
    }
    
    public void setPriorityLoadingEnabled(boolean enabled) {
        this.enablePriorityLoading = enabled;
        config.set("priority-loading.enabled", enabled);
    }
    
    public java.util.List<String> getPriorityMods() {
        return priorityMods;
    }
    
    public void setPriorityMods(java.util.List<String> priorityMods) {
        this.priorityMods = priorityMods;
        config.set("priority-loading.priority-mods", priorityMods);
    }
    
    public boolean isPriorityMod(String modId) {
        return priorityMods != null && priorityMods.contains(modId);
    }
    
    // Item protection settings
    public String getItemProtectionAction() {
        return itemProtectionAction;
    }
    
    public void setItemProtectionAction(String action) {
        this.itemProtectionAction = action;
        config.set("item-protection.action", action);
    }
    
    public boolean isItemReplacementEnabled() {
        return enableItemReplacement;
    }
    
    public void setItemReplacementEnabled(boolean enabled) {
        this.enableItemReplacement = enabled;
        config.set("item-protection.enable-replacement", enabled);
    }
    
    public boolean isItemRemovalEnabled() {
        return enableItemRemoval;
    }
    
    public void setItemRemovalEnabled(boolean enabled) {
        this.enableItemRemoval = enabled;
        config.set("item-protection.enable-removal", enabled);
    }
    
    // Enhanced dependency settings
    public boolean isEnhancedDependencyCheckEnabled() {
        return enableEnhancedDependencyCheck;
    }
    
    public void setEnhancedDependencyCheckEnabled(boolean enabled) {
        this.enableEnhancedDependencyCheck = enabled;
        config.set("dependency-check.enhanced-check", enabled);
    }
    
    public boolean isAllowOptionalDependencies() {
        return allowOptionalDependencies;
    }
    
    public void setAllowOptionalDependencies(boolean enabled) {
        this.allowOptionalDependencies = enabled;
        config.set("dependency-check.allow-optional", enabled);
    }
    
    public boolean isUseLooseVersionCheck() {
        return useLooseVersionCheck;
    }
    
    public void setUseLooseVersionCheck(boolean enabled) {
        this.useLooseVersionCheck = enabled;
        config.set("dependency-check.loose-version-check", enabled);
    }
}