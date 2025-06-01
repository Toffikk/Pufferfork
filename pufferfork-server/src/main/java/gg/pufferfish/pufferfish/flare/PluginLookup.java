package gg.pufferfish.pufferfish.flare;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.PluginClassLoader;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PluginLookup {
    private static final Cache<String, String> pluginNameCache = CacheBuilder.newBuilder()
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .maximumSize(1024)
      .build();

    public static Optional<String> getPluginForClass(String name) {
        if (name.startsWith("net.minecraft") || name.startsWith("java.") || name.startsWith("com.mojang") ||
          name.startsWith("com.google") || name.startsWith("it.unimi") || name.startsWith("sun")) {
            return Optional.empty();
        }

        String existing = pluginNameCache.getIfPresent(name);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }

        String newValue = "";

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            if (classLoader instanceof PluginClassLoader) {
                if (((PluginClassLoader) classLoader)._airplane_hasClass(name)) {
                    newValue = plugin.getName();
                    break;
                }
            }
        }

        pluginNameCache.put(name, newValue);
        return Optional.ofNullable(newValue.isEmpty() ? null : newValue);
    }
}
