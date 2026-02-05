package com.fabricbukkit.loader;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.ChatColor;

/**
 * 专门处理创造模式物品栏数据包问题的保护系统
 */
public class CreativeModeSlotProtection implements Listener {
    
    private final FabricBukkitLoader plugin;
    
    public CreativeModeSlotProtection(FabricBukkitLoader plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 当玩家切换到创造模式时，清理可能引起问题的物品
     */
    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.CREATIVE) {
            return; // 只处理切换到创造模式的情况
        }
        
        Player player = event.getPlayer();
        
        // 延迟执行，确保Gamemode变更完成
        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.getGameMode() == GameMode.CREATIVE) {
                    cleanCreativeInventory(player);
                }
            }
        };
        task.runTaskLater(plugin, 1L); // 延迟1个tick执行
    }
    
    /**
     * 清理可能引起问题的创造模式物品
     */
    private void cleanCreativeInventory(Player player) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        PlayerInventory inventory = player.getInventory();
        boolean hasProblematicItems = false;
        
        // 检查主手物品
        ItemStack mainHand = inventory.getItemInMainHand();
        if (isProblematicItem(mainHand)) {
            hasProblematicItems = true;
            handleProblematicItem(player, "主手", mainHand);
            inventory.setItemInMainHand(null);
        }
        
        // 检查副手物品
        ItemStack offHand = inventory.getItemInOffHand();
        if (isProblematicItem(offHand)) {
            hasProblematicItems = true;
            handleProblematicItem(player, "副手", offHand);
            inventory.setItemInOffHand(null);
        }
        
        // 检查背包物品
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && isProblematicItem(item)) {
                hasProblematicItems = true;
                handleProblematicItem(player, "背包槽位 " + i, item);
                inventory.setItem(i, null);
            }
        }
        
        if (hasProblematicItems) {
            String action = plugin.getConfigManager().getItemProtectionAction();
            player.sendMessage(ChatColor.YELLOW + "[FabricBukkitLoader] 已检测到可能引起连接问题的物品，已进行处理 (" + action + ")");
        }
    }
    
    /**
     * 检查物品是否可能引起问题
     */
    private boolean isProblematicItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // 检查是否有自定义模型数据（通常是mod物品的标志）
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta.hasCustomModelData()) {
                return true;
            }
            
            // 检查是否有自定义显示名称（可能包含mod信息）
            if (meta.hasDisplayName()) {
                String displayName = meta.getDisplayName().toLowerCase();
                // 检查常见的mod关键词
                if (containsModKeywords(displayName)) {
                    return true;
                }
            }
            
            // 检查是否有自定义NBT标签
            try {
                // 检查是否有包含mod特定信息的lore
                if (meta.hasLore()) {
                    for (String loreLine : meta.getLore()) {
                        if (containsModKeywords(loreLine.toLowerCase())) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                // 如果无法读取lore，则假设有问题
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查字符串是否包含mod关键词
     */
    private boolean containsModKeywords(String text) {
        String[] keywords = {
            "mod", "fabric", "forge", "custom", "waystones", "balm", 
            "modded", "unidict", "mekanism", "thermal", "immersive",
            "quark", "dank", "ae2", "refined", "botania", "mystical",
            "mekanism", "pneumatic", "create", "industrial", "tech"
        };
        
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 处理问题物品
     */
    private void handleProblematicItem(Player player, String location, ItemStack item) {
        String action = plugin.getConfigManager().getItemProtectionAction();
        plugin.getLogger().info("在 " + player.getName() + " 的 " + location + 
                               " 检测到问题物品: " + item.getType().name() + 
                               " (保护操作: " + action + ")");
        
        switch (action.toLowerCase()) {
            case "replace":
                // 替换为安全的原版物品（如果启用了替换功能）
                if (plugin.getConfigManager().isItemReplacementEnabled()) {
                    // 可以根据需要替换为安全的原版物品
                }
                break;
            case "warn":
                // 仅记录警告
                player.sendMessage(ChatColor.RED + "警告: " + location + " 中检测到可能的问题物品");
                break;
            case "remove":
                // 已在上面的代码中移除
                break;
            case "protect":
            default:
                // 已在上面的代码中处理
                break;
        }
    }
}