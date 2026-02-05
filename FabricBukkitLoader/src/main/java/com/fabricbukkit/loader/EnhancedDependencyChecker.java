package com.fabricbukkit.loader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强的依赖检查系统，提供更灵活的依赖解析和兼容性检查
 */
public class EnhancedDependencyChecker {
    
    private final FabricBukkitLoader plugin;
    
    public EnhancedDependencyChecker(FabricBukkitLoader plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 检查 mod 依赖是否满足，使用增强的逻辑
     */
    public boolean areDependenciesSatisfied(LoadedMod mod) {
        java.util.List<ModDependency> dependencies = mod.getDependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            return true;
        }
        
        boolean allSatisfied = true;
        
        for (ModDependency dependency : dependencies) {
            // 只检查必需的依赖
            if (dependency.getType() != ModDependency.DependencyType.REQUIRED) {
                continue;
            }
            
            // 获取依赖信息
            String depId = dependency.getModId();
            String versionSpec = dependency.getVersionSpec();
            
            plugin.debug("Checking dependency: " + depId + " version spec: " + versionSpec + " for mod: " + mod.getId());
            
            // 检查是否已加载对应的 mod
            LoadedMod loadedDep = plugin.getLoadedMods().get(depId);
            
            if (loadedDep == null) {
                // 尝试一些常见的依赖别名和兼容性检查
                loadedDep = findCompatibleDependency(depId);
            }
            
            if (loadedDep == null) {
                // 检查是否为内置依赖
                if (!isBuiltInDependency(depId)) {
                    // 检查是否是可选的或有替代品
                    if (isOptionalOrHasAlternative(depId)) {
                        plugin.debug("Dependency " + depId + " is optional or has alternative for mod: " + mod.getId());
                        continue;
                    }
                    
                    plugin.getLogger().warning("Missing required dependency for mod " + mod.getId() + ": " + depId);
                    allSatisfied = false;
                    continue;
                }
            } else {
                // 检查版本兼容性（如果启用了）
                if (plugin.getConfigManager().isEnforceVersionCompatibility()) {
                    if (!dependency.isVersionSatisfied(loadedDep.getVersion())) {
                        plugin.getLogger().warning("Version mismatch for dependency " + depId + 
                            ". Required: " + versionSpec + 
                            ", Found: " + loadedDep.getVersion() + 
                            " for mod " + mod.getId());
                            
                        // 检查是否可以忽略版本不匹配
                        if (canIgnoreVersionMismatch(depId, versionSpec, loadedDep.getVersion())) {
                            plugin.getLogger().info("Version mismatch for " + depId + " is allowed, continuing...");
                        } else {
                            allSatisfied = false;
                            continue;
                        }
                    } else {
                        plugin.debug("Version satisfied for dependency: " + depId + 
                                   ", required: " + versionSpec + 
                                   ", found: " + loadedDep.getVersion());
                    }
                }
            }
        }
        
        return allSatisfied;
    }
    
    /**
     * 查找兼容的依赖（处理别名等）
     */
    private LoadedMod findCompatibleDependency(String depId) {
        // 检查一些常见的依赖别名
        switch (depId) {
            case "balm-fabric":
                // 查找可能的 Balm 替代品
                return findLoadedModWithAlternativeIds(new String[]{"balm"});
            case "fabric-api":
            case "fabric":
                // Fabric API 的不同变体
                return findLoadedModWithAlternativeIds(new String[]{"fabric-api", "fabric", "fabricloader"});
            case "minecraft":
                // Minecraft 本身总是可用的
                return null; // 这种情况会在 isBuiltInDependency 中处理
            default:
                // 查找名称相似的 mod
                return findLoadedModWithAlternativeIds(new String[]{depId});
        }
    }
    
    /**
     * 根据多个可能的 ID 查找已加载的 mod
     */
    private LoadedMod findLoadedModWithAlternativeIds(String[] possibleIds) {
        for (String id : possibleIds) {
            LoadedMod mod = plugin.getLoadedMods().get(id);
            if (mod != null) {
                return mod;
            }
        }
        return null;
    }
    
    /**
     * 检查是否为内置依赖
     */
    private boolean isBuiltInDependency(String dependency) {
        // Common Minecraft/Fabric dependencies that are provided by the server
        java.util.Set<String> builtInDeps = new java.util.HashSet<>(java.util.Arrays.asList(
            "minecraft",
            "fabricloader", 
            "fabric",
            "fabric-api",
            "java",
            "fabric-language-kotlin",
            "cloth-config",
            "modmenu"
        ));
        
        return builtInDeps.contains(dependency) || 
               dependency.startsWith("fabric-") || 
               dependency.startsWith("minecraft-");
    }
    
    /**
     * 检查依赖是否是可选的或有替代品
     */
    private boolean isOptionalOrHasAlternative(String depId) {
        // 某些依赖在实践中可能是可选的
        java.util.Set<String> commonlyOptional = new java.util.HashSet<>(java.util.Arrays.asList(
            "balm-fabric",
            "architectury",
            "emi",
            "jei",
            "roughlyenoughitems",
            "kubejs",
            "rhino"
        ));
        
        return commonlyOptional.contains(depId);
    }
    
    /**
     * 检查是否可以忽略版本不匹配
     */
    private boolean canIgnoreVersionMismatch(String depId, String requiredVersion, String foundVersion) {
        // 对于某些通用库，可以更宽松地处理版本
        if (depId.equals("fabric-api") || depId.startsWith("fabric-")) {
            // Fabric API 版本通常向后兼容
            return true;
        }
        
        // 使用更宽松的版本比较
        return isVersionCompatibleLoose(requiredVersion, foundVersion);
    }
    
    /**
     * 使用宽松规则检查版本兼容性
     */
    private boolean isVersionCompatibleLoose(String requiredSpec, String foundVersion) {
        // 简化版本比较 - 检查主版本号是否匹配
        try {
            // 基于主要版本号的兼容性检查
            String[] requiredParts = requiredSpec.replaceAll("[^0-9.]", "").split("\\.");
            String[] foundParts = foundVersion.replaceAll("[^0-9.]", "").split("\\.");
            
            if (requiredParts.length > 0 && foundParts.length > 0) {
                // 检查 Minecraft 版本是否匹配
                if (requiredParts[0].equals(foundParts[0])) {
                    return true; // 主版本匹配，认为兼容
                }
            }
        } catch (Exception e) {
            plugin.debug("Error in loose version comparison: " + e.getMessage());
        }
        
        return false;
    }
}