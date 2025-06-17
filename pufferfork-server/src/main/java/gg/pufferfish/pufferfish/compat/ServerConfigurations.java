package gg.pufferfish.pufferfish.compat;

import com.google.common.io.Files;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ServerConfigurations {

    public static final String[] configurationFiles = new String[]{
      "server.properties",
      "bukkit.yml",
      "spigot.yml",
      "config/paper-global.yml",
      "config/paper-world-defaults.yml",
      "pufferfish.yml"
    };

    public static Map<String, String> getCleanCopies() throws IOException {
        Map<String, String> files = new HashMap<>(configurationFiles.length);
        for (String file : configurationFiles) {
            files.put(file, getCleanCopy(file));
        }
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        for (net.minecraft.server.level.ServerLevel serverLevel : server.getAllLevels()) {
            File worldDir = serverLevel.getWorld().getWorldFolder();
            File paperWorldConfig = new File(worldDir, "paper-world.yml");
            String cleanConfig = getCleanCopy(paperWorldConfig.getPath());
            if (!cleanConfig.isEmpty()) {
                files.put(paperWorldConfig.getPath(), cleanConfig);
            }
        }
        return files;
    }

    public static boolean matchesRegex(String key, List<String> patterns) {
        for (String configKey : patterns) {
            String regex = configKey.replace(".", "\\.").replace("*", ".*");
            if (key.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static String getCleanCopy(String configName) throws IOException {
        File file = new File(configName);
        List<String> hiddenConfigs = new ArrayList<>(List.of(
            "proxies.velocity.secret",
            "web-services.token",
            "misc.sentry-dsn",
            "database",
            "server-ip",
            "motd",
            "resource-pack",
            "level-seed",
            "rcon.password",
            "rcon.ip",
            "feature-seeds",
            "world-settings.*.feature-seeds",
            "world-settings.*.seed-*",
            "seed-*"
        ));

        switch (Files.getFileExtension(configName)) {
            case "properties": {
                Properties properties = new Properties();
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }
                for (String hiddenConfig : properties.stringPropertyNames()) {
                    if (matchesRegex(hiddenConfig, hiddenConfigs)) properties.remove(hiddenConfig);
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                properties.store(outputStream, "");
                return Arrays.stream(outputStream.toString()
                  .split("\n"))
                  .filter(line -> !line.startsWith("#"))
                  .collect(Collectors.joining("\n"));
            }
            case "yml": {
                YamlConfiguration configuration = new YamlConfiguration();
                try {
                    configuration.load(file);
                } catch (InvalidConfigurationException e) {
                    throw new IOException(e);
                }
                configuration.options().header(null);
                for (String key : configuration.getKeys(true)) {
                    if (matchesRegex(key, hiddenConfigs)) {
                        configuration.set(key, null);
                    }
                }
                if (configuration.getKeys(false).size() == 1) {
                    return "";
                } else {
                    return configuration.saveToString();
                }
            }
            default:
                throw new IllegalArgumentException("Bad file type " + configName);
        }
    }

}
