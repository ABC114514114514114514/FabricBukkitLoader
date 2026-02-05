package com.fabricbukkit.loader;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Level;
import java.util.jar.JarFile;

/**
 * FabricBukkitLoader - A Paper plugin that enables loading Fabric mods on Paper servers
 * 
 * This plugin provides a compatibility layer between Fabric mods and Paper servers,
 * allowing Fabric mods to be loaded and run on Paper-based Minecraft servers.
 */
public class FabricBukkitLoader extends JavaPlugin implements Listener {
    
    private static FabricBukkitLoader instance;
    private final Map<String, LoadedMod> loadedMods = new HashMap<>();
    private ModConfigManager configManager;
    private ModLoader modLoader;
    private EventAdapter eventAdapter;
    private CommandAdapter commandAdapter;
    private PerformanceMonitor performanceMonitor;
    private UpdateChecker updateChecker;
    private SecurityScanner securityScanner;
    private ItemProtectionSystem itemProtectionSystem;
    private CreativeModeSlotProtection creativeModeSlotProtection;
    private EnhancedItemProtectionSystem enhancedItemProtectionSystem;
    private SmartItemProtectionSystem smartItemProtectionSystem;
    private CommandProtectionSystem commandProtectionSystem;
    private boolean isEnabled = false;
    
    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("========================================");
        getLogger().info("FabricBukkitLoader v" + getDescription().getVersion());
        getLogger().info("正在加载Fabric兼容层...");
        getLogger().info("========================================");
        
        // Initialize configuration
        saveDefaultConfig();
        configManager = new ModConfigManager(this);
        
        // Initialize adapters
        eventAdapter = new EventAdapter(this);
        commandAdapter = new CommandAdapter(this);
        
        // Initialize mod loader
        modLoader = new ModLoader(this);
        
        // Initialize performance monitor
        performanceMonitor = new PerformanceMonitor();
        
        // Initialize update checker
        updateChecker = new UpdateChecker(this);
        
        // Initialize security scanner
        securityScanner = new SecurityScanner(this);
        
        // Initialize item protection system
        itemProtectionSystem = new ItemProtectionSystem(this);
        
        // Initialize creative mode slot protection
        creativeModeSlotProtection = new CreativeModeSlotProtection(this);
        
        // Initialize enhanced item protection system
        enhancedItemProtectionSystem = new EnhancedItemProtectionSystem(this);
        
        // Initialize smart item protection system
        smartItemProtectionSystem = new SmartItemProtectionSystem(this);
        
        // Initialize command protection system
        commandProtectionSystem = new CommandProtectionSystem(this);
        
