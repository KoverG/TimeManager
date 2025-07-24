package app.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

public class TimeZoneManager {

    public static final String CODE_KLD = "КЛД"; // +2 GMT
    public static final String CODE_MSK = "МСК"; // +3 GMT


    public record ZoneItem(String code, ZoneId zoneId, String gmtText) {
        @Override public String toString() { return code; } // Чтобы красиво показывалось в ComboBox
    }

    private final Map<String, ZoneItem> zones = new LinkedHashMap<>();
    private final ObservableList<ZoneItem> items = FXCollections.observableArrayList();

    public TimeZoneManager() {
        addZone(CODE_KLD, ZoneId.of("Europe/Kaliningrad"), "+2 GMT");
        addZone(CODE_MSK, ZoneId.of("Europe/Moscow"), "+3 GMT");
    }

    public ObservableList<ZoneItem> getZoneItems() {
        return items;
    }

    public ZoneItem byCode(String code) { return zones.get(code); }

    public ZoneItem byZoneId(ZoneId id) {
        return zones.values().stream()
                .filter(z -> z.zoneId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void addZone(String code, ZoneId id, String gmtText) {
        ZoneItem zi = new ZoneItem(code, id, gmtText);
        zones.put(code, zi);
        items.add(zi);
    }

    // НОВЫЙ метод: гарантирует наличие зоны
    public ZoneItem ensureZone(ZoneId id) {
        ZoneItem ex = byZoneId(id);
        if (ex != null) return ex;

        String code = shortCodeFor(id);
        String gmt  = gmtText(id);
        ZoneItem zi = new ZoneItem(code, id, gmt);
        zones.put(code, zi);
        items.add(zi);
        return zi;
    }

    // НОВЫЕ util-методы
    private static String shortCodeFor(ZoneId id) {
        String raw = id.getId();
        int slash = raw.lastIndexOf('/');
        String city = slash >= 0 ? raw.substring(slash + 1) : raw;
        if (city.length() <= 6) return city.toUpperCase(Locale.ROOT);
        return "UTC" + gmtText(id).replace(" GMT", ""); // напр. UTC+2
    }

    private static String gmtText(ZoneId id) {
        ZoneOffset off = ZonedDateTime.now(id).getOffset();
        int hours = off.getTotalSeconds() / 3600;
        return String.format("%+d GMT", hours);
    }
}
