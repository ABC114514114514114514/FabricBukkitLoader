package com.fabricbukkit.loader;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 智能物品保护系统，允许安全的mod操作同时防止数据包解码错误
 */
public class SmartItemProtectionSystem implements Listener {
    
    private final FabricBukkitLoader plugin;
    
    public SmartItemProtectionSystem(FabricBukkitLoader plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理方块放置事件 - 允许放置但检查是否会导致问题
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        ItemStack item = event.getItemInHand();
        if (item != null && isProblematicItem(item)) {
            // 不完全阻止放置，而是记录警告
            String action = plugin.getConfigManager().getItemProtectionAction();
            if ("remove".equals(action) || "protect".equals(action)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "[FabricBukkitLoader] 检测到问题物品，操作已取消");
            } else if ("warn".equals(action)) {
                event.getPlayer().sendMessage(ChatColor.YELLOW + "[FabricBukkitLoader] 警告：该物品可能在特定情况下导致连接问题");
            }
        }
    }
    
    /**
     * 处理玩家交互事件 - 区分操作类型
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isItemProtectionEnabled()) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item != null && isProblematicItem(item)) {
            // 允许某些交互，但阻止可能导致数据包错误的操作
            if (event.getAction().name().contains("AIR")) {
                // 检查是否是尝试在创造模式中设置物品栏
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    // 在这里取消可能导致 set_creative_mode_slot 错误的操作
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "[FabricBukkitLoader] 为防止连接问题，已阻止此操作");
                }
            }
        }
    }
    
    /**
     * 处理背包点击事件 - 重点监控创造模式
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
        
        // 重点保护创造模式下的物品栏操作
        if (player.getGameMode() == GameMode.CREATIVE && isProblematicItem(clickedItem)) {
            // 检查是否是物品栏槽位操作（可能导致 set_creative_mode_slot 错误）
            if (isCreativeModeSlotOperation(event)) {
                handleProblematicItemClick(player, clickedItem);
                event.setCancelled(true); // 取消操作
            }
        }
    }
    
    /**
     * 判断是否是创造模式物品栏操作
     */
    private boolean isCreativeModeSlotOperation(InventoryClickEvent event) {
        // 如果点击的是玩家背包的特定区域（这些区域通常用于物品栏设置）
        // 热键绑定操作或直接设置物品到工具栏的操作
        return event.getClick().name().contains("HOTBAR") || 
               event.getClick().name().contains("NUMBER") ||
               event.getClick().name().equals("SWAP_OFFHAND");
    }
    
    /**
     * 处理背包拖拽事件 - 重点监控创造模式
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
        
        // 仅在创造模式下严格限制
        if (player.getGameMode() == GameMode.CREATIVE) {
            for (ItemStack item : event.getNewItems().values()) {
                if (isProblematicItem(item)) {
                    handleProblematicItemClick(player, item);
                    event.setCancelled(true); // 取消拖拽操作
                    return;
                }
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
        
        // 仅在创造模式下限制
        if (player.getGameMode() == GameMode.CREATIVE) {
            if (isProblematicItem(mainHand) || isProblematicItem(offHand)) {
                event.setCancelled(true); // 取消交换
                player.sendMessage(ChatColor.RED + "[FabricBukkitLoader] 为防止连接问题，已阻止主副手交换");
            }
        }
    }
    
    /**
     * 当玩家切换到创造模式时，提供警告而非完全阻止
     */
    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        
        if (event.getNewGameMode() == GameMode.CREATIVE) {
            // 检查玩家背包是否包含问题物品
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.getGameMode() == GameMode.CREATIVE) {
                        warnAboutProblematicItems(player);
                    }
                }
            }.runTaskLater(plugin, 20L); // 延迟1秒执行
        }
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
                    warnAboutProblematicItems(player);
                }
            }
        }.runTaskLater(plugin, 20L); // 延迟1秒执行
    }
    
    /**
     * 警告玩家背包中的问题物品
     */
    private void warnAboutProblematicItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean hasProblematicItems = false;
        
        // 检查背包物品
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && isProblematicItem(item)) {
                hasProblematicItems = true;
                // 仅发送警告，不移除物品
                if ("warn".equals(plugin.getConfigManager().getItemProtectionAction())) {
                    player.sendMessage(ChatColor.YELLOW + "[FabricBukkitLoader] 警告: 检测到可能在创造模式下引起问题的物品");
                }
            }
        }
        
        if (hasProblematicItems && "warn".equals(plugin.getConfigManager().getItemProtectionAction())) {
            player.sendMessage(ChatColor.GOLD + "[FabricBukkitLoader] 建议: 在创造模式下谨慎操作mod物品以避免连接问题");
        }
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
                
                // 检查是否包含NBT标签（可能表明是mod物品）
                // 在Bukkit API中，这需要通过其他方式检查，我们主要依赖上面的检查
            }
            
            // 检查物品类型是否为原版类型（非mod添加的类型）
            // 如果不是原版类型，很可能是mod添加的物品
            String typeName = item.getType().name().toLowerCase();
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
     * 检查物品类型是否为非原版类型
     */
    private boolean isNonVanillaItemType(String typeName) {
        // 原版物品类型通常不包含下划线前缀或其他mod特定的前缀
        // 但需要注意原版物品ID本身也可能包含下划线
        // 更好的方法是检查是否存在mod特定的命名空间
        return typeName.contains(":") && !typeName.startsWith("minecraft:");
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
                player.sendMessage(ChatColor.RED + "警告: 检测到可能的问题物品");
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
}