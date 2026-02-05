package com.fabricbukkit.loader;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 增强版物品保护系统，专门处理创造模式物品栏数据包问题
 */
public class EnhancedItemProtectionSystem implements Listener {
    
    private final FabricBukkitLoader plugin;
    
    public EnhancedItemProtectionSystem(FabricBukkitLoader plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 玩家加入时检查背包
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延迟检查，确保玩家完全加载
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getConfigManager().isItemProtectionEnabled()) {
                    cleanPlayerInventory(player);
                }
            }
        }.runTaskLater(plugin, 20L); // 延迟1秒执行
    }
    
    /**
     * 当玩家切换到创造模式时，清理可能引起问题的物品
     */
    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        
        if (event.getNewGameMode() != GameMode.CREATIVE) {
            return; // 只处理切换到创造模式的情况
        }
        
        // 延迟执行，确保Gamemode变更完成
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getGameMode() == GameMode.CREATIVE) {
                    cleanCreativeInventory(player);
                }
            }
        }.runTaskLater(plugin, 1L); // 延迟1个tick执行
    }
    
    /**
     * 处理背包点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (isProblematicItem(clickedItem)) {
            handleProblematicItemClick(player, clickedItem);
            event.setCancelled(true); // 取消操作
        }
    }
    
    /**
     * 处理背包拖拽事件
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // 检查拖拽的物品
        for (ItemStack item : event.getNewItems().values()) {
            if (isProblematicItem(item)) {
                handleProblematicItemClick(player, item);
                event.setCancelled(true); // 取消拖拽操作
                return;
            }
        }
    }
    
    /**
     * 处理主副手交换事件
     */
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        
        if (isProblematicItem(mainHand) || isProblematicItem(offHand)) {
            event.setCancelled(true); // 取消交换
            // 尝试清理物品
            if (player.getGameMode() == GameMode.CREATIVE) {
                cleanPlayerInventory(player);
            }
        }
    }
    
    /**
     * 处理方块放置事件
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        ItemStack item = event.getItemInHand();
        if (isProblematicItem(item)) {
            event.setCancelled(true); // 取消放置
            event.getPlayer().sendMessage(ChatColor.RED + "[FabricBukkitLoader] 检测到问题物品，操作已取消");
        }
    }
    
    /**
     * 处理玩家交互事件
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item != null && isProblematicItem(item)) {
            event.setCancelled(true); // 取消交互
            event.getPlayer().sendMessage(ChatColor.RED + "[FabricBukkitLoader] 检测到问题物品，操作已取消");
        }
    }
    
    /**
     * 清理玩家背包中的问题物品
     */
    private void cleanPlayerInventory(Player player) {
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
     * 清理创造模式物品
     */
    private void cleanCreativeInventory(Player player) {
        cleanPlayerInventory(player);
    }
    
    /**
     * 检查物品是否可能引起问题
     */
    private boolean isProblematicItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        try {
            // 检查是否有物品元数据
            if (item.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                
                // 检查是否有自定义模型数据（通常是mod物品的标志）
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
                
                // 检查是否有自定义lore（可能包含mod信息）
                if (meta.hasLore()) {
                    for (String loreLine : meta.getLore()) {
                        if (containsModKeywords(loreLine.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
            
            // 检查物品类型是否为原版类型（非mod添加的类型）
            // 如果不是原版类型，很可能是mod添加的物品
            String typeName = item.getType().name();
            if (isNonVanillaItemType(typeName)) {
                return true;
            }
            
        } catch (Exception e) {
            // 如果无法检查物品信息，则假设有问题
            plugin.getLogger().warning("无法检查物品信息，假定为问题物品: " + item.getType().name());
            return true;
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
     * 检查物品类型是否为非原版类型
     */
    private boolean isNonVanillaItemType(String typeName) {
        // 原版物品类型通常不包含下划线前缀或其他mod特定的前缀
        // 但需要注意原版物品ID本身也可能包含下划线
        // 更好的方法是检查是否存在mod特定的命名空间
        return typeName.contains(":") && !typeName.startsWith("minecraft:");
    }
    
    /**
     * 处理问题物品的点击
     */
    private void handleProblematicItemClick(Player player, ItemStack item) {
        String action = plugin.getConfigManager().getItemProtectionAction();
        plugin.getLogger().info("玩家 " + player.getName() + " 尝试操作问题物品: " + 
                               item.getType().name() + " (保护操作: " + action + ")");
        
        switch (action.toLowerCase()) {
            case "replace":
                // 替换为安全的原版物品（如果启用了替换功能）
                if (plugin.getConfigManager().isItemReplacementEnabled()) {
                    // 暂时简单地将物品设为null
                    player.setItemOnCursor(null);
                }
                break;
            case "warn":
                // 仅记录警告
                player.sendMessage(ChatColor.RED + "警告: 检测到可能的问题物品，操作已取消");
                break;
            case "remove":
                // 已在事件处理中取消操作
                break;
            case "protect":
            default:
                // 已在事件处理中取消操作
                break;
        }
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