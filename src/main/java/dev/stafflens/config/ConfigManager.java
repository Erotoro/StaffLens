package dev.stafflens.config;

import dev.stafflens.StaffLensPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ConfigManager {

    private final StaffLensPlugin plugin;
    private FileConfiguration messagesConfig;

    public ConfigManager(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        String locale = plugin.getConfig().getString("locale", "en").toLowerCase(Locale.ROOT);
        File localeFolder = new File(plugin.getDataFolder(), "locale");
        if (!localeFolder.exists()) {
            localeFolder.mkdirs();
        }

        File messagesFile = new File(localeFolder, locale + ".yml");
        if (!messagesFile.exists()) {
            saveLocaleResource(locale);
        }
        if (!messagesFile.exists()) {
            messagesFile = new File(localeFolder, "en.yml");
            if (!messagesFile.exists()) {
                plugin.saveResource("locale/en.yml", false);
            }
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        applyBundledDefaults(locale);
    }

    // Use the bundled locale as defaults so keys added in a new version still resolve
    // even if the user keeps an older locale file on disk.
    private void applyBundledDefaults(String locale) {
        InputStream defaults = plugin.getResource("locale/" + locale + ".yml");
        if (defaults == null) {
            defaults = plugin.getResource("locale/en.yml");
        }
        if (defaults != null) {
            messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    private void saveLocaleResource(String locale) {
        try {
            plugin.saveResource("locale/" + locale + ".yml", false);
        } catch (IllegalArgumentException ignored) {
            plugin.saveResource("locale/en.yml", false);
        }
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }
}
