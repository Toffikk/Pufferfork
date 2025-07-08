package gg.pufferfish.pufferfish.compat;

import co.aikar.timings.TimingsManager;
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
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ServerConfigurations {

    public static final String[] configurationFiles = new String[]{
      "server.properties",
      "bukkit.yml",
      "spigot.yml",
      // "paper.yml", // TODO: Figure out what to do with this.
      "pufferfish.yml"
    };

    public static Map<String, String> getCleanCopies() throws IOException {
        Map<String, String> files = new HashMap<>(configurationFiles.length);
        for (String file : configurationFiles) {
            files.put(file, getCleanCopy(file));
        }
        return files;
    }

    public static String getCleanCopy(String configName) throws IOException {
        File file = new File(configName);
        List<String> hiddenConfigs = TimingsManager.hiddenConfigs;

        switch (Files.getFileExtension(configName)) {
            case "properties": {
                Properties properties = new Properties();
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }
                for (String hiddenConfig : hiddenConfigs) {
                    properties.remove(hiddenConfig);
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
                    if (hiddenConfigs.contains(key)) {
                        configuration.set(key, null);
                    }
                }
                return configuration.saveToString();
            }
            default:
                throw new IllegalArgumentException("Bad file type " + configName);
        }
    }

}
