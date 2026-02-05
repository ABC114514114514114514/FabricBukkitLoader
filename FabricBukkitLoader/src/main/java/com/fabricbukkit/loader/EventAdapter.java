package com.fabricbukkit.loader;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Adapts Fabric mod events to Bukkit events and vice versa
 */
public class EventAdapter implements Listener {
    
    private final FabricBukkitLoader plugin;
    private final Map<String, List<RegisteredEventHandler>> modEventHandlers;
    private final Map<String, EventConverter> eventConverters;
    
    public EventAdapter(FabricBukkitLoader plugin) {
        this.plugin = plugin;
        this.modEventHandlers = new ConcurrentHashMap<>();
        this.eventConverters = new HashMap<>();
        
        // Initialize event converters
        initializeEventConverters();
    }
    
    /**
     * Initialize event converters for common events
     */
    private void initializeEventConverters() {
        // Server events
        eventConverters.put("ServerStarting", new EventConverter("org.bukkit.event.server.ServerLoadEvent", EventPriority.NORMAL));
        eventConverters.put("ServerStarted", new EventConverter("org.bukkit.event.server.ServerLoadEvent", EventPriority.NORMAL));
        eventConverters.put("ServerStopping", new EventConverter("org.bukkit.event.server.PluginDisableEvent", EventPriority.NORMAL));
        
        // Player events
        eventConverters.put("PlayerJoin", new EventConverter("org.bukkit.event.player.PlayerJoinEvent", EventPriority.NORMAL));
        eventConverters.put("PlayerLeave", new EventConverter("org.bukkit.event.player.PlayerQuitEvent", EventPriority.NORMAL));
        eventConverters.put("PlayerChat", new EventConverter("org.bukkit.event.player.PlayerChatEvent", EventPriority.NORMAL));
        eventConverters.put("PlayerDeath", new EventConverter("org.bukkit.event.entity.PlayerDeathEvent", EventPriority.NORMAL));
        eventConverters.put("PlayerRespawn", new EventConverter("org.bukkit.event.player.PlayerRespawnEvent", EventPriority.NORMAL));
        
        // Block events
        eventConverters.put("BlockBreak", new EventConverter("org.bukkit.event.block.BlockBreakEvent", EventPriority.NORMAL));
        eventConverters.put("BlockPlace", new EventConverter("org.bukkit.event.block.BlockPlaceEvent", EventPriority.NORMAL));
        
        // Entity events
        eventConverters.put("EntityDeath", new EventConverter("org.bukkit.event.entity.EntityDeathEvent", EventPriority.NORMAL));
        eventConverters.put("EntitySpawn", new EventConverter("org.bukkit.event.entity.EntitySpawnEvent", EventPriority.NORMAL));
        
        // Inventory events
        eventConverters.put("InventoryOpen", new EventConverter("org.bukkit.event.inventory.InventoryOpenEvent", EventPriority.NORMAL));
        eventConverters.put("InventoryClose", new EventConverter("org.bukkit.event.inventory.InventoryCloseEvent", EventPriority.NORMAL));
        eventConverters.put("InventoryClick", new EventConverter("org.bukkit.event.inventory.InventoryClickEvent", EventPriority.NORMAL));
    }
    
    /**
     * Register event handlers for a loaded mod
     */
    public void registerModEvents(LoadedMod mod) {
        if (!plugin.getConfigManager().isEventAdapterEnabled()) {
            plugin.debug("Event adapter is disabled, skipping event registration for mod: " + mod.getId());
            return;
        }
        
        String modId = mod.getId();
        Object modInstance = mod.getInstance();
        
        if (modInstance == null) {
            plugin.debug("Mod instance is null, cannot register events for: " + modId);
            return;
        }
        
        plugin.debug("Registering events for mod: " + modId);
        
        // Use reflection to find event handler methods
        Method[] methods = modInstance.getClass().getDeclaredMethods();
        List<RegisteredEventHandler> handlers = new ArrayList<>();
        
        for (Method method : methods) {
            // Look for methods that might be event handlers
            // Fabric mods typically use @EventHandler annotation or specific naming conventions
            if (isPotentialEventHandler(method)) {
                String eventType = detectEventType(method);
                if (eventType != null) {
                    EventConverter converter = eventConverters.get(eventType);
                    if (converter != null) {
                        try {
                            registerEventHandler(mod, method, eventType, converter);
                            handlers.add(new RegisteredEventHandler(method, eventType, converter));
                            plugin.debug("Registered event handler: " + method.getName() + " for event: " + eventType);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to register event handler: " + method.getName(), e);
                        }
                    }
                }
            }
        }
        
        if (!handlers.isEmpty()) {
            modEventHandlers.put(modId, handlers);
            plugin.debug("Registered " + handlers.size() + " event handlers for mod: " + modId);
        }
    }
    
    /**
     * Unregister event handlers for a mod
     */
    public void unregisterModEvents(LoadedMod mod) {
        String modId = mod.getId();
        List<RegisteredEventHandler> handlers = modEventHandlers.remove(modId);
        
        if (handlers != null) {
            plugin.debug("Unregistered " + handlers.size() + " event handlers for mod: " + modId);
        }
    }
    
