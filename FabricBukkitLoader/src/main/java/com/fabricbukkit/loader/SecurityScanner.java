package com.fabricbukkit.loader;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;

/**
 * Scans mod files for potential security issues
 */
public class SecurityScanner {
    
    private final FabricBukkitLoader plugin;
    // List of potentially dangerous package patterns
    private static final List<String> DANGEROUS_PACKAGES = List.of(
        "java.lang", 
        "java.io", 
        "java.nio", 
        "javax.script", 
        "sun.misc", 
        "sun.reflect", 
        "jdk.internal"
    );
    
    // List of potentially dangerous class names
    private static final List<String> DANGEROUS_CLASSES = List.of(
        "Runtime", 
        "ProcessBuilder", 
        "URLClassLoader", 
        "MethodHandles", 
        "Unsafe",
        "Classpath"
    );
    
    // Pattern to detect potentially dangerous method calls in decompiled code or comments
    private static final Pattern DANGEROUS_CALLS_PATTERN = Pattern.compile(
        "(exec|execute|loadLibrary|load|defineClass|getDeclaredField|getDeclaredMethod|setAccessible|eval|compile)\\s*\\("
    );
    
    public SecurityScanner(FabricBukkitLoader plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Scan a mod file for potential security issues
     */
    public SecurityReport scanMod(File modFile) {
        SecurityReport report = new SecurityReport(modFile.getName());
        
        if (!modFile.exists() || !modFile.canRead()) {
            report.addIssue("File access error", "Mod file does not exist or is not readable: " + modFile.getAbsolutePath());
            return report;
        }
        
        try (JarFile jarFile = new JarFile(modFile)) {
            // Check for dangerous entries in the JAR
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                // Check for dangerous file paths
                String entryName = entry.getName();
                
                // Skip directories
                if (entry.isDirectory()) {
                    continue;
                }
                
                // Check for dangerous file extensions or locations
                if (isDangerousFile(entryName)) {
                    report.addIssue("Potentially dangerous file", 
                        "Found potentially dangerous file: " + entryName);
                }
                
                // Check for dangerous package names in .class files
                if (entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').replace('\\', '.');
                    if (isDangerousClass(className)) {
                        report.addIssue("Suspicious class", 
                            "Found potentially dangerous class: " + className);
                    }
                }
                
                // Check for dangerous resources
                if (entryName.contains("META-INF") && 
                    (entryName.endsWith("MANIFEST.MF") || entryName.endsWith(".SF") || entryName.endsWith(".RSA"))) {
                    // These are standard Java signing files, which are generally safe
                    // But we could check for unusual content if needed
                }
            }
            
            // Check for missing signatures if required
            if (plugin.getConfigManager().isVerifyPluginSignatures()) {
                if (!isSigned(jarFile)) {
                    report.addIssue("Unsigned mod", 
                        "Mod is not signed but signature verification is enabled: " + modFile.getName());
                }
            }
            
            // Add general information
            report.addInfo("Mod file size", String.valueOf(modFile.length()) + " bytes");
            report.addInfo("Number of entries", String.valueOf(jarFile.size()));
            
        } catch (IOException e) {
            report.addIssue("Scan error", "Could not scan mod file: " + e.getMessage());
        }
        
        // If no issues were found, mark as safe
        if (report.getIssueCount() == 0) {
            report.addInfo("Security status", "No security issues detected");
        }
        
        return report;
    }
    
    /**
     * Check if a file path is potentially dangerous
     */
    private boolean isDangerousFile(String fileName) {
        // Check for executable files
        if (fileName.toLowerCase().matches(".*\\.(exe|bat|cmd|sh|ps1|com|scr|pif|vbs|js|jar|class)$")) {
            // JAR and CLASS files are expected in mods, but let's flag anything else
            if (!fileName.toLowerCase().endsWith(".jar") && !fileName.toLowerCase().endsWith(".class")) {
                return true;
            }
        }
        
        // Check for configuration files in unexpected locations
        if (fileName.contains("../") || fileName.contains("..\\\\")) {
            return true; // Directory traversal attempt
        }
        
        // Check for files that might override system resources
        return fileName.toLowerCase().matches(".*\\.(dll|so|dylib)$");
    }
    
    /**
     * Check if a class name is potentially dangerous
     */
    private boolean isDangerousClass(String className) {
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        
        // Check if the class is in a dangerous package
        for (String dangerousPackage : DANGEROUS_PACKAGES) {
            if (className.startsWith(dangerousPackage + ".")) {
                // Allow these packages if they're in the shaded/relocated area
                if (!className.contains("com.fabricbukkit.shaded")) {
                    return true;
                }
            }
        }
        
        // Check for dangerous class names
        for (String dangerousClass : DANGEROUS_CLASSES) {
            if (simpleClassName.contains(dangerousClass)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if the JAR file is signed
     */
    private boolean isSigned(JarFile jarFile) {
        try {
            // Check if the JAR has signature files
            return jarFile.getEntry("META-INF/MANIFEST.MF") != null;
            // Note: A complete signature check would require verifying the signature itself
            // which is complex and outside the scope of this basic scanner
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking signature for " + jarFile.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform a quick security check on a list of mod files
     */
    public Map<String, SecurityReport> scanMods(List<File> modFiles) {
        Map<String, SecurityReport> results = new HashMap<>();
        
        for (File modFile : modFiles) {
            String fileName = modFile.getName();
            plugin.debug("Scanning mod for security: " + fileName);
            
            SecurityReport report = scanMod(modFile);
            results.put(fileName, report);
            
            if (report.getIssueCount() > 0) {
                plugin.getLogger().warning("Security issues found in mod: " + fileName);
                for (String issue : report.getIssues()) {
                    plugin.getLogger().warning("  - " + issue);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Inner class to hold security scan results for a single mod
     */
    public static class SecurityReport {
        private final String fileName;
        private final List<String> info;
        private final List<String> issues;
        
        public SecurityReport(String fileName) {
            this.fileName = fileName;
            this.info = new ArrayList<>();
            this.issues = new ArrayList<>();
        }
        
        public void addInfo(String key, String value) {
            info.add(key + ": " + value);
        }
        
        public void addIssue(String type, String description) {
            issues.add(type + " - " + description);
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public List<String> getInfo() {
            return new ArrayList<>(info);
        }
        
        public List<String> getIssues() {
            return new ArrayList<>(issues);
        }
        
        public int getIssueCount() {
            return issues.size();
        }
        
        public boolean hasIssues() {
            return !issues.isEmpty();
        }
        
        public String getFormattedReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== Security Report for ").append(fileName).append(" ===\n");
            
            if (!info.isEmpty()) {
                report.append("Information:\n");
                for (String infoItem : info) {
                    report.append("  ").append(infoItem).append("\n");
                }
            }
            
            if (!issues.isEmpty()) {
                report.append("\nSecurity Issues Found:\n");
                for (String issue : issues) {
                    report.append("  ").append(issue).append("\n");
                }
            } else {
                report.append("\nNo security issues detected.\n");
            }
            
            return report.toString();
        }
        
        @Override
        public String toString() {
            return "SecurityReport{" +
                    "fileName='" + fileName + '\'' +
                    ", issueCount=" + getIssueCount() +
                    '}';
        }
    }
}