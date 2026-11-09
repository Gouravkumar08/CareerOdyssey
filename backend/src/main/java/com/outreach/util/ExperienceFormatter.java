package com.outreach.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class ExperienceFormatter {

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private ExperienceFormatter() {
    }

    /**
     * "Jan 2024 - Present (1 yr 7 mos)", or null if start is missing.
     */
    public static String formatRange(YearMonth start, YearMonth end) {
        if (start == null) {
            return null;
        }
        YearMonth effectiveEnd = end != null ? end : YearMonth.now();
        String startText = MONTH_YEAR.format(start);
        String endText = end == null ? "Present" : MONTH_YEAR.format(end);
        int totalMonths = monthsBetween(start, effectiveEnd, end != null);
        return startText + " - " + endText + " (" + formatFromMonths(totalMonths) + ")";
    }

    /**
     * Total career duration across the current and previous roles (each counted on its own,
     * not as a calendar span, so a gap between roles isn't counted as experience), e.g.
     * "1 yr 7 mos" — or null if no role dates are set at all.
     */
    public static String formatOverall(YearMonth currentStart, YearMonth currentEnd,
                                        YearMonth previousStart, YearMonth previousEnd) {
        int totalMonths = 0;
        boolean any = false;

        if (currentStart != null) {
            YearMonth effectiveEnd = currentEnd != null ? currentEnd : YearMonth.now();
            totalMonths += monthsBetween(currentStart, effectiveEnd, currentEnd != null);
            any = true;
        }
        if (previousStart != null) {
            YearMonth effectiveEnd = previousEnd != null ? previousEnd : YearMonth.now();
            totalMonths += monthsBetween(previousStart, effectiveEnd, previousEnd != null);
            any = true;
        }

        if (!any) {
            return null;
        }
        return formatFromMonths(totalMonths);
    }

    /**
     * Months between start and end. When the end date is explicit (a completed role), both the
     * start and end month are counted as worked (inclusive). When end is "now" (still employed,
     * shown as "Present"), the current in-progress month isn't counted as complete yet.
     */
    private static int monthsBetween(YearMonth start, YearMonth end, boolean inclusive) {
        int diff = (int) start.until(end, ChronoUnit.MONTHS);
        if (inclusive) {
            diff += 1;
        }
        return Math.max(0, diff);
    }

    private static String formatFromMonths(int totalMonths) {
        int years = totalMonths / 12;
        int months = totalMonths % 12;

        if (years == 0 && months == 0) {
            return "< 1 mo";
        }
        StringBuilder sb = new StringBuilder();
        if (years > 0) {
            sb.append(years).append(years == 1 ? " yr" : " yrs");
        }
        if (months > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(months).append(months == 1 ? " mo" : " mos");
        }
        return sb.toString();
    }
}
