package com.example.demo.patterns.singleton;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * DESIGN PATTERN: Singleton
 * ============================================================
 * Intent: Ensure a class has only one instance and provide
 * a global access point to it.
 *
 * Spring already makes all @Component/@Service beans singletons
 * by default. But here we also show the manual thread-safe
 * implementation for educational purposes.
 * ============================================================
 */
@Component // Spring ensures this is a singleton automatically
public class AppConfigManager {

    // --- Spring way (auto-singleton via @Component) -----------
    private final Map<String, String> config = new HashMap<>();

    public AppConfigManager() {
        // Load default config
        config.put("batch.chunk-size", "5");
        config.put("thread.pool-size", "4");
        config.put("salary.strategy", "egyptianScale");
        config.put("notification.default-type", "email");
    }

    public String get(String key) {
        return config.getOrDefault(key, "");
    }

    public void set(String key, String value) {
        config.put(key, value);
    }

    public Map<String, String> getAll() {
        return Map.copyOf(config); // immutable copy
    }

    // --- Manual Singleton (for learning, not used by Spring) ---
    private static volatile AppConfigManager manualInstance;

    /**
     * Thread-safe double-checked locking singleton.
     * Use this pattern when NOT using a DI framework.
     */
    public static AppConfigManager getManualInstance() {
        if (manualInstance == null) {
            synchronized (AppConfigManager.class) {
                if (manualInstance == null) {
                    manualInstance = new AppConfigManager();
                }
            }
        }
        return manualInstance;
    }
}
