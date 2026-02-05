package com.fabricbukkit.loader;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * Adapts Fabric mod commands to Bukkit commands
 */
public class CommandAdapter implements CommandExecutor {
    
    private final FabricBukkitLoader plugin;
    private final Map<String, ModCommand> registeredCommands;
    
    public CommandAdapter(FabricBukkitLoader plugin) {
        this.plugin = plugin;
        this.registeredCommands = new HashMap<>();
    }
    
    /**
     * Register commands for a loaded mod
     */
    public void registerModCommands(LoadedMod mod) {
        if (!plugin.getConfigManager().isCommandAdapterEnabled()) {
            plugin.debug("Command adapter is disabled, skipping command registration for mod: " + mod.getId());
            return;
        }
        
        Object modInstance = mod.getInstance();
        if (modInstance == null) {
            plugin.debug("Mod instance is null, cannot register commands for: " + mod.getId());
            return;
        }
        
        plugin.debug("Registering commands for mod: " + mod.getId());
        
        // Use reflection to find command registration methods
        Method[] methods = modInstance.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            // Look for methods that might register commands
            if (isCommandRegistrationMethod(method)) {
                try {
                    registerCommandsFromMethod(mod, method);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to register commands from method: " + method.getName(), e);
                }
            }
        }
        
        // Also check for command methods directly
        registerCommandMethods(mod);
    }
    
    /**
     * Unregister commands for a mod
     */
    public void unregisterModCommands(LoadedMod mod) {
        String modId = mod.getId();
        List<String> commandsToRemove = new ArrayList<>();
        
        for (Map.Entry<String, ModCommand> entry : registeredCommands.entrySet()) {
            if (entry.getValue().getModId().equals(modId)) {
                commandsToRemove.add(entry.getKey());
            }
        }
        
        for (String command : commandsToRemove) {
            registeredCommands.remove(command);
            // Note: Bukkit doesn't support unregistering commands easily
            // Commands will remain registered but won't function
        }
        
        if (!commandsToRemove.isEmpty()) {
            plugin.debug("Unregistered " + commandsToRemove.size() + " commands for mod: " + modId);
        }
    }
    
    /**
     * Register built-in commands
     */
    public void registerCommands() {
        // Commands are already registered in plugin.yml
        // This method can be used for dynamic command registration if needed
    }
    
    /**
     * Check if a method is a command registration method
     */
    private boolean isCommandRegistrationMethod(Method method) {
        String methodName = method.getName().toLowerCase();
        return methodName.contains("register") && methodName.contains("command");
    }
    
    /**
     * Register commands from a method
     */
    private void registerCommandsFromMethod(LoadedMod mod, Method method) throws Exception {
        Object modInstance = mod.getInstance();
        Object result = method.invoke(modInstance);
        
        if (result instanceof Map) {
            Map<?, ?> commands = (Map<?, ?>) result;
            for (Map.Entry<?, ?> entry : commands.entrySet()) {
                String commandName = entry.getKey().toString();
                Object commandInfo = entry.getValue();
                
                if (commandInfo instanceof Map) {
                    registerCommandFromMap(mod, commandName, (Map<?, ?>) commandInfo);
                }
            }
        }
    }
    
    /**
     * Register a command from a map
     */
    private void registerCommandFromMap(LoadedMod mod, String commandName, Map<?, ?> commandInfo) {
        String description = getMapValue(commandInfo, "description", "A mod command");
        String usage = getMapValue(commandInfo, "usage", "/" + commandName);
        List<String> aliases = getMapListValue(commandInfo, "aliases");
        String permission = getMapValue(commandInfo, "permission", "");
        
        Command command = new ModCommand(mod.getId(), commandName, description, usage, aliases, permission, mod.getInstance());
        registerCommand(command);
        registeredCommands.put(commandName, (ModCommand) command);
        
        plugin.debug("Registered command: " + commandName + " for mod: " + mod.getId());
    }
    
    /**
     * Register command methods directly
     */
    private void registerCommandMethods(LoadedMod mod) {
        Object modInstance = mod.getInstance();
        Method[] methods = modInstance.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            if (isPotentialCommandHandler(method)) {
                String commandName = method.getName().toLowerCase();
                Command command = new ModCommand(
                    mod.getId(),
                    commandName,
                    "Command for mod: " + mod.getName(),
                    "/" + commandName,
                    Arrays.asList(mod.getId() + ":" + commandName),
                    "",
                    modInstance
                );
                
                registerCommand(command);
                registeredCommands.put(commandName, (ModCommand) command);
                plugin.debug("Registered command method: " + commandName + " for mod: " + mod.getId());
            }
        }
    }
    
    /**
     * Check if a method is a potential command handler
     */
    private boolean isPotentialCommandHandler(Method method) {
        // Check method name patterns
        String methodName = method.getName();
        if (methodName.startsWith("command") || methodName.startsWith("cmd")) {
            return true;
        }
        
        // Check parameter count (command handlers typically have CommandSender and String[] parameters)
        if (method.getParameterCount() == 2) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (CommandSender.class.isAssignableFrom(paramTypes[0]) && 
                paramTypes[1] == String[].class) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Register a command with Bukkit
     */
    private void registerCommand(Command command) {
        try {
            // Get the command map
            Method getCommandMap = Bukkit.getServer().getClass().getMethod("getCommandMap");
            CommandMap commandMap = (CommandMap) getCommandMap.invoke(Bukkit.getServer());
            
            // Register the command
            commandMap.register("fabricbukkit", command);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to register command: " + command.getName(), e);
        }
    }
    
    /**
     * Handle command execution
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ModCommand modCommand = registeredCommands.get(command.getName());
        
        if (modCommand == null) {
            return false;
        }
        
        // Check permission
        if (!modCommand.getPermission().isEmpty() && !sender.hasPermission(modCommand.getPermission())) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        // Execute the command
        try {
            Object modInstance = modCommand.getModInstance();
            if (modInstance != null) {
                // Look for the command method
                Method commandMethod = findCommandMethod(modInstance, command.getName());
                if (commandMethod != null) {
                    commandMethod.invoke(modInstance, sender, args);
                    
                    if (plugin.getConfigManager().isLogCommandExecution()) {
                        plugin.debug("Executed command: " + command.getName() + " for mod: " + modCommand.getModId());
                    }
                    
                    return true;
                }
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error executing command: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "Error executing command: " + command.getName(), e);
        }
        
        return false;
    }
    
    /**
     * Find a command method in a mod instance
     */
    private Method findCommandMethod(Object modInstance, String commandName) {
        Method[] methods = modInstance.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(commandName) ||
                method.getName().equalsIgnoreCase("command" + commandName) ||
                method.getName().equalsIgnoreCase("cmd" + commandName)) {
                
                // Check parameters
                if (method.getParameterCount() == 2) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (CommandSender.class.isAssignableFrom(paramTypes[0]) && 
                        paramTypes[1] == String[].class) {
                        return method;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Helper method to get a value from a map
     */
    private String getMapValue(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Helper method to get a list value from a map
     */
    @SuppressWarnings("unchecked")
    private List<String> getMapListValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        return Collections.emptyList();
    }
    
    /**
     * Custom command class for mod commands
     */
    private static class ModCommand extends Command {
        private final String modId;
        private final String usage;
        private final String permission;
        private final Object modInstance;
        
        public ModCommand(String modId, String name, String description, String usage, 
                         List<String> aliases, String permission, Object modInstance) {
            super(name, description, usage, aliases);
            this.modId = modId;
            this.usage = usage;
            this.permission = permission;
            this.modInstance = modInstance;
        }
        
        public String getModId() {
            return modId;
        }
        
        public String getUsage() {
            return usage;
        }
        
        public String getPermission() {
            return permission;
        }
        
        public Object getModInstance() {
            return modInstance;
        }
        
        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            // This will be handled by the CommandAdapter
            return false;
        }
    }
}