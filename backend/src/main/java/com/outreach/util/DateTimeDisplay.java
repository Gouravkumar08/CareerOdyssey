package com.outreach.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateTimeDisplay {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("d MMMM, yyyy 'at' h:mm a", Locale.ENGLISH);

    private DateTimeDisplay() {
    }

    /**
     * "19 August, 2026 at 2:55 pm", or null if instant is null.
     */
    public static String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        String formatted = FORMAT.format(instant.atZone(ZoneId.systemDefault()));
        return formatted.replace("AM", "am").replace("PM", "pm");
    }
}
