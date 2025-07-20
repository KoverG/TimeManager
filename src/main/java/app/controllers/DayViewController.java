package app.controllers;


//тест ветки
import app.models.DayData;
import app.models.TimeEntry;
import app.services.JsonService;
import app.services.ProductionCalendarService;
import app.utils.DateHelper;
import app.utils.UIHelper;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.List;
import java.util.ArrayList;
import app.controllers.DayType;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;



public class DayViewController {
    private static final Logger logger = Logger.getLogger(DayViewController.class.getName());  // Создаем логгер

    @FXML
    private Label hoursRemainingLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private ComboBox<String> dayTypeCombo;
    @FXML
    private TextField workHoursField;
    @FXML
    private VBox timeSlotsContainer;
    @FXML
    private VBox mainContainer;
    @FXML
    private Button saveButton;
    @FXML
    private Label saveSuccessIcon; // Галочка
    @FXML
    private ProgressIndicator saveProgress;

    private LocalDate currentDate;
    private DayData dayData;
    private Map<String, DayData> allData;
    private int defaultWorkHours;
    private Map<String, TimeEntry> timeEntries;
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private boolean isClearButtonPressed = false;  // Флаг для отслеживания нажатия кнопки "Очистить"
    private int originalDefaultWorkHours;


    public void setMainContainer(VBox mainContainer) {
        this.mainContainer = mainContainer;
    }

