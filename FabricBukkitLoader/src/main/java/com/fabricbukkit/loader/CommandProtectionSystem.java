package com.fabricbukkit.loader;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.ChatColor;

/**
 * 指令执行保护系统，允许必要的mod指令执行同时防止潜在问题
 */
public class CommandProtectionSystem implements Listener {
    
    private final FabricBukkitLoader plugin;
    
    public CommandProtectionSystem(FabricBukkitLoader plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理玩家指令预处理事件
     */
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        String command = event.getMessage().toLowerCase();
        
        // 检查是否是安全的mod指令
        if (isSafeModCommand(command)) {
            // 允许指令执行，但记录日志
            plugin.debug("允许执行mod指令: " + command + " 玩家: " + event.getPlayer().getName());
            return;
        }
        
        // 如果指令被阻止，检查配置并决定是否允许
        String protectionAction = plugin.getConfigManager().getItemProtectionAction();
        if ("protect".equals(protectionAction) || "warn".equals(protectionAction)) {
            // 允许大部分mod指令执行，只阻止已知危险的指令
            if (isDangerousCommand(command)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "[FabricBukkitLoader] 此指令被阻止以防止潜在问题");
                plugin.getLogger().warning("阻止了危险指令: " + command + " 玩家: " + event.getPlayer().getName());
            } else {
                // 允许指令执行
                plugin.debug("允许执行指令: " + command + " 玩家: " + event.getPlayer().getName());
            }
        } else if ("remove".equals(protectionAction)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "[FabricBukkitLoader] 为保护服务器稳定，此指令被阻止");
        }
    }
    
    /**
     * 处理服务器指令事件
     */
    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        String command = event.getCommand().toLowerCase();
        
        // 服务器端指令通常更安全，但也要检查
        if (isDangerousCommand(command)) {
            event.setCancelled(true);
            plugin.getLogger().warning("阻止了危险服务器指令: " + command);
        }
    }
    
    /**
     * 判断是否是安全的mod指令
     */
    private boolean isSafeModCommand(String command) {
        // 常见的mod指令前缀
        String[] safeCommandPrefixes = {
            "/waystone", "/waypoint", "/waystones", 
            "/ftbchunks", "/ftbteams", 
            "/kubejs", 
            "/jei", "/rei",
            "/create", "/deployer", "/sequenced_assembly",
            "/pneumaticcraft", "/drones",
            "/mekanism", "/thermal", "/immersiveengineering",
            "/botania", "/mythicbotany",
            "/apotheosis", "/quark",
            "/dank", "/dankstorage",
            "/refinedstorage", "/ae2", "/ae2fc",
            "/chipped", "/configured", "/configured-server",
            "/buildinggadgets", "/gadget",
            "/mininggadgets", "/magnet",
            "/tanknull", "/waila", "/theoneprobe",
            "/ftbultimine", "/ftbmoney",
            "/sophisticatedbackpacks", "/sba",
            "/sophisticatedstorage", "/ss",
            "/extendedcrafting", "/table",
            "/naturescompass", "/waypoint",
            "/fastleafdecay", "/sampler",
            "/itemfilters", "/filter",
            "/trashcans", "/trash",
            "/torchmaster", "/megatorch",
            "/curios", "/slots",
            "/balm", "/balm-mod",
            "/farmersdelight", "/farmersd",
            "/cyclic", "/cyclic-commands"
        };
        
        for (String prefix : safeCommandPrefixes) {
            if (command.startsWith(prefix)) {
                return true;
            }
        }
        
        // 检查是否包含mod相关的关键词
        String[] modKeywords = {
            "waystone", "waypoint", "ftb", "kubejs", "jei", "rei", 
            "create", "mekanism", "thermal", "immersive", "botania",
            "apotheosis", "quark", "dank", "refined", "ae2", "chipped",
            "buildinggadgets", "mininggadgets", "waila", "theoneprobe",
            "tanknull", "torchmaster", "curios", "farmersdelight", "cyclic"
        };
        
        for (String keyword : modKeywords) {
            if (command.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否是危险的指令
     */
    private boolean isDangerousCommand(String command) {
        // 危险指令模式，可能引起数据包问题
        String[] dangerousPatterns = {
            "/setblockdata", "/setnbt", "/nbt", 
            "/execute if entity", "/execute store",
            "/data ", "/data merge", "/data modify",
            "/fill ", "/clone ", "/setblock "
        };
        
        for (String pattern : dangerousPatterns) {
            if (command.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
}