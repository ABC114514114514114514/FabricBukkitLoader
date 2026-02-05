package com.fabricbukkit.loader;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Handles performance monitoring and statistics for loaded mods
 */
public class PerformanceMonitor {
    
    private final Map<String, ModStats> modStats;
    private final Map<String, Long> loadTimes;
    private final Map<String, Long> eventProcessingTimes;
    private final Map<String, Integer> eventCounts;
    
    public PerformanceMonitor() {
        this.modStats = new ConcurrentHashMap<>();
        this.loadTimes = new ConcurrentHashMap<>();
        this.eventProcessingTimes = new ConcurrentHashMap<>();
        this.eventCounts = new ConcurrentHashMap<>();
    }
    
    /**
     * Record mod load time
     */
    public void recordLoadTime(String modId, long loadTimeMs) {
        loadTimes.put(modId, loadTimeMs);
        getModStats(modId).setLoadTime(loadTimeMs);
    }
    
    /**
     * Record event processing time
     */
    public void recordEventProcessingTime(String modId, String eventName, long processingTimeNs) {
        String key = modId + ":" + eventName;
        Long previousTime = eventProcessingTimes.get(key);
        if (previousTime == null) {
            previousTime = 0L;
        }
        eventProcessingTimes.put(key, previousTime + processingTimeNs);
        
        // Record event count
        Integer count = eventCounts.get(key);
        eventCounts.put(key, count == null ? 1 : count + 1);
        
        // Update mod stats
        getModStats(modId).addEventTime(eventName, processingTimeNs);
    }
    
    /**
     * Get mod statistics
     */
    public ModStats getModStats(String modId) {
        return modStats.computeIfAbsent(modId, k -> new ModStats(modId));
    }
    
    /**
     * Get overall performance report
     */
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Mod Performance Report ===\n");
        
        for (ModStats stats : modStats.values()) {
            report.append(stats.getFormattedStats()).append("\n");
        }
        
        // Add total statistics
        report.append("\n=== Overall Stats ===\n");
        report.append("Total mods loaded: ").append(modStats.size()).append("\n");
        
        long totalLoadTime = loadTimes.values().stream().mapToLong(Long::longValue).sum();
        report.append("Total load time: ").append(totalLoadTime).append(" ms\n");
        
        long totalEventTime = eventProcessingTimes.values().stream().mapToLong(Long::longValue).sum();
        int totalEvents = eventCounts.values().stream().mapToInt(Integer::intValue).sum();
        report.append("Total events processed: ").append(totalEvents).append("\n");
        report.append("Total event processing time: ").append(totalEventTime / 1_000_000).append(" ms\n");
        
        return report.toString();
    }
    
    /**
     * Reset all statistics
     */
    public void resetStats() {
        modStats.clear();
        loadTimes.clear();
        eventProcessingTimes.clear();
        eventCounts.clear();
    }
    
    /**
     * Get statistics for a specific mod
     */
    public String getModReport(String modId) {
        ModStats stats = modStats.get(modId);
        if (stats == null) {
            return "No statistics found for mod: " + modId;
        }
        
        return stats.getFormattedStats();
    }
    
    /**
     * Inner class to hold statistics for a single mod
     */
    public static class ModStats {
        private final String modId;
        private long loadTime;
        private final Map<String, Long> eventProcessingTimes;
        private final Map<String, Integer> eventCounts;
        private int totalEventsProcessed;
        
        public ModStats(String modId) {
            this.modId = modId;
            this.loadTime = 0;
            this.eventProcessingTimes = new HashMap<>();
            this.eventCounts = new HashMap<>();
            this.totalEventsProcessed = 0;
        }
        
        public void setLoadTime(long loadTime) {
            this.loadTime = loadTime;
        }
        
        public void addEventTime(String eventName, long processingTimeNs) {
            Long previousTime = eventProcessingTimes.get(eventName);
            eventProcessingTimes.put(eventName, previousTime == null ? processingTimeNs : previousTime + processingTimeNs);
            
            Integer count = eventCounts.get(eventName);
            eventCounts.put(eventName, count == null ? 1 : count + 1);
            
            totalEventsProcessed++;
        }
        
        public String getFormattedStats() {
            StringBuilder sb = new StringBuilder();
            sb.append("Mod: ").append(modId).append("\n");
            sb.append("  Load time: ").append(loadTime).append(" ms\n");
            sb.append("  Total events processed: ").append(totalEventsProcessed).append("\n");
            
            if (!eventProcessingTimes.isEmpty()) {
                sb.append("  Event processing details:\n");
                for (Map.Entry<String, Long> entry : eventProcessingTimes.entrySet()) {
                    String eventName = entry.getKey();
                    long totalTime = entry.getValue();
                    int count = eventCounts.getOrDefault(eventName, 1);
                    long avgTime = totalTime / count / 1_000_000; // Convert to ms
                    
                    sb.append("    ").append(eventName)
                      .append(": ").append(count).append(" calls, ")
                      .append(totalTime / 1_000_000).append(" ms total, ")
                      .append(avgTime).append(" ms avg\n");
                }
            }
            
            return sb.toString();
        }
        
        // Getters
        public String getModId() { return modId; }
        public long getLoadTime() { return loadTime; }
        public int getTotalEventsProcessed() { return totalEventsProcessed; }
        public Map<String, Long> getEventProcessingTimes() { return new HashMap<>(eventProcessingTimes); }
        public Map<String, Integer> getEventCounts() { return new HashMap<>(eventCounts); }
    }
}