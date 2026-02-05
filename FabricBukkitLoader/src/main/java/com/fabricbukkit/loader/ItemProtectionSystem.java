package com.fabricbukkit.loader;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

/**
 * 防止玩家因 mod 物品被踢出的保护系统
 * 监听物品相关事件，处理可能导致断开连接的问题
 */
public class ItemProtectionSystem implements Listener {
    
    private final FabricBukkitLoader plugin;
    
    public ItemProtectionSystem(FabricBukkitLoader plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听玩家切换手持物品事件
     */
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        try {
            ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
            if (newItem != null && isProblematicItem(newItem)) {
                // 如果是问题物品，可以替换为安全物品或记录警告
                handleProblematicItem(event.getPlayer(), newItem, "held");
            }
        } catch (Exception e) {
            plugin.debug("Error in PlayerItemHeldEvent handler: " + e.getMessage());
        }
    }
    
    /**
     * 监听玩家与物品交互事件
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            ItemStack item = event.getItem();
            if (item != null && isProblematicItem(item)) {
                handleProblematicItem(event.getPlayer(), item, "interact");
                // 取消可能导致问题的操作
                event.setCancelled(true);
            }
        } catch (Exception e) {
            plugin.debug("Error in PlayerInteractEvent handler: " + e.getMessage());
        }
    }
    
    /**
     * 监听背包点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (event.getCurrentItem() != null && isProblematicItem(event.getCurrentItem())) {
                if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
                    handleProblematicItem(player, event.getCurrentItem(), "inventory-click");
                    event.setCancelled(true);
                }
            }
            
            if (event.getCursor() != null && isProblematicItem(event.getCursor())) {
                if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
                    handleProblematicItem(player, event.getCursor(), "inventory-cursor");
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {
            plugin.debug("Error in InventoryClickEvent handler: " + e.getMessage());
        }
    }
    
    /**
     * 监听背包拖拽事件
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        try {
            if (event.getOldCursor() != null && isProblematicItem(event.getOldCursor())) {
                if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
                    handleProblematicItem(player, event.getOldCursor(), "inventory-drag");
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {
            plugin.debug("Error in InventoryDragEvent handler: " + e.getMessage());
        }
    }
    
    /**
     * 监听主副手交换事件
     */
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        try {
            ItemStack mainHand = event.getMainHandItem();
            ItemStack offHand = event.getOffHandItem();
            
            if (mainHand != null && isProblematicItem(mainHand)) {
                handleProblematicItem(event.getPlayer(), mainHand, "swap-main");
                event.setCancelled(true);
                return;
            }
            
            if (offHand != null && isProblematicItem(offHand)) {
                handleProblematicItem(event.getPlayer(), offHand, "swap-off");
                event.setCancelled(true);
            }
        } catch (Exception e) {
            plugin.debug("Error in PlayerSwapHandItemsEvent handler: " + e.getMessage());
        }
    }
    
    /**
     * 检查物品是否可能引起问题
     * 这里可以扩展逻辑来识别特定的 mod 物品
     */
    private boolean isProblematicItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // 检查是否是空气或基本材料（这些通常不会引起问题）
        if (item.getType() == Material.AIR) {
            return false;
        }
        
        // 检查是否有自定义 NBT 标签，这可能表示是 mod 物品
        try {
            // 由于 Bukkit API 限制，我们无法直接访问所有 NBT 数据
            // 但我们可以检查物品的显示名称或其他特征
            if (item.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                
                // 检查是否有自定义模型数据（通常是 mod 物品的标志）
                if (meta.hasCustomModelData()) {
                    return true;
                }
                
                // 如果物品有自定义显示名称，检查是否包含 mod 相关关键词
                if (meta.hasDisplayName()) {
                    String displayName = meta.getDisplayName();
                    if (containsModKeywords(displayName.toLowerCase())) {
                        return true;
                    }
                }
                
                // 检查物品是否有 lore，某些 mod 物品会在 lore 中添加信息
                if (meta.hasLore()) {
                    for (String loreLine : meta.getLore()) {
                        if (containsModKeywords(loreLine.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            // 如果检查过程中出现异常，假定物品是安全的
            // 遵循处理，避免误报
            plugin.getLogger().warning("Error checking item: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 判断是否为潜在的 mod 物品
     */
    private boolean isPotentialModItem(String displayName) {
        // 检查名称是否可能来自 mod
        // 这里可以根据常见 mod 物品的命名模式进行判断
        displayName = displayName.toLowerCase();
        
        // 一些常见的 mod 物品关键词
        String[] modKeywords = {
            "mekanism", "thermal", "immersive", "tech", "industrial", 
            "botania", "astral", "create", "railcraft", "forestry", 
            "chisel", "tinkers", "refined", "ae2", "applied",
            "ender", "quantum", "draconic", "flux", "pneumatic"
        };
        
        for (String keyword : modKeywords) {
            if (displayName.contains(keyword)) {
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
    private void handleProblematicItem(org.bukkit.entity.Player player, ItemStack item, String action) {
        plugin.debug("Detected problematic item for player " + player.getName() + 
                    " during " + action + ": " + item.getType().name());
        
        // 根据配置决定如何处理问题物品
        String actionType = plugin.getConfigManager().getItemProtectionAction();
        
        switch (actionType.toLowerCase()) {
            case "replace":
                // 将问题物品替换为安全版本
                replaceProblematicItem(player, item, action);
                break;
            case "remove":
                // 从玩家背包中移除问题物品
                removeProblematicItem(player, item, action);
                break;
            case "warn":
                // 仅记录警告
                warnPlayer(player, item, action);
                break;
            case "protect":
            default:
                // 实施保护措施，防止踢出
                protectPlayer(player, item, action);
                break;
        }
    }
    
    /**
     * 用安全物品替换问题物品
     */
    private void replaceProblematicItem(org.bukkit.entity.Player player, ItemStack item, String action) {
        if (plugin.getConfigManager().isItemReplacementEnabled()) {
            ItemStack replacement = new ItemStack(Material.STONE); // 使用安全的原版物品
            replacement.setAmount(item.getAmount());
            
            // 尝试找到物品原本在背包中的位置并替换
            org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
            
            // 如果是手持物品
            if (action.equals("held")) {
                int slot = inventory.getHeldItemSlot();
                inventory.setItem(slot, replacement);
            } else {
                // 寻找并替换相同的物品
                inventory.removeItem(item);
                inventory.addItem(replacement);
            }
            
            player.sendMessage(org.bukkit.ChatColor.RED + "检测到不兼容的物品，已被替换为安全版本。");
        }
    }
    
    /**
     * 移除问题物品
     */
    private void removeProblematicItem(org.bukkit.entity.Player player, ItemStack item, String action) {
        if (plugin.getConfigManager().isItemRemovalEnabled()) {
            org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
            inventory.removeItem(item);
            player.sendMessage(org.bukkit.ChatColor.RED + "不兼容的物品已被移除以防止断开连接。");
        }
    }
    
    /**
     * 警告玩家
     */
    private void warnPlayer(org.bukkit.entity.Player player, ItemStack item, String action) {
        plugin.getLogger().warning("Player " + player.getName() + " attempted to use potentially problematic mod item: " + item.getType().name());
        player.sendMessage(org.bukkit.ChatColor.RED + "警告：您正在使用可能不兼容的物品，这可能导致断开连接。");
    }
    
    /**
     * 保护玩家不被踢出
     */
    private void protectPlayer(org.bukkit.entity.Player player, ItemStack item, String action) {
        // 记录事件但不取消操作，而是监控可能导致踢出的操作
        plugin.getLogger().info("Protecting player " + player.getName() + " from mod item: " + item.getType().name());
        
        // 这里可以实现更高级的保护机制
        // 例如：监控网络包或实现物品数据清理
    }
}