package com.fabricbukkit.loader;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Handles the loading and unloading of Fabric mods
 */
public class ModLoader {
    
    private final FabricBukkitLoader plugin;
    private final Map<String, ClassLoader> modClassLoaders;
    private final Yaml yaml;
    private final Map<String, List<String>> modLoadOrder;
    private final EnhancedDependencyChecker enhancedDependencyChecker;
    
    public ModLoader(FabricBukkitLoader plugin) {
        this.plugin = plugin;
        this.modClassLoaders = new HashMap<>();
        this.yaml = new Yaml();
        this.modLoadOrder = new HashMap<>();
        this.enhancedDependencyChecker = new EnhancedDependencyChecker(plugin);
    }
    
    /**
     * Load a Fabric mod from a JAR file
     */
    public LoadedMod loadMod(File modFile) throws Exception {
        long startTime = System.currentTimeMillis();
        
        if (!modFile.exists()) {
            throw new FileNotFoundException("Mod file not found: " + modFile.getAbsolutePath());
        }
        
        if (!modFile.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("Mod file must be a JAR file: " + modFile.getName());
        }
        
        plugin.debug("Loading mod from: " + modFile.getAbsolutePath());
        
        // Extract mod metadata from fabric.mod.json
        Map<String, Object> fabricModJson = extractFabricModJson(modFile);
        if (fabricModJson == null) {
            throw new IOException("fabric.mod.json not found in " + modFile.getName());
        }
        
        // Parse mod information
        String modId = (String) fabricModJson.get("id");
        String modName = (String) fabricModJson.getOrDefault("name", modId);
        String modVersion = (String) fabricModJson.getOrDefault("version", "unknown");
        String modDescription = (String) fabricModJson.getOrDefault("description", "");
        String mainClass = (String) fabricModJson.get("main");
        
        if (modId == null) {
            throw new IOException("Mod ID not found in fabric.mod.json");
        }
        
        // Check if mod is already loaded
        if (plugin.getLoadedMods().containsKey(modId)) {
            throw new IllegalStateException("Mod already loaded: " + modId);
        }
        
        // Check security settings
        if (!plugin.getConfigManager().canLoadPlugin(modId)) {
            throw new SecurityException("Mod is not allowed to be loaded: " + modId);
        }
        
        // Extract authors and dependencies
        List<String> authors = extractAuthors(fabricModJson);
        List<ModDependency> dependencies = extractDependencies(fabricModJson);
        
        plugin.debug("Mod info - ID: " + modId + ", Name: " + modName + ", Version: " + modVersion);
        plugin.debug("Authors: " + authors);
        plugin.debug("Dependencies: " + dependencies.size() + " dependencies found");
        
        // Create class loader for the mod
        ClassLoader classLoader = createModClassLoader(modFile);
        
        // Create LoadedMod instance
        LoadedMod loadedMod = new LoadedMod(
            modId,
            modName,
            modVersion,
            modDescription,
            mainClass,
            modFile,
            fabricModJson,
            authors,
            dependencies,
            classLoader
        );
        
        // Perform dependency check if enabled
        if (plugin.getConfigManager().isDependencyCheckEnabled()) {
            if (!areDependenciesSatisfied(loadedMod)) {
                throw new IllegalStateException("Unsatisfied dependencies for mod: " + modId);
            }
        }
        
        // Perform conflict check if enabled
        if (plugin.getConfigManager().isConflictDetectionEnabled()) {
            if (hasConflicts(loadedMod)) {
                throw new IllegalStateException("Conflicting mods detected for mod: " + modId);
            }
        }
        
        // Perform security scan if enabled
        if (plugin.getConfigManager().isVerifyPluginSignatures() || isSecurityScanningEnabled()) {
            SecurityScanner.SecurityReport securityReport = plugin.getSecurityScanner().scanMod(modFile);
            
            if (securityReport.hasIssues()) {
                // Log security issues
                for (String issue : securityReport.getIssues()) {
                    plugin.getLogger().warning("Security issue in mod " + modId + ": " + issue);
                }
                
                // If strict security is enabled, reject the mod
                if (plugin.getConfigManager().isVerifyPluginSignatures()) {
                    plugin.getLogger().warning("Security scanning failed for mod: " + modId + ", rejecting load.");
                    throw new SecurityException("Mod failed security scan: " + modId);
                }
            }
        }
        
        // Initialize the mod if it has a main class
        if (mainClass != null && !mainClass.isEmpty()) {
            try {
                plugin.debug("Initializing mod main class: " + mainClass);
                Class<?> modClass = classLoader.loadClass(mainClass);
                Object modInstance = modClass.getDeclaredConstructor().newInstance();
                loadedMod.setInstance(modInstance);
                
                // Call onInitialize if the mod has such method
                try {
                    long initStartTime = System.currentTimeMillis();
                    modClass.getMethod("onInitialize").invoke(modInstance);
                    long initTime = System.currentTimeMillis() - initStartTime;
                    plugin.debug("Called onInitialize for mod: " + modId + " (took " + initTime + " ms)");
                } catch (NoSuchMethodException e) {
                    plugin.debug("Mod does not have onInitialize method: " + modId);
                }
                
                loadedMod.setEnabled(true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to initialize mod main class: " + mainClass, e);
            }
        } else {
            loadedMod.setEnabled(true);
        }
        
        // Register with event adapter if enabled
        if (plugin.getConfigManager().isEventAdapterEnabled() && loadedMod.getInstance() instanceof org.bukkit.event.Listener) {
            plugin.getEventAdapter().registerModEvents(loadedMod);
        }
        
        // Register commands if enabled
        if (plugin.getConfigManager().isCommandAdapterEnabled()) {
            plugin.getCommandAdapter().registerModCommands(loadedMod);
        }
        
        // Record load time
        long loadTime = System.currentTimeMillis() - startTime;
        plugin.getPerformanceMonitor().recordLoadTime(modId, loadTime);
        
        // Log priority status if applicable
        if (plugin.getConfigManager().isPriorityMod(modId)) {
            plugin.debug("Successfully loaded PRIORITY mod: " + modId + " (took " + loadTime + " ms)");
        } else {
            plugin.debug("Successfully loaded mod: " + modId + " (took " + loadTime + " ms)");
        }
        return loadedMod;
    }
    
    /**
     * Unload a Fabric mod
     */
    public void unloadMod(LoadedMod mod) throws Exception {
        String modId = mod.getId();
        plugin.debug("Unloading mod: " + modId);
        
        // Call onDispose if the mod has such method
        if (mod.getInstance() != null) {
            try {
                mod.getInstance().getClass().getMethod("onDispose").invoke(mod.getInstance());
                plugin.debug("Called onDispose for mod: " + modId);
            } catch (NoSuchMethodException e) {
                plugin.debug("Mod does not have onDispose method: " + modId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error calling onDispose for mod: " + modId, e);
            }
        }
        
        // Unregister events
        plugin.getEventAdapter().unregisterModEvents(mod);
        
        // Unregister commands
        plugin.getCommandAdapter().unregisterModCommands(mod);
        
        // Set mod as disabled
        mod.setEnabled(false);
        mod.setInstance(null);
        
        // Close class loader if possible
        ClassLoader classLoader = mod.getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            try {
                ((URLClassLoader) classLoader).close();
                plugin.debug("Closed class loader for mod: " + modId);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing class loader for mod: " + modId, e);
            }
        }
        
        plugin.debug("Successfully unloaded mod: " + modId);
    }
    
    /**
     * Extract fabric.mod.json from a JAR file
     */
    private Map<String, Object> extractFabricModJson(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) {
                // Try alternate locations
                entry = jar.getJarEntry("META-INF/fabric.mod.json");
            }
            
            if (entry == null) {
                plugin.getLogger().warning("fabric.mod.json not found in " + jarFile.getName());
                return null;
            }
            
            try (InputStream is = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(is)) {
                return yaml.load(reader);
            }
        }
    }
    
    /**
     * Extract authors from fabric.mod.json
     */
    private List<String> extractAuthors(Map<String, Object> fabricModJson) {
        List<String> authors = new ArrayList<>();
        
        // Handle different author formats
        Object authorsObj = fabricModJson.get("authors");
        if (authorsObj instanceof List) {
            for (Object author : (List<?>) authorsObj) {
                if (author instanceof String) {
                    authors.add((String) author);
                } else if (author instanceof Map) {
                    // Handle author objects with name field
                    Map<?, ?> authorMap = (Map<?, ?>) author;
                    Object name = authorMap.get("name");
                    if (name instanceof String) {
                        authors.add((String) name);
                    }
                }
            }
        } else if (authorsObj instanceof String) {
            authors.add((String) authorsObj);
        }
        
        return authors;
    }
    
    /**
     * Extract dependencies from fabric.mod.json with version ranges
     */
    private List<ModDependency> extractDependencies(Map<String, Object> fabricModJson) {
        List<ModDependency> dependencies = new ArrayList<>();
        
        // Extract 'depends' dependencies (required)
        Object dependsObj = fabricModJson.get("depends");
        if (dependsObj instanceof Map) {
            Map<?, ?> dependsMap = (Map<?, ?>) dependsObj;
            for (Map.Entry<?, ?> entry : dependsMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String modId = (String) entry.getKey();
                    Object versionSpec = entry.getValue();
                    dependencies.add(new ModDependency(modId, versionSpec.toString(), ModDependency.DependencyType.REQUIRED));
                }
            }
        }
        
        // Extract 'recommends' dependencies (optional but recommended)
        Object recommendsObj = fabricModJson.get("recommends");
        if (recommendsObj instanceof Map) {
            Map<?, ?> recommendsMap = (Map<?, ?>) recommendsObj;
            for (Map.Entry<?, ?> entry : recommendsMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String modId = (String) entry.getKey();
                    Object versionSpec = entry.getValue();
                    dependencies.add(new ModDependency(modId, versionSpec.toString(), ModDependency.DependencyType.RECOMMENDED));
                }
            }
        }
        
        // Extract 'suggests' dependencies (optional)
        Object suggestsObj = fabricModJson.get("suggests");
        if (suggestsObj instanceof Map) {
            Map<?, ?> suggestsMap = (Map<?, ?>) suggestsObj;
            for (Map.Entry<?, ?> entry : suggestsMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String modId = (String) entry.getKey();
                    Object versionSpec = entry.getValue();
                    dependencies.add(new ModDependency(modId, versionSpec.toString(), ModDependency.DependencyType.SUGGESTED));
                }
            }
        }
        
        // Extract 'conflicts' dependencies (incompatible mods)
        Object conflictsObj = fabricModJson.get("conflicts");
        if (conflictsObj instanceof Map) {
            Map<?, ?> conflictsMap = (Map<?, ?>) conflictsObj;
            for (Map.Entry<?, ?> entry : conflictsMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String modId = (String) entry.getKey();
                    Object versionSpec = entry.getValue();
                    dependencies.add(new ModDependency(modId, versionSpec.toString(), ModDependency.DependencyType.CONFLICTS));
                }
            }
        }
        
        return dependencies;
    }
    
    /**
     * Create a class loader for a mod
     */
    private ClassLoader createModClassLoader(File modFile) throws Exception {
        URL modUrl = modFile.toURI().toURL();
        
        // Create a new URLClassLoader for the mod
        // Use the plugin's class loader as parent to access Paper API
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{modUrl},
            plugin.getClass().getClassLoader()
        );
        
        plugin.debug("Created class loader for mod: " + modFile.getName());
        return classLoader;
    }
    
    /**
     * Check if all dependencies are satisfied using enhanced checking
     */
    public boolean areDependenciesSatisfied(LoadedMod mod) {
        if (plugin.getConfigManager().isEnhancedDependencyCheckEnabled()) {
            return enhancedDependencyChecker.areDependenciesSatisfied(mod);
        } else {
            // Fallback to original method
            List<ModDependency> dependencies = mod.getDependencies();
            if (dependencies == null || dependencies.isEmpty()) {
                return true;
            }

            for (ModDependency dependency : dependencies) {
                // Skip if this is not a required dependency
                if (dependency.getType() != ModDependency.DependencyType.REQUIRED) {
                    continue;
                }

                // Check if dependency is a loaded mod
                LoadedMod loadedDep = plugin.getLoadedMods().get(dependency.getModId());
                if (loadedDep == null) {
                    // Check if dependency is a built-in Minecraft/Fabric dependency
                    if (!isBuiltInDependency(dependency.getModId())) {
                        plugin.getLogger().warning("Missing required dependency for mod " + mod.getId() + ": " + dependency.getModId());
                        return false;
                    }
                } else {
                    // Check version compatibility if enabled
                    if (plugin.getConfigManager().isEnforceVersionCompatibility()) {
                        if (!dependency.isVersionSatisfied(loadedDep.getVersion())) {
                            plugin.getLogger().warning("Version mismatch for dependency " + dependency.getModId() + 
                                ". Required: " + dependency.getVersionSpec() + 
                                ", Found: " + loadedDep.getVersion() + 
                                " for mod " + mod.getId());
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }
    
    /**
     * Check for conflicts with other loaded mods
     */
    public boolean hasConflicts(LoadedMod mod) {
        if (!plugin.getConfigManager().isConflictDetectionEnabled()) {
            return false;
        }
        
        List<ModDependency> dependencies = mod.getDependencies();
        if (dependencies == null) {
            return false;
        }
        
        // Check if this mod conflicts with any already loaded mods
        for (LoadedMod loadedMod : plugin.getLoadedMods().values()) {
            if (mod.conflictsWith(loadedMod)) {
                plugin.getLogger().warning("Conflict detected: " + mod.getId() + " conflicts with " + loadedMod.getId());
                return true;
            }
            
            // Also check if already loaded mod has a conflict with this one
            if (loadedMod.conflictsWith(mod)) {
                plugin.getLogger().warning("Conflict detected: " + loadedMod.getId() + " conflicts with " + mod.getId());
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isSecurityScanningEnabled() {
        // For now, we'll consider security scanning enabled if the config has security settings enabled
        // This can be expanded to have its own configuration option
        return plugin.getConfigManager().isVerifyPluginSignatures() || 
               plugin.getConfigManager().isAllowUnsignedPlugins() == false;
    }
    
    /**
     * Get load order for all mods based on dependencies and priorities
     */
    public List<LoadedMod> getLoadOrder(List<LoadedMod> mods) {
        if (!plugin.getConfigManager().isPriorityLoadingEnabled()) {
            // If priority loading is disabled, use simple dependency-based order
            List<LoadedMod> result = new ArrayList<>();
            Set<String> processed = new HashSet<>();
            
            for (LoadedMod mod : mods) {
                addModWithDependencies(mod, result, processed);
            }
            
            return result;
        }
        
        // If priority loading is enabled, sort by priority first, then by dependencies
        List<LoadedMod> priorityMods = new ArrayList<>();
        List<LoadedMod> normalMods = new ArrayList<>();
        
        for (LoadedMod mod : mods) {
            if (plugin.getConfigManager().isPriorityMod(mod.getId())) {
                priorityMods.add(mod);
            } else {
                normalMods.add(mod);
            }
        }
        
        // Sort priority mods by the order defined in config
        priorityMods.sort((m1, m2) -> {
            List<String> priorityList = plugin.getConfigManager().getPriorityMods();
            int idx1 = priorityList.indexOf(m1.getId());
            int idx2 = priorityList.indexOf(m2.getId());
            
            // If mod is not in priority list, put it at the end
            if (idx1 == -1) idx1 = Integer.MAX_VALUE;
            if (idx2 == -1) idx2 = Integer.MAX_VALUE;
            
            return Integer.compare(idx1, idx2);
        });
        
        // Process priority mods first, ensuring their dependencies are loaded first
        List<LoadedMod> result = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        
        for (LoadedMod mod : priorityMods) {
            addModWithDependencies(mod, result, processed);
        }
        
        // Then process normal mods
        for (LoadedMod mod : normalMods) {
            addModWithDependencies(mod, result, processed);
        }
        
        return result;
    }
    
    private void addModWithDependencies(LoadedMod mod, List<LoadedMod> result, Set<String> processed) {
        if (processed.contains(mod.getId())) {
            return; // Already processed
        }
        
        // Process dependencies first
        List<ModDependency> dependencies = mod.getDependencies();
        if (dependencies != null) {
            for (ModDependency dep : dependencies) {
                if (dep.getType() == ModDependency.DependencyType.REQUIRED) {
                    LoadedMod dependencyMod = plugin.getLoadedMods().get(dep.getModId());
                    if (dependencyMod != null && !processed.contains(dep.getModId())) {
                        addModWithDependencies(dependencyMod, result, processed);
                    }
                }
            }
        }
        
        // Add this mod after its dependencies
        if (!processed.contains(mod.getId())) {
            result.add(mod);
            processed.add(mod.getId());
        }
    }
    
    /**
     * Check if a dependency is a built-in Minecraft/Fabric dependency
     */
    private boolean isBuiltInDependency(String dependency) {
        // Common Minecraft/Fabric dependencies that are provided by the server
        Set<String> builtInDeps = new HashSet<>(Arrays.asList(
            "minecraft",
            "fabricloader", 
            "fabric",
            "fabric-api",
            "java"
        ));
        
        return builtInDeps.contains(dependency) || 
               dependency.startsWith("fabric-") || 
               dependency.startsWith("minecraft-");
    }
    
    /**
     * Get a list of all mod files in the mods directory
     */
    public List<File> getModFiles() {
        List<File> modFiles = new ArrayList<>();
        
        // Get mods from server root directory
        File serverRoot = plugin.getServer().getWorldContainer().getParentFile();
        File modsDir = new File(serverRoot, "mods");
        
        if (!modsDir.exists() || !modsDir.isDirectory()) {
            return modFiles;
        }
        
        File[] files = modsDir.listFiles((dir, name) -> 
            name.endsWith(".jar") || name.endsWith(".zip")
        );
        
        if (files != null) {
            modFiles.addAll(Arrays.asList(files));
        }
        
        return modFiles;
    }
    
    /**
     * Validate a mod file
     */
    public boolean validateModFile(File modFile) {
        if (!modFile.exists()) {
            plugin.getLogger().warning("Mod file does not exist: " + modFile.getAbsolutePath());
            return false;
        }
        
        if (!modFile.canRead()) {
            plugin.getLogger().warning("Cannot read mod file: " + modFile.getAbsolutePath());
            return false;
        }
        
        try (JarFile jar = new JarFile(modFile)) {
            // Check if it has fabric.mod.json
            if (jar.getEntry("fabric.mod.json") == null && 
                jar.getEntry("META-INF/fabric.mod.json") == null) {
                plugin.getLogger().warning("Mod file does not contain fabric.mod.json: " + modFile.getName());
                return false;
            }
            
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error validating mod file: " + modFile.getName(), e);
            return false;
        }
    }
}