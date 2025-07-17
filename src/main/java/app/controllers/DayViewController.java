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

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DayViewController {
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

    public void setMainContainer(VBox mainContainer) {
        this.mainContainer = mainContainer;
    }

    @FXML
    public void initialize() {
        saveSuccessIcon.setVisible(false); // Галочка скрыта изначально
        saveButton.disableProperty().bind(isDirty.not());
        dayTypeCombo.valueProperty().addListener((o, ov, nv) -> markDirty());
        workHoursField.textProperty().addListener((o, ov, nv) -> markDirty());
    }

    private void markDirty() {
        isDirty.set(true);
    }

    public void setDayData(LocalDate date, DayData data, int workHours, DayType defaultDayType) {
        this.currentDate = date;
        this.dayData = (data != null) ? data : new DayData();  // Если данных нет, создаем новый объект
        this.defaultWorkHours = workHours;

        loadDayDataFromJson();  // Загружаем данные из JSON

        // Если данные по дате пусты, задаем тип дня по умолчанию, полученный из календаря
        if (this.dayData == null || this.dayData.getDayType() == null) {
            this.dayData.setDayType(defaultDayType);  // Используем тип дня из календаря
        }

        timeEntries = createSortedTimeEntries(); // Создание стандартных временных слотов
        updateUIWithData(); // Обновление UI после загрузки данных
    }

    private void loadDayDataFromJson() {
        try {
            this.allData = JsonService.getData();  // Загружаем все данные из JSON
            if (allData.isEmpty()) {  // Проверяем, не пустой ли объект allData
                allData = new HashMap<>();  // Если данные пустые, инициализируем пустой HashMap
            }

            // Логируем полученные данные
            for (Map.Entry<String, DayData> entry : allData.entrySet()) {
                System.out.println("Loaded DayData: " + entry.getKey() + " -> " + entry.getValue().getDayType());
            }

        } catch (IOException e) {
            UIHelper.showError("Ошибка загрузки данных: " + e.getMessage());
            this.allData = new HashMap<>();
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
        dateLabel.setText(DateHelper.formatDisplayDate(currentDate));

        // Получаем тип дня
        DayType dayTypeEnum = dayData.getDayType();  // Это теперь DayType, а не строка
        System.out.println("Loaded DayType for the day: " + dayTypeEnum);

        // Обновляем комбобокс с типами дней, используя DayType
        dayTypeCombo.setItems(FXCollections.observableArrayList(
                DayType.WORKDAY.getType(),  // Рабочий день
                DayType.WEEKEND.getType()   // Выходной день
        ));

        // Устанавливаем правильное значение комбобокса в зависимости от типа дня
        if (DayType.WORKDAY.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.WORKDAY.getType());  // Рабочий день
        } else if (DayType.WEEKEND.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.WEEKEND.getType());  // Выходной день
        } else {
            // Если тип дня не совпадает, ставим выходной по умолчанию
            dayTypeCombo.getSelectionModel().select(DayType.WEEKEND.getType());  // Выходной день
        }

        workHoursField.setText(String.valueOf(dayData.getWorkDayHours() > 0 ? dayData.getWorkDayHours() : defaultWorkHours));

        timeSlotsContainer.getChildren().clear();
        timeSlotsContainer.setStyle("-fx-alignment: CENTER;");

        // Добавляем все временные слоты, даже если для них нет данных
        timeEntries.forEach((timeKey, entry) -> timeSlotsContainer.getChildren().add(createTimeSlotRow(timeKey, entry)));

        isDirty.set(false);
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
        });

        minutesCombo.valueProperty().addListener((o, ov, nv) -> {
            entry.setMinutes(nv != null ? nv.toString() : "0");
            markDirty();
        });

        return row;
    }

    private DayType getDayTypeForDate(LocalDate date) {
        if (ProductionCalendarService.isHoliday(date)) {
            return DayType.WEEKEND;  // Праздник — выходной день
        }
        if (ProductionCalendarService.isShortDay(date)) {
            return DayType.WORKDAY;  // Сокращённый день — рабочий
        }
        if (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;  // Суббота и воскресенье — выходной
        }
        return DayType.WORKDAY;  // По умолчанию рабочий день
    }

    @FXML
    private void previousDay() {
        DayType dayType = getDayTypeForDate(currentDate.minusDays(1)); //Получаем тип дня для предыдущего дня
        setDayData(
                currentDate.minusDays(1),
                allData.get(DateHelper.formatDate(currentDate.minusDays(1))),
                defaultWorkHours,
                dayType  // Передаем тип дня
        );
    }

    @FXML
    private void nextDay() {
        DayType dayType = getDayTypeForDate(currentDate.minusDays(1));  // Получаем тип дня для следующего дня
        setDayData(
                currentDate.plusDays(1),
                allData.get(DateHelper.formatDate(currentDate.plusDays(1))),
                defaultWorkHours,
                dayType  // Передаем тип дня
        );
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
            if (wh < 1 || wh > 24) throw new NumberFormatException();

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

                    // Сохраняем данные
                    JsonService.saveData(allData);
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
        workHoursField.setText(String.valueOf(defaultWorkHours));
        dayTypeCombo.getSelectionModel().select(0);  // Выбираем первый элемент в ComboBox
        timeEntries.clear();

        // Передаем тип дня по умолчанию (например, DayType.WORKDAY)
        setDayData(currentDate, new DayData(), defaultWorkHours, DayType.WORKDAY);

        markDirty();
    }

    @FXML
    private void backToCalendar() {
        CalendarController ctrl = (CalendarController) mainContainer.getUserData();
        if (ctrl != null) ctrl.showCalendar();
    }
}