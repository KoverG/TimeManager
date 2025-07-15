package app.controllers;

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

        loadDayDataFromJson();

        timeEntries = createSortedTimeEntries();
        updateUIWithData();
    }

    private void loadDayDataFromJson() {
        try {
            this.allData = JsonService.getData();
        } catch (IOException e) {
            UIHelper.showError("Ошибка загрузки данных: " + e.getMessage());
            this.allData = new HashMap<>();
        }
    }

    private TreeMap<String, TimeEntry> createSortedTimeEntries() {
        TreeMap<String, TimeEntry> sortedEntries = new TreeMap<>();
        LocalTime t = LocalTime.of(8, 0), end = LocalTime.of(17, 0);

        // Создаем стандартные временные слоты с шагом 30 минут
        while (!t.isAfter(end)) {
            String key = t.toString();
            TimeEntry entry = new TimeEntry();
            sortedEntries.put(key, entry);
            t = t.plusMinutes(30); // Шаг 30 минут
        }

        // Добавляем стандартные слоты в контейнер timeEntries
        dayData.getTasks().forEach(sortedEntries::putIfAbsent);

        return sortedEntries;
    }

    private void updateUIWithData() {
        dateLabel.setText(DateHelper.formatDisplayDate(currentDate));
        dayTypeCombo.setItems(FXCollections.observableArrayList(DayType.WORKDAY.getType(), DayType.WEEKEND.getType()));
        dayTypeCombo.getSelectionModel().select(DayType.WORKDAY.getType().equals(dayData.getDayType()) ? 0 : 1);
        workHoursField.setText(String.valueOf(dayData.getWorkDayHours() > 0 ? dayData.getWorkDayHours() : defaultWorkHours));

        timeSlotsContainer.getChildren().clear();
        timeSlotsContainer.setStyle("-fx-alignment: CENTER;");
        timeEntries.forEach((timeKey, entry) ->
                timeSlotsContainer.getChildren().add(createTimeSlotRow(timeKey, entry))
        );
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

        ComboBox<Integer> minutesCombo = new ComboBox<>(FXCollections.observableArrayList(0, 10, 20, 30, 40, 50));
        minutesCombo.getStyleClass().add("combo-container-withoutRadius");
        minutesCombo.setStyle("-fx-background-radius:0 12 12 0; -fx-border-radius:0 12 12 0;");
        minutesCombo.setPrefSize(65, 30);

        TextField commentField = new TextField(entry.getComment());
        commentField.setPromptText("Комментарий");
        commentField.getStyleClass().add("custom-combo-mod");
        commentField.setPrefSize(370, 25);

        CheckBox completedCheck = new CheckBox();
        completedCheck.setSelected(entry.isCompleted());
        completedCheck.getStyleClass().add("jira-checkbox");

        row.getChildren().addAll(timeLabel, taskField, hoursCombo, minutesCombo, commentField, completedCheck);

        try {
            Integer hVal = Integer.valueOf(entry.getHours());
            hoursCombo.getSelectionModel().select(hoursCombo.getItems().contains(hVal) ? hVal : 0);
        } catch (Exception ex) {
            hoursCombo.getSelectionModel().select(0);
        }

        try {
            Integer mVal = Integer.valueOf(entry.getMinutes());
            minutesCombo.getSelectionModel().select(
                    minutesCombo.getItems().contains(mVal) ? minutesCombo.getItems().indexOf(mVal) : 0
            );
        } catch (Exception ex) {
            minutesCombo.getSelectionModel().select(0);
        }

        hoursCombo.valueProperty().addListener((o, ov, nv) -> {
            int oldH = ov != null ? ov : 0;
            int oldM = minutesCombo.getValue() != null ? minutesCombo.getValue() : 0;
            entry.setHours(nv != null ? nv.toString() : "0");
            adjustTimeSlotRow(time, oldH, oldM, nv != null ? nv : 0, oldM);
            markDirty();
        });

        minutesCombo.valueProperty().addListener((o, ov, nv) -> {
            int oldH = hoursCombo.getValue() != null ? hoursCombo.getValue() : 0;
            int oldM = ov != null ? ov : 0;
            entry.setMinutes(nv != null ? nv.toString() : "0");
            adjustTimeSlotRow(time, oldH, oldM, oldH, nv != null ? nv : 0);
            markDirty();
        });

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

        return row;
    }

    private void logCurrentTimeSlotPositions() {
        System.out.println("Текущие строки на экране:");
        timeSlotsContainer.getChildren().forEach(node -> {
            if (node instanceof HBox) {
                Label lbl = (Label) ((HBox) node).getChildren().get(0);
                System.out.println("Строка времени: " + lbl.getText());
            }
        });
    }

    private void adjustTimeSlotRow(String baseTime, int oldH, int oldM, int newH, int newM) {
        try {
            LocalTime t0 = LocalTime.parse(baseTime);  // Базовое время
            LocalTime oldTime = t0.plusHours(oldH).plusMinutes(oldM);  // Старое время с добавленными значениями
            LocalTime newTime = t0.plusHours(newH).plusMinutes(newM);  // Новое время с добавленными значениями
            String oldKey = oldTime.toString();
            String newKey = newTime.toString();

            // Логируем изменение строки
            System.out.println("Корректируем строку: старое время - " + oldKey + ", новое время - " + newKey);

            // Удаляем строку, если время стало 0
            if (newH == 0 && newM == 0) {
                if (timeEntries.containsKey(oldKey) && !isStandardTimeSlot(oldKey)) {
                    removeNonStandardTimeSlot(oldKey); // Удаляем строку, если она нестандартная
                }
                return; // Прерываем выполнение
            }

            // Добавляем новую строку, если время нестандартное
            if (!isStandardTimeSlot(newKey)) {
                if (!timeEntries.containsKey(newKey)) {  // Если строки с таким временем еще нет
                    TimeEntry ne = new TimeEntry();
                    addOrUpdateNonStandardTimeSlot(newKey, ne);
                    System.out.println("Добавлена строка времени: " + newKey);  // Логируем добавление
                } else {
                    System.out.println("Строка времени уже существует: " + newKey);  // Логируем существующую строку
                }
            }

            // Удаляем старую строку, если она была изменена на стандартное время
            if ((oldH != 0 || oldM != 0) && !oldKey.equals(baseTime) && !isStandardTimeSlot(oldKey)) {
                removeNonStandardTimeSlot(oldKey);
            }

            // Логируем позиции всех строк после изменений
            logCurrentTimeSlotPositions();

        } catch (Exception e) {
            System.err.println("Ошибка при обновлении слота: " + e.getMessage());
        }
    }



    private void addOrUpdateNonStandardTimeSlot(String timeKey, TimeEntry entry) {
        System.out.println("Пытаемся добавить строку с временем: " + timeKey);

        // Если строка нестандартная (не 8:00, 8:30 и т.д.), добавляем ее или обновляем
        if (!timeEntries.containsKey(timeKey)) { // Если строки с таким временем нет в timeEntries
            timeEntries.put(timeKey, entry);  // Добавляем в timeEntries

            // Вставляем строку в нужную позицию, не в конец
            int idx = 0;
            for (int i = 0; i < timeSlotsContainer.getChildren().size(); i++) {
                Label lbl = (Label) ((HBox) timeSlotsContainer.getChildren().get(i)).getChildren().get(0);
                if (LocalTime.parse(lbl.getText()).isBefore(LocalTime.parse(timeKey))) {
                    idx = i + 1;
                } else {
                    break;
                }
            }

            // Добавляем строку в контейнер в правильную позицию
            timeSlotsContainer.getChildren().add(idx, createTimeSlotRow(timeKey, entry));
            System.out.println("Добавлена нестандартная строка: " + timeKey);
            logCurrentTimeSlotPositions();  // Логируем позиции всех строк
        } else {
            System.out.println("Строка с временем " + timeKey + " уже существует, обновляем.");
        }
    }

    private void removeNonStandardTimeSlot(String timeKey) {
        // Логируем попытку удалить строку
        System.out.println("Пытаемся удалить строку с временем: " + timeKey);

        // Если строка не является стандартной, удаляем ее
        if (!isStandardTimeSlot(timeKey)) {  // Проверка на нестандартное время
            timeEntries.remove(timeKey); // Удаляем из данных
            timeSlotsContainer.getChildren().removeIf(node -> {
                Label lbl = (Label)((HBox) node).getChildren().get(0); // Получаем метку времени
                return lbl.getText().equals(timeKey);  // Удаляем строку, если время совпадает
            });
            System.out.println("Удалена нестандартная строка: " + timeKey); // Логируем успешное удаление
            logCurrentTimeSlotPositions();  // Логируем позиции всех строк
        }
    }

    private boolean isStandardTimeSlot(String timeKey) {
        // Проверка на стандартное время с шагом 30 минут от 8:00 до 17:00
        LocalTime t = LocalTime.of(8, 0), end = LocalTime.of(17, 0);
        while (!t.isAfter(end)) {
            if (t.toString().equals(timeKey)) {
                return true; // Это стандартное время
            }
            t = t.plusMinutes(30); // Шаг 30 минут
        }
        return false; // Не стандартное время
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
            int wh = Integer.parseInt(workHoursField.getText());
            if (wh < 1 || wh > 24) throw new NumberFormatException();

            dayData.setWorkDayHours(wh);
            dayData.setDayType(dayTypeCombo.getSelectionModel().getSelectedIndex() == 0 ? DayType.WORKDAY.getType() : DayType.WEEKEND.getType());
            dayData.setTasks(timeEntries);
            allData.put(DateHelper.formatDate(currentDate), dayData);

            // Имитация задержки для демонстрации индикатора загрузки
            PauseTransition fakeLoad = createFakeLoad();
            fakeLoad.setOnFinished(ev -> {
                try {
                    JsonService.saveData(allData);
                    handleSaveSuccess();
                } catch (IOException e) {
                    saveProgress.setVisible(false);
                    UIHelper.showError("Ошибка сохранения: " + e.getMessage());
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