package dev.stafflens.command;

import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditFilter;
import dev.stafflens.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits subcommand arguments into positional tokens, an optional page number, and {@code key:value}
 * filters (action/time/target/staff/flagged). Unknown tokens stay positional, so plain names and
 * search text keep working as before.
 */
public final class CommandArgs {

    private final AuditFilter filter = new AuditFilter();
    private final List<String> positional = new ArrayList<>();
    private final List<String> baseTokens = new ArrayList<>();
    private int page = 1;
    private boolean hasFilters = false;

    private CommandArgs() {
    }

    public static CommandArgs parse(String[] args) {
        CommandArgs result = new CommandArgs();
        long now = System.currentTimeMillis();

        for (String token : args) {
            if (token.isEmpty()) {
                continue;
            }
            if (isInteger(token)) {
                result.page = Math.max(1, Integer.parseInt(token));
                continue;
            }
            result.baseTokens.add(token);
            if (token.equalsIgnoreCase("flagged")) {
                result.filter.flaggedOnly(true);
                result.hasFilters = true;
                continue;
            }
            int colon = token.indexOf(':');
            if (colon > 0 && result.applyKeyValue(token.substring(0, colon), token.substring(colon + 1), now)) {
                continue;
            }
            result.positional.add(token);
        }
        return result;
    }

    private boolean applyKeyValue(String key, String value, long now) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "action" -> {
                try {
                    filter.action(ActionType.fromConfig(value));
                    hasFilters = true;
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            case "time" -> {
                Long millis = TimeUtil.parseDurationMillis(value);
                if (millis == null) {
                    return false;
                }
                filter.since(now - millis);
                hasFilters = true;
                return true;
            }
            case "target" -> {
                filter.targetName(value);
                hasFilters = true;
                return true;
            }
            case "staff" -> {
                filter.staffName(value);
                hasFilters = true;
                return true;
            }
            case "flagged" -> {
                filter.flaggedOnly(isTruthy(value));
                hasFilters = true;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public AuditFilter filter() {
        return filter;
    }

    public List<String> positional() {
        return positional;
    }

    public String firstPositional() {
        return positional.isEmpty() ? null : positional.get(0);
    }

    public String joinedPositional() {
        return String.join(" ", positional);
    }

    /** All tokens except the page number, so pagination links keep the active filters. */
    public String baseArgs() {
        return String.join(" ", baseTokens);
    }

    public int page() {
        return page;
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    private static boolean isInteger(String token) {
        if (token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTruthy(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equals("1");
    }
}
