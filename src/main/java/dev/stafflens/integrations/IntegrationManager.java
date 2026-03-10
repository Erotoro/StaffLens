package dev.stafflens.integrations;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.impl.*;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class IntegrationManager {

    private final StaffLensPlugin plugin;
    private final List<BaseIntegration> integrations = new ArrayList<>();

    public IntegrationManager(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        // Register potential integrations
        registerIfEnabled("LiteBans", new LiteBansIntegration(plugin));
        registerIfEnabled("BanManager", new BanManagerIntegration(plugin));
        registerIfEnabled("AdvancedBan", new AdvancedBanIntegration(plugin));
        registerIfEnabled("Essentials", new EssentialsIntegration(plugin));
        registerIfEnabled("CMI", new CMIIntegration(plugin));
        registerIfEnabled("LuckPerms", new LuckPermsIntegration(plugin));
        
        // Vanilla is always enabled, but we check config
        if (plugin.getConfig().getBoolean("integrations.Vanilla", true)) {
            BaseIntegration vanilla = new VanillaIntegration(plugin);
            vanilla.register();
            integrations.add(vanilla);
            plugin.getLogger().info("Integration loaded: Vanilla");
        }
    }

    public void reloadAll() {
        unloadAll();
        loadAll();
    }

    public void unloadAll() {
        for (BaseIntegration integration : integrations) {
            integration.unregister();
        }
        integrations.clear();
    }

    private void registerIfEnabled(String pluginName, BaseIntegration integration) {
        boolean enabledInConfig = plugin.getConfig().getBoolean("integrations." + pluginName, true);
        if (enabledInConfig && Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            integration.register();
            integrations.add(integration);
            plugin.getLogger().info("Integration loaded: " + pluginName);
        }
    }
}