    @FXML
    public void initialize() {
        saveSuccessIcon.setVisible(false); // Галочка скрыта изначально
        saveButton.disableProperty().bind(isDirty.not());
        dayTypeCombo.valueProperty().addListener((o, ov, nv) -> markDirty());
        workHoursField.textProperty().addListener((o, ov, nv) -> markDirty());
        workHoursField.textProperty().addListener((obs, oldValue, newValue) -> updateHoursRemaining());
        dayTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateHoursRemaining());
    }

    private void markDirty() {
        isDirty.set(true);
    }

    private void updateViewForEmptyDay(DayData resetDayData, int workHours, DayType type) {
        // 1) Обновляем модель
        this.dayData = resetDayData;
        // 2) Обновляем контр. UI
        dayTypeCombo.getSelectionModel().select(type.getType());
        workHoursField.setText(String.valueOf(workHours));
        // 3) Пересоздаем слоты по новому dayData
        this.timeEntries = createSortedTimeEntries();
        timeSlotsContainer.getChildren().clear();
        timeEntries.forEach((t, e) -> timeSlotsContainer.getChildren().add(createTimeSlotRow(t, e)));
        // 4) Разблокируем кнопку «Сохранить»
        markDirty();
    }

    // Инициализация dataRef
    private final AtomicReference<Map<String, DayData>> dataRef = new AtomicReference<>(new HashMap<>());

    public void previousDay() {
        logger.info("Attempting to go to the previous day. Current date: " + currentDate);

        LocalDate previousDate = currentDate.minusDays(1);
        logger.info("Getting day type for previous day: " + previousDate);

        DayData previousDayData = getDayDataForDate(previousDate);
        DayType previousDayType = getDayTypeForDate(previousDate);

        // Получаем стандартные рабочие часы для нового типа дня
        int defaultWorkHoursForNewDay = previousDayData.calculateWorkHoursByDayType(previousDayType, 9);

        // Устанавливаем данные для предыдущего дня с правильным типом и рабочими часами
        setDayData(previousDate, previousDayData, defaultWorkHoursForNewDay, previousDayType);
    }

    public void nextDay() {
        logger.info("Attempting to go to the next day. Current date: " + currentDate);

        LocalDate nextDate = currentDate.plusDays(1);
        logger.info("Getting day type for next day: " + nextDate);

        DayData nextDayData = getDayDataForDate(nextDate);
        DayType nextDayType = getDayTypeForDate(nextDate);

        // Получаем стандартные рабочие часы для нового типа дня
        int defaultWorkHoursForNewDay = nextDayData.calculateWorkHoursByDayType(nextDayType, 9);

        // Устанавливаем данные для следующего дня с правильным типом и рабочими часами
        setDayData(nextDate, nextDayData, defaultWorkHoursForNewDay, nextDayType);
    }



    public void setDayData(LocalDate date, DayData data, int workHours, DayType defaultDayType) {
        logger.info("setDayData called for date: " + date);

        this.currentDate = date;
        this.dayData = (data != null) ? data : new DayData();
        this.defaultWorkHours = workHours;
        this.originalDefaultWorkHours = workHours;

        logger.info("Loaded DayData for date: " + date + ", dayType: " + this.dayData.getDayType());
        logger.info("Set work hours: " + workHours);
        logger.info("Set original work hours: " + workHours);

        loadDayDataFromJson();  // Загружаем данные из JSON

        // Если данные по дате пусты, задаем тип дня по умолчанию, полученный из календаря
        if (this.dayData == null || this.dayData.getDayType() == null) {
            this.dayData.setDayType(defaultDayType);  // Используем тип дня из календаря
            logger.info("Day type is null, setting default: " + defaultDayType);
        }

        logger.info("Day type before setting work hours: " + this.dayData.getDayType());

        // Проверяем, если рабочие часы уже установлены в данных, используем их
        logger.info("Work day hours before checking: " + this.dayData.getWorkDayHours());
        if (this.dayData.getWorkDayHours() < 0) {
            // Если в данных нет рабочих часов, вычисляем их в зависимости от типа дня
            logger.info("Calling calculateWorkHoursByDayType for dayType: " + this.dayData.getDayType() + " and defaultWorkHours: " + defaultWorkHours);
            int workHoursForDay = this.dayData.calculateWorkHoursByDayType(this.dayData.getDayType(), defaultWorkHours);
            this.dayData.setWorkDayHours(workHoursForDay);  // Сохраняем вычисленные рабочие часы в объект
            workHoursField.setText(String.valueOf(workHoursForDay));  // Устанавливаем вычисленные рабочие часы в UI
            logger.info("Calculated work hours for day: " + workHoursForDay);
        } else {
            // Используем уже загруженные рабочие часы из данных
            workHoursField.setText(String.valueOf(this.dayData.getWorkDayHours()));
            logger.info("Work hours set from existing data: " + this.dayData.getWorkDayHours());
        }

        // Сохраняем изменения рабочего времени обратно в DayData
        dayData.setWorkDayHours(Integer.parseInt(workHoursField.getText()));  // Сохраняем рабочие часы
        logger.info("Work hours set to: " + workHoursField.getText());

        timeEntries = createSortedTimeEntries(); // Создание стандартных временных слотов
        updateUIWithData(); // Обновление UI после загрузки данных

        logger.info("Day data successfully set for date: " + date);
        logger.info("Work hours field value after UI update: " + workHoursField.getText());

        dayTypeCombo.valueProperty().addListener((obs, oldType, newType) -> {
            logger.info("DayType changed from " + oldType + " to " + newType);

            DayType selectedType = getDayTypeFromString(newType);
            logger.info("Selected DayType: " + selectedType);


            logger.info("Проверка на то что originalDefaultWorkHours == 0: " + originalDefaultWorkHours);
                        // Если originalDefaultWorkHours == 0, установим стандартное значение (например, 9)
            if (originalDefaultWorkHours >= 0) {
                originalDefaultWorkHours = 9;
            }
            logger.info("Проверка что originalDefaultWorkHours поменялся на 9: " + originalDefaultWorkHours);

            // Пересчитываем рабочие часы для нового типа дня
            int recalculatedWorkHours = dayData.calculateWorkHoursByDayType(selectedType, originalDefaultWorkHours);
            logger.info("Recalculated work hours: " + recalculatedWorkHours);

            workHoursField.setText(String.valueOf(recalculatedWorkHours));  // Обновляем поле с рабочими часами
            logger.info("Work hours field updated with recalculated value: " + recalculatedWorkHours);

            // Обновляем тип дня и рабочие часы в dayData
            dayData.setDayType(selectedType);
            dayData.setWorkDayHours(recalculatedWorkHours); // Сохраняем новые рабочие часы

            markDirty();  // Устанавливаем флаг изменения
        });
    }




    private void loadDayDataFromJson() {
        logger.info("Attempting to load data from JSON.");
        try {
            // Загружаем все данные из JSON (в первую очередь смотрим в time_manager_data.json)
            this.allData = JsonService.getData();

            // Если данных нет в time_manager_data.json, пробуем загрузить из calendar_текущий год.json
            if (allData.isEmpty()) {
                logger.warning("allData is empty. No data available in time_manager_data.json.");
                String currentYear = String.valueOf(currentDate.getYear());
                this.allData = JsonService.loadCalendarData(currentYear);  // Загружаем данные из calendar_текущий год.json
            }

            // Логируем полученные данные
            for (Map.Entry<String, DayData> entry : allData.entrySet()) {
                logger.info("Loaded DayData: " + entry.getKey() + " -> " + entry.getValue().getDayType());
            }

        } catch (IOException e) {
            UIHelper.showError("Ошибка загрузки данных: " + e.getMessage());
            this.allData = new HashMap<>();
        }
    }


    private Map<String, DayData> loadCalendarData(String year) {
        // Формируем имя файла calendar_текущий год.json
        String calendarFileName = "data/calendar_" + year + ".json";
        Path calendarPath = Paths.get(calendarFileName);

        // Проверка существования файла
        if (!Files.exists(calendarPath)) {
            logger.warning("File " + calendarPath.toAbsolutePath() + " not found. Returning empty data.");
            return new HashMap<>();  // Если файл не существует, возвращаем пустую карту
        }

        // Проверка, если файл пустой
        try {
            if (Files.size(calendarPath) == 0) {
                logger.warning("Calendar file is empty, creating new: " + calendarPath.toAbsolutePath());
                return new HashMap<>();  // Если файл пустой, возвращаем пустую карту
            }
        } catch (IOException e) {
            logger.severe("Error checking the size of the calendar file: " + calendarPath.toAbsolutePath());
            return new HashMap<>();  // Возвращаем пустую карту, если произошла ошибка
        }

        // Логируем путь к файлу перед загрузкой
        logger.info("Attempting to load data from calendar file: " + calendarPath.toAbsolutePath());

        // Попытка десериализации данных из файла
        try {
            Map<String, DayData> data = JsonService.MAPPER.readValue(calendarPath.toFile(), JsonService.TYPE_REF);

            // Логируем результат десериализации
            if (data == null || data.isEmpty()) {
                logger.warning("The deserialized data from the calendar is empty.");
                return new HashMap<>();  // Если десериализация пустая, возвращаем пустую карту
            }

            // Убираем null значения
            data.entrySet().removeIf(entry -> entry.getValue() == null);
            logger.info("Final data after removing null values from the calendar: " + data);

            return data;
        } catch (IOException e) {
            logger.severe("Error reading or deserializing the calendar file: " + calendarPath.toAbsolutePath());
            return new HashMap<>();  // Возвращаем пустую карту, если произошла ошибка
        }
    }



    private TreeMap<String, TimeEntry> createSortedTimeEntries() {
        TreeMap<String, TimeEntry> sortedEntries = new TreeMap<>();
        LocalTime t = LocalTime.of(8, 0), end = LocalTime.of(17, 0);

        // Создаем стандартные временные слоты с шагом 10 минут
        while (!t.isAfter(end)) {
            String key = t.toString();
            TimeEntry entry = new TimeEntry();  // По умолчанию создаем пустой объект
            sortedEntries.put(key, entry);
            t = t.plusMinutes(10); // Шаг 10 минут
        }

        // Заполняем стандартные слоты данными, если они есть
        if (dayData != null && dayData.getTasks() != null) {
            dayData.getTasks().forEach((timeKey, entry) -> {
                // Обновляем данные для конкретного временного слота
                if (sortedEntries.containsKey(timeKey)) {
                    sortedEntries.put(timeKey, entry);
                }
            });
        }

        return sortedEntries;
    }

    private List<Integer> generateTimeValues() {
        List<Integer> timeValues = new ArrayList<>();
        for (int i = 0; i < 60; i += 10) {  // Шаг всегда 5 минут
            timeValues.add(i);
        }
        return timeValues;
    }

    private void updateUIWithData() {
        logger.info("Updating UI with data...");
        logger.info("Current work hours field value before UI update: " + workHoursField.getText());  // Логируем значение перед обновлением UI

        // Обновляем метку с текущей датой
        dateLabel.setText(DateHelper.formatDisplayDate(currentDate));

        // Получаем тип дня
        DayType dayTypeEnum = dayData.getDayType();  // Это теперь DayType, а не строка
        System.out.println("Loaded DayType for the day: " + dayTypeEnum);

        // Обновляем комбобокс с типами дней, используя DayType
        dayTypeCombo.setItems(FXCollections.observableArrayList(
                DayType.WORKDAY.getType(),  // Рабочий день
                DayType.WEEKEND.getType(),  // Выходной день
                DayType.SHORT.getType()  // Сокращенный день
        ));

// Устанавливаем правильное значение комбобокса в зависимости от типа дня
        if (DayType.WORKDAY.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.WORKDAY.getType());  // Рабочий день
        } else if (DayType.WEEKEND.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.WEEKEND.getType());  // Выходной день
        } else if (DayType.SHORT.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.SHORT.getType());  // Сокращённый день
        } else {
            // Если тип дня не совпадает, ставим выходной по умолчанию
            dayTypeCombo.getSelectionModel().select(DayType.WEEKEND.getType());  // Выходной день
        }

        // Логируем значение рабочих часов из dayData перед установкой
        logger.info("Work day hours from DayData before setting: " + dayData.getWorkDayHours());

        int workHoursToDisplay = dayData.getWorkDayHours() > 0 ? dayData.getWorkDayHours() : defaultWorkHours;
        workHoursField.setText(String.valueOf(workHoursToDisplay));

        logger.info("Work hours field value after setting: " + workHoursField.getText());

        timeSlotsContainer.getChildren().clear();
        timeSlotsContainer.setStyle("-fx-alignment: CENTER;");

        // Добавляем все временные слоты, даже если для них нет данных
        timeEntries.forEach((timeKey, entry) -> timeSlotsContainer.getChildren().add(createTimeSlotRow(timeKey, entry)));

        isDirty.set(false);
        updateHoursRemaining();
    }





    private HBox createTimeSlotRow(String time, TimeEntry entry) {
        HBox row = new HBox(5);
        row.getStyleClass().add("main-banner-day-string");
        row.setMaxWidth(950);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-slot-label");
        timeLabel.setPrefWidth(40);

        TextField taskField = new TextField(entry.getTask());
        taskField.setPromptText("Задача");
        taskField.getStyleClass().add("custom-combo-mod");
        taskField.setPrefSize(320, 25);

        ComboBox<Integer> hoursCombo = new ComboBox<>(FXCollections.observableArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8));
        hoursCombo.getStyleClass().add("combo-container-withoutRadius");
        hoursCombo.setStyle("-fx-background-radius:12 0 0 12; -fx-border-radius:12 0 0 12;");
        hoursCombo.setPrefSize(65, 30);

        ComboBox<Integer> minutesCombo = new ComboBox<>();
        minutesCombo.getStyleClass().add("combo-container-withoutRadius");
        minutesCombo.setStyle("-fx-background-radius:0 12 12 0; -fx-border-radius:0 12 12 0;");
        minutesCombo.setPrefSize(65, 30);

        // Генерация значений для minutesCombo с шагом 5 минут от 0 до 55
        minutesCombo.setItems(FXCollections.observableArrayList(generateTimeValues()));

        TextField commentField = new TextField(entry.getComment());
        commentField.setPromptText("Комментарий");
        commentField.getStyleClass().add("custom-combo-mod");
        commentField.setPrefSize(370, 25);

        CheckBox completedCheck = new CheckBox();
        completedCheck.setSelected(entry.isCompleted());
        completedCheck.getStyleClass().add("jira-checkbox");

        row.getChildren().addAll(timeLabel, taskField, hoursCombo, minutesCombo, commentField, completedCheck);

        // Устанавливаем значения для комбобоксов
        try {
            Integer hVal = Integer.valueOf(entry.getHours());
            hoursCombo.getSelectionModel().select(hoursCombo.getItems().contains(hVal) ? hVal : 0);
        } catch (Exception ex) {
            hoursCombo.getSelectionModel().select(0);  // Если значение не найдено, выберем 0
        }

        try {
            Integer mVal = Integer.valueOf(entry.getMinutes());
            minutesCombo.getSelectionModel().select(
                    minutesCombo.getItems().contains(mVal) ? minutesCombo.getItems().indexOf(mVal) : 0
            );
        } catch (Exception ex) {
            minutesCombo.getSelectionModel().select(0);  // Если значение не найдено, выберем 0
        }

        // Обработчики изменений
        taskField.textProperty().addListener((o, ov, nv) -> {
            entry.setTask(nv);
            markDirty();
        });

        commentField.textProperty().addListener((o, ov, nv) -> {
            entry.setComment(nv);
            markDirty();
        });

        completedCheck.selectedProperty().addListener((o, ov, nv) -> {
            entry.setCompleted(nv);
            markDirty();
        });

        hoursCombo.valueProperty().addListener((o, ov, nv) -> {
            entry.setHours(nv != null ? nv.toString() : "0");
            markDirty();
            updateHoursRemaining();
        });

        minutesCombo.valueProperty().addListener((o, ov, nv) -> {
            entry.setMinutes(nv != null ? nv.toString() : "0");
            markDirty();
            updateHoursRemaining();
        });


        return row;
    }

    private DayType getDayTypeForDate(LocalDate date) {
        // Сначала проверяем, является ли день сокращенным
        if (ProductionCalendarService.isShortDay(date)) {
            return DayType.SHORT;  // Сокращенный рабочий день (даже если он в holidays)
        }
        // Затем проверяем, является ли день праздником/выходным
        if (ProductionCalendarService.isHoliday(date)) {
            return DayType.WEEKEND;  // Праздник — выходной день
        }
        // Если не сокращенный и не праздник, проверяем субботу/воскресенье
        if (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;  // Выходной
        }
        // В остальных случаях — рабочий день
        return DayType.WORKDAY;
    }

    private DayData getDayDataForDate(LocalDate date) {
        logger.info("Searching for DayData with dateKey: " + date);
        // Преобразуем дату в строку для поиска в allData
        String dateKey = DateHelper.formatDate(date);

        // Логируем ключ для поиска
        logger.info("Searching for DayData with dateKey: " + dateKey);

        // Проверяем, есть ли данные для этой даты в allData
        DayData data = allData.get(dateKey);

        if (data == null) {
            // Логируем, если данных для данной даты нет
            logger.warning("No DayData found for date: " + date + ", creating new DayData.");

            // Проверяем тип дня из календаря (если он не найден, устанавливаем тип по умолчанию)
            DayType dayType = getDayTypeForDate(date);

            // Создаем новый объект DayData и задаем правильный тип дня
            data = new DayData();
            data.setDayType(dayType);
        }

        return data;
    }



    @FXML
    private void saveDay() {
        // Показываем индикатор загрузки, скрываем галочку
        saveProgress.setVisible(true);
        saveSuccessIcon.setVisible(false);
        saveProgress.setStyle("-fx-progress-color: #FF3C00;");

        try {
            // Проверяем и парсим количество рабочих часов
            int wh = Integer.parseInt(workHoursField.getText());
            if (wh < 0 || wh > 24) throw new NumberFormatException();

            // Устанавливаем данные
            dayData.setWorkDayHours(wh);

            // Получаем выбранный тип дня из комбобокса и безопасно преобразуем его в DayType
            String selectedDayTypeString = dayTypeCombo.getSelectionModel().getSelectedItem();
            System.out.println("Selected DayType from ComboBox before save: " + selectedDayTypeString);  // Логируем выбранный тип дня из комбобокса

            DayType selectedDayType = getDayTypeFromString(selectedDayTypeString);
            System.out.println("Converted DayType: " + selectedDayType);  // Логируем, какой DayType был преобразован

            // Логируем значение типа дня перед сохранением
            System.out.println("Setting DayType in DayData: " + selectedDayType);
            dayData.setDayType(selectedDayType);  // Устанавливаем DayType

            // Логируем день после установки типа
            System.out.println("DayType after setting in dayData: " + dayData.getDayType());

            // Устанавливаем задачи
            dayData.setTasks(timeEntries);

            // Обновляем данные в allData
            allData.put(DateHelper.formatDate(currentDate), dayData);

            // Имитация задержки для демонстрации индикатора загрузки
            PauseTransition fakeLoad = createFakeLoad();
            fakeLoad.setOnFinished(ev -> {
                try {
                    // Прежде чем сохранить, убедимся, что данные корректны
                    if (allData.isEmpty()) {
                        saveProgress.setVisible(false);
                        UIHelper.showError("Нет данных для сохранения.");
                        return;
                    }

                    // Логируем данные перед сохранением
                    for (Map.Entry<String, DayData> entry : allData.entrySet()) {
                        System.out.println("DayData before save: " + entry.getKey() + " -> " + entry.getValue().getDayType());
                    }

                    if (isClearButtonPressed) {
                        // Очищаем данные в файле
                        allData.clear();
                        JsonService.saveData(allData);  // Сохраняем пустую карту в файл
                        isClearButtonPressed = false;  // Сбрасываем флаг
                        logger.info("time_manager_data.json очищен.");
                    } else {
                        // Если кнопка "Очистить" не была нажата, просто сохраняем данные
                        JsonService.saveData(allData);
                    }


                    handleSaveSuccess(); // Обрабатываем успешное сохранение
                } catch (IOException e) {
                    saveProgress.setVisible(false);
                    UIHelper.showError("Ошибка при сохранении данных: " + e.getMessage());
                }
            });
            fakeLoad.play();

        } catch (NumberFormatException e) {
            saveProgress.setVisible(false);
            UIHelper.showError("Некорректное количество рабочих часов.\nВведите число от 1 до 24.");
        }
    }



    // Метод для безопасного преобразования строки в DayType
    private DayType getDayTypeFromString(String dayTypeString) {
        if (dayTypeString == null) {
            return DayType.WORKDAY;  // По умолчанию рабочий день
        }

        // Убираем возможные пробелы и приводим к нужному регистру
        return DayType.fromString(dayTypeString.trim());
    }



    private void handleSaveSuccess() {
        saveProgress.setVisible(false);
        saveSuccessIcon.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), saveSuccessIcon);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(evt -> saveSuccessIcon.setVisible(false));
            pause.play();
        });
        fadeIn.play();

        isDirty.set(false);
    }

    private PauseTransition createFakeLoad() {
        return new PauseTransition(Duration.millis(500));
    }

    @FXML
    private void clearDay() {
        logger.info("Нажата кнопка 'Очистить' для даты: " + currentDate);

        // 1. Создаем абсолютно новый DayData
        DayData reset = new DayData();

        // 2. Вычисляем тип дня и часы чисто по календарю
        DayType t = getDayTypeForDate(currentDate);
        int hrs = reset.calculateWorkHoursByDayType(t, originalDefaultWorkHours);
        logger.info("Часы для UI после очистки: " + hrs);

        // 3. Обновляем только UI (не трогая defaultWorkHours и слушатели)
        updateViewForEmptyDay(reset, hrs, t);
        logger.info("Интерфейс для пустого дня обновлен");

        // 4. Ставим флаг, чтобы saveDay() почистил JSON
        isClearButtonPressed = true;
        logger.info("isClearButtonPressed = true");
    }



    // Новый метод для форматирования времени
    private String formatTime(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    // Добавьте новый метод для обновления оставшихся часов
    private void updateHoursRemaining() {
        try {
            int totalWorkMinutes = Integer.parseInt(workHoursField.getText()) * 60;
            int enteredMinutes = calculateEnteredMinutes();
            int remaining = Math.max(0, totalWorkMinutes - enteredMinutes);

            hoursRemainingLabel.setText("Осталось времени: " + formatTime(remaining));
        } catch (NumberFormatException e) {
            hoursRemainingLabel.setText("Осталось времени: --:--");
        }
    }

    // Метод для расчета введенных часов
    private int calculateEnteredMinutes() {
        int total = 0;
        if (timeEntries != null) {
            for (TimeEntry entry : timeEntries.values()) {
                try {
                    int hours = Integer.parseInt(entry.getHours());
                    int minutes = Integer.parseInt(entry.getMinutes());
                    total += hours * 60 + minutes;
                } catch (NumberFormatException e) {
                    // Игнорируем невалидные значения
                }
            }
        }
        return total;
    }


    @FXML
    private void backToCalendar() {
        CalendarController ctrl = (CalendarController) mainContainer.getUserData();
        if (ctrl != null) ctrl.showCalendar();
    }
}