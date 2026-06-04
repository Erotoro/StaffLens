package dev.stafflens.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeUtil {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private TimeUtil() {
    }

    public static String format(long timestamp) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    /** Parses a compact duration like {@code 30m}, {@code 24h}, {@code 7d}, {@code 2w}; null if invalid. */
    public static Long parseDurationMillis(String input) {
        if (input == null || input.length() < 2) {
            return null;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        char unit = value.charAt(value.length() - 1);
        String numberPart = value.substring(0, value.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(numberPart);
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) {
            return null;
        }
        return switch (unit) {
            case 's' -> amount * 1000L;
            case 'm' -> amount * 60_000L;
            case 'h' -> amount * 3_600_000L;
            case 'd' -> amount * 86_400_000L;
            case 'w' -> amount * 604_800_000L;
            default -> null;
        };
    }
}