    /**
     * Check if a method is a potential event handler
     */
    private boolean isPotentialEventHandler(Method method) {
        // Check method name patterns
        String methodName = method.getName();
        if (methodName.startsWith("on") || methodName.startsWith("handle")) {
            return true;
        }
        
        // Check parameter count (event handlers typically have 1 parameter)
        if (method.getParameterCount() == 1) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Detect the event type from a method
     */
    private String detectEventType(Method method) {
        String methodName = method.getName();
        
        // Extract event type from method name
        // e.g., onPlayerJoin -> PlayerJoin, handleBlockBreak -> BlockBreak
        String eventType = null;
        
        if (methodName.startsWith("on")) {
            eventType = methodName.substring(2);
        } else if (methodName.startsWith("handle")) {
            eventType = methodName.substring(6);
        }
        
        // Check if it's a known event type
        if (eventType != null && eventConverters.containsKey(eventType)) {
            return eventType;
        }
        
        return null;
    }
    
    /**
     * Register an event handler
     */
    @SuppressWarnings("unchecked")
    private void registerEventHandler(LoadedMod mod, Method method, String eventType, EventConverter converter) throws Exception {
        Object modInstance = mod.getInstance();
        Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(converter.getBukkitEventClass());
        
        EventExecutor executor = (listener, event) -> {
            long startTime = System.nanoTime();
            try {
                // Check if the event type matches
                if (eventClass.isInstance(event)) {
                    // Convert Bukkit event to Fabric event format if needed
                    Object fabricEvent = convertToFabricEvent(event, eventType);
                    
                    // Call the mod's event handler
                    if (fabricEvent != null) {
                        method.invoke(modInstance, fabricEvent);
                    } else {
                        method.invoke(modInstance, event);
                    }
                    
                    if (plugin.getConfigManager().isLogEventHandling()) {
                        plugin.debug("Handled event: " + eventType + " for mod: " + mod.getId());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in event handler: " + method.getName() + " for mod: " + mod.getId(), e);
            } finally {
                // Record event processing time
                long processingTime = System.nanoTime() - startTime;
                plugin.getPerformanceMonitor().recordEventProcessingTime(mod.getId(), eventType, processingTime);
            }
        };
        
        // Register with Bukkit
        plugin.getServer().getPluginManager().registerEvent(
            eventClass,
            new Listener() {},
            converter.getPriority(),
            executor,
            plugin
        );
    }
    
    /**
     * Convert Bukkit event to Fabric event format
     */
    private Object convertToFabricEvent(Event bukkitEvent, String eventType) {
        // This is a simplified conversion
        // In a full implementation, you would create proper Fabric event wrappers
        
        try {
            switch (eventType) {
                case "PlayerJoin":
                    if (bukkitEvent instanceof PlayerJoinEvent) {
                        return new FabricPlayerEvent((PlayerJoinEvent) bukkitEvent);
                    }
                    break;
                case "PlayerLeave":
                    if (bukkitEvent instanceof PlayerQuitEvent) {
                        return new FabricPlayerEvent((PlayerQuitEvent) bukkitEvent);
                    }
                    break;
                case "BlockBreak":
                    if (bukkitEvent instanceof BlockBreakEvent) {
                        return new FabricBlockEvent((BlockBreakEvent) bukkitEvent);
                    }
                    break;
                case "BlockPlace":
                    if (bukkitEvent instanceof BlockPlaceEvent) {
                        return new FabricBlockEvent((BlockPlaceEvent) bukkitEvent);
                    }
                    break;
                default:
                    // For unhandled events, return the Bukkit event as-is
                    return bukkitEvent;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error converting event: " + eventType, e);
        }
        
        return bukkitEvent;
    }
    
    /**
     * Event converter class
     */
    private static class EventConverter {
        private final String bukkitEventClass;
        private final EventPriority priority;
        
        public EventConverter(String bukkitEventClass, EventPriority priority) {
            this.bukkitEventClass = bukkitEventClass;
            this.priority = priority;
        }
        
        public String getBukkitEventClass() {
            return bukkitEventClass;
        }
        
        public EventPriority getPriority() {
            return priority;
        }
    }
    
    /**
     * Registered event handler info
     */
    private static class RegisteredEventHandler {
        private final Method method;
        private final String eventType;
        private final EventConverter converter;
        
        public RegisteredEventHandler(Method method, String eventType, EventConverter converter) {
            this.method = method;
            this.eventType = eventType;
            this.converter = converter;
        }
        
        public Method getMethod() {
            return method;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public EventConverter getConverter() {
            return converter;
        }
    }
    
    // Simple event wrapper classes for Fabric compatibility
    public static class FabricPlayerEvent {
        private final Event bukkitEvent;
        
        public FabricPlayerEvent(Event bukkitEvent) {
            this.bukkitEvent = bukkitEvent;
        }
        
        public Event getBukkitEvent() {
            return bukkitEvent;
        }
        
        // Add Fabric-compatible methods here
        public String getPlayerName() {
            if (bukkitEvent instanceof PlayerJoinEvent) {
                return ((PlayerJoinEvent) bukkitEvent).getPlayer().getName();
            } else if (bukkitEvent instanceof PlayerQuitEvent) {
                return ((PlayerQuitEvent) bukkitEvent).getPlayer().getName();
            }
            return null;
        }
    }
    
    public static class FabricBlockEvent {
        private final Event bukkitEvent;
        
        public FabricBlockEvent(Event bukkitEvent) {
            this.bukkitEvent = bukkitEvent;
        }
        
        public Event getBukkitEvent() {
            return bukkitEvent;
        }
        
        // Add Fabric-compatible methods here
        public String getBlockType() {
            if (bukkitEvent instanceof BlockBreakEvent) {
                return ((BlockBreakEvent) bukkitEvent).getBlock().getType().name();
            } else if (bukkitEvent instanceof BlockPlaceEvent) {
                return ((BlockPlaceEvent) bukkitEvent).getBlock().getType().name();
            }
            return null;
        }
    }
}