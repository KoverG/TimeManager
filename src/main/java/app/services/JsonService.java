package app.services;

import app.models.DayData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class JsonService {
    private static final String DATA_FILE = "data/time_manager_data.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public static synchronized Map<String, DayData> loadData() throws IOException {
        Path path = Paths.get(DATA_FILE);
        if (!Files.exists(path)) {
            System.out.println("Файл данных не существует, создается новый");
            return new HashMap<>();
        }
        return mapper.readValue(path.toFile(), new TypeReference<Map<String, DayData>>() {});
    }

    public static synchronized void saveData(Map<String, DayData> data) throws IOException {
        Path path = Paths.get(DATA_FILE);
        Files.createDirectories(path.getParent());

        File file = path.toFile();
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Не удалось создать файл: " + path);
            }
        }

        mapper.writeValue(file, data);
        System.out.println("Данные успешно сохранены в: " + path.toAbsolutePath());
    }

    public static synchronized Map<String, DayData> getData() {
        try {
            return loadData();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки данных: " + e.getMessage());
            return new HashMap<>();
        }
    }
}
