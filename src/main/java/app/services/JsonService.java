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
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private static final TypeReference<Map<String, DayData>> TYPE_REF =
            new TypeReference<Map<String, DayData>>() {};

    /**
     * Загружает данные из JSON-файла.
     * Если файл отсутствует, возвращает пустую мапу.
     * При ошибке чтения/разбора JSON выбрасывает IOException.
     */
    public static synchronized Map<String, DayData> loadData() throws IOException {
        Path path = Paths.get(DATA_FILE);

        // Проверка, если файл не существует, возвращаем пустую карту
        if (!Files.exists(path)) {
            System.out.println("Файл данных не существует, создается новый: " + path.toAbsolutePath());
            return new HashMap<>();
        }

        // Если файл существует, проверяем его размер
        if (Files.size(path) == 0) {
            System.out.println("Файл данных пустой, создается новый: " + path.toAbsolutePath());
            return new HashMap<>();  // Возвращаем пустую карту, если файл пустой
        }

        // Попытка десериализации данных из JSON
        try {
            Map<String, DayData> data = MAPPER.readValue(path.toFile(), TYPE_REF);

            // Заполнение дефолтными значениями для отсутствующих дней
            data.entrySet().removeIf(entry -> entry.getValue() == null);  // Убираем любые null значения
            System.out.println("Загружены данные: " + data);  // Для логирования
            return data;
        } catch (IOException e) {
            System.out.println("Ошибка при чтении/десериализации файла: " + path.toAbsolutePath());
            throw new IOException("Ошибка чтения или десериализации JSON файла: " + e.getMessage());
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