        getLogger().info("FabricBukkitLoader加载成功！");
    }
    
    @Override
    public void onEnable() {
        getLogger().info("正在启用FabricBukkitLoader...");
        
        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(eventAdapter, this);
        getServer().getPluginManager().registerEvents(itemProtectionSystem, this);
        getServer().getPluginManager().registerEvents(creativeModeSlotProtection, this);
        getServer().getPluginManager().registerEvents(enhancedItemProtectionSystem, this);
        getServer().getPluginManager().registerEvents(smartItemProtectionSystem, this);
        getServer().getPluginManager().registerEvents(commandProtectionSystem, this);
        
        // Register command adapter
        commandAdapter.registerCommands();
        
        // Load configuration
        reloadConfig();
        configManager.loadConfig();
        
        // Create necessary directories
        createDirectories();
        
        // Load Fabric mods asynchronously
        if (configManager.isPluginLoadingEnabled()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    loadFabricMods();
                }
            }.runTaskLater(this, 20L); // Load 1 second after server starts
        }
        
        isEnabled = true;
        getLogger().info("========================================");
        getLogger().info("FabricBukkitLoader启用成功！");
        getLogger().info("使用 /fabricloader help 查看命令");
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("正在禁用FabricBukkitLoader...");
        
        // Unload all loaded mods
        unloadAllMods();
        
        // Save configuration
        if (configManager != null) {
            configManager.saveConfig();
        }
        
        isEnabled = false;
        getLogger().info("FabricBukkitLoader已禁用！");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fabricloader") || 
            command.getName().equalsIgnoreCase("fbl")) {
            return handleFabricLoaderCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("fblmods")) {
            return handleModsCommand(sender);
        }
        return false;
    }
    
    private boolean handleFabricLoaderCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;
                
            case "reload":
                if (!sender.hasPermission("fabricbukkit.loader.reload")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                reloadPlugin(sender);
                break;
                
            case "mods":
                if (!sender.hasPermission("fabricbukkit.loader.mods")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                listMods(sender);
                break;
                
            case "info":
                if (!sender.hasPermission("fabricbukkit.loader.info")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                showInfo(sender);
                break;
                
            case "load":
                if (!sender.hasPermission("fabricbukkit.loader.admin")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /fabricloader load <mod文件>");
                    return true;
                }
                loadMod(sender, args[1]);
                break;
                
            case "unload":
                if (!sender.hasPermission("fabricbukkit.loader.admin")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /fabricloader unload <mod-id>");
                    return true;
                }
                unloadMod(sender, args[1]);
                break;
                
            case "perf":
            case "performance":
                if (!sender.hasPermission("fabricbukkit.loader.admin")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /fabricloader perf <report|mod <mod-id>|reset>");
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("report")) {
                    sender.sendMessage(ChatColor.GOLD + "性能报告:");
                    sender.sendMessage(performanceMonitor.getPerformanceReport());
                } else if (args[1].equalsIgnoreCase("mod") && args.length >= 3) {
                    sender.sendMessage(ChatColor.GOLD + "Mod性能报告 (" + args[2] + "):");
                    sender.sendMessage(performanceMonitor.getModReport(args[2]));
                } else if (args[1].equalsIgnoreCase("reset")) {
                    performanceMonitor.resetStats();
                    sender.sendMessage(ChatColor.GREEN + "性能统计数据已重置。");
                } else {
                    sender.sendMessage(ChatColor.RED + "用法: /fabricloader perf <report|mod <mod-id>|reset>");
                }
                break;
                
            case "update":
            case "updates":
                if (!sender.hasPermission("fabricbukkit.loader.admin")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                // Check for updates asynchronously
                sender.sendMessage(ChatColor.YELLOW + "正在检查mod更新...");
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        String updateReport = FabricBukkitLoader.this.getUpdateChecker().getUpdateReport();
                        sender.sendMessage(updateReport);
                    }
                }.runTaskAsynchronously(this);
                break;
                
            case "scan":
            case "security":
                if (!sender.hasPermission("fabricbukkit.loader.admin")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "正在扫描所有mod的安全问题...");
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        // Get all mod files from the mods directory
                        java.io.File serverRoot = getServer().getWorldContainer().getParentFile();
                        java.io.File modsDir = new java.io.File(serverRoot, "mods");
                        
                        if (!modsDir.exists() || !modsDir.isDirectory()) {
                            sender.sendMessage(ChatColor.RED + "Mods目录不存在: " + modsDir.getAbsolutePath());
                            return;
                        }
                        
                        java.io.File[] modFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".zip"));
                        
                        if (modFiles == null || modFiles.length == 0) {
                            sender.sendMessage(ChatColor.YELLOW + "在mods目录中未找到mod文件。");
                            return;
                        }
                        
                        // Scan each mod file
                        for (java.io.File modFile : modFiles) {
                            SecurityScanner.SecurityReport report = getSecurityScanner().scanMod(modFile);
                            sender.sendMessage(report.getFormattedReport());
                        }
                    }
                }.runTaskAsynchronously(this);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "未知子命令！使用 /fabricloader help 查看可用命令。");
                break;
        }
        
        return true;
    }
    
    private boolean handleModsCommand(CommandSender sender) {
        if (!sender.hasPermission("fabricbukkit.loader.mods")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        listMods(sender);
        return true;
    }
    
        private void sendHelp(CommandSender sender) {
            sender.sendMessage(ChatColor.GOLD + "========== FabricBukkitLoader 帮助 ==========");
            sender.sendMessage(ChatColor.YELLOW + "/fabricloader help" + ChatColor.WHITE + " - 显示此帮助信息");
            
            if (sender.hasPermission("fabricbukkit.loader.mods")) {
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader mods" + ChatColor.WHITE + " - 列出已加载的Fabric mods");
            }
            
            if (sender.hasPermission("fabricbukkit.loader.info")) {
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader info" + ChatColor.WHITE + " - 显示插件信息");
            }
            
            if (sender.hasPermission("fabricbukkit.loader.reload")) {
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader reload" + ChatColor.WHITE + " - 重载插件");
            }
            
            if (sender.hasPermission("fabricbukkit.loader.admin")) {
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader load <mod文件>" + ChatColor.WHITE + " - 加载指定mod");
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader unload <mod-id>" + ChatColor.WHITE + " - 卸载指定mod");
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader perf report" + ChatColor.WHITE + " - 显示性能报告");
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader perf mod <mod-id>" + ChatColor.WHITE + " - 显示特定mod性能报告");
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader perf reset" + ChatColor.WHITE + " - 重置性能统计数据");
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader update" + ChatColor.WHITE + " - 检查mod更新");
                sender.sendMessage(ChatColor.YELLOW + "/fabricloader scan" + ChatColor.WHITE + " - 扫描mod安全问题");
            }
            
            sender.sendMessage(ChatColor.YELLOW + "/fblmods" + ChatColor.WHITE + " - 快捷方式：列出已加载mods");
            sender.sendMessage(ChatColor.GOLD + "==========================================");
        }
        
        private void reloadPlugin(CommandSender sender) {
            sender.sendMessage(ChatColor.YELLOW + "正在重载FabricBukkitLoader...");
            
            // Save current state
            configManager.saveConfig();
            
            // Reload configuration
            reloadConfig();
            configManager.loadConfig();
            
            // Reload mods
            unloadAllMods();
            
            if (configManager.isPluginLoadingEnabled()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        loadFabricMods();
                        sender.sendMessage(ChatColor.GREEN + "FabricBukkitLoader重载成功！");
                    }
                }.runTaskLater(this, 20L);
            } else {
                sender.sendMessage(ChatColor.GREEN + "FabricBukkitLoader重载成功！");
            }
        }
        
        private void showInfo(CommandSender sender) {
            sender.sendMessage(ChatColor.GOLD + "========== FabricBukkitLoader 信息 ==========");
            sender.sendMessage(ChatColor.YELLOW + "版本: " + ChatColor.WHITE + getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "作者: " + ChatColor.WHITE + getDescription().getAuthors().get(0));
            sender.sendMessage(ChatColor.YELLOW + "网站: " + ChatColor.WHITE + getDescription().getWebsite());
            sender.sendMessage(ChatColor.YELLOW + "状态: " + ChatColor.GREEN + (isEnabled ? "已启用" : "已禁用"));
            sender.sendMessage(ChatColor.YELLOW + "已加载Mods: " + ChatColor.WHITE + loadedMods.size());
            sender.sendMessage(ChatColor.YELLOW + "插件加载: " + ChatColor.WHITE + (configManager.isPluginLoadingEnabled() ? "已启用" : "已禁用"));
            sender.sendMessage(ChatColor.YELLOW + "调试日志: " + ChatColor.WHITE + (configManager.isDebugLoggingEnabled() ? "已启用" : "已禁用"));
            sender.sendMessage(ChatColor.GOLD + "==========================================");
        }
        
        private void listMods(CommandSender sender) {
            if (loadedMods.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "当前没有加载任何Fabric mods。");
                return;
            }
            
            sender.sendMessage(ChatColor.GOLD + "========== 已加载的Fabric Mods (" + loadedMods.size() + "个) ==========");
            
            for (LoadedMod mod : loadedMods.values()) {
                String statusColor = mod.isEnabled() ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
                sender.sendMessage(ChatColor.AQUA + mod.getId() + ChatColor.WHITE + " - " +
                                 ChatColor.YELLOW + "v" + mod.getVersion() + ChatColor.WHITE + " [" +
                                 statusColor + (mod.isEnabled() ? "已启用" : "已禁用") + ChatColor.WHITE + "]");
                
                if (mod.getDescription() != null && !mod.getDescription().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  " + mod.getDescription());
                }
            }
            
            sender.sendMessage(ChatColor.GOLD + "==========================================");
        }    
    private void loadMod(CommandSender sender, String modFileName) {
        File modFile = new File(getDataFolder(), "mods" + File.separator + modFileName);
        
        if (!modFile.exists()) {
            sender.sendMessage(ChatColor.RED + "未找到mod文件: " + modFileName);
            sender.sendMessage(ChatColor.YELLOW + "请确保mod文件在服务器根目录的mods/文件夹中");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "正在加载mod: " + modFileName);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    LoadedMod mod = modLoader.loadMod(modFile);
                    if (mod != null) {
                        loadedMods.put(mod.getId(), mod);
                        sender.sendMessage(ChatColor.GREEN + "成功加载mod: " + mod.getName() + " v" + mod.getVersion());
                    } else {
                        sender.sendMessage(ChatColor.RED + "加载mod失败: " + modFileName);
                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "加载mod时出错: " + e.getMessage());
                    getLogger().log(Level.SEVERE, "加载mod时出错: " + modFileName, e);
                }
            }
        }.runTaskAsynchronously(this);
    }
    
    private void unloadMod(CommandSender sender, String modId) {
        LoadedMod mod = loadedMods.get(modId);
        
        if (mod == null) {
            sender.sendMessage(ChatColor.RED + "未找到mod: " + modId);
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "正在卸载mod: " + modId);
        
        try {
            modLoader.unloadMod(mod);
            loadedMods.remove(modId);
            sender.sendMessage(ChatColor.GREEN + "成功卸载mod: " + modId);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "卸载mod时出错: " + e.getMessage());
            getLogger().log(Level.SEVERE, "卸载mod时出错: " + modId, e);
        }
    }
    
    private void createDirectories() {
        // Create mods directory in server root
        File serverRoot = getServer().getWorldContainer().getParentFile();
        File modsDir = new File(serverRoot, "mods");
        if (!modsDir.exists()) {
            modsDir.mkdirs();
            getLogger().info("已创建mods目录: " + modsDir.getAbsolutePath());
        }
        
        // Create libs directory in plugin data folder
        File libsDir = new File(getDataFolder(), "libs");
        if (!libsDir.exists()) {
            libsDir.mkdirs();
            getLogger().info("已创建libs目录: " + libsDir.getAbsolutePath());
        }
    }
    
    private void loadFabricMods() {
        if (!configManager.isPluginLoadingEnabled()) {
            getLogger().info("配置中已禁用插件加载。");
            return;
        }
        
        // Load mods from server root directory
        File serverRoot = getServer().getWorldContainer().getParentFile();
        File modsDir = new File(serverRoot, "mods");
        if (!modsDir.exists() || !modsDir.isDirectory()) {
            getLogger().warning("mods目录不存在: " + modsDir.getAbsolutePath());
            return;
        }
        
        File[] modFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".zip"));
        
        if (modFiles == null || modFiles.length == 0) {
            getLogger().info("在mods目录中未找到mod文件。");
            return;
        }
        
        getLogger().info("找到 " + modFiles.length + " 个潜在的mod文件。正在加载...");
        
        int loadedCount = 0;
        int failedCount = 0;
        
        for (File modFile : modFiles) {
            try {
                getLogger().info("正在加载mod: " + modFile.getName());
                LoadedMod mod = modLoader.loadMod(modFile);
                
                if (mod != null) {
                    loadedMods.put(mod.getId(), mod);
                    loadedCount++;
                    getLogger().info("成功加载mod: " + mod.getName() + " v" + mod.getVersion());
                } else {
                    failedCount++;
                    getLogger().warning("加载mod失败: " + modFile.getName());
                }
            } catch (Exception e) {
                failedCount++;
                getLogger().log(Level.SEVERE, "加载mod时出错: " + modFile.getName(), e);
            }
        }
        
        getLogger().info("========================================");
        getLogger().info("Mod加载完成！");
        getLogger().info("成功: " + loadedCount + " 个mod");
        getLogger().info("失败: " + failedCount + " 个mod");
        getLogger().info("总计: " + loadedMods.size() + " 个mod已加载");
        getLogger().info("========================================");
    }
    
    private void unloadAllMods() {
        getLogger().info("正在卸载所有Fabric mods...");
        
        int count = 0;
        for (LoadedMod mod : loadedMods.values()) {
            try {
                modLoader.unloadMod(mod);
                count++;
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "卸载mod时出错: " + mod.getId(), e);
            }
        }
        
        loadedMods.clear();
        getLogger().info("已卸载 " + count + " 个mod。");
    }
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        // Handle plugin dependencies if needed
    }
    
    // Getters
    public static FabricBukkitLoader getInstance() {
        return instance;
    }
    
    public ModConfigManager getConfigManager() {
        return configManager;
    }
    
    public ModLoader getModLoader() {
        return modLoader;
    }
    
    public EventAdapter getEventAdapter() {
        return eventAdapter;
    }
    
    public CommandAdapter getCommandAdapter() {
        return commandAdapter;
    }
    
    public Map<String, LoadedMod> getLoadedMods() {
        return Collections.unmodifiableMap(loadedMods);
    }
    
    public boolean isPluginEnabled() {
        return isEnabled;
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
    
    public SecurityScanner getSecurityScanner() {
        return securityScanner;
    }
    
    public ItemProtectionSystem getItemProtectionSystem() {
        return itemProtectionSystem;
    }
    
    public CreativeModeSlotProtection getCreativeModeSlotProtection() {
        return creativeModeSlotProtection;
    }
    
    public EnhancedItemProtectionSystem getEnhancedItemProtectionSystem() {
        return enhancedItemProtectionSystem;
    }
    
    public SmartItemProtectionSystem getSmartItemProtectionSystem() {
        return smartItemProtectionSystem;
    }
    
    public CommandProtectionSystem getCommandProtectionSystem() {
        return commandProtectionSystem;
    }
    
    // Logging helpers
    public void debug(String message) {
        if (configManager != null && configManager.isDebugLoggingEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    public void debug(String message, Throwable throwable) {
        if (configManager != null && configManager.isDebugLoggingEnabled()) {
            getLogger().log(Level.INFO, "[DEBUG] " + message, throwable);
        }
    }
}