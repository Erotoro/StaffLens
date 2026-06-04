package dev.stafflens.notify;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.util.TimeUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Posts audit alerts to a Discord webhook. Sends are fire-and-forget, so a slow or unreachable
 * webhook never blocks the server; delivery failures are logged as a warning and otherwise ignored.
 */
public class DiscordNotifier {

    private final StaffLensPlugin plugin;
    private final boolean enabled;
    private final String webhookUrl;
    private final String username;
    private final boolean sendCritical;
    private final boolean sendAnomalies;
    private final HttpClient httpClient;

    public DiscordNotifier(StaffLensPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        this.webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        this.username = plugin.getConfig().getString("discord.username", "StaffLens");
        this.sendCritical = plugin.getConfig().getBoolean("discord.send-critical", true);
        this.sendAnomalies = plugin.getConfig().getBoolean("discord.send-anomalies", true);
        this.httpClient = (enabled && isConfigured())
                ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                : null;
    }

    public boolean isActive() {
        return httpClient != null;
    }

    private boolean isConfigured() {
        return webhookUrl != null && webhookUrl.startsWith("http");
    }

    public void sendCritical(AuditEntry entry) {
        if (!isActive() || !sendCritical) {
            return;
        }
        send(":rotating_light: **Critical action**\n" + format(entry));
    }

    public void sendAnomaly(AuditEntry entry, List<String> reasons) {
        if (!isActive() || !sendAnomalies) {
            return;
        }
        send(":warning: **Anomaly detected** (" + String.join(", ", reasons) + ")\n" + format(entry));
    }

    private String format(AuditEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("`").append(TimeUtil.format(entry.timestamp())).append("` ");
        sb.append("**").append(entry.staffName()).append("** ");
        sb.append(entry.action().displayName());
        if (entry.targetName() != null && !entry.targetName().isBlank()) {
            sb.append(" -> ").append(entry.targetName());
        }
        if (entry.reason() != null && !entry.reason().isBlank()) {
            sb.append(" (").append(entry.reason()).append(")");
        }
        if (entry.hasLocation()) {
            sb.append(" @ ").append(entry.world()).append(" ")
                    .append(entry.x()).append(",").append(entry.y()).append(",").append(entry.z());
        }
        if (entry.serverName() != null && !entry.serverName().isBlank()) {
            sb.append(" [").append(entry.serverName()).append("]");
        }
        return sb.toString();
    }

    private void send(String content) {
        String payload = "{\"username\":\"" + jsonEscape(username)
                + "\",\"content\":\"" + jsonEscape(content)
                + "\",\"allowed_mentions\":{\"parse\":[]}}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Discord webhook delivery failed: " + throwable.getMessage());
                        return null;
                    });
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid Discord webhook URL: " + e.getMessage());
        }
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
