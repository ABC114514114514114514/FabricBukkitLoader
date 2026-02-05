package com.fabricbukkit.loader;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Represents a loaded Fabric mod
 */
public class LoadedMod {
    
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String mainClass;
    private final File modFile;
    private final Map<String, Object> metadata;
    private final List<String> authors;
    private final List<ModDependency> dependencies;
    private final ClassLoader classLoader;
    
    private boolean enabled;
    private Object instance;
    
    public LoadedMod(String id, String name, String version, String description, 
                     String mainClass, File modFile, Map<String, Object> metadata,
                     List<String> authors, List<ModDependency> dependencies, ClassLoader classLoader) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.mainClass = mainClass;
        this.modFile = modFile;
        this.metadata = metadata;
        this.authors = authors;
        this.dependencies = dependencies;
        this.classLoader = classLoader;
        this.enabled = false;
        this.instance = null;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getMainClass() {
        return mainClass;
    }
    
    public File getModFile() {
        return modFile;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public List<String> getAuthors() {
        return authors;
    }
    
    public List<ModDependency> getDependencies() {
        return dependencies;
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Object getInstance() {
        return instance;
    }
    
    public void setInstance(Object instance) {
        this.instance = instance;
    }
    
    public boolean hasDependency(String dependencyId) {
        return dependencies != null && dependencies.contains(dependencyId);
    }
    
    public String getFormattedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Mod ID: ").append(id).append("\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Version: ").append(version).append("\n");
        if (description != null && !description.isEmpty()) {
            sb.append("Description: ").append(description).append("\n");
        }
        if (authors != null && !authors.isEmpty()) {
            sb.append("Authors: ").append(String.join(", ", authors)).append("\n");
        }
        if (dependencies != null && !dependencies.isEmpty()) {
            // Convert dependencies to string representation
            java.util.List<String> depStrings = new java.util.ArrayList<>();
            for (ModDependency dep : dependencies) {
                depStrings.add(dep.getModId() + " (" + dep.getType() + ")");
            }
            sb.append("Dependencies: ").append(String.join(", ", depStrings)).append("\n");
        }
        sb.append("File: ").append(modFile.getName()).append("\n");
        sb.append("Status: ").append(enabled ? "Enabled" : "Disabled").append("\n");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "LoadedMod{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", enabled=" + enabled +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LoadedMod that = (LoadedMod) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    /**
     * Get all dependencies of a specific type
     */
    public List<ModDependency> getDependenciesByType(ModDependency.DependencyType type) {
        if (dependencies == null) return new java.util.ArrayList<>();
        
        List<ModDependency> result = new java.util.ArrayList<>();
        for (ModDependency dep : dependencies) {
            if (dep.getType() == type) {
                result.add(dep);
            }
        }
        return result;
    }
    
    /**
     * Check if this mod conflicts with another mod
     */
    public boolean conflictsWith(LoadedMod otherMod) {
        if (dependencies == null) return false;
        
        for (ModDependency dep : dependencies) {
            if (dep.getType() == ModDependency.DependencyType.CONFLICTS && 
                dep.getModId().equals(otherMod.getId())) {
                return true;
            }
        }
        return false;
    }
}