package dev.stafflens.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    public static String format(long timestamp) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }
}
