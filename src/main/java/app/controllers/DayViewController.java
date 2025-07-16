package app.controllers;
//тест ветки
import app.models.DayData;
import app.models.TimeEntry;
import app.services.JsonService;
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

    private enum DayType {
        WORKDAY("workday"), WEEKEND("weekend");

        private final String type;

        DayType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

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

    public void setDayData(LocalDate date, DayData data, int workHours) {
        this.currentDate = date;
        this.dayData = (data != null) ? data : new DayData();
        this.defaultWorkHours = workHours;

        loadDayDataFromJson();  // Загрузка данных из JSON

        timeEntries = createSortedTimeEntries(); // Создание стандартных временных слотов
        updateUIWithData(); // Обновление UI после загрузки данных
    }

    private void loadDayDataFromJson() {
        try {
            this.allData = JsonService.getData();  // Загружаем все данные из JSON
            if (allData.isEmpty()) {  // Проверяем, не пустой ли объект allData
                allData = new HashMap<>();  // Если данные пустые, инициализируем пустой HashMap
            }
        } catch (IOException e) {
            UIHelper.showError("9Ошибка загрузки данных: " + e.getMessage());  // Обрабатываем ошибку загрузки
            this.allData = new HashMap<>();  // В случае ошибки, инициализируем пустой HashMap
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
        dayTypeCombo.setItems(FXCollections.observableArrayList(DayType.WORKDAY.getType(), DayType.WEEKEND.getType()));
        dayTypeCombo.getSelectionModel().select(DayType.WORKDAY.getType().equals(dayData.getDayType()) ? 0 : 1);
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

    @FXML
    private void previousDay() {
        setDayData(
                currentDate.minusDays(1),
                allData.get(DateHelper.formatDate(currentDate.minusDays(1))),
                defaultWorkHours
        );
    }

    @FXML
    private void nextDay() {
        setDayData(
                currentDate.plusDays(1),
                allData.get(DateHelper.formatDate(currentDate.plusDays(1))),
                defaultWorkHours
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
            dayData.setDayType(dayTypeCombo.getSelectionModel().getSelectedIndex() == 0 ? DayType.WORKDAY.getType() : DayType.WEEKEND.getType());
            dayData.setTasks(timeEntries);
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

                    // Сохраняем данные
                    JsonService.saveData(allData);
                    handleSaveSuccess(); // Обрабатываем успешное сохранение
                } catch (IOException e) {
                    saveProgress.setVisible(false);
                    UIHelper.showError("Error1 load data: " + e.getMessage());
                }
            });
            fakeLoad.play();

        } catch (NumberFormatException e) {
            saveProgress.setVisible(false);
            UIHelper.showError("Некорректное количество рабочих часов.\nВведите число от 1 до 24.");
        }
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
        dayTypeCombo.getSelectionModel().select(0);
        timeEntries.clear();
        setDayData(currentDate, new DayData(), defaultWorkHours);
        markDirty();
    }

    @FXML
    private void backToCalendar() {
        CalendarController ctrl = (CalendarController) mainContainer.getUserData();
        if (ctrl != null) ctrl.showCalendar();
    }
}