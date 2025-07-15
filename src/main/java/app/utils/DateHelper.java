package app.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateHelper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String[] MONTH_NAMES = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static String formatDisplayDate(LocalDate date) {
        return date.format(DISPLAY_FORMATTER);
    }

    public static String getMonthName(int month) {
        if (month < 1 || month > 12) return "";
        return MONTH_NAMES[month - 1];
    }

    public static String getMonthName(LocalDate date) {
        return getMonthName(date.getMonthValue());
    }
}
