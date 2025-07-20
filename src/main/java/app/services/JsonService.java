package app.services;

import app.models.DayData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class JsonService {
    private static final String DATA_FILE = "data/time_manager_data.json";
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    public static final TypeReference<Map<String, DayData>> TYPE_REF =
            new TypeReference<Map<String, DayData>>() {};

    /**
     * Загружает данные из JSON-файла.
     * Если файл отсутствует, возвращает пустую мапу.
     * При ошибке чтения/разбора JSON выбрасывает IOException.
     */
    public static synchronized Map<String, DayData> loadData() throws IOException {
        Path path = Paths.get(DATA_FILE);

        // Проверка существования файла
        if (!Files.exists(path)) {
            System.out.println("Файл данных не существует, создается новый: " + path.toAbsolutePath());
            return new HashMap<>();  // Возвращаем пустую карту, если файл не существует
        }

        // Проверка, если файл пустой
        if (Files.size(path) == 0) {
            System.out.println("Файл данных пустой, создается новый: " + path.toAbsolutePath());
            return new HashMap<>();  // Возвращаем пустую карту, если файл пустой
        }

        // Логируем путь к файлу перед загрузкой
        System.out.println("Attempting to load data from: " + path.toAbsolutePath());

        // Попытка десериализации данных из JSON
        try {
            Map<String, DayData> data = MAPPER.readValue(path.toFile(), TYPE_REF);

            // Логируем результат десериализации
            if (data == null || data.isEmpty()) {
                System.out.println("The deserialized data is empty.");
            } else {
                System.out.println("Deserialized data: " + data);  // Логируем загруженные данные
            }

            // Убираем null значения
            data.entrySet().removeIf(entry -> entry.getValue() == null);
            System.out.println("Final data after removing null values: " + data);

            return data;
        } catch (IOException e) {
            System.out.println("Ошибка при чтении/десериализации файла: " + path.toAbsolutePath());
            throw new IOException("Ошибка чтения или десериализации JSON файла: " + e.getMessage());
        }
    }

    /**
     * Загружает данные из файла календаря (calendar_текущий год.json).
     */
    public static synchronized Map<String, DayData> loadCalendarData(String year) throws IOException {
        // Формируем имя файла calendar_текущий год.json
        String calendarFileName = "data/calendar_" + year + ".json";
        Path calendarPath = Paths.get(calendarFileName);

        // Проверка существования файла
        if (!Files.exists(calendarPath)) {
            System.out.println("Файл календаря не существует, создается новый: " + calendarPath.toAbsolutePath());
            return new HashMap<>();
        }

        // Проверка, если файл пустой
        if (Files.size(calendarPath) == 0) {
            System.out.println("Файл календаря пустой, создается новый: " + calendarPath.toAbsolutePath());
            return new HashMap<>();
        }

        // Логируем путь к файлу перед загрузкой
        System.out.println("Attempting to load data from calendar file: " + calendarPath.toAbsolutePath());

        // Попытка десериализации данных из JSON
        try {
            Map<String, DayData> data = MAPPER.readValue(calendarPath.toFile(), TYPE_REF);

            // Логируем результат десериализации
            if (data == null || data.isEmpty()) {
                System.out.println("The deserialized calendar data is empty.");
            } else {
                System.out.println("Deserialized calendar data: " + data);
            }

            // Убираем null значения
            data.entrySet().removeIf(entry -> entry.getValue() == null);
            System.out.println("Final data after removing null values from calendar: " + data);

            return data;
        } catch (IOException e) {
            System.out.println("Ошибка при чтении/десериализации календарного файла: " + calendarPath.toAbsolutePath());
            throw new IOException("Ошибка чтения или десериализации календарного JSON файла: " + e.getMessage());
        }
    }

    /**
     * Сохраняет данные в JSON-файл атомарно: в tmp-файл с последующей заменой.
     * При ошибке выбрасывает IOException.
     */
    public static synchronized void saveData(Map<String, DayData> data) throws IOException {
        Path path = Paths.get(DATA_FILE);
        Files.createDirectories(path.getParent());
        Path tmp = Files.createTempFile(path.getParent(), "time_manager_data", ".tmp");
        MAPPER.writeValue(tmp.toFile(), data);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("Данные успешно сохранены в: " + path.toAbsolutePath());
    }

    /**
     * Возвращает копию загруженных данных.
     * При ошибке чтения выбрасывает IOException.
     */
    public static synchronized Map<String, DayData> getData() throws IOException {
        // Возвращаем изменяемую копию, чтобы её можно было модифицировать
        return new HashMap<>(loadData());
    }
}
