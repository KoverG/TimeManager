package app.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import app.controllers.DayType;  // Импортируем DayType из контроллера
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger; // Импортируем Logger

@JsonIgnoreProperties(ignoreUnknown = true)
public class DayData {
    private static final Logger logger = Logger.getLogger(DayData.class.getName());  // Логгер для класса
    private int workDayHours = 9;  // Значение по умолчанию для рабочих часов
    private DayType dayType = DayType.WORKDAY;  // Тип дня (по умолчанию рабочий)
    private Map<String, TimeEntry> tasks = new HashMap<>();  // Список задач для дня

    public DayData() {
        this.workDayHours = 0;  // Устанавливаем начальное значение для работы с пустыми данными
        logger.info("90DayData object created with workDayHours: " + this.workDayHours);
    }



    // Метод для получения рабочих часов дня с учетом типа дня
    public int getWorkDayHours(int defaultWorkHours) {
        logger.info("getWorkDayHours called with defaultWorkHours: " + defaultWorkHours);

        // Если рабочие часы уже заданы и больше 0, возвращаем их
        if (this.workDayHours > 0) {
            logger.info("78Work day hours are already set: " + this.workDayHours);
            return this.workDayHours;
        }

        // Если рабочие часы не заданы или равны 0, вычисляем их в зависимости от типа дня
        logger.info("Work day hours are not set, calculating based on day type...");
        int calculatedWorkHours = calculateWorkHoursByDayType(this.dayType, defaultWorkHours);
        logger.info("Calculated work day hours: " + calculatedWorkHours);

        // Устанавливаем вычисленные рабочие часы и возвращаем их
        this.workDayHours = calculatedWorkHours;
        return calculatedWorkHours;
    }


    // Метод для вычисления рабочих часов в зависимости от типа дня
    public int calculateWorkHoursByDayType(DayType dayType, int defaultWorkHours) {
        logger.info("calculateWorkHoursByDayType called with dayType: " + dayType + " and defaultWorkHours: " + defaultWorkHours);

        switch (dayType) {
            case SHORT:
                logger.info("Day is SHORT. Reducing hours by 1.");
                return defaultWorkHours - 1;  // Сокращённый рабочий день на 1 час меньше
            case WEEKEND:
                logger.info("Day is WEEKEND. Setting work hours to 0.");
                return 0;  // Для выходных дней рабочие часы равны 0
            case WORKDAY:
            default:
                logger.info("Day is WORKDAY. Setting work hours to default: " + defaultWorkHours);
                return defaultWorkHours;  // Для рабочих дней по умолчанию 9 часов
        }
    }

    // Метод для получения прогресса (используется для вычисления завершенности задач)
    public double getProgress(int defaultWorkHours) {
        logger.info("getProgress called with defaultWorkHours: " + defaultWorkHours);

        int totalMinutes = tasks.values().stream()
                .mapToInt(TimeEntry::getTotalMinutes)
                .sum();
        int workHours = workDayHours > 0 ? workDayHours : defaultWorkHours;

        double progress = Math.min((double) totalMinutes / (workHours * 60), 1.0);
        if (workHours == 0) return 0.0;
        logger.info("Calculated progress: " + progress);
        return progress;
    }

    // Геттеры и сеттеры
    public int getWorkDayHours() {
        logger.info("getWorkDayHours called. Returning: " + this.workDayHours);
        return workDayHours;
    }

    public void setWorkDayHours(int workDayHours) {
        logger.info("setWorkDayHours called with value: " + workDayHours);
        if (this.workDayHours != workDayHours) {
            logger.info("Changing workDayHours from " + this.workDayHours + " to " + workDayHours);
            this.workDayHours = workDayHours;  // Устанавливаем новое значение рабочих часов
        } else {
            logger.info("Work day hours are already set to: " + workDayHours);  // Если значение не изменилось
        }
    }

    public DayType getDayType() {
        logger.info("getDayType called. Returning: " + this.dayType);
        return dayType;  // Возвращаем DayType
    }

    public void setDayType(DayType dayType) {
        logger.info("setDayType called with value: " + dayType);
        this.dayType = dayType;  // Устанавливаем тип дня
    }

    public Map<String, TimeEntry> getTasks() {
        logger.info("getTasks called. Returning: " + tasks);
        return tasks;
    }

    public void setTasks(Map<String, TimeEntry> tasks) {
        logger.info("setTasks called with tasks: " + tasks);
        this.tasks = tasks;
    }

    // Переопределяем метод toString()
    @Override
    public String toString() {
        return "DayData{" +
                "workDayHours=" + workDayHours +
                ", dayType='" + dayType + '\'' +
                ", tasks=" + tasks +
                '}';
    }

    // Метод для получения типа дня для очистки
    public DayType getClearDayType() {
        logger.info("getClearDayType called.");

        // Проверяем текущий тип дня (если он не установлен, то берем дефолтный тип)
        if (this.dayType != null) {
            return this.dayType;
        }

        // Если типа дня нет, то возвращаем значение по умолчанию
        return DayType.WORKDAY;  // Если данных нет, возвращаем WORKDAY
    }

    // Метод для получения рабочих часов для очистки
    public int getClearWorkHours(int defaultWorkHours) {
        logger.info("getClearWorkHours called with defaultWorkHours: " + defaultWorkHours);

        // Если рабочие часы уже установлены (больше 0), возвращаем их
        if (this.workDayHours > 0) {
            logger.info("Work day hours are already set: " + this.workDayHours);
            return this.workDayHours;
        }

        // Если рабочих часов нет (или они равны 0), то вычисляем их на основе типа дня
        int calculatedWorkHours = calculateWorkHoursByDayType(getClearDayType(), defaultWorkHours);
        logger.info("Calculated work day hours: " + calculatedWorkHours);
        return calculatedWorkHours;
    }



}
