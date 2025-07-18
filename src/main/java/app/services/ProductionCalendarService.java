package app.services;

import app.utils.UIHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

public class ProductionCalendarService {
    private static final String CALENDAR_DATA_DIR = "data/calendar/";
    private static final String BASE_URL = "https://xmlcalendar.ru/data/ru/%d/calendar.json";
    private static final ConcurrentHashMap<Integer, Set<LocalDate>> HOLIDAYS_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Set<LocalDate>> SHORT_DAYS_CACHE = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        new File(CALENDAR_DATA_DIR).mkdirs();
        loadExistingCalendars(); // Добавлено: загрузка существующих календарей
    }

    // Добавленный метод: загрузка существующих календарей
    private static void loadExistingCalendars() {
        File dir = new File(CALENDAR_DATA_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("calendar_") && name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String fileName = file.getName();
                int year = Integer.parseInt(fileName.replace("calendar_", "").replace(".json", ""));
                loadFromLocalFile(year, file.toPath());
            } catch (Exception e) {
                System.err.println("Ошибка загрузки календаря из файла: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    public static List<String> loadShortDays(String year) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String filePath = "data/calendar/calendar_" + year + ".json"; // Ensure the path is correct
            File calendarFile = new File(filePath);

            if (!calendarFile.exists()) {
                System.out.println("File not found for year " + year + " (" + filePath + "), returning empty list for short days.");
                return List.of();
            }

            // Load data from the file
            Map<String, Object> calendarData = objectMapper.readValue(calendarFile, Map.class);

            // Log the loading process
            System.out.println("Successfully loaded short days data from file " + filePath);
            if (calendarData.containsKey("shortDays")) {
                System.out.println("Loaded short days from file " + filePath + ": " + calendarData.get("shortDays"));
            } else {
                System.out.println("No short days found in " + filePath);
            }

            return (List<String>) calendarData.get("shortDays");
        } catch (IOException e) {
            // Log error and return an empty list if an error occurs
            System.err.println("Error loading short days for year " + year + ": " + e.getMessage());
            return List.of(); // Return an empty list of short days
        }
    }

    public static List<String> loadHolidays(String year) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String filePath = "data/calendar/calendar_" + year + ".json"; // Ensure the path is correct
            File calendarFile = new File(filePath);

            if (!calendarFile.exists()) {
                System.out.println("File not found for year " + year + " (" + filePath + "), returning empty list for holidays.");
                return List.of();
            }

            // Load data from the file
            Map<String, Object> calendarData = objectMapper.readValue(calendarFile, Map.class);

            // Log the loading process
            System.out.println("Successfully loaded holidays data from file " + filePath);
            if (calendarData.containsKey("holidays")) {
                System.out.println("Loaded holidays from file " + filePath + ": " + calendarData.get("holidays"));
            } else {
                System.out.println("No holidays found in " + filePath);
            }

            return (List<String>) calendarData.get("holidays");
        } catch (IOException e) {
            // Log error and return an empty list if an error occurs
            System.err.println("Error loading holidays for year " + year + ": " + e.getMessage());
            return List.of(); // Return an empty list of holidays
        }
    }

    private static void loadFromLocalFile(int year, Path path) throws IOException {
        // Log the file path
        System.out.println("Loading data from local file: " + path.toAbsolutePath());

        JsonNode root = mapper.readTree(path.toFile());
        Set<LocalDate> holidays = parseDates(root.path("holidays"));
        Set<LocalDate> shortDays = parseDates(root.path("shortDays"));

        // Log the number of found data
        System.out.println("Loaded " + holidays.size() + " holidays and " + shortDays.size() + " short days for year " + year);

        if (!holidays.isEmpty() || !shortDays.isEmpty()) {
            HOLIDAYS_CACHE.put(year, holidays);
            SHORT_DAYS_CACHE.put(year, shortDays);
        } else {
            System.out.println("No data found for holidays or short days in file " + path);
        }
    }

    private static Set<LocalDate> parseDates(JsonNode node) {
        Set<LocalDate> dates = new HashSet<>();
        if (node.isArray()) {
            for (JsonNode dateNode : node) {
                dates.add(LocalDate.parse(dateNode.asText()));
            }
        }
        return dates;
    }

    public static boolean isCalendarLoaded(int year) {
        return HOLIDAYS_CACHE.containsKey(year);
    }

    public static boolean isShortDaysLoaded(int year) {
        return SHORT_DAYS_CACHE.containsKey(year);
    }

    public static void loadCalendarForYear(int year) throws IOException, InterruptedException {
        if (isCalendarLoaded(year) && isShortDaysLoaded(year)) return;

        Path localPath = Paths.get(CALENDAR_DATA_DIR + "calendar_" + year + ".json");
        boolean fileExists = Files.exists(localPath);

        if (fileExists) {
            try {
                loadFromLocalFile(year, localPath);
                return;
            } catch (IOException e) {
                System.err.println("Ошибка загрузки из локального файла: " + e.getMessage());
                Files.deleteIfExists(localPath);
            }
        }

        downloadCalendarData(year);
    }


    private static void downloadCalendarData(int year) throws IOException, InterruptedException {
        String url = String.format(BASE_URL, year);
        System.out.println("Downloading data from: " + url);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json; charset=utf-8")
                .timeout(java.time.Duration.ofSeconds(20))
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println("Response status: " + response.statusCode());

            if (response.statusCode() == 200) {
                String responseBody = new String(response.body(), StandardCharsets.UTF_8);
                System.out.println("Response size: " + responseBody.length() + " characters");

                Path rawResponsePath = Paths.get("raw_response_" + year + ".json");
                Files.write(rawResponsePath, responseBody.getBytes(StandardCharsets.UTF_8));
                System.out.println("Raw response saved at: " + rawResponsePath.toAbsolutePath());

                JsonNode root = mapper.readTree(responseBody);
                Set<LocalDate> holidays = new HashSet<>();
                Set<LocalDate> shortDays = new HashSet<>();

                if (root.has("year") && root.has("months")) {
                    System.out.println("New data format (2025+) detected");
                    processNewFormat(root, year, holidays, shortDays);
                }
                else if (root.isArray()) {
                    System.out.println("Data format: months array (old format)");
                    for (JsonNode monthNode : root) {
                        processMonthNode(monthNode, year, holidays, shortDays);
                    }
                } else if (root.isObject() && root.has("months")) {
                    System.out.println("Data format: object with months (old format)");
                    JsonNode months = root.path("months");
                    if (!months.isMissingNode()) {
                        JsonNode monthArray = months.path("month");
                        if (monthArray.isArray()) {
                            for (JsonNode monthNode : monthArray) {
                                processMonthNode(monthNode, year, holidays, shortDays);
                            }
                        }
                    }
                } else {
                    throw new IOException("Unsupported data format from server");
                }

                System.out.println("Found holidays: " + holidays.size());
                System.out.println("Found short days: " + shortDays.size());

                if (holidays.isEmpty() && shortDays.isEmpty()) {
                    throw new IOException("Server returned empty calendar");
                }

                HOLIDAYS_CACHE.put(year, holidays);
                SHORT_DAYS_CACHE.put(year, shortDays);
                saveToLocalFile(year, holidays, shortDays);
            } else {
                throw new IOException("Server returned error code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error downloading data: " + e.getMessage());
        }
    }

    private static void processNewFormat(JsonNode root, int year, Set<LocalDate> holidays, Set<LocalDate> shortDays) {
        JsonNode months = root.path("months");
        if (!months.isArray()) {
            System.out.println("Поле 'months' не является массивом");
            return;
        }

        for (JsonNode monthNode : months) {
            int monthNum = monthNode.path("month").asInt();
            String daysStr = monthNode.path("days").asText();

            if (daysStr == null || daysStr.isEmpty()) {
                System.out.println("Пустая строка дней для месяца " + monthNum);
                continue;
            }

            String[] dayEntries = daysStr.split(",");
            for (String dayEntry : dayEntries) {
                try {
                    String cleanDay = dayEntry.replaceAll("[^0-9]", "");
                    if (cleanDay.isEmpty()) {
                        System.out.println("Пустой день в записи: " + dayEntry);
                        continue;
                    }

                    int dayNum = Integer.parseInt(cleanDay);
                    LocalDate date = LocalDate.of(year, monthNum, dayNum);

                    holidays.add(date);

                    if (dayEntry.contains("*")) {
                        shortDays.add(date);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка обработки дня: " + dayEntry + " в месяце " + monthNum);
                }
            }
        }
    }

    private static void processMonthNode(JsonNode monthNode, int year, Set<LocalDate> holidays, Set<LocalDate> shortDays) {
        if (monthNode.isMissingNode() || monthNode.isNull()) {
            System.out.println("Пропущен пустой узел месяца");
            return;
        }

        int monthNum;
        if (monthNode.has("month")) {
            String monthStr = monthNode.get("month").asText();
            try {
                monthNum = Integer.parseInt(monthStr.split("-")[1]);
            } catch (Exception e) {
                System.err.println("Ошибка разбора номера месяца: " + monthStr);
                return;
            }
        } else if (monthNode.has("num")) {
            monthNum = monthNode.get("num").asInt();
        } else {
            System.err.println("Узел месяца не содержит поля 'month' или 'num'");
            return;
        }

        JsonNode days = monthNode.path("days");
        if (!days.isArray()) {
            System.out.println("Дни не являются массивом для месяца " + monthNum);
            return;
        }

        System.out.println("Обработка месяца " + monthNum + ", дней: " + days.size());

        for (JsonNode day : days) {
            int dayNum = day.path("d").asInt();
            int type = day.path("t").asInt(0);
            int holidayFlag = day.path("h").asInt(0);
            boolean isHoliday = holidayFlag == 1;

            try {
                LocalDate date = LocalDate.of(year, monthNum, dayNum);

                if (isHoliday) {
                    holidays.add(date);
                } else if (type == 3) {
                    shortDays.add(date);
                }
            } catch (Exception e) {
                System.err.println("Ошибка создания даты: " + year + "-" + monthNum + "-" + dayNum);
            }
        }
    }

    private static void saveToLocalFile(int year, Set<LocalDate> holidays, Set<LocalDate> shortDays) throws IOException {
        System.out.println("Сохранение данных для " + year);
        System.out.println("Праздники: " + holidays.size() + " дней");
        System.out.println("Сокращенные дни: " + shortDays.size() + " дней");

        ObjectNode root = mapper.createObjectNode();

        ArrayNode holidaysArray = root.putArray("holidays");
        for (LocalDate date : holidays) {
            holidaysArray.add(date.toString());
        }

        ArrayNode shortDaysArray = root.putArray("shortDays");
        for (LocalDate date : shortDays) {
            shortDaysArray.add(date.toString());
        }

        Path path = Paths.get(CALENDAR_DATA_DIR + "calendar_" + year + ".json");
        System.out.println("Путь для сохранения: " + path.toAbsolutePath());

        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
        System.out.println("Файл успешно сохранен");
    }

    public static boolean isHoliday(LocalDate date) {
        int year = date.getYear();
        if (!HOLIDAYS_CACHE.containsKey(year)) {
            return UIHelper.isWeekend(date);
        }
        return HOLIDAYS_CACHE.getOrDefault(year, Collections.emptySet()).contains(date);
    }

    public static boolean isShortDay(LocalDate date) {
        int year = date.getYear();
        if (!SHORT_DAYS_CACHE.containsKey(year)) {
            return false;
        }
        return SHORT_DAYS_CACHE.getOrDefault(year, Collections.emptySet()).contains(date);
    }

    public static boolean isWeekend(LocalDate date) {
        return isHoliday(date) || UIHelper.isWeekend(date);
    }
}
