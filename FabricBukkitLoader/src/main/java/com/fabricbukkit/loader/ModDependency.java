package com.fabricbukkit.loader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a dependency for a mod, including version requirements
 */
public class ModDependency {
    
    private final String modId;
    private final String versionSpec;
    private final DependencyType type;
    
    public enum DependencyType {
        REQUIRED,     // Must be present
        RECOMMENDED,  // Should be present but not required
        SUGGESTED,    // Nice to have but optional
        CONFLICTS     // Incompatible with this mod
    }
    
    public ModDependency(String modId, String versionSpec, DependencyType type) {
        this.modId = modId;
        this.versionSpec = versionSpec;
        this.type = type;
    }
    
    public String getModId() {
        return modId;
    }
    
    public String getVersionSpec() {
        return versionSpec;
    }
    
    public DependencyType getType() {
        return type;
    }
    
    /**
     * Check if a given version satisfies the version specification
     * Supports formats like: ">=1.0.0", "<=2.0.0", ">=1.0.0 <2.0.0", "*"
     */
    public boolean isVersionSatisfied(String version) {
        // If version spec is "*", any version is acceptable
        if ("*".equals(versionSpec)) {
            return true;
        }
        
        // Parse version spec which might contain multiple conditions like ">=1.0.0 <2.0.0"
        String[] conditions = versionSpec.trim().split("\\s+");
        
        for (String condition : conditions) {
            if (!checkSingleCondition(condition, version)) {
                return false; // If any condition fails, the spec isn't satisfied
            }
        }
        
        return true;
    }
    
    private boolean checkSingleCondition(String condition, String version) {
        // Remove whitespace
        condition = condition.trim();
        
        // Handle exact version match
        if (!condition.contains(">") && !condition.contains("<") && !condition.contains("=")) {
            return compareVersions(version, condition) == 0;
        }
        
        // Handle comparison operators
        if (condition.startsWith(">=")) {
            String requiredVersion = condition.substring(2);
            return compareVersions(version, requiredVersion) >= 0;
        } else if (condition.startsWith(">")) {
            String requiredVersion = condition.substring(1);
            return compareVersions(version, requiredVersion) > 0;
        } else if (condition.startsWith("<=")) {
            String requiredVersion = condition.substring(2);
            return compareVersions(version, requiredVersion) <= 0;
        } else if (condition.startsWith("<")) {
            String requiredVersion = condition.substring(1);
            return compareVersions(version, requiredVersion) < 0;
        } else if (condition.startsWith("!=") || condition.startsWith("<>")) {
            String requiredVersion = condition.substring(2);
            return compareVersions(version, requiredVersion) != 0;
        }
        
        // If we can't parse the condition, be permissive
        return true;
    }
    
    /**
     * Compare two version strings
     * Returns: negative if v1 < v2, 0 if v1 == v2, positive if v1 > v2
     */
    private int compareVersions(String v1, String v2) {
        // Handle null versions
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        
        // Split version strings into components
        String[] parts1 = v1.split("[.\\-_]");
        String[] parts2 = v2.split("[.\\-_]");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            String part1 = i < parts1.length ? parts1[i] : "0";
            String part2 = i < parts2.length ? parts2[i] : "0";
            
            // Try to parse as numbers first
            try {
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);
                if (num1 != num2) {
                    return num1 - num2;
                }
            } catch (NumberFormatException e) {
                // If not numbers, compare as strings
                int result = part1.compareTo(part2);
                if (result != 0) {
                    return result;
                }
            }
        }
        
        return 0;
    }
    
    @Override
    public String toString() {
        return "ModDependency{" +
                "modId='" + modId + '\'' +
                ", versionSpec='" + versionSpec + '\'' +
                ", type=" + type +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ModDependency that = (ModDependency) obj;
        return modId.equals(that.modId) && type == that.type;
    }
    
    @Override
    public int hashCode() {
        return modId.hashCode() + type.hashCode();
    }
}